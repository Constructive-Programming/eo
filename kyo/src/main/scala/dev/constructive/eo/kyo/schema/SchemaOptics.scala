package dev.constructive.eo
package kyo
package schema

import _root_.kyo.*

import optics.{GetReplaceLens, Lens, MendTearPrism, Optional, Prism, Traversal}

/** kyo-schema integration (optional dependency — add `kyo-schema` yourself to use this
  * sub-package). Two seams:
  *
  *   - '''Focus bridge''' — kyo-schema's `Focus[Root, Value, Mode]` stores plain
  *     getter/setter/update functions behind a mode lattice, and each mode maps onto the matching
  *     eo carrier: [[lens(Focus)]] (`Focus.Id`, product paths), [[toOptional(Focus)]] (`Maybe`,
  *     sum-variant paths), and [[traversal(Focus)]] (`Chunk`, collection paths — kyo's `Chunk` IS a
  *     `Seq`, so the mode is a Lens onto the collection slot composed with `Traversal.each[Seq, E]`
  *     under cats' stock `Traverse[Seq]`; no extra instances). Bridged optics compose with
  *     everything else in eo — `Record` lenses, byte-face prisms below, capability-consuming APIs.
  *   - '''Codec byte faces''' — `Schema[A].encode` / `decode` under any kyo codec (json, msgpack,
  *     protobuf, …) are exactly a Prism's two halves: [[prism(Schema)]] over the encoded
  *     `Span[Byte]`, [[stringPrism(Schema)]] over the encoded `String`. `get ∘ reverseGet` is the
  *     identity (the codec roundtrip law); `reverseGet ∘ get` re-encodes, so byte-level layout
  *     normalizes — the same documented caveat as the avro/circe bridges. Decoding consumes ONE
  *     value: trailing input after it is accepted on reads and dropped by rewrites (normalization
  *     includes truncation). Malformed or schema-mismatched input is a miss, and misses pass
  *     through writes untouched.
  */

extension [R, V](self: Focus[R, V, Focus.Id])

  /** The product-path Focus as the fused eo Lens — same get/set pair, eo composition. */
  def lens: GetReplaceLens[R, R, V, V] =
    Lens[R, V](r => self.get(r), (r, v) => self.set(r, v))

extension [R, V](self: Focus[R, V, Maybe])

  /** The sum-variant Focus as an eo Optional: absent variant ⇒ writes pass the root through
    * untouched (`Focus.update` semantics), present ⇒ replace in place. (Named `toOptional` because
    * `Focus.optional` is already kyo's is-this-field-optional metadata query.)
    */
  def toOptional: Optional[R, R, V, V] =
    Optional[R, R, V, V](
      r => self.get(r).fold(Left(r))(Right(_)),
      (r, v) => self.update(r)(_ => v),
    )

extension [R, E](self: Focus[R, E, Chunk])

  /** The collection-path Focus as an eo Traversal — a Lens onto the `Chunk` slot (read as the `Seq`
    * it already is, written back through `Chunk.from`) composed with `Traversal.each[Seq, E]`. eo
    * Traversal writes are size-preserving by construction (element-wise `modify` / broadcast
    * `replace`), which is exactly the regime where kyo's positional Chunk setter is lawful.
    */
  def traversal: Traversal[R, R, E, E] =
    Lens[R, Seq[E]](r => self.get(r), (r, es) => self.set(r, Chunk.from(es)))
      .andThen(Traversal.each[Seq, E])

extension [A](self: Schema[A])

  /** Prism between `C`-encoded bytes and `A`: `Schema[Person].prism[Json]`. Compose with a bridged
    * Focus lens to read/modify a field inside an encoded payload in one expression.
    *
    * `decode`'s `Result` folds straight into the `Either` tear the [[optics.MendTearPrism]] carrier
    * stores — one inline `foldError` (failures AND panics are the miss arm, which carries the
    * original input back losslessly), no `Maybe`/`Option` hops.
    */
  def prism[C <: Codec](using C, Frame): MendTearPrism[Span[Byte], Span[Byte], A, A] =
    Prism[Span[Byte], A](
      bytes => self.decode(bytes).foldError(Right(_), _ => Left(bytes)),
      a => self.encode(a),
    )

  /** [[prism(Schema)]]'s String face (UTF-8), `Schema[Person].stringPrism[Json]` — primarily useful
    * with textual codecs.
    */
  def stringPrism[C <: Codec](using C, Frame): MendTearPrism[String, String, A, A] =
    Prism[String, A](
      s => self.decodeString(s).foldError(Right(_), _ => Left(s)),
      a => self.encodeString(a),
    )
