package eo
package data

import cats.{Bifunctor, Distributive, Traverse}
import cats.instances.function._
import cats.syntax.functor._

class SetterF[A, B](val setter: (Fst[A], Snd[A] => B)) extends AnyVal

object SetterF:
  given map[S, A]: ForgetfulFunctor[SetterF] with
    def map[X, B, C](fa: SetterF[X, B], f: B => C): SetterF[X, C] =
      SetterF(fa.setter._1, fa.setter._2.andThen(f))

  given traverse[S, A]: ForgetfulTraverse[SetterF, Distributive] with
    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] =
      s => g => D.tupleLeft(D.distribute(s.setter._2)(g), s.setter._1).map(SetterF(_))

  /** Coerce any Tuple2-carrier optic (typically a Lens) into a SetterF
    * optic. Every Lens is-a Setter: modify is `setter.from(SetterF(s, f))`,
    * which re-runs the Lens's get/replace path with `f` applied to the
    * focus. Enables `lens.morph[SetterF]`.
    */
  given tuple2setter: Composer[Tuple2, SetterF] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, SetterF] =
      new Optic[S, T, A, B, SetterF]:
        type X = (S, A)
        def to: S => SetterF[X, A] = s => SetterF((s, identity[A]))
        def from: SetterF[X, B] => T = sfxb =>
          val (s, f)  = sfxb.setter
          val (xo, a) = o.to(s)
          o.from((xo, f(a)))
