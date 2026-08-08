package dev.constructive.eo
package zio
package schema

import _root_.zio.Chunk
import _root_.zio.schema.codec.BinaryCodec
import _root_.zio.schema.{AccessorBuilder, Schema}

import optics.{GetReplaceLens, MendTearPrism, PickMendPrism, Prism}

/** zio-schema integration (optional dependency — add `zio-schema` yourself to use this
  * sub-package). Three seams:
  *
  *   - '''[[EoAccessorBuilder]]''' — zio-schema's own extension point for optics libraries:
  *     `schema.makeAccessors(EoAccessorBuilder)` returns an eo Lens per record field, an eo Prism
  *     per enum case, and an eo Traversal per collection, for any `Schema[A]`. No macros on our
  *     side — the schema already knows its structure.
  *   - '''Typed ↔ untyped''' — [[dynamicPrism(Schema)]], a Prism between the untyped
  *     [[_root_.zio.schema.DynamicValue]] tree and `A` (`toDynamic` / `toTypedValue`). Compose it
  *     with the [[DynamicValues]] navigation kit to decode only the leaves you touch.
  *   - '''Codec byte face''' — [[prism(BinaryCodec)]] on any `BinaryCodec[A]` (zio-schema-json,
  *     -protobuf, -avro, -msgpack, -thrift all produce one): encode/decode are exactly a Prism's
  *     two halves over the encoded `Chunk[Byte]`. Same laws and caveats as the avro/circe/kyo byte
  *     faces: `get ∘ reverseGet` is the codec roundtrip identity, `reverseGet ∘ get` re-encodes
  *     (byte-level layout normalises), a failed decode is a miss, and misses pass through writes
  *     untouched.
  */
object EoAccessorBuilder extends AccessorBuilder:

  /** The field-name singleton `F` is zio-schema bookkeeping — eo lenses don't carry it. */
  type Lens[F, S, A] = GetReplaceLens[S, S, A, A]
  type Prism[F, S, A] = PickMendPrism[S, A, A]
  type Traversal[S, A] = optics.Traversal[S, S, A, A]

  /** Record field ⇒ fused eo Lens — `Field` already stores the get/set pair. */
  def makeLens[F, S, A](product: Schema.Record[S], term: Schema.Field[S, A]): Lens[F, S, A] =
    optics.Lens[S, A](term.get, term.set)

  /** Enum case ⇒ eo Prism — `deconstructOption` / `construct` are the two halves. */
  def makePrism[F, S, A](sum: Schema.Enum[S], term: Schema.Case[S, A]): Prism[F, S, A] =
    optics.Prism.optional(term.deconstructOption, term.construct)

  /** Collection ⇒ eo Traversal — a Lens onto the `Chunk` slot (`toChunk` / `fromChunk`) composed
    * with [[Chunks.each]]. eo Traversal writes are size- and shape-preserving by construction,
    * which is the regime where `fromChunk ∘ toChunk` is the identity.
    */
  def makeTraversal[S, A](
      collection: Schema.Collection[S, A],
      element: Schema[A],
  ): Traversal[S, A] =
    optics
      .Lens[S, Chunk[A]](collection.toChunk, (_, ch) => collection.fromChunk(ch))
      .andThen(Chunks.each[A, A])

extension [A](self: Schema[A])

  /** Prism between the untyped `DynamicValue` tree and `A` — the typed ↔ untyped face. A failed
    * `toTypedValue` is a miss carrying the original tree back losslessly; decode-then-encode
    * normalises the tree (defaults filled, one canonical shape), the same documented caveat as
    * every byte face.
    */
  def dynamicPrism
      : MendTearPrism[_root_.zio.schema.DynamicValue, _root_.zio.schema.DynamicValue, A, A] =
    Prism[_root_.zio.schema.DynamicValue, A](
      dv => dv.toTypedValue(using self).fold(_ => Left(dv), Right(_)),
      a => self.toDynamic(a),
    )

extension [A](self: BinaryCodec[A])

  /** Prism between `self`-encoded bytes and `A` — `JsonCodec.schemaBasedBinaryCodec[A].prism`
    * reads/rewrites a typed value inside an encoded payload; compose outward with byte transports,
    * inward with field optics.
    */
  def prism: MendTearPrism[Chunk[Byte], Chunk[Byte], A, A] =
    Prism[Chunk[Byte], A](
      bytes => self.decode(bytes).fold(_ => Left(bytes), Right(_)),
      a => self.encode(a),
    )
