package dev.constructive.eo
package data

import cats.Distributive
import cats.instances.function.*
import cats.syntax.functor.*

/** Carrier for the `Setter` family — pairs a source `Fst[A]` with a continuation `Snd[A] => B` that
  * decides what to do with the focus when it's written back.
  *
  * Like [[Affine]], SetterF's `A` is encoded as a `Tuple2` that threads both halves of the write
  * path together. SetterF has no `AssociativeFunctor` instance: composing two `SetterF` optics via
  * `Optic.andThen` is not yet supported. Compose a Lens chain in `Tuple2` and reach for SetterF
  * only at the leaf.
  *
  * @tparam A
  *   existential leftover tuple — `Fst[A]` / `Snd[A]` reduce when `A` is a concrete `Tuple2`.
  * @tparam B
  *   focus written back through the continuation.
  */
class SetterF[A, B](val setter: (Fst[A], Snd[A] => B)) extends AnyVal

/** Typeclass instances for [[SetterF]]. */
object SetterF:

  /** `ForgetfulFunctor[SetterF]` — maps the right-side continuation through `f`, leaving the source
    * unchanged. Unlocks `.modify` and `.replace` on Setter-carrier optics.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[SetterF] with

    def map[X, B, C](fa: SetterF[X, B], f: B => C): SetterF[X, C] =
      val inner = fa.setter._2
      SetterF(fa.setter._1, a => f(inner(a)))

  /** `ForgetfulTraverse[SetterF, Distributive]` — lifts an effectful `B => G[C]` through the
    * continuation using `Distributive[G]` (a stronger counterpart to `Applicative` that suits the
    * read-once / write-once Setter shape).
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[SetterF, Distributive] with

    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] =
      s => g => D.tupleLeft(D.distribute(s.setter._2)(g), s.setter._1).map(SetterF(_))

  /** Shared skeleton for the `Composer[F, SetterF]` instances below. Every one of the four
    * carriers (`Tuple2`, `Either`, `Affine`, `PowerSeries`) materialises a coerced
    * `Optic[S, T, A, B, SetterF]` with the same shape — `type X = (S, A)`, the same identity
    * `to` ("seed the SetterF with the original `s` and the identity focus-fn"), and a `from`
    * that delegates the per-carrier focus rewrite to `applyWrite`.
    *
    * Routing through this helper collapses the four near-identical bodies that previously sat
    * in each `Composer` instance. Not `inline` — anonymous-class definitions in inline bodies
    * trigger `-Werror`-fatal "duplicated at each inline site" warnings, and the per-carrier
    * call-site allocates the same single anonymous-`Optic` instance either way.
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

  /** Coerce any Tuple2-carrier optic (typically a Lens) into a SetterF optic. Every Lens is-a
    * Setter: modify is `setter.from(SetterF(s, f))`, which re-runs the Lens's get/replace path with
    * `f` applied to the focus. Powers cross-carrier `lens.andThen(setter)`, via
    * `Morph[Tuple2, SetterF]`.
    *
    * @group Instances
    */
  given tuple2setter: Composer[Tuple2, SetterF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Tuple2]): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        val (xo, a) = o.to(s)
        o.from((xo, f(a)))

  /** Coerce an `Either`-carrier optic (Prism) into a SetterF optic. Hit branch: write `f(a)`
    * through the Prism's build path. Miss branch: pass the original leftover back through the
    * Prism's miss reconstruction — `o.from(Left(xo))`. Both halves are observably the same as the
    * original Prism's `.modify(f)`, just with a Setter-shaped read API that doesn't expose the
    * hit/miss structure to downstream callers.
    *
    * Resolves Gap-1 (Prism × Setter) for cross-library `eo Prism + monocle Setter` chains in
    * `eo-monocle` Unit 6.
    *
    * @group Instances
    */
  given either2setter: Composer[Either, SetterF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Either]): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        o.to(s) match
          case Right(a) => o.from(Right(f(a)))
          case Left(xo) => o.from(Left(xo))

  /** Coerce an `Affine`-carrier optic (Optional) into a SetterF optic. Same shape as
    * [[either2setter]] split across the explicit `Affine.Hit` / `Affine.Miss` constructors so the
    * miss path uses [[Affine.Miss.widenB widenB]] rather than allocating a fresh `Miss`.
    *
    * Resolves Gap-1 (Optional × Setter) for cross-library `eo Optional + monocle Setter` chains.
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

  /** Coerce a `PowerSeries`-carrier optic (Traversal.each) into a SetterF optic. The Setter's
    * `modify` applies the focus function to *every* candidate in the traversal — equivalent to
    * `traversal.modify(f)` on the original optic. Implementation routes through
    * `ForgetfulFunctor[PowerSeries].map`, which is the same map the `Optic.modify` extension uses
    * for PowerSeries-carrier optics.
    *
    * Resolves Gap-1 (Traversal × Setter) for cross-library `eo Traversal + monocle Setter` chains.
    *
    * @group Instances
    */
  given powerseries2setter: Composer[PowerSeries, SetterF] with

    def to[S, T, A, B](
        o: optics.Optic[S, T, A, B, PowerSeries]
    ): optics.Optic[S, T, A, B, SetterF] =
      coerceToSetter: (s, f) =>
        o.from(summon[ForgetfulFunctor[PowerSeries]].map(o.to(s), f))
