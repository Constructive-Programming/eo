package dev.constructive.eo
package instances

import cats.{Applicative, Bitraverse, Eval, Foldable, Functor, Representable, Traverse}

import data.MultiFocus
import optics.{Fold, ForgetFold, GetReplaceLens, Lens, Modify, Optic, Traversal}

/** Optic CONSTRUCTORS derived from cats typeclasses — construct via optic, consume via capability,
  * with the given declarations left to the client. Each `F[A]` shape admits several lawful optics
  * (whole-container traversal, positional lens, grate rebuild, …), so unlike `eo.kyo`'s
  * one-lawful-optic types nothing here can be THE canonical given for its `(F[A], A)` pair. Pick
  * the optic you mean and bind it in your own scope, either as an optic given (the capability
  * derivations do the rest) or as a direct capability instance:
  *
  * {{{
  *   import dev.constructive.eo.instances.*
  *
  *   // optic given — CanModify AND CanFold demands now resolve for the pair:
  *   given Traversal[List[Int], List[Int], Int, Int] = traverseEach
  *
  *   // or a direct capability instance, no optic at all (CanModifyP is SAM-convertible):
  *   given CanModify[Vector[Int], Int] = f => _.map(f)
  * }}}
  *
  * The menu: [[traverseEach]] (`Traverse` ⇒ Traversal — `CanModify` + `CanFold`), [[functorModify]]
  * (`Functor` ⇒ write-only Modify), [[foldableFold]] (`Foldable` ⇒ Fold), [[bitraverseFirst]] /
  * [[bitraverseSecond]] / [[bitraverseBoth]] (`Bitraverse` ⇒ slot Traversals for `Either`,
  * `Tuple2`, `Ior`, `Validated`, …), [[representableLens]] (`Representable` ⇒ a lawful positional
  * Lens at one representation point), and [[representableGrate]] (`Representable` ⇒ the
  * whole-container grate).
  *
  * Two bridges are left for the interested reader (declare them in your own scope):
  *
  *   - `Applicative[F]` ⇒ [[CanReverseGet]] — `pure` IS `reverseGet`. Ship it as a DIRECT
  *     capability given (`given [F[_]: Applicative as F, A]: CanReverseGet[F[A], A] = F.pure`), not
  *     a `Review[F[A], A]` optic given: [[CanReverseGet]]'s optic derivation searches with `S` and
  *     `A` free, so the Review would ambiguate against any element optic on the same container.
  *   - The more interesting dual: `Comonad[F]` and Review. `extract` is a lawful read (a direct
  *     [[CanGet]]), and `coflatMap` opens the door to Grate-shaped positional rebuilds — but note
  *     `extract` + `map` is NOT a lawful Lens (`replace(get(s))(s) ≠ s` on any multi-position `F`).
  */

/** `Traverse[F]` as a [[optics.Traversal]] over the elements — element-wise modify, monoidal folds
  * (`List`, `Vector`, `Option`, `Either[E, *]`, `Chain`, `Map[K, *]`, …).
  */
def traverseEach[F[_]: Traverse, A]: Traversal[F[A], F[A], A, A] =
  Traversal.each[F, A]

/** `Functor[F]` as a write-only [[optics.Modify]]: `modify = F.map` (`Function1[R, *]`, `Eval`, …:
  * mappable containers that offer no fold).
  */
def functorModify[F[_], A](using F: Functor[F]): Modify[F[A], F[A], A, A] =
  Modify(f => fa => F.map(fa)(f))

/** `Foldable[F]` as a read-only [[optics.Fold]] over the elements (`SortedSet`, …). */
def foldableFold[F[_]: Foldable, A]: ForgetFold[F[A], F, A] =
  Fold[F, A]

/** `Bitraverse[F]` as a [[optics.Traversal]] over the FIRST slot — the other slot rides along
  * untouched (`Either`'s left, `Tuple2._1`, `Ior`'s left, `Validated`'s invalid, …).
  */
def bitraverseFirst[F[_, _], A, B, C](using BT: Bitraverse[F]): Traversal[F[A, C], F[B, C], A, B] =
  Traversal.pEach[[x] =>> F[x, C], A, B](using
    new Traverse[[x] =>> F[x, C]]:
      def traverse[G[_]: Applicative, A1, B1](fa: F[A1, C])(f: A1 => G[B1]): G[F[B1, C]] =
        BT.bitraverse(fa)(f, Applicative[G].pure)
      def foldLeft[A1, B1](fa: F[A1, C], b: B1)(f: (B1, A1) => B1): B1 =
        BT.bifoldLeft(fa, b)(f, (b1, _) => b1)
      def foldRight[A1, B1](fa: F[A1, C], lb: Eval[B1])(f: (A1, Eval[B1]) => Eval[B1]): Eval[B1] =
        BT.bifoldRight(fa, lb)(f, (_, lb1) => lb1)
  )

/** [[bitraverseFirst]]'s twin over the SECOND slot (`Either`'s right, `Tuple2._2`, …). */
def bitraverseSecond[F[_, _], A, B, C](using BT: Bitraverse[F]): Traversal[F[C, A], F[C, B], A, B] =
  Traversal.pEach[[x] =>> F[C, x], A, B](using
    new Traverse[[x] =>> F[C, x]]:
      def traverse[G[_]: Applicative, A1, B1](fa: F[C, A1])(f: A1 => G[B1]): G[F[C, B1]] =
        BT.bitraverse(fa)(Applicative[G].pure, f)
      def foldLeft[A1, B1](fa: F[C, A1], b: B1)(f: (B1, A1) => B1): B1 =
        BT.bifoldLeft(fa, b)((b1, _) => b1, f)
      def foldRight[A1, B1](fa: F[C, A1], lb: Eval[B1])(f: (A1, Eval[B1]) => Eval[B1]): Eval[B1] =
        BT.bifoldRight(fa, lb)((_, lb1) => lb1, f)
  )

/** `Bitraverse[F]` at a single element type, both slots — every `A` position of an `F[A, A]`
  * (`Ior[A, A]`, `(A, A)`, `Validated[A, A]`: fold or rewrite all of them in one pass).
  */
def bitraverseBoth[F[_, _], A, B](using BT: Bitraverse[F]): Traversal[F[A, A], F[B, B], A, B] =
  Traversal.pEach[[x] =>> F[x, x], A, B](using
    new Traverse[[x] =>> F[x, x]]:
      def traverse[G[_]: Applicative, A1, B1](fa: F[A1, A1])(f: A1 => G[B1]): G[F[B1, B1]] =
        BT.bitraverse(fa)(f, f)
      def foldLeft[A1, B1](fa: F[A1, A1], b: B1)(f: (B1, A1) => B1): B1 =
        BT.bifoldLeft(fa, b)(f, f)
      def foldRight[A1, B1](fa: F[A1, A1], lb: Eval[B1])(f: (A1, Eval[B1]) => Eval[B1]): Eval[B1] =
        BT.bifoldRight(fa, lb)(f, f)
  )

/** `Representable[F]` (`F[A] ≅ Representation => A`) as a lawful [[optics.Lens]] into ONE position:
  * `get = index(fa)(r)`, and the write rebuilds via `tabulate` with every other position read back
  * from the original — siblings survive, all three Lens laws hold. The one thing none of the
  * element bridges can produce: a Lens into a function's value at a point, or position `r` of any
  * tabulated shape.
  *
  * Lawful provided `==` is meaningful on `Representation` (it keys the rebuild).
  */
def representableLens[F[_], A](using
    R: Representable[F]
)(r: R.Representation): GetReplaceLens[F[A], F[A], A, A] =
  Lens[F[A], A](
    fa => R.index(fa)(r),
    (fa, a) => R.tabulate(x => if x == r then a else R.index(fa)(x)),
  )

/** `Representable[F]` as the whole-container grate — eo's grate-absorbed
  * `MultiFocus[Function1[Representation, *]]` carrier ([[data.MultiFocus.representable]]):
  * `modify = F.map`, `replace` broadcasts, and positional rebuilds compose through the
  * Function1-shaped kernel. This is the cats-side strengthening of `Distributive` (a `Distributive`
  * with concrete `Representation`); eo's carrier needs `index`/`tabulate`, which is why the
  * constructor keys on `Representable` rather than `Distributive` itself.
  */
def representableGrate[F[_], A](using
    R: Representable[F]
): Optic[F[A], F[A], A, A, MultiFocus[Function1[R.Representation, *]]] =
  MultiFocus.representable[F, A]
