package dev.constructive.eo
package data

import cats.{Applicative, Monoid}

import accessor.{Graft, PartialAccessor}
import compose.*
import forgetful.*
import optics.Optic

/** Carrier for the decoration (`Gather`/`Scatter`) family of the recursion-scheme zoo —
  * [[Affine]]'s data shape worn on the *build* seam. Where `Affine.Miss` means "the read found no
  * focus", [[BiAffine.Done]] means "**this slot is already finished** — the engine must not call
  * the coalgebra for it". Its payload's meaning is pinned per optic value via the existential `A`
  * (`Fst[A]`): an apomorphism's `Done` carries a finished subtree (prefill the slot, O(1) graft); a
  * futumorphism's `Done` carries a prebuilt layer (unroll it, still no coalgebra call).
  * [[BiAffine.Step]] is the keep-going arm: focus `b` alongside a one-F-layer leftover context
  * (`Snd[A]`).
  *
  * Same `Fst` / `Snd` match-type discipline as [[Affine]]: at every constructor site `A` is a
  * concrete `Tuple2` (so `Fst[A]` / `Snd[A]` reduce); carried through an `Optic[…, BiAffine]`
  * existential, `A` is abstract and the match types stay inert.
  *
  * Same-carrier composition is shipped as [[BiAffine.assoc]] — the build-side mirror of
  * [[Affine.assoc]] (`Done` ↔ `Miss`, `Step` ↔ `Hit`), so `biaffine.andThen(biaffine)` type-checks
  * and runs (`Done`/`Step` coherence across `andThen`). The cross-carrier `Composer` bridges into
  * `BiAffine` remain follow-up, alongside the recursion-scheme citizens that would consume them.
  *
  * @tparam A
  *   existential leftover tuple
  * @tparam B
  *   focus type
  */
sealed trait BiAffine[A, B]:
  import BiAffine.*

  /** Monomorphic fold — pattern-match on Done/Step and run the matching branch.
    *
    * @tparam C
    *   output type
    */
  def fold[C](onDone: Fst[A] => C, onStep: (Snd[A], B) => C): C = this match
    case d: Done[A, B] => onDone(d.fst)
    case s: Step[A, B] => onStep(s.snd, s.b)

/** Constructors and typeclass instances for [[BiAffine]]. */
object BiAffine:

  /** Finished arm — no further building, stores `fst: Fst[A]` directly. `B` is phantom at runtime;
    * callers re-typing across a phantom-B change should prefer [[widenB]] over `asInstanceOf`.
    */
  final class Done[A, B](val fst: Fst[A]) extends BiAffine[A, B]:
    override def toString(): String = s"Done($fst)"

    override def equals(that: Any): Boolean = that match
      case other: Done[?, ?] => fst == other.fst
      case _                 => false

    override def hashCode(): Int = if fst.asInstanceOf[AnyRef] == null then 0 else fst.hashCode

    /** Re-type this `Done[A, B]` as `Done[A, B2]` without allocating a new instance. Safe because
      * `Done` stores only `fst: Fst[A]` — the `B` parameter is phantom at the runtime shape.
      */
    inline def widenB[B2]: Done[A, B2] = this.asInstanceOf[Done[A, B2]]

  /** Keep-going arm: focus present alongside its one-layer leftover context. */
  final class Step[A, B](val snd: Snd[A], val b: B) extends BiAffine[A, B]:
    override def toString(): String = s"Step($snd, $b)"

    override def equals(that: Any): Boolean = that match
      case other: Step[?, ?] => snd == other.snd && b == other.b
      case _                 => false

    override def hashCode(): Int =
      (if snd.asInstanceOf[AnyRef] == null then 0 else snd.hashCode) * 31 +
        (if b.asInstanceOf[AnyRef] == null then 0 else b.hashCode)

  /** Finished-arm constructor.
    *
    * @group Constructors
    */
  def ofDone[X, B](fst: Fst[X]): BiAffine[X, B] = new Done[X, B](fst)

  /** Keep-going-arm constructor.
    *
    * @group Constructors
    */
  def ofStep[X, B](snd: Snd[X], b: B): BiAffine[X, B] = new Step[X, B](snd, b)

  /** `ForgetfulFunctor[BiAffine]` — maps the focus `B` through the Step arm, passing Done through.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[BiAffine] with

    def map[X, A, B](fa: BiAffine[X, A], f: A => B): BiAffine[X, B] = fa match
      case d: Done[X, A] => new Done[X, B](d.fst)
      case s: Step[X, A] => new Step[X, B](s.snd, f(s.b))

  /** `ForgetfulFold[BiAffine]` — Done empty, Step runs `f` on the focus.
    *
    * @group Instances
    */
  given fold: ForgetfulFold[BiAffine] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: BiAffine[X, A]): M = fa match
      case _: Done[X, A] => Monoid[M].empty
      case s: Step[X, A] => f(s.b)

  /** `ForgetfulTraverse[BiAffine, Applicative]` — runs `f` on the Step arm, passes Done through via
    * `Applicative.pure`.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[BiAffine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: BiAffine[X, A], f: A => G[B]): G[BiAffine[X, B]] =
      fa match
        case d: Done[X, A] => Applicative[G].pure(new Done[X, B](d.fst))
        case s: Step[X, A] => Applicative[G].map(f(s.b))(b => new Step[X, B](s.snd, b))

  /** `PartialAccessor[BiAffine]` — Step has the focus, Done has none.
    *
    * @group Instances
    */
  given partial: PartialAccessor[BiAffine] with
    def getOption[X, A](fa: BiAffine[X, A]): Option[A] = fa.fold(_ => None, (_, b) => Some(b))

  /** `Graft[BiAffine]` — the build-channel injection vocabulary: [[Done]] is the finished arm,
    * [[Step]] the keep-going arm.
    *
    * @group Instances
    */
  given graft: Graft[BiAffine] with
    def done[X, B](fst: Fst[X]): BiAffine[X, B] = new Done[X, B](fst)
    def step[X, B](snd: Snd[X], b: B): BiAffine[X, B] = new Step[X, B](snd, b)

  /** Composition functor for `BiAffine` carriers — the build-side mirror of [[Affine.assoc]]
    * (`Done` ↔ `Miss`, `Step` ↔ `Hit`), so the generic `Optic.andThen` resolves for
    * `BiAffine`-carried optics. `Z` is identical to Affine's: the outer/inner leftovers nested
    * through the `Done`/`Step` arms. `Done` short-circuits (an outer finished slot ends the
    * composition); `Step` threads the focus through `inner` and recombines the one-layer contexts.
    *
    * `Xo` / `Xi` are deliberately unbounded — `BiAffine`'s `Fst` / `Snd` match types stay inert
    * when the existential is not a `Tuple`, sound for every concrete optic (each concrete `X` is a
    * `Tuple2`), exactly as on [[Affine.assoc]].
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[BiAffine, Xo, Xi] with
    type Z = (Either[Fst[Xo], (Snd[Xo], Fst[Xi])], (Snd[Xo], Snd[Xi]))

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, BiAffine] { type X = Xo },
        inner: Optic[A, B, C, D, BiAffine] { type X = Xi },
    ): BiAffine[Z, C] = outer.to(s) match
      case od: Done[Xo, A] =>
        new Done[Z, C](Left(od.fst))
      case os: Step[Xo, A] =>
        inner.to(os.b) match
          case id: Done[Xi, C] =>
            new Done[Z, C](Right((os.snd, id.fst)))
          case is: Step[Xi, C] =>
            new Step[Z, C]((os.snd, is.snd), is.b)

    def composeFrom[S, T, A, B, C, D](
        xd: BiAffine[Z, D],
        inner: Optic[A, B, C, D, BiAffine] { type X = Xi },
        outer: Optic[S, T, A, B, BiAffine] { type X = Xo },
    ): T = xd match
      case d: Done[Z, D] =>
        // Fst[Z] = Either[Fst[Xo], (Snd[Xo], Fst[Xi])] — the match-type reduction can't be
        // proven at the trait level, so we cast (as on Affine.assoc).
        d.fst.asInstanceOf[Either[Fst[Xo], (Snd[Xo], Fst[Xi])]] match
          case Left(y)         => outer.from(new Done[Xo, B](y))
          case Right((x1, y0)) =>
            val b: B = inner.from(new Done[Xi, D](y0))
            outer.from(new Step[Xo, B](x1, b))
      case s: Step[Z, D] =>
        val pair = s.snd.asInstanceOf[(Snd[Xo], Snd[Xi])]
        val b: B = inner.from(new Step[Xi, D](pair._2, s.b))
        outer.from(new Step[Xo, B](pair._1, b))
