package dev.constructive.eo
package data

import kernel.Distributive
import forgetful.*
import compose.*

class ModifyF[A, B](val modifier: (Fst[A], Snd[A] => B)) extends AnyVal

object ModifyF:

  given map: ForgetfulFunctor[ModifyF] with

    def map[X, B, C](fa: ModifyF[X, B], f: B => C): ModifyF[X, C] =
      val inner = fa.modifier._2
      ModifyF(fa.modifier._1, a => f(inner(a)))

  given traverse: ForgetfulTraverse[ModifyF, Distributive] with

    def traverse[X, B, C, G[_]](fa: ModifyF[X, B], g: B => G[C])(using
        D: Distributive[G]
    ): G[ModifyF[X, C]] =
      D.map(D.tupleLeft(D.distribute(fa.modifier._2)(g), fa.modifier._1))(ModifyF(_))

  private def coerceToModify[F[_, _], S, T, A, B](
      applyWrite: (S, A => B) => T
  ): optics.Optic[S, T, A, B, ModifyF] =
    new optics.Optic[S, T, A, B, ModifyF]:
      type X = (S, A)
      def to(s: S): ModifyF[X, A] = ModifyF((s, identity[A]))

      def from(sfxb: ModifyF[X, B]): T =
        val (s, f) = sfxb.modifier
        applyWrite(s, f)

  given tuple2modify: Composer[Tuple2, ModifyF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Tuple2]): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        val (xo, a) = o.to(s)
        o.from((xo, f(a)))

  given result2modify: Composer[kyo.Result, ModifyF] with

    def to[S, T, A, B](
        o: optics.Optic[S, T, A, B, kyo.Result]
    ): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        o.to(s).foldError(a => o.from(kyo.Result.succeed(f(a))), err => o.from(err))

  given affine2modify: Composer[Affine, ModifyF] with

    def to[S, T, A, B](o: optics.Optic[S, T, A, B, Affine]): optics.Optic[S, T, A, B, ModifyF] =
      coerceToModify: (s, f) =>
        o.to(s) match
          case h: Affine.Hit[o.X, A] =>
            o.from(new Affine.Hit[o.X, B](h.snd, f(h.b)))
          case m: Affine.Miss[o.X, A] =>
            o.from(m.widenB[B])

  // MultiFocus[F] → ModifyF lives in `MultiFocus.scala` (`multifocus2modify`).

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
      val xo: Fst[Xo] = xd.modifier._1.asInstanceOf[Fst[Xo]]
      val c2d: Snd[Xi] => D = xd.modifier._2.asInstanceOf[Snd[Xi] => D]
      val innerModify: A => B = a => inner.from(ModifyF((a.asInstanceOf[Fst[Xi]], c2d)))
      outer.from(ModifyF((xo, innerModify.asInstanceOf[Snd[Xo] => B])))
