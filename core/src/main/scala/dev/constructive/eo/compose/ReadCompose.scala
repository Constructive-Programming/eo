package dev.constructive.eo
package compose

import cats.{Foldable, Functor, FunctorFilter, Monoid}

import forgetful.ForgetfulFold
import accessor.{Accessor, PartialAccessor}
import data.Forget
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
  *   Fold[F] ∘ total   = Fold[F]       (F has a cats Functor: the fold stays in its container)
  *   Fold[F] ∘ partial = Fold[F]       (F has a cats FunctorFilter: an inner miss is F's empty)
  *   many on either side = Fold        (ForgetfulFold fallback, List-backed)
  * }}}
  *
  * The two `Fold[F]` cells keep the outer fold's container through single-focus inners — a fold
  * into `Option`, `Either[E, *]` or an error-carrying result type stays one, with the inner read
  * applied inside it, instead of being flattened to a `List` of foci.
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
object ReadCompose extends MidPriorityReadCompose:

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
      val ro = totalRead(outer, AO)
      val ri = totalRead(inner, AI)
      Getter(s => ri(ro(s)))

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
      val ro = totalRead(outer, AO)
      val ri = partialRead(inner, PI)
      PickFold(s => ri(ro(s)))

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
      val ro = partialRead(outer, PO)
      val ri = totalRead(inner, AI)
      PickFold(s => ro(s).map(ri))

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
      val ro = partialRead(outer, PO)
      val ri = partialRead(inner, PI)
      PickFold(s => ro(s).flatMap(ri))

  /** `Fold[F]` ∘ total → [[optics.ForgetFold]] over the same `F`: `Functor.map` the inner read over
    * the outer's foci, keeping the container (an `Option` fold stays an `Option`, an `Either[E, *]`
    * fold keeps its `Left`). @group Instances
    */
  given forgetTotal[F[_], G[_, _]](using
      AI: Accessor[G],
      FN: Functor[F],
      FF: Foldable[F],
  ): ReadCompose[Forget[F], G] with
    type Out[S, C] = ForgetFold[S, F, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, Forget[F]],
        inner: Optic[A, V, C, D, G],
    ): ForgetFold[S, F, C] =
      val ro = forgetRead(outer)
      val ri = totalRead(inner, AI)
      new ForgetFold[S, F, C](s => FN.map(ro(s))(ri))

/** The total read of `o`, resolved once at composition time: its own `get` when it carries one
  * ([[CanGet]] — every concrete total class: `Getter`, `BijectionIso`, `GetReplaceLens`,
  * `SplitCombineLens`, …), else the carrier read `acc.get(o.to(s))`. The direct `get` skips the
  * carrier value `to` builds — for a split-combine lens (eo-generics' `lens[S](_.field)`) that is a
  * `(complement, focus)` tuple per read, which a read-only composite immediately throws away.
  */
private def totalRead[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F], acc: Accessor[F]): S => A =
  o match
    case g: CanGet[S @unchecked, A @unchecked] => g.get
    case _                                     => s => acc.get(o.to(s))

/** [[totalRead]]'s partial twin: `o`'s own `getOption` when it is a [[CanGetOption]], else the
  * carrier read.
  */
private def partialRead[S, T, A, B, F[_, _]](
    o: Optic[S, T, A, B, F],
    acc: PartialAccessor[F],
): S => Option[A] =
  o match
    case g: CanGetOption[S @unchecked, A @unchecked] => g.getOption
    case _                                           => s => acc.getOption(o.to(s))

/** The outer fold's read, `S => F[A]`: a [[optics.ForgetFold]]'s own `read`, else its carrier's
  * value.
  */
private def forgetRead[S, T, A, B, F[_]](o: Optic[S, T, A, B, Forget[F]]): S => F[A] =
  o match
    case f: ForgetFold[S @unchecked, F @unchecked, A @unchecked] => f.read
    case _                                                       => s => o.to(s).value

/** Middle drawer for [[ReadCompose]]: a fold ∘ partial inner that stays in the fold's container.
  * Below the total cell so a carrier with both an `Accessor` and a `PartialAccessor` takes the
  * total read; above the `List` fallback so a container with a `FunctorFilter` keeps its type.
  */
trait MidPriorityReadCompose extends LowPriorityReadCompose:

  /** `Fold[F]` ∘ partial → [[optics.ForgetFold]] over the same `F`: `FunctorFilter.mapFilter` the
    * inner `getOption` over the outer's foci, so an inner miss is `F`'s own empty (`None`, an empty
    * `List`, an error-carrying result's "empty value"). @group Instances
    */
  given forgetPartial[F[_], G[_, _]](using
      PI: PartialAccessor[G],
      FFi: FunctorFilter[F],
      FF: Foldable[F],
  ): ReadCompose[Forget[F], G] with
    type Out[S, C] = ForgetFold[S, F, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, Forget[F]],
        inner: Optic[A, V, C, D, G],
    ): ForgetFold[S, F, C] =
      val ro = forgetRead(outer)
      val ri = partialRead(inner, PI)
      new ForgetFold[S, F, C](s => FFi.mapFilter(ro(s))(ri))

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
