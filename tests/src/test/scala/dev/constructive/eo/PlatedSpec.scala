package dev.constructive.eo

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.{Lens, Plated, Prism}
import dev.constructive.eo.optics.Optic.* // .andThen / .modify
import dev.constructive.eo.optics.Plated.* // .transformAll / .universeOf extensions
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

  // ----- Behaviour: the cookbook example, uppercase every Var occurrence -----

  "transform uppercases every Var occurrence and leaves the structure (and Lam binders) intact" >> {
    val shout: Expr => Expr = {
      case Expr.Var(n) => Expr.Var(n.toUpperCase)
      case other       => other
    }
    val tree = Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))
    // f -> F, the bound occurrence y -> Y, but the Lam binder "y" stays lowercase.
    Plated.transform(shout)(tree) ==
      Expr.App(Expr.Var("F"), Expr.Lam("y", Expr.Var("Y")))
  }

  // ----- Composition: `plate` is a first-class optic — it `.andThen`s a Prism + Lens -----

  "plate composes with a Prism + Lens via andThen (one level of children)" >> {
    val varP = Prism[Expr, Expr.Var](
      {
        case v: Expr.Var => Right(v)
        case other       => Left(other)
      },
      identity,
    )
    val nameL = Lens[Expr.Var, String](_.name, (v, n) => v.copy(name = n))

    // plate (immediate children) → Prism (those that are Var) → Lens (their name).
    // The composed value is itself an Optic, so `.modify` works on the whole chain.
    val immediateVarNames = Plated[Expr].plate.andThen(varP).andThen(nameL)

    val tree = Expr.App(Expr.Var("f"), Expr.Lam("y", Expr.Var("y")))
    // App's immediate children are Var("f") and the Lam; only Var("f") is hit (-> "F").
    // The Lam, and the nested Var("y") under it, are deeper than one level, so untouched.
    // (For *every* variable in the tree, iterate plate via `transform` / `universe` above.)
    immediateVarNames.modify(_.toUpperCase)(tree) ==
      Expr.App(Expr.Var("F"), Expr.Lam("y", Expr.Var("y")))
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
    var deep: Bin = Bin.Leaf(0)
    var i = 0
    while i < n do
      deep = Bin.Node(deep, Bin.Leaf(i))
      i += 1

    // Increment every leaf; must not blow the stack.
    val bumped = binPlate.transformAll {
      case Bin.Leaf(v) => Bin.Leaf(v + 1)
      case node        => node
    }(deep)

    // node count = 1 (initial leaf) + 2 per iteration; universe must enumerate them all.
    binPlate.universeOf(deep).length == (2 * n + 1) &&
    binPlate.universeOf(bumped).length == (2 * n + 1)
  }
