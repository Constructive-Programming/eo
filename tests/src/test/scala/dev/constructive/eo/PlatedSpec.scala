package dev.constructive.eo

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq

import cats.Eval
import cats.data.{Chain, State}
import dev.constructive.eo.generics.{lens, plate, prism}
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

// Container-bearing recursive ADT: Option + List + ArraySeq + Chain (via Traverse[Chain]).
final case class RoseNode(
    value: Int,
    optChild: Option[RoseNode],
    listChildren: List[RoseNode],
    arrChildren: ArraySeq[RoseNode],
    chainChildren: Chain[RoseNode],
)

// O'Connor's (2011, §1 / §5.1) mutually recursive statement/expression AST.
final case class AstVar(name: String)

enum AstTyp:
  case TInt, TFloat

enum AstStm:
  case SDecl(t: AstTyp, v: AstVar)
  case SAss(v: AstVar, e: AstExpr)
  case SBlock(ss: List[AstStm])
  case SReturn(e: AstExpr)

enum AstExpr:
  case EStm(s: AstStm)
  case EAdd(e1: AstExpr, e2: AstExpr)
  case EVar(v: AstVar)
  case EInt(i: Int)

type AstLang = AstStm | AstExpr | AstVar | AstTyp

object PlatedFixtures:

  // Macro-derived self-traversals — exercised by the discipline laws below.
  given platedExpr: Plated[Expr] = plate[Expr]
  given platedBin: Plated[Bin] = plate[Bin]
  given platedRose: Plated[RoseNode] = plate[RoseNode]
  given platedLang: Plated[AstLang] = plate[AstLang]

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

  private def genRose(depth: Int): Gen[RoseNode] =
    val leaf = Gen.choose(-50, 50).map(RoseNode(_, None, Nil, ArraySeq.empty, Chain.empty))
    if depth <= 0 then leaf
    else
      Gen.frequency(
        2 -> leaf,
        1 -> Gen.lzy(
          for
            v <- Gen.choose(-50, 50)
            opt <- Gen.option(genRose(depth - 1))
            lst <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, genRose(depth - 1)))
            arr <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, genRose(depth - 1))).map(ArraySeq.from)
            ch <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, genRose(depth - 1))).map(Chain.fromSeq)
          yield RoseNode(v, opt, lst, arr, ch)
        ),
      )

  given Arbitrary[RoseNode] = Arbitrary(Gen.sized(d => genRose(math.min(d, 3))))

  private val genAstVar: Gen[AstVar] =
    Gen.oneOf("x", "y", "z", "count").map(AstVar(_))

  private val genAstTyp: Gen[AstTyp] =
    Gen.oneOf(AstTyp.TInt, AstTyp.TFloat)

  private def genAstExpr(depth: Int): Gen[AstExpr] =
    val leaf = Gen.oneOf(genAstVar.map(AstExpr.EVar(_)), Gen.choose(-20, 20).map(AstExpr.EInt(_)))
    if depth <= 0 then leaf
    else
      Gen.frequency(
        3 -> leaf,
        1 -> Gen.lzy(Gen.zip(genAstExpr(depth - 1), genAstExpr(depth - 1)).map(AstExpr.EAdd.apply)),
        1 -> Gen.lzy(genAstStm(depth - 1).map(AstExpr.EStm(_))),
      )

  private def genAstStm(depth: Int): Gen[AstStm] =
    val decl = Gen.zip(genAstTyp, genAstVar).map(AstStm.SDecl.apply)
    if depth <= 0 then decl
    else
      Gen.frequency(
        2 -> decl,
        2 -> Gen.lzy(Gen.zip(genAstVar, genAstExpr(depth - 1)).map(AstStm.SAss.apply)),
        1 -> Gen.lzy(genAstExpr(depth - 1).map(AstStm.SReturn(_))),
        1 -> Gen.lzy(
          Gen.choose(0, 2).flatMap(Gen.listOfN(_, genAstStm(depth - 1))).map(AstStm.SBlock(_))
        ),
      )

  given Arbitrary[AstLang] = Arbitrary(
    Gen.sized { d =>
      val bound = math.min(d, 3)
      Gen.oneOf(genAstStm(bound), genAstExpr(bound), genAstVar, genAstTyp)
    }
  )

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

  checkAll(
    "Plated[RoseNode] (macro-derived: Option + List + ArraySeq + Chain[Traverse])",
    new PlatedTests[RoseNode]:
      val laws = new PlatedLaws[RoseNode] {}
    .plated,
  )

  checkAll(
    "Plated[AstLang] (macro-derived Multiplate: AstStm | AstExpr | AstVar | AstTyp)",
    new PlatedTests[AstLang]:
      val laws = new PlatedLaws[AstLang] {}
      override def sameSort(x: AstLang, a: AstLang): Boolean = (x, a) match
        case (_: AstStm, _: AstStm)   => true
        case (_: AstExpr, _: AstExpr) => true
        case (_: AstVar, _: AstVar)   => true
        case (_: AstTyp, _: AstTyp)   => true
        case _                        => false
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

  // covers: Plated.transform / transformMachine leaf shortcuts (Plated.scala:126,150) —
  // `kids.isEmpty` weakened to `false` would route leaves through `rebuild` too instead of
  // applying `f` directly. A spy `rebuild` counts calls; leaves must never bump it, on
  // both the call-stack path (shallow tree) and the heap-machine path (past the 512 depth
  // handoff), where the internal-node count is known exactly (one Node per spine level).
  "rebuild fires only for non-leaf nodes, on both the recursive and machine-handoff paths" >> {
    var rebuildCount = 0
    val spyPlate: Plated[Bin] = Plated.fromChildren[Bin](
      {
        case Bin.Node(l, r) => List(l, r)
        case Bin.Leaf(_)    => Nil
      },
      (parent, kids) =>
        rebuildCount += 1
        (parent, kids) match
          case (Bin.Node(_, _), l :: r :: Nil) => Bin.Node(l, r)
          case (leaf, _)                       => leaf,
    )
    val bump: Bin => Bin = { case Bin.Leaf(v) => Bin.Leaf(v + 1); case n => n }

    // (a) shallow: Node(Node(Leaf,Leaf),Leaf) has exactly 2 internal nodes.
    val shallow = Bin.Node(Bin.Node(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))
    rebuildCount = 0
    Plated.transform(bump)(shallow)(using spyPlate)
    val shallowCount = rebuildCount

    // (b) deep: a 1500-deep left spine crosses transformRecursionLimit (512), handing the
    // rest to transformMachine. Exactly one Node per spine level, so the count is exact.
    val depth = 1500
    @tailrec def spine(deep: Bin, i: Int): Bin =
      if i < depth then spine(Bin.Node(deep, Bin.Leaf(i)), i + 1) else deep
    val deep = spine(Bin.Leaf(0), 0)
    rebuildCount = 0
    Plated.transform(bump)(deep)(using spyPlate)
    val deepCount = rebuildCount

    (shallowCount must beEqualTo(2)).and(deepCount must beEqualTo(depth))
  }

  "plateComposeModify catches a stale-parent rebuild that plateModifyIdentity misses" >> {
    // A buggy Plated whose rebuild reads `parent`'s old right child whenever the new left child is
    // negative: `plateModifyIdentity` passes on non-negative trees, but a two-step modify where the
    // first step makes the left child negative and the second makes it positive again exposes the
    // stale-parent bug via `plateComposeModify`.
    val buggyPlate: Plated[Bin] = Plated.fromChildren[Bin](
      {
        case Bin.Node(l, r) => List(l, r)
        case Bin.Leaf(_)    => Nil
      },
      {
        case (Bin.Node(_, oldR), (l @ Bin.Leaf(v)) :: _ :: Nil) if v < 0 => Bin.Node(l, oldR)
        case (Bin.Node(_, _), l :: r :: Nil)                             => Bin.Node(l, r)
        case (leaf, _)                                                   => leaf
      },
    )
    val buggyLaws = new PlatedLaws[Bin](using buggyPlate) {}
    val tree = Bin.Node(Bin.Leaf(1), Bin.Leaf(2))
    val f: Bin => Bin = { case Bin.Leaf(v) => Bin.Leaf(-v); case n => n }
    val g: Bin => Bin = { case Bin.Leaf(v) => Bin.Leaf(-v + 10); case n => n }

    (buggyLaws.plateModifyIdentity(tree) must beTrue)
      .and(buggyLaws.transformIdentity(tree) must beTrue)
      .and(buggyLaws.plateComposeModify(tree, f, g) must beFalse)
  }

  "Multiplate (AstStm | AstExpr | AstVar | AstTyp): everywhereIn + single-pass multi-sort transform" >> {
    val prog: AstStm = AstStm.SBlock(
      List(
        AstStm.SDecl(AstTyp.TInt, AstVar("a")),
        AstStm.SAss(
          AstVar("a"),
          AstExpr.EAdd(
            AstExpr.EVar(AstVar("b")),
            AstExpr.EStm(AstStm.SReturn(AstExpr.EAdd(AstExpr.EVar(AstVar("c")), AstExpr.EInt(5)))),
          ),
        ),
      )
    )
    val varNameIso = lens[AstVar](_.name)
    val renameStm =
      Plated.everywhereIn[AstLang, AstStm].andThen(prism[AstLang, AstVar]).andThen(varNameIso)
    val renameExpr =
      Plated.everywhereIn[AstLang, AstExpr].andThen(prism[AstLang, AstVar]).andThen(varNameIso)

    val renamedStm: AstStm = renameStm.modify(v => (name = "_" + v.name))(prog)
    val renamedExpr: AstExpr =
      renameExpr.modify(v => (name = "_" + v.name))(AstExpr.EStm(prog))

    // Simultaneous multi-sort pass (O'Connor 2011 §5.1 / §6): rewrites AstVar, AstExpr.EInt, and
    // AstTyp in a single bottom-up traversal.
    val multiPass = Plated.transform[AstLang] {
      case AstVar(n)       => AstVar(n.toUpperCase)
      case AstExpr.EInt(i) => AstExpr.EInt(i * 10)
      case AstTyp.TInt     => AstTyp.TFloat
      case other           => other
    }(prog)

    val stmVars = Plated.universe[AstLang](renamedStm).collect { case AstVar(n) => n }
    val exprVars = Plated.universe[AstLang](renamedExpr).collect { case AstVar(n) => n }
    val multiVars = Plated.universe[AstLang](multiPass).collect { case AstVar(n) => n }
    val multiInts = Plated.universe[AstLang](multiPass).collect { case AstExpr.EInt(i) => i }
    val multiTyps = Plated.universe[AstLang](multiPass).collect { case t: AstTyp => t }

    (stmVars === List("_a", "_a", "_b", "_c"))
      .and(exprVars === List("_a", "_a", "_b", "_c"))
      .and(multiVars === List("A", "A", "B", "C"))
      .and(multiInts === List(50))
      .and(multiTyps === List(AstTyp.TFloat))
  }

  "transformM / transformAllM: post-order effects, short-circuiting, and 100k-deep stack safety" >> {
    val tree = Bin.Node(Bin.Node(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

    // (1) State monad: stamps each leaf with its visit index in left-to-right post-order.
    val stamped: (Int, Bin) = Plated
      .transformM[State[Int, *], Bin] {
        case Bin.Leaf(_) => State(i => (i + 1, Bin.Leaf(i)))
        case node        => State.pure(node)
      }(tree)
      .run(10)
      .value
    val stateOk = stamped == (13, Bin.Node(Bin.Node(Bin.Leaf(10), Bin.Leaf(11)), Bin.Leaf(12)))

    // (2) Short-circuiting Either: stops at the first failing leaf without visiting later nodes.
    var visited = 0
    val shortCircuit = Plated.transformM[Either[String, *], Bin] {
      case Bin.Leaf(v) =>
        visited += 1
        if v == 2 then Left("boom at 2") else Right(Bin.Leaf(v * 10))
      case node => Right(node)
    }(tree)
    val shortCircuitOk = shortCircuit == Left("boom at 2") && visited == 2

    // (3) 100k-deep spine stack safety + .transformAllM extension on a self-traversal optic.
    val n = 100000
    @tailrec def spine(deep: Bin, i: Int): Bin =
      if i < n then spine(Bin.Node(deep, Bin.Leaf(i)), i + 1) else deep
    val deep = spine(Bin.Leaf(0), 0)

    val deepEither = Plated[Bin]
      .plate
      .transformAllM[Either[String, *]] {
        case Bin.Leaf(v) => Right(Bin.Leaf(v + 1))
        case node        => Right(node)
      }(deep)
    val deepFail = Plated.transformM[Either[String, *], Bin] {
      case Bin.Leaf(50000) => Left("deep stop")
      case other           => Right(other)
    }(deep)
    val deepEval = Plated
      .transformM[Eval, Bin] {
        case Bin.Leaf(v) => Eval.now(Bin.Leaf(v + 1))
        case node        => Eval.now(node)
      }(deep)
      .value

    stateOk && shortCircuitOk &&
    deepEither.map(Plated.universe(_).length) == Right(2 * n + 1) &&
    deepFail == Left("deep stop") &&
    Plated.universe(deepEval).length == (2 * n + 1)
  }
