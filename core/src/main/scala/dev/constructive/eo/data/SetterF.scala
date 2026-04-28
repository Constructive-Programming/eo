package dev.constructive.eo
package data

import cats.Distributive
import cats.instances.function.*
import cats.syntax.functor.*

/** Carrier for the `Setter` family — pairs a source `Fst[A]` with a continuation `Snd[A] => B`. No
  * `AssociativeFunctor[SetterF]` ships, so SetterF.andThen(SetterF) is not supported; compose a
  * Lens chain in `Tuple2` and reach for SetterF at the leaf.
  *
  * @tparam A
  *   existential leftover tuple
  * @tparam B
  *   focus written back
  */
class SetterF[A, B](val setter: (Fst[A], Snd[A] => B)) extends AnyVal

/** Typeclass instances for [[SetterF]]. */
object SetterF:

  /** `ForgetfulFunctor[SetterF]` — maps the continuation through `f`, leaving the source unchanged.
    * Unlocks `.modify` / `.replace` on Setter-carrier optics.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[SetterF] with

    def map[X, B, C](fa: SetterF[X, B], f: B => C): SetterF[X, C] =
      val inner = fa.setter._2
      SetterF(fa.setter._1, a => f(inner(a)))

  /** `ForgetfulTraverse[SetterF, Distributive]` — lifts an effectful `B => G[C]` through the
    * continuation under `Distributive[G]` (the right shape for read-once / write-once Setter).
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[SetterF, Distributive] with

    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] =
      s => g => D.tupleLeft(D.distribute(s.setter._2)(g), s.setter._1).map(SetterF(_))

  /** Shared skeleton for `Composer[F, SetterF]` instances. Every carrier materialises a coerced
    * Optic with the same `type X = (S, A)` shape and an identity `to` that seeds the SetterF; only
    * `applyWrite` differs per carrier.
    */
  private def coerceToSetter[F[_, _], S, T, A, B](
      applyWrite: (S, A => B) => T
  ): optics.Optic[S, T, A, B, SetterF] =
    new optics.Optic[S, T, A, B, SetterF]:
      type X = (S, A)
      val to: S => SetterF[X, A] = s => SetterF((s, identity[A]))

      val from: SetterF[X, B] => T = sfxb =>
        val (s, f) = sfxb.setter
        applyWrite(s, f)

  /** Lens → SetterF. Every Lens is-a Setter; powers cross-carrier `lens.andThen(setter)` via
    * `Morph[Tuple2, SetterF]`.
    *
    * @group Instances
    */
  given tuple2setter: Composer[Tuple2, SetterF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Tuple2]): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        val (xo, a) = o.to(s)
        o.from((xo, f(a)))

  /** Prism → SetterF. Hit writes `f(a)` through the Prism's build path; miss passes the leftover
    * back via `o.from(Left(xo))` — observably the same as the Prism's own `.modify(f)`.
    *
    * @group Instances
    */
  given either2setter: Composer[Either, SetterF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Either]): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        o.to(s) match
          case Right(a) => o.from(Right(f(a)))
          case Left(xo) => o.from(Left(xo))

  /** Optional → SetterF. Mirror of [[either2setter]] split across `Affine.Hit` / `Affine.Miss`;
    * miss uses `widenB` instead of allocating a fresh `Miss`.
    *
    * @group Instances
    */
  given affine2setter: Composer[Affine, SetterF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Affine]): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        o.to(s) match
          case h: Affine.Hit[o.X, A] =>
            o.from(new Affine.Hit[o.X, B](h.snd, f(h.b)))
          case m: Affine.Miss[o.X, A] =>
            o.from(m.widenB[B])

  // MultiFocus[F] → SetterF lives in `MultiFocus.scala` (`multifocus2setter`).
