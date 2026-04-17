package eo.circe

import eo.optics.Optic

import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json}

/** A specialised Prism from [[io.circe.Json]] to a native type `A`,
  * backed by a circe `Codec[A]` and cursor-based path navigation.
  *
  * Shape on the Optic type:
  *
  * {{{
  *   JsonPrism[A] <: Optic[Json, Json, A, A, Either]
  *   type X = (DecodingFailure, HCursor)
  * }}}
  *
  * Extends `Optic` directly, in the same vein as
  * [[eo.optics.MendTearPrism]] — so it composes with the rest of the
  * cats-eo universe via stock `.andThen`, and its abstract `to` /
  * `from` satisfy the Either-carrier contract.
  *
  * The class body shadows the generic `modify` / `transform` /
  * `place` / `transfer` extensions with cursor-aware implementations.
  * For a JsonPrism whose `navigate` lands at a nested path, the
  * write-back goes through `ACursor.set(...).top`: a single traversal
  * that rewrites at the focus and reassembles the root Json in one
  * pass. The intermediate native values (Person, Address, …) are
  * never materialised.
  *
  * Forgiving semantics: if `navigate(json)` lands on a missing
  * field or a value that cannot be decoded as `A`, `modify` returns
  * the input Json unchanged, and `transform` applies the user's
  * function to `Json.Null`.
  *
  * Failure diagnostics: the `Left` branch of `to` carries the
  * `DecodingFailure` (with cursor history) plus an `HCursor` at the
  * input's root. `HCursor.value` recovers the (sub-)Json at any
  * point; `DecodingFailure.history` enumerates the navigation
  * operations that led there.
  */
final class JsonPrism[A] private[circe] (
    private[circe] val navigate: Json => ACursor,
    private[circe] val encoder:  Encoder[A],
    private[circe] val decoder:  Decoder[A],
) extends Optic[Json, Json, A, A, Either]:
  type X = (DecodingFailure, HCursor)

  // ---- Abstract Optic members ---------------------------------------

  def to: Json => Either[(DecodingFailure, HCursor), A] =
    json =>
      val c = navigate(json)
      c.as[A](using decoder) match
        case Right(a) => Right(a)
        case Left(df) => Left((df, json.hcursor))

  def from: Either[(DecodingFailure, HCursor), A] => Json = {
    case Left((_, hc)) => hc.top.getOrElse(hc.value)
    case Right(a)      => encoder(a)
  }

  // ---- Cursor-aware overrides for the hot path ----------------------
  //
  // These shadow the generic `Optic` extension methods. The generic
  // ones would route through `from`, which for a nested `JsonPrism`
  // has no way to carry the write-back context (it just encodes the
  // focus and loses the surrounding Json). The class-level methods
  // use the stored `navigate` on the INPUT json to get an `ACursor`,
  // then `set(...).top` to reassemble. One traversal, no decode of
  // anything but the focused value.

  inline def modify[X](f: A => A): Json => Json =
    json =>
      val c = navigate(json)
      c.as[A](using decoder) match
        case Right(a) => c.set(encoder(f(a))).top.getOrElse(json)
        case Left(_)  => json

  inline def transform[X](f: Json => Json): Json => Json =
    json =>
      val c = navigate(json)
      val current = c.focus.getOrElse(Json.Null)
      c.set(f(current)).top.getOrElse(json)

  inline def place[X](a: A): Json => Json =
    json =>
      val c = navigate(json)
      c.set(encoder(a)).top.getOrElse(json)

  inline def transfer[C, X](f: C => A): Json => C => Json =
    json => c => place(f(c))(json)

  inline def reverseGet(a: A): Json = encoder(a)

  inline def getOption(json: Json): Option[A] = to(json).toOption

  // ---- Path extension helper used by the field-drill macro ----------

  /** Extend the cursor path by one step and swap to a narrower
    * Encoder/Decoder pair. Used by the [[field]] macro; not
    * intended for direct call by end users. */
  private[circe] def widenPath[B](
      step: ACursor => ACursor,
  )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
    new JsonPrism[B](json => step(navigate(json)), encB, decB)

object JsonPrism:

  /** Construct a root-level `JsonPrism[S]` — the cursor starts at
    * the top of the Json, no navigation applied yet. Field-drill
    * methods extend the cursor from here. */
  def apply[S](using enc: Encoder[S], dec: Decoder[S]): JsonPrism[S] =
    new JsonPrism[S](_.hcursor, enc, dec)

  /** Drill into a named field via a selector lambda. The macro
    * extracts the field name at compile time; the resulting
    * `JsonPrism[B]` extends the parent's cursor path by one
    * `downField` step and carries the inner `Encoder[B]` /
    * `Decoder[B]` for encoding and decoding at that position.
    *
    * Chain freely: `codecPrism[Person].field(_.address).field(_.street)`. */
  extension [A](o: JsonPrism[A])
    transparent inline def field[B](
        inline selector: A => B,
    )(using encB: Encoder[B], decB: Decoder[B]): JsonPrism[B] =
      ${ JsonPrismMacro.fieldImpl[A, B]('o, 'selector, 'encB, 'decB) }
