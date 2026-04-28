package dev.constructive.eo
package data

import cats.Distributive
import cats.instances.function.*
import cats.syntax.functor.*

/** Carrier for the `Setter` family — pairs a source `Fst[A]` with a continuation `Snd[A] => B`.
  *
  * Same-carrier composition (`setter.andThen(setter)`) ships via [[SetterF.assocSetterF]] —
  * `AssociativeFunctor[SetterF, Xo, Xi]` with `type Z = (Fst[Xo], Snd[Xi])`. The deferred-modify
  * semantic fits the protocol once you observe that `composeTo` only needs to seed `(xo, identity)`
  * (no inner-`to` call required, since SetterF's continuation is structurally identity at every
  * canonical construction site — `coerceToSetter` and `Setter.apply`); `composeFrom` then extracts
  * the user's `c2d` from the mapped continuation and routes it through `inner.from` then
  * `outer.from`. The asInstanceOf casts inside the instance are sound under the universal
  * convention that every SetterF optic stores `X = (S_outer, A_focus)` (enforced at every
  * construction site).
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

  /** Same-carrier composition for `Optic[…, SetterF]` — closes top-5 plan gap #4.
    *
    * Encoding: `Z = (Fst[Xo], Snd[Xi])`. `composeTo` seeds the SetterF with `(outer-source,
    * identity[C])` — no `inner.to` call needed because SetterF's continuation is structurally
    * `identity[C]` at every canonical construction site (every bundled SetterF optic uses
    * [[coerceToSetter]] or [[optics.Setter.apply]], both of which seed identity). `composeFrom`
    * extracts the user's `c2d: C => D` from the post-`map` continuation and applies it through
    * `inner.from` then `outer.from`, matching the deferred-modify semantic
    * `composedModify(c2d)(s) = outer.modify(inner.modify(c2d))(s)`.
    *
    * The asInstanceOf casts coerce abstract `Fst[Xo] / Snd[Xo] / Fst[Xi] / Snd[Xi]` to the
    * canonical `(S, A)` / `(A, C)` decomposition. Sound under the universal SetterF convention,
    * unsafe only for hand-built SetterF optics that violate it — and there's no public API path to
    * build such an optic.
    *
    * @group Instances
    */
  given assocSetterF[Xo, Xi]: AssociativeFunctor[SetterF, Xo, Xi] with
    type Z = (Fst[Xo], Snd[Xi])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: optics.Optic[S, T, A, B, SetterF] { type X = Xo },
        inner: optics.Optic[A, B, C, D, SetterF] { type X = Xi },
    ): SetterF[Z, C] =
      val xo: Fst[Xo] = outer.to(s).setter._1
      SetterF((xo, identity[C].asInstanceOf[Snd[Xi] => C]))

    def composeFrom[S, T, A, B, C, D](
        xd: SetterF[Z, D],
        inner: optics.Optic[A, B, C, D, SetterF] { type X = Xi },
        outer: optics.Optic[S, T, A, B, SetterF] { type X = Xo },
    ): T =
      val xo: Fst[Xo] = xd.setter._1.asInstanceOf[Fst[Xo]]
      val c2d: Snd[Xi] => D = xd.setter._2.asInstanceOf[Snd[Xi] => D]
      val innerModify: A => B = a => inner.from(SetterF((a.asInstanceOf[Fst[Xi]], c2d)))
      outer.from(SetterF((xo, innerModify.asInstanceOf[Snd[Xo] => B])))
