package dev.constructive.eo.laws.data

import cats.{Applicative, Id}
import dev.constructive.eo.accessor.{Graft, PartialAccessor}
import dev.constructive.eo.data.{Affine, Fst, Snd}
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulFunctor, ForgetfulTraverse}

/** Carrier-level laws for `Affine[X, A]`.
  *
  * `Affine` is the carrier behind `Optional`: a sum of a "no write path" case (`Fst[X]`) and a
  * tuple-like "got the focus" case (`(Snd[X], A)`). The laws pin down its two main type-class
  * instances:
  *
  *   - `ForgetfulFunctor[Affine]` — identity and composition.
  *   - `ForgetfulTraverse[Affine, Applicative]` at `Id` — identity.
  *
  * The `AssociativeFunctor[Affine, X, Y]` instance is already exercised by `Optional ∘ Optional` at
  * the optic level (see [[dev.constructive.eo.laws.eo.OptionalComposeLaws]]); re-stating its
  * associativity equations as a standalone law class would duplicate that coverage without adding
  * signal.
  *
  * On the BUILD seam (see [[Affine.graft]]), the two arms are the decoration vocabulary — `Miss` =
  * the slot is finished (an apo graft, a futu unroll), `Hit` = keep going. The build-channel laws
  * pin the finished arm as *final*: invisible to the focus (`getOption` empty, `foldMap` empty) and
  * inert under `map` — the carrier-shaped halves of "done is final"; the per-value
  * `graft(done(t)) == t` equation is stated against concrete decoration citizens (which pin
  * `Fst[X]`), not here.
  */
trait AffineLaws[X, A]:

  /** `map(fa, identity) == fa`. Sample `Affine` values are scalacheck-generated through the
    * `forAll` calls in [[dev.constructive.eo.laws.data.discipline.AffineTests]].
    */
  def functorIdentity(fa: Affine[X, A])(using
      FF: ForgetfulFunctor[Affine]
  ): Boolean =
    FF.map(fa, identity[A]) == fa

  /** `map(map(fa, f), g) == map(fa, f andThen g)`. */
  def functorComposition(fa: Affine[X, A], f: A => A, g: A => A)(using
      FF: ForgetfulFunctor[Affine]
  ): Boolean =
    FF.map(FF.map(fa, f), g) == FF.map(fa, f.andThen(g))

  /** `traverse[Id]` is `map` — the degenerate case of the traverse identity law.
    */
  def traverseIdentity(fa: Affine[X, A])(using
      FT: ForgetfulTraverse[Affine, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](fa, a => a: Id[A])(using Applicative[Id]) ==
      fa

  // ----- Build-seam (Graft) laws — the finished arm is final ------------------------------

  /** The finished arm carries no focus. */
  def doneHasNoFocus(fst: Fst[X])(using
      G: Graft[Affine],
      P: PartialAccessor[Affine],
  ): Boolean =
    P.getOption(G.done[X, A](fst)).isEmpty

  /** The keep-going arm carries exactly its focus. */
  def stepHasFocus(snd: Snd[X], a: A)(using
      G: Graft[Affine],
      P: PartialAccessor[Affine],
  ): Boolean =
    P.getOption(G.step[X, A](snd, a)).contains(a)

  /** The finished arm is inert under `map` — finished means finished. */
  def doneMapInert(fst: Fst[X], f: A => A)(using
      G: Graft[Affine],
      FF: ForgetfulFunctor[Affine],
  ): Boolean =
    FF.map(G.done[X, A](fst), f) == G.done[X, A](fst)

  /** The finished arm contributes nothing to a fold. */
  def doneFoldEmpty(fst: Fst[X])(using
      G: Graft[Affine],
      FD: ForgetfulFold[Affine],
  ): Boolean =
    FD.foldMap[X, A, Int](_ => 1, G.done[X, A](fst)) == 0
