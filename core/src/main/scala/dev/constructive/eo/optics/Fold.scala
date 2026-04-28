package dev.constructive.eo
package optics

import cats.Foldable
import dev.constructive.eo.data.Forget

/** Constructors for `Fold` — read-only multi-focus optic, backed by `Forget[F]` (`Forget[F][X, A] =
  * F[A]`). `T = Unit` rules out the write path; `.foldMap` is the consumption surface.
  * `Fold.select(p)` narrows to a one-element `Option` stream.
  */
object Fold:

  /** Fold over any `Foldable[F]`.
    *
    * @group Constructors
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
    // `Foldable[F]` is documentation; `.foldMap` summons `ForgetfulFold[Forget[F]]` at use site.
    new Optic[F[A], Unit, A, A, Forget[F]]:
      type X = Nothing
      val to: F[A] => F[A] = identity
      val from: F[A] => Unit = _ => ()

  /** Filtering Fold — backed by `Forget[Option]`. @group Constructors */
  def select[A](p: A => Boolean): Optic[A, Unit, A, A, Forget[Option]] =
    new Optic[A, Unit, A, A, Forget[Option]]:
      type X = Nothing
      val to: A => Option[A] = a => Option(a).filter(p)
      val from: Option[A] => Unit = _ => ()
