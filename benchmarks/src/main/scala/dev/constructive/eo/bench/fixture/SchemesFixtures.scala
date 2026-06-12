package dev.constructive.eo
package bench
package fixture

import cats.{Applicative, Eval, Traverse}
import higherkindness.droste.data.Fix
import higherkindness.droste.{Algebra, Coalgebra}

import dev.constructive.eo.schemes.Basis

/** Pattern functor for the native [[Bin]] tree (`Leaf(Int)` / `Node(Bin, Bin)`).
  *
  * droste requires this pattern-functor + `Fix` encoding to express recursion schemes; eo works on
  * the native `Bin` directly through its `Plated` instance, and the hand-wired baseline recurses on
  * `Bin` straight. The three encodings are what `SchemesBench` contrasts on the *same* fold
  * (leaf-sum) and the *same* build (a perfect binary tree of `2^depth` leaves).
  */
enum BinF[+A]:
  case LeafF(value: Int)
  case NodeF(left: A, right: A)

object SchemesFixtures:

  /** `Traverse[BinF]` — serves both droste (which needs only `Functor[BinF]`, obtained via the
    * `Traverse <: Functor` subtype) and eo's typed schemes (which need the full `Traverse`). A
    * single instance avoids an ambiguous `Functor[BinF]` summon. `foldRight` is `Eval`-based so the
    * typed driver's trampoline stays lazy.
    */
  given binTraverse: Traverse[BinF] with

    def traverse[G[_]: Applicative, A, B](fa: BinF[A])(f: A => G[B]): G[BinF[B]] =
      fa match
        case BinF.LeafF(v)    => Applicative[G].pure(BinF.LeafF(v))
        case BinF.NodeF(l, r) => Applicative[G].map2(f(l), f(r))(BinF.NodeF(_, _))

    def foldLeft[A, B](fa: BinF[A], b: B)(f: (B, A) => B): B =
      fa match
        case BinF.LeafF(_)    => b
        case BinF.NodeF(l, r) => f(f(b, l), r)

    def foldRight[A, B](fa: BinF[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match
        case BinF.LeafF(_)    => lb
        case BinF.NodeF(l, r) => f(l, Eval.defer(f(r, lb)))

  /** `Basis[BinF, Bin]` — the `Project`/`Embed` correspondence between the native `Bin` and its
    * pattern functor, for the typed `cataF`/`anaF` benches.
    */
  given binBasis: Basis[BinF, Bin] = Basis(
    {
      case Bin.Leaf(v)    => BinF.LeafF(v)
      case Bin.Node(l, r) => BinF.NodeF(l, r)
    },
    {
      case BinF.LeafF(v)    => Bin.Leaf(v)
      case BinF.NodeF(l, r) => Bin.Node(l, r)
    },
  )

  // ----- droste algebra / coalgebra (over BinF, on Fix[BinF]) ----------------

  val drosteSum: Algebra[BinF, Int] = Algebra {
    case BinF.LeafF(v)    => v
    case BinF.NodeF(l, r) => l + r
  }

  /** Build a perfect binary tree: depth `d` ⇒ `2^d` `Leaf(1)`s. */
  val drosteBuild: Coalgebra[BinF, Int] = Coalgebra { d =>
    if d <= 0 then BinF.LeafF(1) else BinF.NodeF(d - 1, d - 1)
  }

  // ----- eo TYPED algebras (over the pattern functor BinF via Basis/Traverse) ----------------

  /** Typed cata gather — the leaf-sum, pattern-matching `BinF`'s named constructors. */
  val eoTypedSum: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(v)    => v
      case BinF.NodeF(l, r) => l + r

  /** Typed coalgebra (the single fused `Seed => F[Seed]` shape) — builds the perfect binary tree. */
  val eoTypedCoalg: Int => BinF[Int] = d =>
    if d <= 0 then BinF.LeafF(1) else BinF.NodeF(d - 1, d - 1)

  /** Typed fused-hylo algebra — folds to `Int` directly, never building a `Bin`. */
  val eoTypedHyloAlg: (Int, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(_)    => 1
      case BinF.NodeF(l, r) => l + r

  // ----- hand-wired recursion (the baseline you'd write without either lib) --

  def handSum(b: Bin): Int =
    b match
      case Bin.Leaf(v)    => v
      case Bin.Node(l, r) => handSum(l) + handSum(r)

  def handBuild(d: Int): Bin =
    if d <= 0 then Bin.Leaf(1) else Bin.Node(handBuild(d - 1), handBuild(d - 1))

  def handHylo(d: Int): Int =
    if d <= 0 then 1 else handHylo(d - 1) + handHylo(d - 1)

  // ----- prebuilt trees for the cata benches (built once, not measured) ------

  def balancedBin(d: Int): Bin = handBuild(d)

  def balancedFix(d: Int): Fix[BinF] =
    if d <= 0 then Fix(BinF.LeafF(1))
    else Fix(BinF.NodeF(balancedFix(d - 1), balancedFix(d - 1)))

  // ----- zoo fixtures (para / apo / histo / futu — eo vs droste) -------------

  import higherkindness.droste.{CVAlgebra, CVCoalgebra, RAlgebra, RCoalgebra}
  import higherkindness.droste.data.{Attr => DAttr, Coattr => DCoattr}
  import dev.constructive.eo.schemes.{Attr => EoAttr, Coattr => EoCoattr, Gather}
  import dev.constructive.eo.data.BiAffine
  import dev.constructive.eo.optics.Optic

  // para: the same leaf-sum with subterms IGNORED — measures pure decoration
  // overhead (eo pairs subterms from the walked nodes; droste re-embeds each).
  val eoParaAlg: (Bin, BinF[(Bin, Int)]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(v)              => v
      case BinF.NodeF((_, l), (_, r)) => l + r

  val drosteParaAlg: RAlgebra[Fix[BinF], BinF, Int] = RAlgebra {
    case BinF.LeafF(v)              => v
    case BinF.NodeF((_, l), (_, r)) => l + r
  }

  // apo, never grafting: the build-side decoration overhead row.
  val eoApoCoalg: Int => BinF[Either[Bin, Int]] = d =>
    if d <= 0 then BinF.LeafF(1) else BinF.NodeF(Right(d - 1), Right(d - 1))

  val drosteApoCoalg: RCoalgebra[Fix[BinF], BinF, Int] = RCoalgebra { d =>
    if d <= 0 then BinF.LeafF(1) else BinF.NodeF(Right(d - 1), Right(d - 1))
  }

  // histo, heads only: the course-of-value bookkeeping cost.
  val eoHistoAlg: (Bin, BinF[EoAttr[BinF, Int]]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(v)    => v
      case BinF.NodeF(l, r) => l.head + r.head

  val drosteHistoAlg: CVAlgebra[BinF, Int] = CVAlgebra {
    case BinF.LeafF(v)    => v
    case BinF.NodeF(l, r) => DAttr.un(l)._1 + DAttr.un(r)._1
  }

  // futu, single layer per step: the free-wrapper cost.
  val eoFutuCoalg: Int => BinF[EoCoattr[BinF, Int]] = d =>
    if d <= 0 then BinF.LeafF(1)
    else BinF.NodeF(EoCoattr.Pure(d - 1), EoCoattr.Pure(d - 1))

  val drosteFutuCoalg: CVCoalgebra[BinF, Int] = CVCoalgebra { d =>
    if d <= 0 then BinF.LeafF(1)
    else BinF.NodeF(DCoattr.pure(d - 1), DCoattr.pure(d - 1))
  }

  // generic decoration route: a USER-WRITTEN id gather (not the Gather.cata
  // singleton, so the driver cannot take the identity fast path) — D4's
  // dispatch-cost honesty number.
  val userIdGather: Gather[BinF, Int, Int] =
    new Optic[Unit, Int, Unit, Int, BiAffine]:
      type X = (Unit, BinF[Int])
      def to(u: Unit): BiAffine[X, Unit] =
        throw new UnsupportedOperationException("vestigial")
      def from(xb: BiAffine[X, Int]): Int = xb match
        case s: BiAffine.Step[X, Int] => s.b
        case _: BiAffine.Done[X, Int] => throw new UnsupportedOperationException("fold-side")
