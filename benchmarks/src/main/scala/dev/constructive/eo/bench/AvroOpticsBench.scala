package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.avro.{AvroCodec, AvroFailure, AvroPrism, codecPrism}

import cats.data.{Chain, Ior}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.IndexedRecord

/** AvroPrism vs. naive decode-modify-encode round-trip via the kindlings codec.
  *
  * The motivating story for eo-avro is the same as eo-circe's: modifying a single field of an Avro
  * payload shouldn't pay to decode / re-encode the entire record. `AvroPrism[A].modify` walks the
  * `IndexedRecord` along a `PathStep` array, decodes only at the focus, and stitches the parents
  * back together — O(path depth) rather than O(all fields).
  *
  * Two depths benched here — depth 1 (`Person.name`, shallow) and depth 3
  * (`Deep3.d2.d1.atom.value`, deep) — each with paired `eo*` / `native*` rows so JMH reports them
  * side-by-side. The `eo*` side is the `*Unsafe` (silent) hot path; the `native*` side is the raw
  * kindlings codec round-trip (`decodeEither` → modify case-class tree → `encode`). Comparison is
  * honest because both sides skip the Ior-bearing diagnostic surface.
  */

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroOpticsBench extends JmhDefaults:

  import AvroOpticsBench.*

  // Pre-encoded inputs — the bench measures the rewrite only,
  // not the initial encode / Codec lookup.
  private val aliceRecord: IndexedRecord =
    summon[AvroCodec[Person]].encode(Alice).asInstanceOf[IndexedRecord]

  private val deep3Record: IndexedRecord =
    summon[AvroCodec[Deep3]]
      .encode(
        Deep3("root", Deep2("middle", Deep1("leaf", Atom("Alice"))))
      )
      .asInstanceOf[IndexedRecord]

  // The prisms — constructed once, outside the bench loop.
  private val nameD1: AvroPrism[String] =
    codecPrism[Person].field(_.name)

  private val leafD3: AvroPrism[String] =
    codecPrism[Deep3].field(_.d2).field(_.d1).field(_.atom).field(_.value)

  // ---- Modify benchmarks ---------------------------------------------
  //
  // `eoModify*` — direct-walk hot path via `AvroPrism.modifyUnsafe`.
  // `nativeModify*` — kindlings round-trip baseline: `decodeEither` →
  // case-class `.copy` chain → `encode`. The codec's `decodeEither`
  // returns `Either[Throwable, A]`; we `.toOption.get` to match the
  // throw-on-failure shape of `*Unsafe`.

  @Benchmark def eoModifyShallow: IndexedRecord =
    nameD1.modifyUnsafe(_.toUpperCase)(aliceRecord)

  @Benchmark def nativeModifyShallow: IndexedRecord =
    val codec = summon[AvroCodec[Person]]
    val p = codec.decodeEither(aliceRecord).toOption.get
    codec.encode(p.copy(name = p.name.toUpperCase)).asInstanceOf[IndexedRecord]

  @Benchmark def eoModifyDeep: IndexedRecord =
    leafD3.modifyUnsafe(_.toUpperCase)(deep3Record)

  @Benchmark def nativeModifyDeep: IndexedRecord =
    val codec = summon[AvroCodec[Deep3]]
    val d = codec.decodeEither(deep3Record).toOption.get
    val updated =
      d.copy(d2 =
        d.d2.copy(d1 = d.d2.d1.copy(atom = d.d2.d1.atom.copy(value = d.d2.d1.atom.value.toUpperCase)))
      )
    codec.encode(updated).asInstanceOf[IndexedRecord]

  // ---- Get benchmarks ------------------------------------------------
  //
  // `eoGet*` — `getOptionUnsafe` (silent) on the focused leaf.
  // `nativeGet*` — full kindlings decode, then field projection.

  @Benchmark def eoGetShallow: Option[String] =
    nameD1.getOptionUnsafe(aliceRecord)

  @Benchmark def nativeGetShallow: String =
    summon[AvroCodec[Person]].decodeEither(aliceRecord).toOption.get.name

  @Benchmark def eoGetDeep: Option[String] =
    leafD3.getOptionUnsafe(deep3Record)

  @Benchmark def nativeGetDeep: String =
    summon[AvroCodec[Deep3]]
      .decodeEither(deep3Record)
      .toOption
      .get
      .d2
      .d1
      .atom
      .value

  // ---- Ior surface (default) — informational --------------------------
  //
  // Same shape as JsonPrismBench's `eoModifyIor_*` rows: pays a single
  // `Ior.Right` allocation per call relative to `*Unsafe`. Reported so
  // the docs can quote the "Ior tax" datapoint analogous to eo-circe.

  @Benchmark def eoModifyIorShallow: Ior[Chain[AvroFailure], IndexedRecord] =
    nameD1.modify(_.toUpperCase)(aliceRecord)

  @Benchmark def eoModifyIorDeep: Ior[Chain[AvroFailure], IndexedRecord] =
    leafD3.modify(_.toUpperCase)(deep3Record)

end AvroOpticsBench

object AvroOpticsBench:

  // ---- Depth-1 fixture (shallow) -------------------------------------

  final case class Person(name: String, age: Int)

  object Person:

    given AvroEncoder[Person] = AvroEncoder.derived
    given AvroDecoder[Person] = AvroDecoder.derived
    given AvroSchemaFor[Person] = AvroSchemaFor.derived

  val Alice: Person = Person("Alice", 30)

  // ---- Depth-3 fixture (deep) ---------------------------------------

  final case class Atom(value: String)

  object Atom:

    given AvroEncoder[Atom] = AvroEncoder.derived
    given AvroDecoder[Atom] = AvroDecoder.derived
    given AvroSchemaFor[Atom] = AvroSchemaFor.derived

  final case class Deep1(label: String, atom: Atom)

  object Deep1:

    given AvroEncoder[Deep1] = AvroEncoder.derived
    given AvroDecoder[Deep1] = AvroDecoder.derived
    given AvroSchemaFor[Deep1] = AvroSchemaFor.derived

  final case class Deep2(label: String, d1: Deep1)

  object Deep2:

    given AvroEncoder[Deep2] = AvroEncoder.derived
    given AvroDecoder[Deep2] = AvroDecoder.derived
    given AvroSchemaFor[Deep2] = AvroSchemaFor.derived

  final case class Deep3(label: String, d2: Deep2)

  object Deep3:

    given AvroEncoder[Deep3] = AvroEncoder.derived
    given AvroDecoder[Deep3] = AvroDecoder.derived
    given AvroSchemaFor[Deep3] = AvroSchemaFor.derived

end AvroOpticsBench
