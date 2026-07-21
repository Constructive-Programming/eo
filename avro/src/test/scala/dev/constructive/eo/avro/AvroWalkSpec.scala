package dev.constructive.eo.avro

import scala.language.implicitConversions

import java.util.{ArrayList, Arrays, LinkedHashMap, List as JList}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}
import org.specs2.mutable.Specification

/** Direct walker tests for [[AvroWalk]] — covers the four reachable Avro shapes (records, arrays,
  * maps, union alternatives) on both the strict and lenient policies, plus the round-trip
  * `walk + identity rebuild` invariant.
  */
class AvroWalkSpec extends Specification:

  import AvroSpecFixtures.*

  // ---- Schemas under test ------------------------------------------

  /** Schema for `record Wrapper { array<Person> people; }` — needed for the array-walk specs. */
  private val wrapperSchema: Schema =
    val fields = new ArrayList[Schema.Field]()
    fields.add(
      new Schema.Field("people", Schema.createArray(personSchema), null, null)
    )
    Schema.createRecord("Wrapper", null, "eo.avro.test", false, fields)

  /** Schema for `record TaggedMap { map<string> tags; }` — used by the map-walk spec. */
  private val taggedMapSchema: Schema =
    val fields = new ArrayList[Schema.Field]()
    fields.add(
      new Schema.Field("tags", Schema.createMap(Schema.create(Schema.Type.STRING)), null, null)
    )
    Schema.createRecord("TaggedMap", null, "eo.avro.test", false, fields)

  /** Schema for a `union<null, long>` field embedded in a wrapper record. */
  private val maybeLongSchema: Schema =
    val unionSchema = Schema.createUnion(
      Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG))
    )
    val fields = new ArrayList[Schema.Field]()
    fields.add(new Schema.Field("amount", unionSchema, null, null))
    Schema.createRecord("MaybeLong", null, "eo.avro.test", false, fields)

  private val colorSchema: Schema =
    Schema.createEnum("Color", null, "eo.avro.test", Arrays.asList("RED", "GREEN", "BLUE"))

  private val hashSchema: Schema = Schema.createFixed("Hash", null, "eo.avro.test", 4)

  /** Schema for `record UnionLeaves { union<null, Color> color; union<null, Hash> hash; union<null,
    * bytes> blob; }` — the named-leaf and bytes union alternatives.
    */
  private val unionLeavesSchema: Schema =
    def opt(s: Schema) = Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), s))
    val fields = new ArrayList[Schema.Field]()
    fields.add(new Schema.Field("color", opt(colorSchema), null, null))
    fields.add(new Schema.Field("hash", opt(hashSchema), null, null))
    fields.add(new Schema.Field("blob", opt(Schema.create(Schema.Type.BYTES)), null, null))
    Schema.createRecord("UnionLeaves", null, "eo.avro.test", false, fields)

  // ---- Record / Array / Map / Union walks (one composite per shape) ---------
  //
  // 2026-04-29 consolidation: 11 → 5 named blocks. Each block covers the happy +
  // miss + (where applicable) mismatch branches for one shape.

  // covers: walk a 1-deep record field returns terminal value + parent stack of length 1,
  //   walk a missing record field surfaces PathMissing,
  //   walking a Field step into a non-record parent surfaces NotARecord
  "Record walk: 1-deep field, miss, non-record parent" >> {
    val r = personRecord(Person("Alice", 30))

    val happy = AvroWalk.walkPath(r, Array(PathStep.Field("name"))) match
      case Right((cur, parents)) =>
        (cur.toString === "Alice").and(parents.length === 1).and(parents(0) === r)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val strictMiss = AvroWalk.walkPath(r, Array(PathStep.Field("nope"))) ===
      Left(AvroFailure.PathMissing(PathStep.Field("nope")))

    val notARecord =
      AvroWalk.walkPath(r, Array(PathStep.Field("name"), PathStep.Field("x"))) ===
        Left(AvroFailure.NotARecord(PathStep.Field("x")))

    happy.and(strictMiss).and(notARecord)
  }

  // covers: walk into people[0].name + parent stack length 3,
  //   OOB array index surfaces IndexOutOfRange,
  //   indexing into a non-array surfaces NotAnArray
  "Array walk: index, OOB → IndexOutOfRange, non-array parent → NotAnArray" >> {
    val people: GenericData.Array[GenericRecord] =
      buildArray(personSchema, Vector(personRecord(Person("Alice", 30))))
    val wrapper = buildRecord(wrapperSchema)("people" -> people)

    val happy = AvroWalk.walkPath(
      wrapper,
      Array(PathStep.Field("people"), PathStep.Index(0), PathStep.Field("name")),
    ) match
      case Right((cur, parents)) =>
        (cur.toString === "Alice").and(parents.length === 3)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val oob = AvroWalk.walkPath(wrapper, Array(PathStep.Field("people"), PathStep.Index(5))) ===
      Left(AvroFailure.IndexOutOfRange(PathStep.Index(5), 1))

    val notArray =
      AvroWalk.walkPath(wrapper, Array(PathStep.Index(0))) ===
        Left(AvroFailure.NotAnArray(PathStep.Index(0)))

    happy.and(oob).and(notArray)
  }

  // covers: exact index boundaries — idx == -1 and idx == size both surface IndexOutOfRange (the
  //   `idx < 0 || idx >= size` guard's two disjuncts individually, not just an interior OOB index)
  "Array walk: exact boundary indices -1 and size both surface IndexOutOfRange" >> {
    val people: GenericData.Array[GenericRecord] =
      buildArray(personSchema, Vector(personRecord(Person("Alice", 30))))
    val wrapper = buildRecord(wrapperSchema)("people" -> people)

    val negOne =
      AvroWalk.walkPath(wrapper, Array(PathStep.Field("people"), PathStep.Index(-1))) ===
        Left(AvroFailure.IndexOutOfRange(PathStep.Index(-1), 1))
    val atSize =
      AvroWalk.walkPath(wrapper, Array(PathStep.Field("people"), PathStep.Index(1))) ===
        Left(AvroFailure.IndexOutOfRange(PathStep.Index(1), 1))

    negOne.and(atSize)
  }

  // covers: walk into a map<string> entry by key returns the entry value
  "Map walk: by string key" >> {
    val tags = new LinkedHashMap[String, String]()
    tags.put("env", "prod")
    tags.put("region", "us")
    val record = buildRecord(taggedMapSchema)("tags" -> tags)

    AvroWalk.walkPath(record, Array(PathStep.Field("tags"), PathStep.Field("env"))) match
      case Right((cur, _)) => cur.toString === "prod"
      case other           =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // covers: union branch matching resolves "long" alt to its long value,
  //   mismatched union branch ("string" on long) surfaces UnionResolutionFailed,
  //   terminalOf returns Field("") for empty, last step otherwise
  "Union walk + terminalOf: long-alt resolution, branch mismatch, terminalOf endpoints" >> {
    val record = buildRecord(maybeLongSchema)("amount" -> java.lang.Long.valueOf(42L))

    val longOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("amount"), PathStep.UnionBranch("long")),
    ) match
      case Right((cur, _)) => cur === java.lang.Long.valueOf(42L)
      case other           =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val mismatchOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("amount"), PathStep.UnionBranch("string")),
    ) match
      case Left(AvroFailure.UnionResolutionFailed(_, PathStep.UnionBranch("string"))) =>
        org.specs2.execute.Success(): org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result

    val terminalEmpty = AvroWalk.terminalOf(Array.empty[PathStep]) === PathStep.Field("")
    val terminalLast =
      AvroWalk.terminalOf(Array(PathStep.Field("a"), PathStep.Index(2))) === PathStep.Index(2)

    longOk.and(mismatchOk).and(terminalEmpty).and(terminalLast)
  }

  // covers: enum value resolves its full-name union branch,
  //   fixed value resolves its full-name union branch,
  //   ByteBuffer value resolves the "bytes" union branch,
  //   enum value whose runtime symbol is outside the schema's declared set surfaces BadEnumSymbol
  //     carrying the symbol + declared set,
  //   enum value against a non-matching branch still surfaces UnionResolutionFailed
  "Union walk: enum / fixed / bytes alternatives resolve; corrupt enum symbol surfaces BadEnumSymbol" >> {
    val record = buildRecord(unionLeavesSchema)(
      "color" -> new GenericData.EnumSymbol(colorSchema, "GREEN"),
      "hash" -> new GenericData.Fixed(hashSchema, Array[Byte](1, 2, 3, 4)),
      "blob" -> java.nio.ByteBuffer.wrap(Array[Byte](9, 8)),
    )

    val enumOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("color"), PathStep.UnionBranch("eo.avro.test.Color")),
    ) match
      case Right((cur, _)) => cur.toString === "GREEN"
      case other           =>
        org.specs2.execute.Failure(s"expected Right(GREEN), got $other"): org.specs2.execute.Result

    val fixedOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("hash"), PathStep.UnionBranch("eo.avro.test.Hash")),
    ) match
      case Right((cur: GenericData.Fixed, _)) => cur.bytes.toSeq === Seq[Byte](1, 2, 3, 4)
      case other                              =>
        org.specs2.execute.Failure(s"expected Right(Fixed), got $other"): org.specs2.execute.Result

    val bytesOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("blob"), PathStep.UnionBranch("bytes")),
    ) match
      case Right((cur: java.nio.ByteBuffer, _)) => cur.array.toSeq === Seq[Byte](9, 8)
      case other                                =>
        org
          .specs2
          .execute
          .Failure(s"expected Right(ByteBuffer), got $other"): org.specs2.execute.Result

    // EnumSymbol doesn't validate at construction — a hand-built payload can carry a symbol the
    // schema never declared. Resolution must refuse it with the dedicated failure, not pass it on.
    val corrupt = buildRecord(unionLeavesSchema)(
      "color" -> new GenericData.EnumSymbol(colorSchema, "MAGENTA"),
      "hash" -> new GenericData.Fixed(hashSchema, Array[Byte](1, 2, 3, 4)),
      "blob" -> java.nio.ByteBuffer.wrap(Array[Byte](9, 8)),
    )
    val badSymbolOk = AvroWalk.walkPath(
      corrupt,
      Array(PathStep.Field("color"), PathStep.UnionBranch("eo.avro.test.Color")),
    ) match
      case Left(AvroFailure.BadEnumSymbol(sym, valid, step)) =>
        (sym === "MAGENTA")
          .and(valid === List("RED", "GREEN", "BLUE"))
          .and(step === PathStep.UnionBranch("eo.avro.test.Color"))
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected BadEnumSymbol, got $other"): org.specs2.execute.Result

    val mismatchOk = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("color"), PathStep.UnionBranch("string")),
    ) match
      case Left(AvroFailure.UnionResolutionFailed(_, PathStep.UnionBranch("string"))) =>
        org.specs2.execute.Success(): org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result

    enumOk.and(fixedOk).and(bytesOk).and(badSymbolOk).and(mismatchOk)
  }

  // covers: identity rebuild yields a structurally-equal record (immutable boundary),
  //   walk-into people[0].name, modify the leaf, rebuild — original untouched + rebuilt updated
  "Round-trip walk + rebuild: identity rebuild equals input; deep-modify leaves original untouched" >> {
    val r = personRecord(Person("Alice", 30))
    val path = Array[PathStep](PathStep.Field("name"))
    val identityRebuild = AvroWalk.walkPath(r, path) match
      case Right((cur, parents)) =>
        val rebuilt = AvroWalk.rebuildPath(parents, path, cur)
        rebuilt.asInstanceOf[IndexedRecord] === r
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val originalAlice = personRecord(Person("Alice", 30))
    val people: GenericData.Array[GenericRecord] = buildArray(personSchema, Vector(originalAlice))
    val wrapper = buildRecord(wrapperSchema)("people" -> people)
    val deepPath =
      Array[PathStep](PathStep.Field("people"), PathStep.Index(0), PathStep.Field("name"))

    val deepModify = AvroWalk.walkPath(wrapper, deepPath) match
      case Right((_, parents)) =>
        val rebuilt = AvroWalk
          .rebuildPath(parents, deepPath, "Bob")
          .asInstanceOf[IndexedRecord]
        // After rebuild, original wrapper is still Alice; rebuilt is Bob.
        val origPeople = wrapper
          .get(wrapperSchema.getField("people").pos)
          .asInstanceOf[JList[GenericRecord]]
        val origName = origPeople.get(0).get(personSchema.getField("name").pos).toString
        val newPeople = rebuilt
          .get(wrapperSchema.getField("people").pos)
          .asInstanceOf[JList[GenericRecord]]
        val newName = newPeople.get(0).get(personSchema.getField("name").pos).toString
        (origName === "Alice").and(newName === "Bob")
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    identityRebuild.and(deepModify)
  }

  // ---- Schema-name resolution (issue #35 escape-hatch machinery) -----------------------
  //
  // `AvroWalk.schemaAt` / `fieldNameAt` / `resolveFieldName` back `.field(_.x)`'s CONSTRUCTION-TIME
  // schema-name resolution. Direct calls (private[avro], same package) exercise every Left / throw
  // arm the happy `.field(_.x)` paths never touch.

  // covers: schemaAt Field-step on a non-record schema -> Left("expected a record..."),
  //   schemaAt Field-step whose name is absent from the record -> Left("has no field..."),
  //   schemaAt UnionBranch-step on a non-union schema -> Left("expected a union..."),
  //   schemaAt UnionBranch-step whose branch name isn't declared -> Left("has no branch..."),
  //   schemaAt Index-step on neither an array nor a map -> Left("expected an array/map..."),
  //   schemaAt Index-step happy arms (array element type / map value type)
  "AvroWalk.schemaAt: every non-happy branch (non-record, missing field, non-union, unknown branch, non-array/map) + Index happy arms" >> {
    val stringSchema = Schema.create(Schema.Type.STRING)
    val unionSchema =
      Schema.createUnion(
        Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG))
      )

    val nonRecordOk = AvroWalk.schemaAt(stringSchema, Array(PathStep.Field("x"))).isLeft === true
    val missingFieldOk =
      AvroWalk.schemaAt(personSchema, Array(PathStep.Field("nope"))).isLeft === true
    val nonUnionOk =
      AvroWalk.schemaAt(stringSchema, Array(PathStep.UnionBranch("long"))).isLeft === true
    val unknownBranchOk =
      AvroWalk.schemaAt(unionSchema, Array(PathStep.UnionBranch("string"))).isLeft === true
    val nonArrayMapOk = AvroWalk.schemaAt(stringSchema, Array(PathStep.Index(0))).isLeft === true

    val arraySchema = Schema.createArray(stringSchema)
    val mapSchema = Schema.createMap(stringSchema)
    val arrayOk = AvroWalk.schemaAt(arraySchema, Array(PathStep.Index(0))) === Right(stringSchema)
    val mapOk = AvroWalk.schemaAt(mapSchema, Array(PathStep.Index(0))) === Right(stringSchema)

    nonRecordOk
      .and(missingFieldOk)
      .and(nonUnionOk)
      .and(unknownBranchOk)
      .and(nonArrayMapOk)
      .and(arrayOk)
      .and(mapOk)
  }

  // covers: resolveFieldName's declIdx < 0 fallback returns the literal scalaName WITHOUT
  //   consulting the schema at all (a non-record root would make schemaAt fail loudly if it were
  //   consulted, proving the -1 branch short-circuits before that)
  "AvroWalk.resolveFieldName: declIdx < 0 short-circuits to the literal scalaName" >> {
    val stringSchema = Schema.create(Schema.Type.STRING)
    AvroWalk.resolveFieldName(stringSchema, Array.empty, "literalName", -1, "test") ===
      "literalName"
  }

  // covers: fieldNameAt on a non-record schema throws IllegalArgumentException,
  //   fieldNameAt with declIdx past the field count throws IllegalArgumentException
  "AvroWalk.fieldNameAt: non-record parent and out-of-range declIdx both throw loudly" >> {
    val stringSchema = Schema.create(Schema.Type.STRING)
    val nonRecordThrows =
      try
        AvroWalk.fieldNameAt(stringSchema, "x", 0, "test")
        false
      catch case _: IllegalArgumentException => true

    val outOfRangeThrows =
      try
        AvroWalk.fieldNameAt(personSchema, "x", 99, "test")
        false
      catch case _: IllegalArgumentException => true

    (nonRecordThrows === true).and(outOfRangeThrows === true)
  }

end AvroWalkSpec
