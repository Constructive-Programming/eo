package eo
package data

import cats.{Bifunctor, Distributive, Traverse}
import cats.instances.function._
import cats.syntax.functor._

/** Carrier for the `Setter` family — pairs a source `Fst[A]` with a continuation `Snd[A] => B` that
  * decides what to do with the focus when it's written back.
  *
  * Like [[Affine]], SetterF's `A` is encoded as a `Tuple2` that threads both halves of the write
  * path together. SetterF has no `AssociativeFunctor` instance: composing two `SetterF` optics via
  * `Optic.andThen` is not yet supported. Compose a Lens chain in `Tuple2` and reach for SetterF
  * only at the leaf.
  */
class SetterF[A, B](val setter: (Fst[A], Snd[A] => B)) extends AnyVal

/** Typeclass instances for [[SetterF]]. */
object SetterF:

  /** `ForgetfulFunctor[SetterF]` — maps the right-side continuation through `f`, leaving the source
    * unchanged. Unlocks `.modify` and `.replace` on Setter-carrier optics.
    *
    * @group Instances
    */
  given map[S, A]: ForgetfulFunctor[SetterF] with

    def map[X, B, C](fa: SetterF[X, B], f: B => C): SetterF[X, C] =
      SetterF(fa.setter._1, fa.setter._2.andThen(f))

  /** `ForgetfulTraverse[SetterF, Distributive]` — lifts an effectful `B => G[C]` through the
    * continuation using `Distributive[G]` (a stronger counterpart to `Applicative` that suits the
    * read-once / write-once Setter shape).
    *
    * @group Instances
    */
  given traverse[S, A]: ForgetfulTraverse[SetterF, Distributive] with

    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] =
      s => g => D.tupleLeft(D.distribute(s.setter._2)(g), s.setter._1).map(SetterF(_))

  /** Coerce any Tuple2-carrier optic (typically a Lens) into a SetterF optic. Every Lens is-a
    * Setter: modify is `setter.from(SetterF(s, f))`, which re-runs the Lens's get/replace path with
    * `f` applied to the focus. Enables `lens.morph[SetterF]`.
    *
    * @group Instances
    */
  given tuple2setter: Composer[Tuple2, SetterF] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, SetterF] =
      new Optic[S, T, A, B, SetterF]:
        type X = (S, A)
        def to: S => SetterF[X, A] = s => SetterF((s, identity[A]))

        def from: SetterF[X, B] => T = sfxb =>
          val (s, f) = sfxb.setter
          val (xo, a) = o.to(s)
          o.from((xo, f(a)))
