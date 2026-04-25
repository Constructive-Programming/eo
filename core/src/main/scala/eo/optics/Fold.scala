package eo
package optics

import cats.Foldable
import eo.data.Forget

/** Constructors for `Fold` — the read-only multi-focus optic, backed by the `Forget[F]` carrier
  * (`type Forget[F] = [X, A] =>> F[A]`).
  *
  * A `Fold[F, A]` walks every element of a `Foldable[F]` and exposes them through `foldMap[M:
  * Monoid]`. `Fold.select(p)` narrows the walk to the single-element `Option` stream of values
  * matching the predicate.
  *
  * Like [[Getter]], Fold has `T = Unit` so it does not write — the sole consumption path is
  * `foldMap`, available through [[Optic.foldMap]].
  */
object Fold:

  /** Construct a Fold over any `Foldable[F]`. The focus type is the element type `A`;
    * `.foldMap(g)(s)` combines every `A` in `s: F[A]` via `Monoid[M]`.
    *
    * @group Constructors
    * @tparam F
    *   container type constructor — must admit `Foldable[F]`
    * @tparam A
    *   element type being folded
    *
    * @example
    *   {{{
    * import cats.instances.list.given
    * val listFold = Fold[List, Int]
    * listFold.foldMap(identity[Int])(List(1, 2, 3))   // 6
    *   }}}
    */
  def apply[F[_], A](using
      @scala.annotation.unused ev: Foldable[F]
  ): Optic[F[A], Unit, A, A, Forget[F]] =
    // `Foldable[F]` not used in the body: the Forget[F] carrier's
    // ForgetfulFold instance (summoned at the call site via `.foldMap`)
    // is what does the actual work. Bound is kept as API documentation
    // ("Fold only makes sense when F is Foldable").
    new Optic[F[A], Unit, A, A, Forget[F]]:
      type X = Nothing
      val to: F[A] => F[A] = identity
      val from: F[A] => Unit = _ => ()

  /** Filtering Fold — keeps only elements matching `p`. Backed by `Forget[Option]`, so
    * `.foldMap(f)(a)` produces `f(a)` when the predicate holds and `Monoid[M].empty` when it
    * doesn't.
    *
    * @group Constructors
    * @tparam A
    *   element type being filtered
    */
  def select[A](p: A => Boolean): Optic[A, Unit, A, A, Forget[Option]] =
    new Optic[A, Unit, A, A, Forget[Option]]:
      type X = Nothing
      val to: A => Option[A] = a => Option(a).filter(p)
      val from: Option[A] => Unit = _ => ()
