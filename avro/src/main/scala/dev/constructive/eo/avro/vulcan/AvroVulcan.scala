package dev.constructive.eo.avro.vulcan

import _root_.vulcan.Codec as VCodec
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
  * The bridge comes in two shapes. `codec(schema)` is total: the schema is in hand, there is
  * nothing to resolve. `codec(using)` resolves the schema from the codec and can fail, so it
  * returns `Either[Exception, AvroCodec[A]]`. The opt-in given below is the one site with no
  * failure channel — a summon must produce a codec — so a schema that will not resolve fails
  * eagerly, at the given site.
  *
  * Error mapping — vulcan is `Either`-typed where eo is total or `Throwable`-typed:
  *   - schema resolution: `Left` from `codec(using)`; impossible for `codec(schema)`; eager at the
  *     given site for the given (see above).
  *   - `encode` throws on error (eo's `encode(a: A): Any` is total — an encode failure under a
  *     matching schema is a codec-definition bug, not a per-record condition).
  *   - `decode` errors surface as `Left` via `AvroError.throwable`, matching [[AvroCodec]]'s
  *     structured-failure convention.
  *
  * vulcan rides on `cats-eo-avro` as an `Optional` dependency (the `AvroJson` / circe pattern):
  * this sub-package's API surface names `vulcan.Codec`, so any caller already depends on vulcan
  * directly — `Optional` keeps it off downstream classpaths, and avro-only users never load these
  * classfiles.
  */
object AvroVulcan:

  /** Bridge `c` under an EXPLICIT schema — the total form: nothing is resolved, so nothing can fail
    * at construction. Use when the schema is already in hand (a parsed `.avsc`, a registry lookup,
    * the filer's own maintained schema).
    */
  def codec[A](schema: Schema)(using c: VCodec[A]): AvroCodec[A] =
    val v = c
    val s = schema
    new AvroCodec[A]:
      val schema: Schema = s
      def encode(a: A): Any = v.encode(a).fold(e => throw e.throwable, identity)
      def decodeEither(any: Any): Either[Throwable, A] =
        v.decode(any, schema).left.map(_.throwable)

  /** Bridge the in-scope `vulcan.Codec[A]`, resolving its schema — `Left` (vulcan's own error, as
    * an [[Exception]]) when the schema will not resolve. The schema-resolved codec is returned
    * whole; nothing throws.
    */
  def codec[A](using c: VCodec[A]): Either[Exception, AvroCodec[A]] =
    c.schema
      .fold(
        e =>
          Left(e.throwable match
            case ex: Exception => ex
            case other         => IllegalStateException("vulcan schema resolution failed", other),
          ),
        schema => Right(codec(schema))
      )

  /** The derived whole-record builder (issue #95): `A ⇒ GenericData.Record`, leaf by leaf, at
    * hand-built cost with zero hand-maintained lines.
    *
    * The macro walks `A`'s case fields at COMPILE time; construction resolves every case field's
    * schema slot by NAME (all-or-nothing, issue #105's doctrine) and validates every arm against
    * the schema it writes into — so [[WholeRecordBuilder.toRecord]] is pure positional puts. This
    * is the positional-over-held-leaf-codecs builder the issue filer benchmarked and rejected, with
    * the missing piece: it RECURSES into nested case classes (each sub-record built by the same
    * rule against its own schema) instead of re-entering `Codec[Sub].encode` — the composition
    * their prototype kept paying per sub-record, which is what cost them the 7.5x time / 16.9x
    * allocation regression on the real nested ClickInfo.
    *
    * Per-field arms, classified from the case-class shape at expansion:
    *   - Boolean / Int / Long / Float / Double / String → the value itself (the Avro datum);
    *   - nested case class → a sub-record level (recursive; self-recursive types resolve through
    *     the runtime level chain, so recursive case classes terminate);
    *   - `Option[X]` → `None` puts null exactly as vulcan's `OptionCodec`, `Some(v)` recurses;
    *   - everything else (enums, bytes, logical types, collections, sums, value classes) → the
    *     field type's own `vulcan.Codec`, summoned HERE — a missing leaf codec is a compile error
    *     pointing at the field.
    *
    * Construction is TOTAL: every way assembly can fail comes back as the [[Exception]] half,
    * naming the field and the record, before any record is built — a case field naming no schema
    * column (computed/derived schema columns are fine and keep their in-record default), two case
    * fields claiming one column, an arm disagreeing with its schema field's shape, or the codec's
    * schema itself refusing to resolve. Hold the builder in a `val` / `given`: construction is the
    * once-cost, `toRecord` is the hot path.
    *
    * Wire-compatible with [[codec]]'s encode on the fields the case class holds; the one documented
    * difference is schema-only columns (see [[WholeRecordBuilder]]). The common install:
    *
    * {{{
    *   val clickBuilder = AvroVulcan.recordBuilder[ClickInfo]   // Exception | WholeRecordBuilder
    *   given AvroCodec[ClickInfo] = clickBuilder.fold(e => throw e, _.asAvroCodec)
    * }}}
    */
  inline def recordBuilder[A](using c: VCodec[A]): Exception | WholeRecordBuilder[A] =
    ${ RecordBuilderMacro.builderImpl[A]('c) }

/** `import dev.constructive.eo.avro.vulcan.given` makes every in-scope `vulcan.Codec[A]` usable
  * wherever eo demands `AvroCodec[A]` evidence. Opt-in by import — don't combine with
  * kindlings-derived `AvroCodec` givens for the same `A` in one scope, or the summon turns
  * ambiguous. A given must produce its value, so a schema that will not resolve fails HERE — prefer
  * the explicit [[AvroVulcan.codec]] forms where an `Either` can be surfaced.
  */
given vulcanAvroCodec[A](using VCodec[A]): AvroCodec[A] =
  AvroVulcan.codec[A].fold(e => throw e, identity)
