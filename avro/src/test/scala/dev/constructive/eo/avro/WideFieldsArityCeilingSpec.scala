package dev.constructive.eo.avro

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.specs2.mutable.Specification

/** Behaviour spec for `.fields` '''above''' the 22-selector NamedTuple ceiling (issue #96).
  *
  * `AvroPrism.fields(_.a, _.b, …)` synthesises a `NamedTuple[Names, Values]` and summons an
  * `AvroCodec` for it at the call site. With no hand-written given in scope that summon
  * auto-derives through kindlings, whose `AvroDecoderHandleAsNamedTupleRule` has to '''construct'''
  * the tuple — and construction goes through hearth's `SyntheticNamedTupleConstructor`.
  *
  * Up to 22 selectors `Values` has a `TupleN` spelling, so hearth emits `new TupleN(args…)` (that
  * is what PR #97 fixed, and it is a '''permanent''' requirement, not a workaround: hearth's
  * `n < 23` branch still calls the underlying tuple's primary constructor, and a `*:` cons chain
  * declares no value parameters). At 23 selectors and above there '''is''' no `TupleN` spelling —
  * the focus type IS a `*:` cons chain — and only hearth ≥ 0.4.1 can build one, via
  * `Tuple.fromArray` boxing each element to `Object` (hearth issue #314, shipped to us by
  * kindlings-avro-derivation ≥ 0.3.2).
  *
  * '''This file deliberately declares no `AvroCodec[NamedTuple[…]]` given at all''' — the same rule
  * as [[NamedTupleSpellingSpec]]. Adding one would short-circuit kindlings' NamedTuple rule and
  * silently stop testing the thing this file exists for.
  *
  * Boundary trio: '''22''' (the old ceiling — must keep working through the `TupleN` branch),
  * '''23''' (the first newly-unlocked arity, the first `Tuple.fromArray` case) and '''40''' (a
  * wide, mixed-primitive cover — the `fromArray` branch boxes every element to `Object`, so a
  * homogeneous `String` probe would not have exercised the unboxing on the way back out).
  *
  * The bump removes the '''spelling''' ceiling, not every ceiling. Two limits remain and are
  * deliberately NOT pinned as compile-time negatives here, because neither is a property of eo:
  *
  *   - '''254 selectors, hard and permanent.''' `.fields` selects from a case class and the JVM
  *     caps a parameter list at 254 slots, so a 255-field case class fails to compile on its own
  *     ("Platform restriction: a parameter list's length cannot exceed 254") before `.fields` is
  *     reached. Measured: 254 compiles, 255 does not.
  *   - '''The compiler thread's `-Xss`, which binds far lower.''' The derivation recurses per
  *     field. Measured on a full-cover `.fields` probe, varying only `-Xss`: 1m (the JVM default)
  *     derives 32 and overflows at 36; 2m derives 66, overflows at 100; 4m (sbt's launcher default)
  *     derives 150, overflows at 254; 8m — what this repo's `.jvmopts` sets, and the only reason
  *     these suites reach 254 — derives 254. A DOWNSTREAM consumer inherits none of that: the
  *     published artifact cannot carry an `-Xss`.
  *
  * Compile time is the third cost — the derivation grows superlinearly in arity, so a very wide
  * cover may want a higher `-Xmacro-settings:avroDerivation.timeout=30s` (build.sbt). Note the
  * '''unit suffix''': kindlings parses that value with a regex requiring `ms`/`s`/`m`, and a bare
  * integer is silently discarded, leaving the 5s default in force.
  */
class WideFieldsArityCeilingSpec extends Specification:

  import WideFieldsArityCeilingSpec.*

  // ---- 22: the old ceiling, still on the TupleN branch --------------------

  // covers: a 22-selector `.fields` cover still compiles with NO hand-written NamedTuple codec
  //   (hearth's `n < 23` `new TupleN(…)` branch — PR #97's spelling fix is what makes this work,
  //   and the bump must NOT regress it),
  //   every one of the 22 written slots lands under its own schema name,
  //   the three un-selected siblings survive reference-identical,
  //   the decoded focus is a `scala.Tuple22` at runtime (i.e. the constructor branch, not fromArray)
  "`.fields` at arity 22: TupleN branch still writes all 22 slots and leaves siblings identical" >> {
    val record = wide25Record(wide25A)
    val L = codecPrism[Wide25]
      .fields(
        _.f01,
        _.f02,
        _.f03,
        _.f04,
        _.f05,
        _.f06,
        _.f07,
        _.f08,
        _.f09,
        _.f10,
        _.f11,
        _.f12,
        _.f13,
        _.f14,
        _.f15,
        _.f16,
        _.f17,
        _.f18,
        _.f19,
        _.f20,
        _.f21,
        _.f22,
      )
      .record

    val out = L.placeUnsafe(
      (
        f01 = "b01",
        f02 = "b02",
        f03 = "b03",
        f04 = "b04",
        f05 = "b05",
        f06 = "b06",
        f07 = "b07",
        f08 = "b08",
        f09 = "b09",
        f10 = "b10",
        f11 = "b11",
        f12 = "b12",
        f13 = "b13",
        f14 = "b14",
        f15 = "b15",
        f16 = "b16",
        f17 = "b17",
        f18 = "b18",
        f19 = "b19",
        f20 = "b20",
        f21 = "b21",
        f22 = "b22",
      )
    )(record)

    val written = Seq(
      "f01" -> "b01",
      "f02" -> "b02",
      "f03" -> "b03",
      "f04" -> "b04",
      "f05" -> "b05",
      "f06" -> "b06",
      "f07" -> "b07",
      "f08" -> "b08",
      "f09" -> "b09",
      "f10" -> "b10",
      "f11" -> "b11",
      "f12" -> "b12",
      "f13" -> "b13",
      "f14" -> "b14",
      "f15" -> "b15",
      "f16" -> "b16",
      "f17" -> "b17",
      "f18" -> "b18",
      "f19" -> "b19",
      "f20" -> "b20",
      "f21" -> "b21",
      "f22" -> "b22",
    )
    val untouched = Seq("f23", "f24", "f25")

    val decoded = L.getOptionUnsafe(out)
    val read = decoded match
      case Some(nt) => Some((nt.f01, nt.f22))
      case None     => None

    (mismatches(wide25Schema, out, written) === Nil)
      .and(
        mismatches(
          wide25Schema,
          out,
          Seq(
            "f23" -> "a23",
            "f24" -> "a24",
            "f25" -> "a25",
          )
        ) === Nil
      )
      .and(untouched.forall(n => sameRef(wide25Schema, out, record, n)) === true)
      .and(focusClassName(decoded) === "scala.Tuple22")
      .and(read === Some(("b01", "b22")))
  }

  // ---- 23: the first newly-unlocked arity --------------------------------

  // covers: a 23-selector `.fields` cover compiles with NO hand-written NamedTuple codec — the
  //   arity at which `Values` has no `TupleN` spelling left and hearth must take the
  //   `Tuple.fromArray` branch (hearth #314, kindlings ≥ 0.3.2). On hearth 0.4.0 this line is the
  //   RED: `wrong number of arguments at inlining (while expanding macro) … expected: 0, found: 23`
  //   — the `*:` cons chain's primary constructor takes no value parameters. (Inside
  //   `typeCheckErrors` the same defect surfaces as `too many arguments for constructor *:`, which
  //   is what `NamedTupleSpellingSpec`'s inverted ceiling row asserts.)
  //   every one of the 23 written slots lands under its own schema name,
  //   the two un-selected siblings survive reference-identical,
  //   the decoded focus is a `scala.runtime.TupleXXL` — the fromArray branch actually executing
  "`.fields` at arity 23: the fromArray branch writes all 23 slots and leaves siblings identical" >> {
    val record = wide25Record(wide25A)
    val L = codecPrism[Wide25]
      .fields(
        _.f01,
        _.f02,
        _.f03,
        _.f04,
        _.f05,
        _.f06,
        _.f07,
        _.f08,
        _.f09,
        _.f10,
        _.f11,
        _.f12,
        _.f13,
        _.f14,
        _.f15,
        _.f16,
        _.f17,
        _.f18,
        _.f19,
        _.f20,
        _.f21,
        _.f22,
        _.f23,
      )
      .record

    val out = L.placeUnsafe(
      (
        f01 = "b01",
        f02 = "b02",
        f03 = "b03",
        f04 = "b04",
        f05 = "b05",
        f06 = "b06",
        f07 = "b07",
        f08 = "b08",
        f09 = "b09",
        f10 = "b10",
        f11 = "b11",
        f12 = "b12",
        f13 = "b13",
        f14 = "b14",
        f15 = "b15",
        f16 = "b16",
        f17 = "b17",
        f18 = "b18",
        f19 = "b19",
        f20 = "b20",
        f21 = "b21",
        f22 = "b22",
        f23 = "b23",
      )
    )(record)

    val written = Seq(
      "f01" -> "b01",
      "f02" -> "b02",
      "f03" -> "b03",
      "f04" -> "b04",
      "f05" -> "b05",
      "f06" -> "b06",
      "f07" -> "b07",
      "f08" -> "b08",
      "f09" -> "b09",
      "f10" -> "b10",
      "f11" -> "b11",
      "f12" -> "b12",
      "f13" -> "b13",
      "f14" -> "b14",
      "f15" -> "b15",
      "f16" -> "b16",
      "f17" -> "b17",
      "f18" -> "b18",
      "f19" -> "b19",
      "f20" -> "b20",
      "f21" -> "b21",
      "f22" -> "b22",
      "f23" -> "b23",
    )
    val untouched = Seq("f24", "f25")

    val decoded = L.getOptionUnsafe(out)
    val read = decoded match
      case Some(nt) => Some((nt.f01, nt.f23))
      case None     => None

    (mismatches(wide25Schema, out, written) === Nil)
      .and(
        mismatches(
          wide25Schema,
          out,
          Seq(
            "f24" -> "a24",
            "f25" -> "a25",
          )
        ) === Nil
      )
      .and(untouched.forall(n => sameRef(wide25Schema, out, record, n)) === true)
      .and(focusClassName(decoded) === "scala.runtime.TupleXXL")
      .and(read === Some(("b01", "b23")))
  }

  // ---- 40, mixed primitives: the boxing path under load ------------------

  // covers: a 40-selector cover over INTERLEAVED String/Int/Long/Double/Boolean fields — the
  //   `Tuple.fromArray` branch boxes every element to `Object`, so a homogeneous probe would not
  //   catch a mis-unboxing on the read side,
  //   every one of the 40 written slots lands under its own schema name with its own Avro type,
  //   the two un-selected siblings survive reference-identical,
  //   the decoded focus round-trips one field of each primitive kind, first and near-last
  "`.fields` at arity 40 over mixed primitives: every slot lands, boxing round-trips" >> {
    val record = mixed42Record(mixed42A)
    val L = codecPrism[Mixed42]
      .fields(
        _.f01,
        _.f02,
        _.f03,
        _.f04,
        _.f05,
        _.f06,
        _.f07,
        _.f08,
        _.f09,
        _.f10,
        _.f11,
        _.f12,
        _.f13,
        _.f14,
        _.f15,
        _.f16,
        _.f17,
        _.f18,
        _.f19,
        _.f20,
        _.f21,
        _.f22,
        _.f23,
        _.f24,
        _.f25,
        _.f26,
        _.f27,
        _.f28,
        _.f29,
        _.f30,
        _.f31,
        _.f32,
        _.f33,
        _.f34,
        _.f35,
        _.f36,
        _.f37,
        _.f38,
        _.f39,
        _.f40,
      )
      .record

    val out = L.placeUnsafe(
      (
        f01 = "b01",
        f02 = 9002,
        f03 = 7000000003L,
        f04 = 4.25,
        f05 = true,
        f06 = "b06",
        f07 = 9007,
        f08 = 7000000008L,
        f09 = 9.25,
        f10 = false,
        f11 = "b11",
        f12 = 9012,
        f13 = 7000000013L,
        f14 = 14.25,
        f15 = true,
        f16 = "b16",
        f17 = 9017,
        f18 = 7000000018L,
        f19 = 19.25,
        f20 = false,
        f21 = "b21",
        f22 = 9022,
        f23 = 7000000023L,
        f24 = 24.25,
        f25 = true,
        f26 = "b26",
        f27 = 9027,
        f28 = 7000000028L,
        f29 = 29.25,
        f30 = false,
        f31 = "b31",
        f32 = 9032,
        f33 = 7000000033L,
        f34 = 34.25,
        f35 = true,
        f36 = "b36",
        f37 = 9037,
        f38 = 7000000038L,
        f39 = 39.25,
        f40 = false,
      )
    )(record)

    val written = Seq(
      "f01" -> "b01",
      "f02" -> "9002",
      "f03" -> "7000000003",
      "f04" -> "4.25",
      "f05" -> "true",
      "f06" -> "b06",
      "f07" -> "9007",
      "f08" -> "7000000008",
      "f09" -> "9.25",
      "f10" -> "false",
      "f11" -> "b11",
      "f12" -> "9012",
      "f13" -> "7000000013",
      "f14" -> "14.25",
      "f15" -> "true",
      "f16" -> "b16",
      "f17" -> "9017",
      "f18" -> "7000000018",
      "f19" -> "19.25",
      "f20" -> "false",
      "f21" -> "b21",
      "f22" -> "9022",
      "f23" -> "7000000023",
      "f24" -> "24.25",
      "f25" -> "true",
      "f26" -> "b26",
      "f27" -> "9027",
      "f28" -> "7000000028",
      "f29" -> "29.25",
      "f30" -> "false",
      "f31" -> "b31",
      "f32" -> "9032",
      "f33" -> "7000000033",
      "f34" -> "34.25",
      "f35" -> "true",
      "f36" -> "b36",
      "f37" -> "9037",
      "f38" -> "7000000038",
      "f39" -> "39.25",
      "f40" -> "false",
    )
    val untouched = Seq("f41", "f42")

    val decoded = L.getOptionUnsafe(out)
    val read = decoded match
      case Some(nt) => Some((nt.f01, nt.f02, nt.f03, nt.f04, nt.f05, nt.f39))
      case None     => None

    (mismatches(mixed42Schema, out, written) === Nil)
      .and(
        mismatches(
          mixed42Schema,
          out,
          Seq(
            "f41" -> "a41",
            "f42" -> "a42",
          )
        ) === Nil
      )
      .and(untouched.forall(n => sameRef(mixed42Schema, out, record, n)) === true)
      .and(focusClassName(decoded) === "scala.runtime.TupleXXL")
      .and(read === Some(("b01", 9002, 7000000003L, 4.25, true, 39.25)))
  }

  // ---- helpers -----------------------------------------------------------

  /** Every `(schemaName, renderedValue)` pair the record does NOT carry. `Nil` on success, and a
    * readable per-field diff on failure — a positional shuffle shows up as the whole list.
    */
  private def mismatches(
      schema: Schema,
      out: IndexedRecord,
      expected: Seq[(String, String)],
  ): Seq[(String, String, String)] =
    expected.flatMap: (name, want) =>
      val got = String.valueOf(out.get(schema.getField(name).pos))
      if got == want then Nil else Seq((name, want, got))

  /** Un-selected siblings must come through the positional overlay untouched — not merely equal. */
  private def sameRef(
      schema: Schema,
      out: IndexedRecord,
      in: IndexedRecord,
      name: String
  ): Boolean =
    val pos = schema.getField(name).pos
    out.get(pos).asInstanceOf[AnyRef] eq in.get(pos).asInstanceOf[AnyRef]

  /** The runtime class of a decoded focus. A `NamedTuple` is opaque-erased to its `Values` tuple,
    * so this names the hearth branch that built it: `scala.TupleN` (constructor) below 23,
    * `scala.runtime.TupleXXL` (`Tuple.fromArray`) at 23 and above.
    */
  private def focusClassName(focus: Option[Any]): String =
    focus.fold("<none>")(_.asInstanceOf[AnyRef].getClass.getName)

end WideFieldsArityCeilingSpec

object WideFieldsArityCeilingSpec:

  /** 25 `String` fields: 22 or 23 to select, the rest to watch survive reference-identical. */
  case class Wide25(
      f01: String,
      f02: String,
      f03: String,
      f04: String,
      f05: String,
      f06: String,
      f07: String,
      f08: String,
      f09: String,
      f10: String,
      f11: String,
      f12: String,
      f13: String,
      f14: String,
      f15: String,
      f16: String,
      f17: String,
      f18: String,
      f19: String,
      f20: String,
      f21: String,
      f22: String,
      f23: String,
      f24: String,
      f25: String,
  )

  object Wide25:

    given AvroEncoder[Wide25] = AvroEncoder.derived
    given AvroDecoder[Wide25] = AvroDecoder.derived
    given AvroSchemaFor[Wide25] = AvroSchemaFor.derived

  /** 42 fields of interleaved primitives: 40 to select, 2 to watch survive. */
  case class Mixed42(
      f01: String,
      f02: Int,
      f03: Long,
      f04: Double,
      f05: Boolean,
      f06: String,
      f07: Int,
      f08: Long,
      f09: Double,
      f10: Boolean,
      f11: String,
      f12: Int,
      f13: Long,
      f14: Double,
      f15: Boolean,
      f16: String,
      f17: Int,
      f18: Long,
      f19: Double,
      f20: Boolean,
      f21: String,
      f22: Int,
      f23: Long,
      f24: Double,
      f25: Boolean,
      f26: String,
      f27: Int,
      f28: Long,
      f29: Double,
      f30: Boolean,
      f31: String,
      f32: Int,
      f33: Long,
      f34: Double,
      f35: Boolean,
      f36: String,
      f37: Int,
      f38: Long,
      f39: Double,
      f40: Boolean,
      f41: String,
      f42: String,
  )

  object Mixed42:

    given AvroEncoder[Mixed42] = AvroEncoder.derived
    given AvroDecoder[Mixed42] = AvroDecoder.derived
    given AvroSchemaFor[Mixed42] = AvroSchemaFor.derived

  val wide25A: Wide25 = Wide25(
    "a01",
    "a02",
    "a03",
    "a04",
    "a05",
    "a06",
    "a07",
    "a08",
    "a09",
    "a10",
    "a11",
    "a12",
    "a13",
    "a14",
    "a15",
    "a16",
    "a17",
    "a18",
    "a19",
    "a20",
    "a21",
    "a22",
    "a23",
    "a24",
    "a25",
  )

  val mixed42A: Mixed42 = Mixed42(
    "a01",
    1002,
    5000000003L,
    4.5,
    false,
    "a06",
    1007,
    5000000008L,
    9.5,
    true,
    "a11",
    1012,
    5000000013L,
    14.5,
    false,
    "a16",
    1017,
    5000000018L,
    19.5,
    true,
    "a21",
    1022,
    5000000023L,
    24.5,
    false,
    "a26",
    1027,
    5000000028L,
    29.5,
    true,
    "a31",
    1032,
    5000000033L,
    34.5,
    false,
    "a36",
    1037,
    5000000038L,
    39.5,
    true,
    "a41",
    "a42",
  )

  lazy val wide25Schema: Schema = summon[AvroCodec[Wide25]].schema
  lazy val mixed42Schema: Schema = summon[AvroCodec[Mixed42]].schema

  def wide25Record(w: Wide25): GenericRecord =
    summon[AvroCodec[Wide25]].encode(w).asInstanceOf[GenericRecord]

  def mixed42Record(m: Mixed42): GenericRecord =
    summon[AvroCodec[Mixed42]].encode(m).asInstanceOf[GenericRecord]

  // Deliberately NO `given AvroCodec[NamedTuple[…]]` anywhere in this file — see the class
  // scaladoc.

end WideFieldsArityCeilingSpec
