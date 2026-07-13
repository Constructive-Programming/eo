package dev.constructive.eo
package optics

import kyo.{Maybe, Result}

import kernel.Monoid

/** Prism constructors — the sum-type optic family over the [[kyo.Result]] carrier (hit is an
  * unboxed `Success`, miss reuses the `Failure`/`Error` value).
  */
object Prism:

  def apply[S, A](
      getOrModify: S => Result[S, A],
      reverseGet: A => S,
  ) =
    pPrism(getOrModify, reverseGet)

  def pPrism[S, T, A, B](
      getOrModify: S => Result[T, A],
      reverseGet: B => T,
  ) =
    MendTearPrism(getOrModify, reverseGet)

  def optional[S, A](getOption: S => Maybe[A], reverseGet: A => S) =
    PickMendPrism[S, A, A](getOption, reverseGet)

  def pOptional[S, A, B](getOption: S => Maybe[A], reverseGet: B => S) =
    PickMendPrism[S, A, B](getOption, reverseGet)

final class MendTearPrism[S, T, A, B](
    val tear: S => Result[T, A],
    val mend: B => T,
) extends Optic[S, T, A, B, Result],
      CanGetOption[S, A],
      CanReverseGet[T, B],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = T
  def to(s: S): Result[T, A] = tear(s)

  def from(e: Result[T, B]): T =
    e.foldOrThrow(mend, t => t)

  inline def modify(f: A => B): S => T =
    s => tear(s).foldOrThrow(a => mend(f(a)), t => t)

  override inline def replace(b: B): S => T =
    s => tear(s).foldOrThrow(_ => mend(b), t => t)

  inline def getOption(s: S): Maybe[A] = tear(s).toMaybe
  inline def reverseGet(b: B): T = mend(b)

  def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    tear(s).foldOrThrow(f, _ => M.empty)

  def tearFrom[S1](f: S1 => S): MendTearPrism[S1, T, A, B] =
    new MendTearPrism(f.andThen(tear), mend)

  def mendFrom[B1](g: B1 => B): MendTearPrism[S, T, A, B1] =
    new MendTearPrism(tear, g.andThen(mend))

  private inline def fuseToMendTear[C, D](
      innerTear: A => Result[T, C],
      innerMend: D => B,
  ): MendTearPrism[S, T, C, D] =
    new MendTearPrism(
      tear = s => tear(s).foldError(a => innerTear(a), err => err),
      mend = d => mend(innerMend(d)),
    )

  private inline def fuseToOptional[C, D](
      innerHit: A => Result[T, C],
      innerWrite: (A, D) => B,
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s => tear(s).foldError(a => innerHit(a), err => err),
      reverseGet = (s, d) => tear(s).foldOrThrow(a => mend(innerWrite(a, d)), t => t),
    )

  inline def andThen[C, D](inner: MendTearPrism[A, B, C, D]): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a => inner.tear(a).mapFailure(mend),
      innerMend = d => inner.mend(d),
    )

  def andThen[C, D](inner: BijectionIso[A, B, C, D]): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a => Result.succeed(inner.get(a)),
      innerMend = d => inner.reverseGet(d),
    )

  def andThen[C, D](inner: PickMendPrism[A, C, D])(using
      ev: A =:= B
  ): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a => inner.pick(a).fold(Result.fail(mend(ev(a))))(c => Result.succeed(c)),
      innerMend = d => ev(inner.mend(d)),
    )

  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = a => Result.succeed(inner.get(a)),
      innerWrite = (a, d) => inner.enplace(a, d),
    )

  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = a => inner.getOrModify(a).mapFailure(mend),
      innerWrite = (a, d) => inner.reverseGet(a, d),
    )

final class PickMendPrism[S, A, B](
    val pick: S => Maybe[A],
    val mend: B => S,
) extends Optic[S, S, A, B, Result],
      CanGetOption[S, A],
      CanReverseGet[S, B],
      CanModifyP[S, S, A, B],
      CanFold[S, A]:
  type X = S

  def to(s: S): Result[S, A] =
    pick(s).fold(Result.fail(s))(a => Result.succeed(a))

  def from(e: Result[S, B]): S =
    e.foldOrThrow(mend, s => s)

  inline def modify(f: A => B): S => S =
    s => pick(s).fold(s)(a => mend(f(a)))

  override inline def replace(b: B): S => S =
    s => pick(s).fold(s)(_ => mend(b))

  inline def getOption(s: S): Maybe[A] = pick(s)
  inline def reverseGet(b: B): S = mend(b)

  def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    pick(s).fold(M.empty)(f)

  inline def andThen[C, D](
      inner: PickMendPrism[A, C, D]
  )(using ev: A =:= B): PickMendPrism[S, C, D] =
    new PickMendPrism(
      pick = s => pick(s).flatMap(inner.pick),
      mend = d => mend(ev(inner.mend(d))),
    )

  def andThen[C, D](inner: MendTearPrism[A, B, C, D])(using
      ev: A =:= B
  ): MendTearPrism[S, S, C, D] =
    new MendTearPrism(
      tear = s => pick(s).fold(Result.fail(s))(a => inner.tear(a).mapFailure(mend)),
      mend = d => mend(inner.mend(d)),
    )

  def andThen[C, D](inner: BijectionIso[A, B, C, D]): PickMendPrism[S, C, D] =
    new PickMendPrism(
      pick = s => pick(s).map(inner.get),
      mend = d => mend(inner.reverseGet(d)),
    )
