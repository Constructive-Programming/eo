package dev.constructive.eo
package kyo
package schema

import _root_.kyo.*

import optics.{GetReplaceLens, Lens, Optional, PickMendPrism, Prism}

/** kyo-schema integration (optional dependency — add `kyo-schema` yourself to use this
  * sub-package). Two seams:
  *
  *   - '''Focus bridge''' — kyo-schema's `Focus[Root, Value, Mode]` stores plain
  *     getter/setter/update functions behind a mode lattice, so the two single-focus modes map
  *     directly onto eo carriers: [[lens(Focus)]] (`Focus.Id`, product paths) and
  *     [[toOptional(Focus)]] (`Maybe`, sum-variant paths). Bridged optics compose with everything
  *     else in eo — `Record` lenses, byte-face prisms below, capability-consuming APIs. `Chunk`
  *     mode (collection paths) is not bridged: eo Traversals are `MultiFocus`-carrier based and
  *     kyo's positional zip-set has no lawful mapping onto them yet.
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

extension [A](self: Schema[A])

  /** Prism between `C`-encoded bytes and `A`: `Schema[Person].prism[Json]`. Compose with a bridged
    * Focus lens to read/modify a field inside an encoded payload in one expression.
    */
  def prism[C <: Codec](using C, Frame): PickMendPrism[Span[Byte], A, A] =
    Prism.optional(bytes => self.decode(bytes).toMaybe.toOption, a => self.encode(a))

  /** [[prism(Schema)]]'s String face (UTF-8), `Schema[Person].stringPrism[Json]` — primarily useful
    * with textual codecs.
    */
  def stringPrism[C <: Codec](using C, Frame): PickMendPrism[String, A, A] =
    Prism.optional(s => self.decodeString(s).toMaybe.toOption, a => self.encodeString(a))
