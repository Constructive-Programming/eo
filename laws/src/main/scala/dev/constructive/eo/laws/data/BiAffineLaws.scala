package dev.constructive.eo.laws.data

import cats.{Applicative, Id}
import dev.constructive.eo.accessor.{Graft, PartialAccessor}
import dev.constructive.eo.data.{BiAffine, Fst, Snd}
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulFunctor, ForgetfulTraverse}

/** Carrier-level laws for `BiAffine[X, A]` — the decoration carrier of the recursion-scheme zoo
  * (`Affine`'s data shape worn on the build seam: `Done` = finished, `Step` = keep going).
  *
  * Two groups:
  *
  *   - Instance laws mirroring [[AffineLaws]]: `ForgetfulFunctor` identity / composition,
  *     `ForgetfulTraverse` at `Id`.
  *   - Graft-channel laws: the `Done` arm is *final* — invisible to the focus (`getOption` empty,
  *     `foldMap` empty) and inert under `map` — while `Step` carries the focus. These are the
  *     carrier-shaped halves of D1's "Done is final"; the per-value `graft(Done(t)) == t` equation
  *     is stated against concrete `Gather`/`Scatter` citizens (which pin `Fst[X]`), not here.
  *
  * The `AssociativeFunctor[BiAffine]` coherence laws are deliberately absent — the carrier ships
  * without its composition-matrix row (follow-up PR), so there is no `andThen` for them to govern.
  */
trait BiAffineLaws[X, A]:

  def functorIdentity(fa: BiAffine[X, A])(using
      FF: ForgetfulFunctor[BiAffine]
  ): Boolean =
    FF.map(fa, identity[A]) == fa

  def functorComposition(fa: BiAffine[X, A], f: A => A, g: A => A)(using
      FF: ForgetfulFunctor[BiAffine]
  ): Boolean =
    FF.map(FF.map(fa, f), g) == FF.map(fa, f.andThen(g))

  /** `traverse[Id]` is `map` — the degenerate case of the traverse identity law. */
  def traverseIdentity(fa: BiAffine[X, A])(using
      FT: ForgetfulTraverse[BiAffine, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](fa, a => a: Id[A])(using Applicative[Id]) ==
      fa

  /** The finished arm carries no focus. */
  def doneHasNoFocus(fst: Fst[X])(using
      G: Graft[BiAffine],
      P: PartialAccessor[BiAffine],
  ): Boolean =
    P.getOption(G.done[X, A](fst)).isEmpty

  /** The keep-going arm carries exactly its focus. */
  def stepHasFocus(snd: Snd[X], a: A)(using
      G: Graft[BiAffine],
      P: PartialAccessor[BiAffine],
  ): Boolean =
    P.getOption(G.step[X, A](snd, a)).contains(a)

  /** `Done` is inert under `map` — finished means finished. */
  def doneMapInert(fst: Fst[X], f: A => A)(using
      G: Graft[BiAffine],
      FF: ForgetfulFunctor[BiAffine],
  ): Boolean =
    FF.map(G.done[X, A](fst), f) == G.done[X, A](fst)

  /** `Done` contributes nothing to a fold. */
  def doneFoldEmpty(fst: Fst[X])(using
      G: Graft[BiAffine],
      FD: ForgetfulFold[BiAffine],
  ): Boolean =
    FD.foldMap[X, A, Int](_ => 1, G.done[X, A](fst)) == 0
