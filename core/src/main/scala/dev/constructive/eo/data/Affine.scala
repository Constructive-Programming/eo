package dev.constructive.eo
package data

import cats.{Applicative, Monoid}

import forgetful.*
import compose.*
import optics.Optic

/** Extract the first element type of a `Tuple2`. Stays as an unreduced match type when `T` is not a
  * `Tuple2` — this is load bearing for [[Affine.assoc]] accepting unbounded existentials.
  */
type Fst[T] = T match
  case (f, s) => f

/** Extract the second element type of a `Tuple2`. See [[Fst]]. */
type Snd[T] = T match
  case (f, s) => s

/** Carrier for the `Optional` family — sealed hierarchy: [[Affine.Miss]] (no focus; carries
  * `Fst[A]`) and [[Affine.Hit]] (focus present; carries `Snd[A]` + `B`). Miss / Hit store fields
  * directly (no Either / Tuple2 wrapper); a `.affine: Either[Fst[A], (Snd[A], B)]` legacy accessor
  * reconstructs the old view on demand. Prefer matching `Miss(...)` / `Hit(...)` for zero-alloc
  * access.
  *
  * At every constructor site `A` is a concrete `Tuple2` (so `Fst[A]` / `Snd[A]` reduce); when
  * carried through an `Optic[…, Affine]` existential, `A` is abstract and the match types stay
  * inert.
  *
  * @tparam A
  *   existential leftover tuple
  * @tparam B
  *   focus type
  */
sealed trait Affine[A, B]:
  import Affine.*

  /** Monomorphic fold — pattern-match on Miss/Hit and run the matching branch.
    *
    * @tparam C
    *   output type
    */
  def fold[C](onMiss: Fst[A] => C, onHit: (Snd[A], B) => C): C = this match
    case m: Miss[A, B] => onMiss(m.fst)
    case h: Hit[A, B]  => onHit(h.snd, h.b)

  /** Fold both branches into a new `Affine[A, C]`. Dispatches via direct pattern match (no
    * intermediate Either).
    *
    * @tparam C
    *   focus type of the output
    */
  def aFold[C](
      f: Fst[A] => Affine[A, C],
      g: ((Snd[A], B)) => Affine[A, C],
  ): Affine[A, C] = this match
    case m: Miss[A, B] => f(m.fst)
    case h: Hit[A, B]  => g((h.snd, h.b))

  /** Effectful traversal — runs `f` on the hit branch, passes the miss branch through via
    * `Applicative.pure`.
    *
    * @tparam C
    *   focus produced by `f`
    * @tparam G
    *   effect constructor
    */
  def aTraverse[C, G[_]: Applicative](f: B => G[C]): G[Affine[A, C]] = this match
    case m: Miss[A, B] =>
      Applicative[G].pure[Affine[A, C]](m.widenB[C])
    case h: Hit[A, B] =>
      Applicative[G].map(f(h.b))(c => new Hit[A, C](h.snd, c))

/** Constructors and typeclass instances for [[Affine]]. */
object Affine:

  /** Miss-branch variant — no focus, stores `fst: Fst[A]` directly. `B` is phantom at runtime;
    * callers re-typing across a phantom-B change should prefer [[widenB]] over `asInstanceOf`.
    */
  final class Miss[A, B](val fst: Fst[A]) extends Affine[A, B]:
    override def toString(): String = s"Miss($fst)"

    override def equals(that: Any): Boolean = that match
      case other: Miss[?, ?] => fst == other.fst
      case _                 => false

    override def hashCode(): Int = fst.hashCode

    /** Re-type this `Miss[A, B]` as `Miss[A, B2]` without allocating a new instance. Safe because
      * `Miss` stores only `fst: Fst[A]` — the `B` parameter is phantom at the runtime shape.
      */
    inline def widenB[B2]: Miss[A, B2] = this.asInstanceOf[Miss[A, B2]]

  /** Hit-branch variant: focus present. Stores `snd` and `b` as direct fields. */
  final class Hit[A, B](val snd: Snd[A], val b: B) extends Affine[A, B]:
    override def toString(): String = s"Hit($snd, $b)"

    override def equals(that: Any): Boolean = that match
      case other: Hit[?, ?] => snd == other.snd && b == other.b
      case _                => false

    override def hashCode(): Int = snd.hashCode * 31 + (if b == null then 0 else b.hashCode)

  /** Legacy smart constructor — dispatches a raw Either to the matching variant. Prefer
    * `new Miss(...)` / `new Hit(...)` in new code.
    *
    * @group Constructors
    */
  def apply[A, B](e: Either[Fst[A], (Snd[A], B)]): Affine[A, B] = e match
    case Left(l)       => new Miss[A, B](l)
    case Right((s, b)) => new Hit[A, B](s, b)

  /** Convenience wrapping extension — turns a raw `Either[Fst[X], (Snd[X], B)]` into an
    * `Affine[X, B]` via the [[apply]] smart constructor.
    *
    * @group Instances
    */
  extension [X <: Tuple, B](e: Either[Fst[X], (Snd[X], B)]) def affine: Affine[X, B] = Affine(e)

  /** Miss-branch constructor — produce an `Affine[X, B]` carrying `l` as its Fst component.
    *
    * @group Constructors
    */
  def ofLeft[X, B](l: Fst[X]): Affine[X, B] = new Miss[X, B](l)

  /** Hit-branch constructor — produce an `Affine[X, B]` carrying `r._1` as Snd and `r._2` as the
    * focus.
    *
    * @group Constructors
    */
  def ofRight[X, B](r: (Snd[X], B)): Affine[X, B] = new Hit[X, B](r._1, r._2)

  /** `ForgetfulFunctor[Affine]` — maps the focus `B`, passing the miss branch through. One
    * allocation per hit-branch map; pure pass-through on miss.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[Affine] with

    def map[X, A, B](fa: Affine[X, A], f: A => B): Affine[X, B] = fa match
      case m: Miss[X, A] => m.widenB[B]
      case h: Hit[X, A]  => new Hit[X, B](h.snd, f(h.b))

  /** `ForgetfulFold[Affine]` — miss empty, hit runs `f` on the focus. Carrier-owned (like [[map]] /
    * [[traverse]]), so it resolves through Affine's companion with no import.
    *
    * @group Instances
    */
  given fold: ForgetfulFold[Affine] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: Affine[X, A]): M = fa match
      case _: Miss[X, A] => Monoid[M].empty
      case h: Hit[X, A]  => f(h.b)

  /** `ForgetfulTraverse[Affine, Applicative]` — lifts a focus-level `A => G[B]` into an
    * `Affine[X, A] => G[Affine[X, B]]`. Unlocks `.modifyA` / `.all` / `.modifyF` on Affine- carrier
    * optics.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Affine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: Affine[X, A], f: A => G[B]): G[Affine[X, B]] =
      fa.aTraverse(f)

  /** Composition functor for `Affine` carriers. `X` / `Y` are deliberately unbounded — Affine's
    * `Fst[X]` / `Snd[X]` match types stay inert when `X` is not a `Tuple`, which is sound for every
    * shipped concrete optic (every concrete X is a Tuple2). A `ValidCarrier[F, X]` threading is a
    * possible post-0.1.0 cleanup.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Affine, Xo, Xi] with
    // Miss discrimination: an untagged `Fst[Xo] | FstXi` union is NOT runtime-separable
    // (Fst[Xo] is abstract and may itself be a Tuple2), so the outer Miss object doubles
    // as the tag — it is already allocated, its B is phantom, and it can never be a Tuple2.
    type FstXi = (Snd[Xo], Fst[Xi])
    type SndXi = (Snd[Xo], Snd[Xi])
    type Z = (Miss[Xo, Nothing] | FstXi, SndXi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
    ): Affine[Z, C] = outer.to(s) match
      case om: Miss[Xo, A] =>
        new Miss[Z, C](om.widenB[Nothing])
      case oh: Hit[Xo, A] =>
        inner.to(oh.b) match
          case im: Miss[Xi, C] =>
            new Miss[Z, C]((oh.snd, im.fst))
          case ih: Hit[Xi, C] =>
            new Hit[Z, C]((oh.snd, ih.snd), ih.b)

    def composeFrom[S, T, A, B, C, D](
        xd: Affine[Z, D],
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
    ): T = xd match
      case m: Miss[Z, D] =>
        m.fst match
          case om: Miss[?, ?] =>
            outer.from(om.asInstanceOf[Miss[Xo, Nothing]].widenB[B])
          case fstXi: FstXi @unchecked =>
            val b: B = inner.from(new Miss[Xi, D](fstXi._2))
            outer.from(new Hit[Xo, B](fstXi._1, b))
      case h: Hit[Z, D] =>
        val pair = h.snd
        val xoSnd = pair._1
        val xiSnd = pair._2
        val b: B = inner.from(new Hit[Xi, D](xiSnd, h.b))
        outer.from(new Hit[Xo, B](xoSnd, b))

  /** Lens → Affine. Always-Hit at read; lets `lens.andThen(optional)` type-check via the
    * cross-carrier `Morph[Tuple2, Affine]` that picks up this composer.
    *
    * @group Instances
    */
  given tuple2affine: Composer[Tuple2, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (T, o.X)
        def to(s: S): Affine[X, A] =
          val (xo, a) = o.to(s)
          new Hit[X, A](xo, a)

        def from(a: Affine[X, B]): T =
          a match
            case m: Miss[X, B] => m.fst
            case h: Hit[X, B]  => o.from((h.snd, h.b))

  // Iso → Affine is handled by the low-priority `chainViaTuple2` fallback in
  // `LowPriorityComposerInstances`. No direct `Composer[Direct, Affine]` is shipped.

  /** Prism → Affine — reuses the `Either` decomposition for Affine's miss / hit branches.
    *
    * @group Instances
    */
  given either2affine: Composer[Either, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (o.X, S)
        def to(s: S): Affine[X, A] =
          o.to(s) match
            case Right(a) => new Hit[X, A](s, a)
            case Left(x)  => new Miss[X, A](x)
        def from(xb: Affine[X, B]): T =
          xb match
            case m: Miss[X, B] => o.from(Left(m.fst))
            case h: Hit[X, B]  => o.from(Right(h.b))
