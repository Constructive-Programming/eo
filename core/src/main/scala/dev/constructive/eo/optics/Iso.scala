package dev.constructive.eo
package optics

import data.Forgetful

/** Constructor for `Iso` — a bijective single-focus optic, backed by `Forgetful`. An `Iso[S, A]`
  * (short for `Optic[S, S, A, A, Forgetful]`) encodes a data-shape bijection. `Forgetful[X, A] = A`
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
  * skips the `Accessor[Forgetful]` / `ReverseAccessor[Forgetful]` typeclass dispatches the generic
  * extensions would perform — same storage shape as Monocle's `Iso`. Returned by [[Iso.apply]] so
  * hand-written isos pick up the fused path automatically.
  */
final class BijectionIso[S, T, A, B](
    val get: S => A,
    val reverseGet: B => T,
) extends Optic[S, T, A, B, Forgetful]:
  type X = Nothing
  val to: S => A = get
  val from: B => T = reverseGet

  inline def modify(f: A => B): S => T =
    s => reverseGet(f(get(s)))

  inline def replace(b: B): S => T =
    val t = reverseGet(b)
    _ => t

  /** Fused `Iso.andThen(Iso)` — composes `get`s and `reverseGet`s directly. */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): BijectionIso[S, T, C, D] =
    new BijectionIso(
      get = s => inner.get(get(s)),
      reverseGet = d => reverseGet(inner.reverseGet(d)),
    )

  /** Fused `Iso.andThen(Lens)` — threads the iso around the inner lens. */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      get = s => inner.get(get(s)),
      enplace = (s, d) => reverseGet(inner.enplace(get(s), d)),
    )

  /** Fused `Iso.andThen(Prism)` — on inner miss, outer.reverseGet lifts the leftover. */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): MendTearPrism[S, T, C, D] =
    new MendTearPrism(
      tear = s =>
        inner.tear(get(s)) match
          case Left(b)  => Left(reverseGet(b))
          case Right(c) => Right(c),
      mend = d => reverseGet(inner.mend(d)),
    )

  /** Fused `Iso.andThen(Optional)` — iso is transparent; result is `Optional`. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.getOrModify(get(s)) match
          case Left(b)  => Left(reverseGet(b))
          case Right(c) => Right(c),
      reverseGet = (s, d) =>
        val newB = inner.reverseGet(get(s), d)
        reverseGet(newB),
    )
