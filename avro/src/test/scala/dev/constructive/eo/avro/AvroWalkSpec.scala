package dev.constructive.eo.avro

import scala.language.implicitConversions

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
    val fields = new java.util.ArrayList[Schema.Field]()
    fields.add(
      new Schema.Field("people", Schema.createArray(personSchema), null, null)
    )
    Schema.createRecord("Wrapper", null, "eo.avro.test", false, fields)

  /** Schema for `record TaggedMap { map<string> tags; }` — used by the map-walk spec. */
  private val taggedMapSchema: Schema =
    val fields = new java.util.ArrayList[Schema.Field]()
    fields.add(
      new Schema.Field("tags", Schema.createMap(Schema.create(Schema.Type.STRING)), null, null)
    )
    Schema.createRecord("TaggedMap", null, "eo.avro.test", false, fields)

  /** Schema for a `union<null, long>` field embedded in a wrapper record. */
  private val maybeLongSchema: Schema =
    val unionSchema = Schema.createUnion(
      java.util.Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG))
    )
    val fields = new java.util.ArrayList[Schema.Field]()
    fields.add(new Schema.Field("amount", unionSchema, null, null))
    Schema.createRecord("MaybeLong", null, "eo.avro.test", false, fields)

  // ---- Record walk -------------------------------------------------

  // covers: walk a 1-deep record path → terminal value matches direct `record.get(name)`
  "walkPath: 1-deep record field" >> {
    val r = personRecord(Person("Alice", 30))
    val result = AvroWalk.walkPath(r, Array(PathStep.Field("name")))
    result match
      case Right((cur, parents)) =>
        (cur.toString === "Alice").and(parents.length === 1).and(parents(0) === r)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // covers: walk a missing record field surfaces PathMissing
  "stepInto strict: missing field surfaces PathMissing" >> {
    val r = personRecord(Person("Alice", 30))
    val result = AvroWalk.walkPath(r, Array(PathStep.Field("nope")))
    result === Left(AvroFailure.PathMissing(PathStep.Field("nope")))
  }

  // covers: walk a missing record field with Lenient policy returns null + parent
  "stepInto lenient: missing field returns null leaf" >> {
    val r = personRecord(Person("Alice", 30))
    val result =
      AvroWalk.walkPath(r, Array(PathStep.Field("nope")), AvroWalk.OnMissingField.Lenient)
    result match
      case Right((cur, parents)) =>
        (cur === null).and(parents.length === 1)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // covers: walking a non-record parent at a Field step surfaces NotARecord
  "stepInto strict: non-record parent surfaces NotARecord" >> {
    val cur: Any = "I am not a record"
    AvroWalk.stepInto(
      PathStep.Field("x"),
      cur,
      Vector.empty,
      AvroWalk.OnMissingField.Strict,
    ) === Left(AvroFailure.NotARecord(PathStep.Field("x")))
  }

  // ---- Array walk --------------------------------------------------

  // covers: walk an array index, sibling indices visible, OOB index surfaces IndexOutOfRange
  "stepInto: array index walk + OOB → IndexOutOfRange" >> {
    val people: GenericData.Array[GenericRecord] =
      buildArray(personSchema, Vector(personRecord(Person("Alice", 30))))
    val wrapper = buildRecord(wrapperSchema)("people" -> people)

    // Happy path: walk into people[0].name
    val result = AvroWalk.walkPath(
      wrapper,
      Array(PathStep.Field("people"), PathStep.Index(0), PathStep.Field("name")),
    )
    val happy = result match
      case Right((cur, parents)) =>
        (cur.toString === "Alice").and(parents.length === 3)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    // OOB path
    val oob = AvroWalk.walkPath(
      wrapper,
      Array(PathStep.Field("people"), PathStep.Index(5)),
    )
    val oobOk = oob === Left(AvroFailure.IndexOutOfRange(PathStep.Index(5), 1))

    happy.and(oobOk)
  }

  // covers: indexing into a non-array surfaces NotAnArray
  "stepInto: non-array parent at Index step surfaces NotAnArray" >> {
    val cur: Any = personRecord(Person("Alice", 30))
    AvroWalk.stepInto(
      PathStep.Index(0),
      cur,
      Vector.empty,
      AvroWalk.OnMissingField.Strict,
    ) === Left(AvroFailure.NotAnArray(PathStep.Index(0)))
  }

  // ---- Map walk ----------------------------------------------------

  // covers: walk into a map<string> entry by key
  "stepInto: map walk by string key" >> {
    val tags = new java.util.LinkedHashMap[String, String]()
    tags.put("env", "prod")
    tags.put("region", "us")
    val record = buildRecord(taggedMapSchema)("tags" -> tags)

    val result = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("tags"), PathStep.Field("env")),
    )
    result match
      case Right((cur, _)) => cur.toString === "prod"
      case other           =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // ---- Union walk --------------------------------------------------

  // covers: union branch matching, union branch mismatch surfaces UnionResolutionFailed
  "stepInto: UnionBranch resolves long alt + mismatched branch surfaces UnionResolutionFailed" >> {
    val record = buildRecord(maybeLongSchema)("amount" -> java.lang.Long.valueOf(42L))
    val resultLong = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("amount"), PathStep.UnionBranch("long")),
    )
    val longOk = resultLong match
      case Right((cur, _)) => cur === java.lang.Long.valueOf(42L)
      case other           =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    // Mismatch: ask for a "string" branch on a long value
    val resultString = AvroWalk.walkPath(
      record,
      Array(PathStep.Field("amount"), PathStep.UnionBranch("string")),
    )
    val stringOk = resultString match
      case Left(AvroFailure.UnionResolutionFailed(_, PathStep.UnionBranch("string"))) =>
        org.specs2.execute.Success(): org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result

    longOk.and(stringOk)
  }

  // ---- terminalOf --------------------------------------------------

  // covers: terminalOf returns sentinel for empty path, last step otherwise
  "terminalOf: empty → Field(\"\"), non-empty → last step" >> {
    val empty = AvroWalk.terminalOf(Array.empty[PathStep])
    val nonEmpty = AvroWalk.terminalOf(Array(PathStep.Field("a"), PathStep.Index(2)))
    (empty === PathStep.Field("")).and(nonEmpty === PathStep.Index(2))
  }

  // ---- Round-trip walk + rebuild -----------------------------------

  // covers: walk + identity rebuild yields a structurally-equal record (immutable boundary).
  "rebuildPath: identity rebuild yields a structurally-equal record" >> {
    val r = personRecord(Person("Alice", 30))
    val path = Array[PathStep](PathStep.Field("name"))
    val result = AvroWalk.walkPath(r, path) match
      case Right((cur, parents)) =>
        val rebuilt = AvroWalk.rebuildPath(parents, path, cur)
        rebuilt.asInstanceOf[IndexedRecord] === r
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
    result
  }

  // covers: walk into people[0].name, modify the leaf, rebuild — yields a new record where the
  // original is unchanged (immutable boundary)
  "rebuildPath: modify leaf produces fresh record, original untouched" >> {
    val originalAlice = personRecord(Person("Alice", 30))
    val people: GenericData.Array[GenericRecord] = buildArray(personSchema, Vector(originalAlice))
    val wrapper = buildRecord(wrapperSchema)("people" -> people)
    val path = Array[PathStep](PathStep.Field("people"), PathStep.Index(0), PathStep.Field("name"))

    val result = AvroWalk.walkPath(wrapper, path) match
      case Right((_, parents)) =>
        val rebuilt = AvroWalk
          .rebuildPath(parents, path, "Bob")
          .asInstanceOf[IndexedRecord]
        // After rebuild, original wrapper is still Alice; rebuilt is Bob.
        val origPeople = wrapper
          .get(wrapperSchema.getField("people").pos)
          .asInstanceOf[java.util.List[GenericRecord]]
        val origName = origPeople.get(0).get(personSchema.getField("name").pos).toString
        val newPeople = rebuilt
          .get(wrapperSchema.getField("people").pos)
          .asInstanceOf[java.util.List[GenericRecord]]
        val newName = newPeople.get(0).get(personSchema.getField("name").pos).toString
        (origName === "Alice").and(newName === "Bob")
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
    result
  }

end AvroWalkSpec
