package dev.constructive.eo
package data

import cats.Distributive
import cats.instances.function.*
import cats.syntax.functor.*

import forgetful.*
import compose.*

/** Carrier for the `Modify` family — pairs a source `Fst[A]` with a continuation `Snd[A] => B`.
  *
  * Same-carrier composition (`modify.andThen(modify)`) ships via [[ModifyF.assocModifyF]] —
  * `AssociativeFunctor[ModifyF, Xo, Xi]` with `type Z = (Fst[Xo], Snd[Xi])`. The deferred-modify
  * semantic fits the protocol once you observe that `composeTo` only needs to seed `(xo, identity)`
  * (no inner-`to` call required, since ModifyF's continuation is structurally identity at every
  * canonical construction site — `coerceToModify` and `Modify.apply`); `composeFrom` then extracts
  * the user's `c2d` from the mapped continuation and routes it through `inner.from` then
  * `outer.from`. The asInstanceOf casts inside the instance are sound under the universal
  * convention that every ModifyF optic stores `X = (S_outer, A_focus)` (enforced at every
  * construction site).
  *
  * @tparam A
  *   existential leftover tuple
  * @tparam B
  *   focus written back
  */
class ModifyF[A, B](val modifier: (Fst[A], Snd[A] => B)) extends AnyVal

/** Typeclass instances for [[ModifyF]]. */
object ModifyF:

  /** `ForgetfulFunctor[ModifyF]` — maps the continuation through `f`, leaving the source unchanged.
    * Unlocks `.modify` / `.replace` on Modify-carrier optics.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[ModifyF] with

    def map[X, B, C](fa: ModifyF[X, B], f: B => C): ModifyF[X, C] =
      val inner = fa.modifier._2
      ModifyF(fa.modifier._1, a => f(inner(a)))

  /** `ForgetfulTraverse[ModifyF, Distributive]` — lifts an effectful `B => G[C]` through the
    * continuation under `Distributive[G]` (the right shape for read-once / write-once Modify).
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[ModifyF, Distributive] with

    def traverse[X, B, C, G[_]](fa: ModifyF[X, B], g: B => G[C])(using
        D: Distributive[G]
    ): G[ModifyF[X, C]] =
      D.tupleLeft(D.distribute(fa.modifier._2)(g), fa.modifier._1).map(ModifyF(_))

  /** Shared skeleton for `Composer[F, ModifyF]` instances. Every carrier materialises a coerced
    * Optic with the same `type X = (S, A)` shape and an identity `to` that seeds the ModifyF; only
    * `applyWrite` differs per carrier.
    */
  private def coerceToModify[F[_, _], S, T, A, B](
      applyWrite: (S, A => B) => T
  ): optics.Optic[S, T, A, B, ModifyF] =
    new optics.Optic[S, T, A, B, ModifyF]:
      type X = (S, A)
      def to(s: S): ModifyF[X, A] = ModifyF((s, identity[A]))

      def from(sfxb: ModifyF[X, B]): T =
        val (s, f) = sfxb.modifier
        applyWrite(s, f)

  /** Lens → ModifyF. Every Lens is-a Modify; powers cross-carrier `lens.andThen(modify)` via
    * `Morph[Tuple2, ModifyF]`.
    *
    * @group Instances
    */
  given tuple2modify: Composer[Tuple2, ModifyF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Tuple2]): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        val (xo, a) = o.to(s)
        o.from((xo, f(a)))

  /** Prism → ModifyF. Hit writes `f(a)` through the Prism's build path; miss passes the leftover
    * back via `o.from(Left(xo))` — observably the same as the Prism's own `.modify(f)`.
    *
    * @group Instances
    */
  given either2modify: Composer[Either, ModifyF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Either]): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        o.to(s) match
          case Right(a)    => o.from(Right(f(a)))
          case l @ Left(_) => o.from(l.widenRight[B])

  /** Optional → ModifyF. Mirror of [[either2modify]] split across `Affine.Hit` / `Affine.Miss`;
    * miss passes through directly (`Miss` is `Affine[A, Nothing]`, covariance retypes it for free).
    *
    * @group Instances
    */
  given affine2modify: Composer[Affine, ModifyF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Affine]): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        o.to(s) match
          case h: Affine.Hit[o.X, A] =>
            o.from(new Affine.Hit[o.X, B](h.snd, f(h.b)))
          case m: Affine.Miss[o.X] =>
            o.from(m)

  // MultiFocus[F] → ModifyF lives in `MultiFocus.scala` (`multifocus2modify`).

  /** Same-carrier composition for `Optic[…, ModifyF]` — `modify.andThen(modify)`.
    *
    * Encoding: `Z = (Fst[Xo], Snd[Xi])`. `composeTo` seeds the ModifyF with `(outer-source,
    * identity[C])` — no `inner.to` call needed because ModifyF's continuation is structurally
    * `identity[C]` at every canonical construction site (every bundled ModifyF optic uses
    * [[coerceToModify]] or [[optics.Modify.apply]], both of which seed identity). `composeFrom`
    * extracts the user's `c2d: C => D` from the post-`map` continuation and applies it through
    * `inner.from` then `outer.from`, matching the deferred-modify semantic
    * `composedModify(c2d)(s) = outer.modify(inner.modify(c2d))(s)`.
    *
    * The three remaining asInstanceOf casts coerce abstract `Snd[Xo] / Fst[Xi] / Snd[Xi]` to the
    * canonical `(S, A)` / `(A, C)` decomposition. Sound under the universal ModifyF convention
    * (`type X = (S, A)` at every construction site — `coerceToModify` and `Modify.apply`), unsafe
    * only for hand-built ModifyF optics that violate it — and there's no public API path to build
    * such an optic. They are IRREDUCIBLE under the `AssociativeFunctor` SPI: the instance
    * quantifies `Xo` / `Xi` independently of `composeTo`/`composeFrom`'s method-level `S..D`, so
    * the convention's tie (`Snd[Xi] = C`, `Fst[Xi] = A`, `Snd[Xo] = A`) has no type-level channel.
    * (The `Z` projections DO reduce — `Z` is a concrete Tuple2 — see `composeFrom`.)
    *
    * @group Instances
    */
  given assocModifyF[Xo, Xi]: AssociativeFunctor[ModifyF, Xo, Xi] with
    type Z = (Fst[Xo], Snd[Xi])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: optics.Optic[S, T, A, B, ModifyF] { type X = Xo },
        inner: optics.Optic[A, B, C, D, ModifyF] { type X = Xi },
    ): ModifyF[Z, C] =
      val xo: Fst[Xo] = outer.to(s).modifier._1
      ModifyF((xo, identity[C].asInstanceOf[Snd[Xi] => C]))

    def composeFrom[S, T, A, B, C, D](
        xd: ModifyF[Z, D],
        inner: optics.Optic[A, B, C, D, ModifyF] { type X = Xi },
        outer: optics.Optic[S, T, A, B, ModifyF] { type X = Xo },
    ): T =
      // `Z = (Fst[Xo], Snd[Xi])` is a concrete Tuple2 type, so `Fst[Z]` / `Snd[Z]` REDUCE —
      // both projections are compiler-typed, no coercion.
      val xo: Fst[Xo] = xd.modifier._1
      val c2d: Snd[Xi] => D = xd.modifier._2
      val innerModify: A => B = a => inner.from(ModifyF((a.asInstanceOf[Fst[Xi]], c2d)))
      outer.from(ModifyF((xo, innerModify.asInstanceOf[Snd[Xo] => B])))
