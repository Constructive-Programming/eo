package dev.constructive.eo
package optics

import cats.instances.option.given
import cats.{Foldable, Monoid}
import dev.constructive.eo.data.Forget

/** Constructors for `Fold` — read-only multi-focus optic, backed by `Forget[F]` (`Forget[F][X, A] =
  * F[A]`). `T = Unit` rules out the write path; `.foldMap` is the consumption surface.
  * `Fold.select(p)` narrows to a one-element `Option` stream.
  *
  * Both constructors return the concrete [[ForgetFold]] subclass so a hand-written Fold picks up
  * its eager, carrier-free `foldMap` member (see [[ForgetFold.foldMap]]).
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
  def apply[F[_], A](using Foldable[F]): ForgetFold[F[A], F, A] =
    new ForgetFold[F[A], F, A](identity)

  /** Filtering Fold — backed by `Forget[Option]`. @group Constructors */
  def select[A](p: A => Boolean): ForgetFold[A, Option, A] =
    new ForgetFold[A, Option, A](a => Option(a).filter(p))

/** Concrete `Optic` subclass for [[Fold]], storing the source projection `to` and the underlying
  * `Foldable[F]` directly. This lets the terminal [[foldMap]] fold the focus eagerly through the
  * captured `Foldable[F]`, skipping both the per-call `ForgetfulFold[Forget[F]]` summon and the
  * intermediate `S => M` closure the generic `Optic.foldMap` extension builds — the same
  * specialisation `GetReplaceLens` / `SetterOptic` / `MultiFocusSingleton` apply to their hot
  * paths.
  *
  * Returned by every `Fold.*` constructor so hand-written folds pick up the fast path
  * automatically. A *composed* Fold (the result of `.andThen`) surfaces as the erased
  * `Optic[…, Forget[F]]` and keeps the generic extension — the same trade-off [[GetReplaceLens]]
  * accepts.
  */
final class ForgetFold[S, F[_], A](
    val to: S => F[A]
)(using FF: Foldable[F])
    extends Optic[S, Unit, A, Unit, Forget[F]]:
  type X = Nothing
  val from: F[Unit] => Unit = _ => ()

  /** Eager `foldMap` — folds `s` straight through the captured `Foldable[F]`, returning `M` with no
    * intermediate `S => M` closure and no `ForgetfulFold` summon. Wins over the [[Optic.foldMap]]
    * extension by member precedence whenever the receiver is statically a `ForgetFold`; the call
    * shape (`fold.foldMap(f)(s)`) is identical, so this is a transparent drop-in.
    */
  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M =
    FF.foldMap(to(s))(f)
