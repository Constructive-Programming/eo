package dev.constructive.eo
package instances

import scala.util.NotGiven

import cats.{Applicative, Foldable, Functor, Traverse}

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
  * `Applicative[F]` additionally provides [[CanReverseGet]] (`reverseGet = F.pure`) — as a DIRECT
  * capability given, not a `Review` optic given: [[CanReverseGet]]'s optic derivation searches with
  * `S` and `A` free, so a `Review[F[A], A]` given would ambiguate against [[traverseEach]] on every
  * `Traverse ∧ Applicative` container. The direct given sidesteps the optic search and coexists
  * with the element optics above.
  *
  * Deliberately absent: `Comonad` ⇒ Getter (its `extract` + `map` pair is not a lawful Lens —
  * `replace(get(s))(s) ≠ s` for any multi-position `F`).
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

/** `Applicative[F]` is exactly a build: `reverseGet = F.pure`. Direct capability given (see the
  * package note for why it must not be a `Review` optic given).
  */
given applicativePure[F[_], A](using F: Applicative[F]): CanReverseGet[F[A], A] =
  a => F.pure(a)
