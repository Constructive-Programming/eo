package dev.constructive.eo

import scala.annotation.tailrec

import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.Optic.* // .andThen / .modify
import dev.constructive.eo.optics.Plated.* // .transformAll / .universeOf extensions
import dev.constructive.eo.optics.{Lens, Plated, Prism}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import laws.PlatedLaws
import laws.discipline.PlatedTests

// Top-level fixtures: the `plate[S]` macro emits `new V(...)`, which loses its
// outer-accessor wiring if the ADT is nested in the spec class. Hoisted here.
enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

enum Bin:
  case Leaf(value: Int)
  case Node(left: Bin, right: Bin)

object PlatedFixtures:

  // Macro-derived self-traversals — exercised by the discipline laws below.
  given platedExpr: Plated[Expr] = plate[Expr]
  given platedBin: Plated[Bin] = plate[Bin]

  private def genExpr(depth: Int): Gen[Expr] =
    val leaf = Gen.alphaLowerStr.map(s => Expr.Var(if s.isEmpty then "x" else s))
    if depth <= 0 then leaf
    else
      Gen.frequency(
        3 -> leaf,
        1 -> Gen.lzy(for
          f <- genExpr(depth / 2)
          x <- genExpr(depth / 2)
        yield Expr.App(f, x)),
        1 -> Gen.lzy(for
          b <- Gen.alphaLowerStr
          body <- genExpr(depth - 1)
        yield Expr.Lam(if b.isEmpty then "x" else b, body)),
      )

  given Arbitrary[Expr] = Arbitrary(Gen.sized(d => genExpr(math.min(d, 6))))

  private def genBin(depth: Int): Gen[Bin] =
    val leaf = Gen.choose(0, 100).map(Bin.Leaf(_))
    if depth <= 0 then leaf
    else
      Gen.frequency(
        2 -> leaf,
        1 -> Gen.lzy(for
          l <- genBin(depth / 2)
          r <- genBin(depth / 2)
        yield Bin.Node(l, r)),
      )

  given Arbitrary[Bin] = Arbitrary(Gen.sized(d => genBin(math.min(d, 8))))

/** `Plated` recursion: laws on the macro-derived self-traversals, plus the two behaviours that
  * aren't unconditional laws — `rewrite` reaching a fixpoint, and stack safety on a deep tree.
  */
class PlatedSpec extends Specification with Discipline:

  import PlatedFixtures.given

  // ----- Discipline laws on the *derived* plates (macro correctness lives here) -----

  checkAll(
    "Plated[Expr] (macro-derived)",
    new PlatedTests[Expr]:
      val laws = new PlatedLaws[Expr] {}
    .plated,
  )

  checkAll(
    "Plated[Bin] (macro-derived)",
    new PlatedTests[Bin]:
      val laws = new PlatedLaws[Bin] {}
    .plated,
  )

  // ----- Behaviour + composition: transform / plate∘Prism∘Lens / everywhere∘Prism∘Lens -----
  //
  // 2026-06-12 consolidation: 3 blocks → 1. The three blocks shared the same tree and
  // duplicated the Var-prism + name-lens fixtures; all three assertions are kept verbatim.

  // covers: transform uppercases every Var and leaves structure + Lam binders intact;
  //   plate.andThen(Prism).andThen(Lens) rewrites IMMEDIATE children only (one level);
  //   everywhere.andThen(Prism).andThen(Lens) rewrites ALL variables at every depth
  //   (everywhere.modify(h) == transform(h), the composition law applied at each node).
  "transform / plate∘Prism∘Lens (one level) / everywhere∘Prism∘Lens (all depths) semantics" >> {
    val varP = Prism[Expr, Expr.Var](
      {
        case v: Expr.Var => Right(v)
        case other       => Left(other)
      },
      identity,
    )
    val nameL = Lens[Expr.Var, String](_.name, (v, n) => v.copy(name = n))
    val tree = Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))

    // f -> F, the bound occurrence y -> Y, but the Lam binder "y" stays lowercase.
    val shout: Expr => Expr = {
      case Expr.Var(n) => Expr.Var(n.toUpperCase)
      case other       => other
    }
    val transformOk = Plated.transform(shout)(tree) ==
      Expr.App(Expr.Var("F"), Expr.Lam("y", Expr.Var("Y")))

    // plate (immediate children) → Prism (those that are Var) → Lens (their name):
    // only Var("f") is hit; the Lam and the nested Var("y") under it stay untouched.
    val immediateVarNames = Plated[Expr].plate.andThen(varP).andThen(nameL)
    val oneLevelOk = immediateVarNames.modify(_.toUpperCase)(tree) ==
      Expr.App(Expr.Var("F"), Expr.Lam("y", Expr.Var("y")))

    // everywhere: EVERY variable at every depth (Lam binder is a String, not a Var node).
    val allVarNames = Plated.everywhere[Expr].andThen(varP).andThen(nameL)
    val allDepthsOk = allVarNames.modify(_.toUpperCase)(tree) ==
      Expr.App(Expr.Var("F"), Expr.Lam("y", Expr.Var("Y")))

    transformOk && oneLevelOk && allDepthsOk
  }

  // ----- Behaviour: rewrite re-fires to a fixpoint (bottom-up constant fold) -----

  "rewrite folds Node(Leaf,Leaf) repeatedly until a single total remains" >> {
    val foldAdjacent: Bin => Option[Bin] = {
      case Bin.Node(Bin.Leaf(a), Bin.Leaf(b)) => Some(Bin.Leaf(a + b))
      case _                                  => None
    }
    val tree = Bin.Node(Bin.Node(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))
    Plated.rewrite(foldAdjacent)(tree) == Bin.Leaf(6)
  }

  // ----- Behaviour: stack safety via the hand-built selfChildren + extension API -----

  "transform / universe are stack-safe on a 100k-deep tree (hand-built plate)" >> {
    // A hand-built plate via the List-shaped `Plated.fromChildren` convenience — no macro,
    // exercises the core constructor and the `.transformAll` / `.universeOf` extension surface.
    val binPlate = Plated
      .fromChildren[Bin](
        {
          case Bin.Node(l, r) => List(l, r)
          case Bin.Leaf(_)    => Nil
        },
        {
          case (Bin.Node(_, _), l :: r :: Nil) => Bin.Node(l, r)
          case (leaf, _)                       => leaf
        },
      )
      .plate

    val n = 100000
    @tailrec def spine(deep: Bin, i: Int): Bin =
      if i < n then spine(Bin.Node(deep, Bin.Leaf(i)), i + 1) else deep
    val deep = spine(Bin.Leaf(0), 0)

    // Increment every leaf; must not blow the stack.
    val bumped = binPlate.transformAll {
      case Bin.Leaf(v) => Bin.Leaf(v + 1)
      case node        => node
    }(deep)

    // Exercise the remaining extension-API methods (`childrenOf`, `rewriteAll`) on a small tree.
    val small = Bin.Node(Bin.Node(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))
    val foldAdjacent: Bin => Option[Bin] = {
      case Bin.Node(Bin.Leaf(a), Bin.Leaf(b)) => Some(Bin.Leaf(a + b))
      case _                                  => None
    }
    val extrasOk =
      binPlate.childrenOf(small).length == 2 &&
        binPlate.rewriteAll(foldAdjacent)(small) == Bin.Leaf(6)

    // node count = 1 (initial leaf) + 2 per iteration; universe must enumerate them all.
    binPlate.universeOf(deep).length == (2 * n + 1) &&
    binPlate.universeOf(bumped).length == (2 * n + 1) &&
    extrasOk
  }

  "rewrite is stack-safe on both axes: a 100k-deep descent AND a 200k-long re-fire chain" >> {
    // `rewrite` trampolines through `cats.Eval`, so neither the bottom-up *descent* over a deep
    // tree nor the *fixpoint re-firing* at a single node runs on the JVM call stack. Both of the
    // shapes below would overflow a naive call-stack implementation; both must complete here.
    val binPlate = Plated
      .fromChildren[Bin](
        {
          case Bin.Node(l, r) => List(l, r)
          case Bin.Leaf(_)    => Nil
        },
        {
          case (Bin.Node(_, _), l :: r :: Nil) => Bin.Node(l, r)
          case (leaf, _)                       => leaf
        },
      )

    // Axis 1 — deep descent: a 100k-deep spine, folding Node(Leaf, Leaf) at every node up to a
    // single Leaf. Each fire re-processes only a shallow result, so this stresses the descent.
    val n = 100000
    @tailrec def spine(deep: Bin, i: Int): Bin =
      if i <= n then spine(Bin.Node(deep, Bin.Leaf(i)), i + 1) else deep
    val deep = spine(Bin.Leaf(0), 1)
    val foldAdjacent: Bin => Option[Bin] = {
      case Bin.Node(Bin.Leaf(a), Bin.Leaf(b)) => Some(Bin.Leaf(a + b))
      case _                                  => None
    }
    val descentOk = Plated.rewrite(foldAdjacent)(deep)(using binPlate) match
      case Bin.Leaf(_) => true
      case _           => false

    // Axis 2 — deep re-fire: a single Leaf whose rule re-fires 200k times in a chain (decrement
    // to zero). This is the case that overflows a call-stack re-fire; the Eval trampoline clears it.
    val decrement: Bin => Option[Bin] = {
      case Bin.Leaf(k) if k > 0 => Some(Bin.Leaf(k - 1))
      case _                    => None
    }
    val refireOk = Plated.rewrite(decrement)(Bin.Leaf(200000))(using binPlate) == Bin.Leaf(0)

    descentOk && refireOk
  }

  "transform rewrites the FULL depth of a spine that crosses the machine handoff" >> {
    // covers: Plated.transformMachine internals (enter's leaf check at line 146, the
    // drain loop at line 149). The 100k stack-safety test above reaches the machine but
    // only asserts shallow shape, so a machine that returns its subtree UNCHANGED
    // (drain loop skipped) survived. This pins the deepest leaf and the spine length
    // after a transform that crosses transformRecursionLimit (512).
    val depth = 1500
    var deep: Bin = Bin.Leaf(0)
    var i = 0
    while i < depth do
      deep = Bin.Node(deep, Bin.Leaf(1))
      i += 1
    val bump: Bin => Bin = { case Bin.Leaf(k) => Bin.Leaf(k + 1); case n => n }
    val bumped = Plated.transform(bump)(deep)(using PlatedFixtures.platedBin)
    def leftmost(b: Bin, d: Int): (Int, Int) = b match
      case Bin.Node(l, _) => leftmost(l, d + 1)
      case Bin.Leaf(k)    => (d, k)
    val topRight = bumped match
      case Bin.Node(_, Bin.Leaf(k)) => k
      case _                        => -1
    (leftmost(bumped, 0), topRight) must beEqualTo(((depth, 1), 2))
  }
