package dev.constructive.eo.avro.vulcan

import dev.constructive.eo.avro.AvroCodec
import org.apache.avro.Schema

/** vulcan → eo bridge: derive an [[AvroCodec]] from a `vulcan.Codec` (issue #73).
  *
  * Every typed entry point in eo-avro — `codecPrism[A]`, `AvroPrism.field` / `widenPath*`,
  * `AvroTraversal`, the `AvroJson` diagonals — is keyed on eo's own [[AvroCodec]] evidence. A
  * codebase whose codecs are vulcan gets in through this bridge instead of hand-writing the same
  * four-line adapter per use site:
  *
  * {{{
  * import dev.constructive.eo.avro.vulcan.given   // every vulcan.Codec[A] now serves as AvroCodec[A]
  *
  * val p = codecPrism[ClickInfo].field(_.entities) // summons through the bridge
  * }}}
  *
  * or, named and explicit, `given AvroCodec[ClickInfo] = AvroVulcan.codec`.
  *
  * Error mapping — vulcan is `Either`-typed where eo is total or `Throwable`-typed:
  *   - `schema` is resolved ONCE at construction and throws vulcan's own `AvroError.throwable` if
  *     invalid — fail fast: an invalid schema is a codec-definition bug, not a per-record
  *     condition, and eo treats schemas as static.
  *   - `encode` throws on error for the same reason (eo's `encode(a: A): Any` is total; an encode
  *     failure under a matching schema is a definition bug).
  *   - `decode` errors surface as `Left` via `AvroError.throwable`, matching [[AvroCodec]]'s
  *     structured-failure convention.
  *
  * vulcan rides on `cats-eo-avro` as an `Optional` dependency (the `AvroJson` / circe pattern):
  * this sub-package's API surface names `vulcan.Codec`, so any caller already depends on vulcan
  * directly — `Optional` keeps it off downstream classpaths, and avro-only users never load these
  * classfiles.
  */
object AvroVulcan:

  /** Bridge the in-scope `vulcan.Codec[A]` into an [[AvroCodec]][A]. Throws at construction if the
    * vulcan schema is invalid (see the object docs for the full error mapping).
    */
  def codec[A](using c: _root_.vulcan.Codec[A]): AvroCodec[A] = new AvroCodec[A]:
    val schema: Schema = c.schema.fold(e => throw e.throwable, identity)
    def encode(a: A): Any = c.encode(a).fold(e => throw e.throwable, identity)
    def decodeEither(any: Any): Either[Throwable, A] = c.decode(any, schema).left.map(_.throwable)

/** `import dev.constructive.eo.avro.vulcan.given` makes every in-scope `vulcan.Codec[A]` usable
  * wherever eo demands `AvroCodec[A]` evidence. Opt-in by import — don't combine with
  * kindlings-derived `AvroCodec` givens for the same `A` in one scope, or the summon turns
  * ambiguous.
  */
given vulcanAvroCodec[A](using _root_.vulcan.Codec[A]): AvroCodec[A] = AvroVulcan.codec[A]
