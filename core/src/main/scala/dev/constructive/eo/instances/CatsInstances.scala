package dev.constructive.eo
package instances

import scala.util.NotGiven

import cats.{Foldable, Functor, Traverse}

import optics.{Fold, ForgetFold, Modify, Traversal}

/** Optic givens derived from cats typeclasses — `import dev.constructive.eo.instances.given` and
  * any `F[A]` with a cats instance satisfies the matching eo capability demands:
  *
  *   - `Traverse[F]` ⇒ a [[optics.Traversal]] over the elements — [[CanModify]] AND [[CanFold]]
  *     (`List`, `Vector`, `Option`, `Either[E, *]`, `Chain`, `Map[K, *]`, …).
  *   - `Functor[F]` without `Traverse` ⇒ a write-only [[optics.Modify]] — [[CanModify]] only
  *     (`Function1[R, *]`, `Eval`, …: mappable but not foldable into view).
  *   - `Foldable[F]` without `Traverse` ⇒ a [[optics.Fold]] — [[CanFold]] only (`SortedSet`, …:
  *     foldable but not mappable without extra evidence).
  *
  * The `NotGiven[Traverse[F]]` guards keep the coherence doctrine intact: exactly ONE optic given
  * per `(F[A], A)` pair, with the strongest available cats class electing it. The rare `F` with
  * both `Functor` and `Foldable` but no `Traverse` still gets two competing givens — summon the one
  * you mean explicitly there.
  *
  * Two bridges are left for the interested reader (declare them in your own scope):
  *
  *   - `Applicative[F]` ⇒ [[CanReverseGet]] — `pure` IS `reverseGet`. Ship it as a DIRECT
  *     capability given (`given [F[_]: Applicative as F, A]: CanReverseGet[F[A], A] = F.pure`), not
  *     a `Review[F[A], A]` optic given: [[CanReverseGet]]'s optic derivation searches with `S` and
  *     `A` free, so the Review would ambiguate against [[traverseEach]] on every `Traverse ∧
  *     Applicative` container — `List` and `Option`, exactly the types `pure` matters for.
  *   - The more interesting dual: `Comonad[F]` and Review. `extract` is a lawful read (a direct
  *     [[CanGet]]), and `coflatMap` opens the door to Grate-shaped positional rebuilds — but note
  *     `extract` + `map` is NOT a lawful Lens (`replace(get(s))(s) ≠ s` on any multi-position `F`),
  *     which is why neither ships here by default.
  */

/** `Traverse[F]` elects the full [[optics.Traversal]] — element-wise modify, monoidal folds. */
given traverseEach[F[_]: Traverse, A]: Traversal[F[A], F[A], A, A] =
  Traversal.each[F, A]

/** `Functor[F]` (with no `Traverse` to outrank it) is exactly a write-only [[optics.Modify]]:
  * `modify = F.map`.
  */
given functorModify[F[_], A](using
    F: Functor[F],
    ng: NotGiven[Traverse[F]],
): Modify[F[A], F[A], A, A] =
  Modify(f => fa => F.map(fa)(f))

/** `Foldable[F]` (with no `Traverse` to outrank it) is exactly a read-only [[optics.Fold]] over the
  * elements.
  */
given foldableFold[F[_], A](using
    F: Foldable[F],
    ng: NotGiven[Traverse[F]],
): ForgetFold[F[A], F, A] =
  Fold[F, A]
