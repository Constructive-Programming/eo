package dev.constructive.eo

import cats.Monoid

import data.{Accessor, PartialAccessor}
import optics.{ForgetFold, Getter, Optic, PickFold}

/** Read-side composition across the read/write seam — the join that `AssociativeFunctor` (which
  * needs B-threading on a *shared* carrier) structurally cannot express. Composes the READ halves
  * of two optics, ignoring both write sides, and lands at the read-only join of their strengths:
  *
  * {{{
  *   total   ∘ total   = Getter        (Accessor ∘ Accessor)
  *   total   ∘ partial = AffineFold    (any mix involving a PartialAccessor)
  *   partial ∘ total   = AffineFold
  *   partial ∘ partial = AffineFold
  *   many on either side = Fold        (ForgetfulFold fallback, List-backed)
  * }}}
  *
  * Drives the read-collapse `andThen` members: the `Optic` trait's *any-outer ∘ read-only-inner*
  * overload and the *read-only-outer ∘ any-inner* members on [[optics.Getter]] /
  * [[optics.PickFold]] / [[optics.ForgetFold]]. Instances live in this companion, so they are in
  * implicit scope for every carrier pair with no import.
  *
  * @tparam F
  *   outer optic's carrier
  * @tparam G
  *   inner optic's carrier
  */
trait ReadCompose[F[_, _], G[_, _]]:
  /** Concrete read-only optic family the composite lands in — `Getter`, `PickFold`, or
    * `ForgetFold[*, List, *]`; concrete so collapsed chains keep collapsing through the fused
    * members.
    */
  type Out[_, _]

  def compose[S, T, A, B, V, C, D](
      outer: Optic[S, T, A, B, F],
      inner: Optic[A, V, C, D, G],
  ): Out[S, C]

/** Typeclass instances for [[ReadCompose]] — the read-arity lattice. The four total/partial cells
  * sit at normal priority; the many-fold fallback sits in [[LowPriorityReadCompose]] because
  * carriers with an `Accessor` / `PartialAccessor` usually admit a `ForgetfulFold` too, and the
  * stronger read must win.
  */
object ReadCompose extends LowPriorityReadCompose:

  /** total ∘ total → [[optics.Getter]]. @group Instances */
  given totalTotal[F[_, _], G[_, _]](using
      AO: Accessor[F],
      AI: Accessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = Getter[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): Getter[S, C] =
      Getter(s => AI.get(inner.to(AO.get(outer.to(s)))))

  /** total ∘ partial → [[optics.PickFold]]. @group Instances */
  given totalPartial[F[_, _], G[_, _]](using
      AO: Accessor[F],
      PI: PartialAccessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PI.getOption(inner.to(AO.get(outer.to(s)))))

  /** partial ∘ total → [[optics.PickFold]]. @group Instances */
  given partialTotal[F[_, _], G[_, _]](using
      PO: PartialAccessor[F],
      AI: Accessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PO.getOption(outer.to(s)).map(a => AI.get(inner.to(a))))

  /** partial ∘ partial → [[optics.PickFold]]. @group Instances */
  given partialPartial[F[_, _], G[_, _]](using
      PO: PartialAccessor[F],
      PI: PartialAccessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PO.getOption(outer.to(s)).flatMap(a => PI.getOption(inner.to(a))))

/** Low-priority drawer for [[ReadCompose]] — the many-fold fallback. */
trait LowPriorityReadCompose:

  /** Anything foldable ∘ anything foldable → [[optics.ForgetFold]] over `List`. Fires only when no
    * total/partial instance applies, i.e. at least one side is genuinely multi-focus
    * (`MultiFocus[F]` / `Forget[F]`).
    *
    * @group Instances
    */
  given foldFold[F[_, _], G[_, _]](using
      FF: ForgetfulFold[F],
      FG: ForgetfulFold[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = ForgetFold[S, List, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): ForgetFold[S, List, C] =
      new ForgetFold[S, List, C](s =>
        FF.foldMap(
          (a: A) => FG.foldMap((c: C) => c :: Nil, inner.to(a))(using Monoid[List[C]]),
          outer.to(s),
        )(using Monoid[List[C]])
      )
