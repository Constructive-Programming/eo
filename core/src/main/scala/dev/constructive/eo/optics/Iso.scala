package dev.constructive.eo
package optics

import cats.Monoid

import data.Direct

/** Constructor for `Iso` — a bijective single-focus optic, backed by `Direct`. An `Iso[S, A]`
  * (short for `Optic[S, S, A, A, Direct]`) encodes a data-shape bijection. `Direct[X, A] = A`
  * carries no leftover, so every Iso operation reduces to plain function application.
  */
object Iso:

  /** Construct an Iso from forward `get: S => A` and reverse `reverseGet: B => T`. Polymorphic; use
    * `S = T = A = B` for the monomorphic case.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int, name: String)
    * val pairIso = Iso[(Int, String), (Int, String), Person, Person](
    *   t => Person(t._1, t._2),
    *   p => (p.age, p.name),
    * )
    *   }}}
    */
  def apply[S, T, A, B](f: S => A, g: B => T) =
    BijectionIso[S, T, A, B](f, g)

/** Concrete Optic subclass for an isomorphism. Stores `get` / `reverseGet` directly so the hot path
  * skips the `Accessor[Direct]` / `ReverseAccessor[Direct]` typeclass dispatches the generic
  * extensions would perform — same storage shape as Monocle's `Iso`. Returned by [[Iso.apply]] so
  * hand-written isos pick up the fused path automatically.
  */
final class BijectionIso[S, T, A, B](
    read: S => A,
    build: B => T,
) extends Optic[S, T, A, B, Direct],
      CanGet[S, A],
      CanReverseGet[T, B],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = Nothing
  def get(s: S): A = read(s)
  def reverseGet(b: B): T = build(b)
  def to(s: S): Direct[X, A] = Direct(read(s))
  def from(d: Direct[X, B]): T = build(d.value)

  inline def modify(f: A => B): S => T =
    s => reverseGet(f(get(s)))

  override inline def replace(b: B): S => T =
    val t = reverseGet(b)
    _ => t

  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = f(read(s))

  /** Fused `Iso.andThen(Iso)` — composes `get`s and `reverseGet`s directly. `inline` so each
    * compose site splices distinct lambdas, keeping a deep `iso.andThen(iso)…` chain under C2's
    * recursive-inline cap (see [[Getter.andThen]] / [[GetReplaceLens.andThen]]).
    */
  inline def andThen[C, D](inner: BijectionIso[A, B, C, D]): BijectionIso[S, T, C, D] =
    new BijectionIso(
      read = s => inner.get(get(s)),
      build = d => reverseGet(inner.reverseGet(d)),
    )

  /** Fused `Iso.andThen(Lens)` — threads the iso around the inner lens. */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      read = s => inner.get(get(s)),
      enplace = (s, d) => reverseGet(inner.enplace(get(s), d)),
    )

  /** Fused `Iso.andThen(Prism)` — on inner miss, outer.reverseGet lifts the leftover. */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): MendTearPrism[S, T, C, D] =
    new MendTearPrism(
      tear = s =>
        inner.tear(get(s)) match
          case Left(b)      => Left(reverseGet(b))
          case r @ Right(_) => r.widenLeft[T],
      mend = d => reverseGet(inner.mend(d)),
    )

  /** Fused `Iso.andThen(Optional)` — iso is transparent; result is `Optional`. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.getOrModify(get(s)) match
          case Left(b)      => Left(reverseGet(b))
          case r @ Right(_) => r.widenLeft[T],
      reverseGet = (s, d) =>
        val newB = inner.reverseGet(get(s), d)
        reverseGet(newB),
    )
