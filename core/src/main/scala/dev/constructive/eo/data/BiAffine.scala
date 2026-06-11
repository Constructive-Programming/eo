package dev.constructive.eo
package data

import cats.{Applicative, Monoid}

import accessor.{Graft, PartialAccessor}
import forgetful.*

/** Carrier for the decoration (`Decor`) family of the recursion-scheme zoo — [[Affine]]'s data
  * shape worn on the *build* seam. Where `Affine.Miss` means "the read found no focus",
  * [[BiAffine.Done]] means "**this slot is already finished** — the engine must not call the
  * coalgebra for it". Its payload's meaning is pinned per optic value via the existential `A`
  * (`Fst[A]`): an apomorphism's `Done` carries a finished subtree (prefill the slot, O(1) graft); a
  * futumorphism's `Done` carries a prebuilt layer (unroll it, still no coalgebra call).
  * [[BiAffine.Step]] is the keep-going arm: focus `b` alongside a one-F-layer leftover context
  * (`Snd[A]`).
  *
  * Same `Fst` / `Snd` match-type discipline as [[Affine]]: at every constructor site `A` is a
  * concrete `Tuple2` (so `Fst[A]` / `Snd[A]` reduce); carried through an `Optic[…, BiAffine]`
  * existential, `A` is abstract and the match types stay inert.
  *
  * The composition-matrix row (`AssociativeFunctor[BiAffine]`, `Composer` bridges) is deliberately
  * NOT shipped here — it is follow-up work; the laws that need it (`Done`/`Step` coherence across
  * `andThen`) live with it.
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

    override def hashCode(): Int = fst.hashCode

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

    override def hashCode(): Int = snd.hashCode * 31 + (if b == null then 0 else b.hashCode)

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
