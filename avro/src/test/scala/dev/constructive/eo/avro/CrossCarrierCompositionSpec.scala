package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import dev.constructive.eo.generics.lens
import dev.constructive.eo.optics.{AffineFold, Lens, Optic}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.{GenericData, IndexedRecord}
import org.specs2.mutable.Specification

/** Unit 9: cross-carrier composition regression specs for the Avro carrier.
  *
  * Mirrors `dev.constructive.eo.circe.CrossCarrierCompositionSpec` block-for-block: 5 named
  * scenarios, one per macro shape, each asserting success / miss / diagnostic.
  *
  * '''Avro-vs-circe deviation.''' The circe analog uses `case class Box(payload: Json)` /
  * `Envelope(tag: String, payload: Json)` because `Json` is a value type that participates cleanly
  * in case-class equality. The Avro carrier is `IndexedRecord` (a Java interface), so the outer
  * `payload` field has to be typed as `IndexedRecord` (any `GenericRecord` instance is assignable).
  * Case-class equality on `Box(record)` then routes through Java's `Record.equals` — which is
  * schema-instance-sensitive — but the spec body never compares whole `Box` values, only the
  * focused payload via the [[Eq]]`[IndexedRecord]` given from the package object. That keeps the
  * structural-equality assertion semantics aligned with circe's `Json.===`.
  */
class CrossCarrierCompositionSpec extends Specification:

  import AvroSpecFixtures.*
  import CrossCarrierCompositionSpec.*
  import CrossCarrierCompositionSpec.given

  // covers: chain on adult payload returns Some(name), chain on minor returns
  // None (AffineFold miss), chain on a payload that doesn't decode returns None
  "(1) plain Lens → AvroFieldsPrism → AffineFold: success / minor-miss / undecodable miss" >> {
    val box = Lens[Box, IndexedRecord](_.payload, (b, r) => b.copy(payload = r))
    val personFields: Optic[IndexedRecord, IndexedRecord, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val adultName: AffineFold[NameAge, String] =
      AffineFold(nt => Option.when(nt.age >= 18)(nt.name))
    val boxToFields: Optic[Box, Box, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      box.andThen(personFields)
    val chain: Box => Option[String] =
      (b: Box) => boxToFields.getOption(b).flatMap(nt => adultName.getOption(nt))

    val adultBox = Box(personRecord(Person("Alice", 30)))
    val minorBox = Box(personRecord(Person("Bob", 12)))
    // An "undecodable" Avro record: a record under a schema that has no `name` field at all.
    val brokenSchema = {
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "tag",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Broken", null, "eo.avro.test", false, fields)
    }
    val brokenRec = new GenericData.Record(brokenSchema)
    brokenRec.put(0, "not a person")
    val brokenBox = Box(brokenRec)

    (chain(adultBox) === Some("Alice"))
      .and(chain(minorBox) === None)
      .and(chain(brokenBox) === None)
  }

  // covers: getOption reads the embedded name on a valid envelope, getOption returns
  // None when the payload doesn't decode, diagnostic case via direct AvroPrism
  "(2) generics lens[S](_.field) → single-field AvroPrism: success / Affine miss / Ior diagnostic" >> {
    val outer = Lens[Envelope, IndexedRecord](_.payload, (e, r) => e.copy(payload = r))
    val inner: Optic[IndexedRecord, IndexedRecord, String, String, Either] =
      codecPrism[Person].field(_.name)
    val chain = outer.andThen(inner)

    val validEnv = Envelope("env", personRecord(Person("Alice", 30)))
    // An envelope whose payload is an empty record under a schema with no `name` field.
    val emptySchema = {
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "other",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Empty", null, "eo.avro.test", false, fields)
    }
    val emptyRec = new GenericData.Record(emptySchema)
    emptyRec.put(0, Integer.valueOf(0))
    val emptyEnv = Envelope("env", emptyRec)

    val direct = codecPrism[Person](personSchema).field(_.name)
    val diagOk = direct.modify(_.toUpperCase)(emptyRec) match
      case Ior.Both(c, _) => c.headOption.get === AvroFailure.PathMissing(PathStep.Field("name"))
      case other          => ko(s"expected Ior.Both, got $other")

    (chain.getOption(validEnv) === Some("Alice"))
      .and(chain.getOption(emptyEnv) === None)
      .and(diagOk)
  }

  // covers: modify updates name and age through the cross-carrier chain,
  // concrete-class Ior surface preserves age (direct call), diagnostic case:
  // missing age surfaces through direct Ior surface
  "(3) generics lens → multi-field AvroPrism (.fields): cross-carrier modify + Ior diagnostic" >> {
    val outerGen: Optic[Envelope, Envelope, IndexedRecord, IndexedRecord, Tuple2] =
      lens[Envelope](_.payload)
    val inner: Optic[IndexedRecord, IndexedRecord, NameAge, NameAge, Either] =
      codecPrism[Person].fields(_.name, _.age)
    val chain: Optic[Envelope, Envelope, NameAge, NameAge, dev.constructive.eo.data.Affine] =
      outerGen.andThen(inner)
    val validEnv = Envelope("env", personRecord(Person("Alice", 30)))

    val f: NameAge => NameAge = nt => (name = nt.name.toUpperCase, age = nt.age + 1)
    val out: Envelope = chain.modify(f)(validEnv)
    val outName = out.payload.asInstanceOf[GenericData.Record].get("name").toString
    val outAge = out.payload.asInstanceOf[GenericData.Record].get("age").asInstanceOf[Int]
    val modOk = (outName === "ALICE").and(outAge === 31)

    val p = Person("Alice", 30)
    val concrete = codecPrism[Person].fields(_.name, _.age)
    val concreteOk = concrete.modify(f)(personRecord(p)) match
      case Ior.Right(rec) =>
        val nameOut = rec.asInstanceOf[GenericData.Record].get("name").toString
        val ageOut = rec.asInstanceOf[GenericData.Record].get("age").asInstanceOf[Int]
        (nameOut === "ALICE").and(ageOut === 31)
      case other => ko(s"expected Ior.Right, got $other")

    // Diagnostic case: a record under a schema that has only the `name` field but not `age`. The
    // multi-field prism should accumulate one PathMissing(age) failure.
    val nameOnlySchema = {
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "name",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    }
    val nameOnlyRec = new GenericData.Record(nameOnlySchema)
    nameOnlyRec.put(0, "Alice")
    val diagOk = codecPrism[Person](personSchema).fields(_.name, _.age).get(nameOnlyRec) match
      case Ior.Left(c) =>
        c.toList.contains(AvroFailure.PathMissing(PathStep.Field("age"))) === true
      case other => ko(s"expected Ior.Left, got $other")

    modOk.and(concreteOk).and(diagOk)
  }

  // covers: manual composition outer.modify(trav.modifyUnsafe(f)) works,
  // manual composition with Ior unwrap, diagnostic case: traversal.modify directly
  // observes chain failures
  "(4) generics lens → AvroTraversal: manual composition (Unsafe + Ior unwrap + diagnostic)" >> {
    val outer = Lens[Envelope, IndexedRecord](_.payload, (e, r) => e.copy(payload = r))
    val trav = codecPrism[Basket].items.each.name

    val basket = Basket("Alice", List(Order("x", 1.0, 1), Order("y", 2.0, 2), Order("z", 3.0, 3)))
    val env = Envelope("env", basketRecord(basket))
    val expected = basketRecord(
      basket.copy(items = List(Order("X", 1.0, 1), Order("Y", 2.0, 2), Order("Z", 3.0, 3)))
    )

    val unsafeOk = recordsEqual(
      outer.modify(trav.modifyUnsafe((s: String) => s.toUpperCase))(env).payload,
      expected,
    )

    val iorOk = {
      val out = outer.modify { (r: IndexedRecord) =>
        trav.modify((s: String) => s.toUpperCase)(r) match
          case Ior.Right(v)   => v
          case Ior.Both(_, v) => v
          case Ior.Left(_)    => r
      }(env)
      recordsEqual(out.payload, expected)
    }

    // Diagnostic case: a basket whose `items` array contains one element whose `name` slot is a
    // primitive (not a record) — the per-element walk surfaces NotARecord. Build the array by
    // hand so the per-element shape is genuinely broken.
    val brokenElems: Seq[AnyRef] = Seq(
      "oops".asInstanceOf[AnyRef],
      orderRecord(Order("y", 2.0, 2)),
    )
    val brokenBasket = basketRoot(brokenElems)
    val diagOk = trav.modify((s: String) => s.toUpperCase)(brokenBasket) match
      case Ior.Both(c, _) =>
        // The per-element walk surfaces NotARecord(prefix-terminal) for the primitive element,
        // since the focus's per-element hook expects an IndexedRecord.
        c.headOption.get match
          case AvroFailure.NotARecord(_) => ok
          case other                     => ko(s"expected NotARecord, got $other")
      case other => ko(s"expected Ior.Both, got $other")

    (unsafeOk === true).and(iorOk === true).and(diagOk)
  }

  // covers: manual composition happy path (Unsafe), manual composition default Ior,
  // diagnostic case: missing per-element field surfaces through direct .modify
  "(5) generics lens → multi-field AvroTraversal: manual (Unsafe + Ior + per-elem diagnostic)" >> {
    val outer = Lens[Envelope, IndexedRecord](_.payload, (e, r) => e.copy(payload = r))
    val fieldsT = codecPrism[Basket].items.each.fields(_.name, _.price)

    val basket = Basket("Alice", List(Order("x", 1.0, 1), Order("y", 2.0, 2)))
    val env = Envelope("env", basketRecord(basket))
    val expectedDoubled = basketRecord(
      basket.copy(items = List(Order("X", 2.0, 1), Order("Y", 4.0, 2)))
    )

    val unsafeOk = recordsEqual(
      outer
        .modify(
          fieldsT.modifyUnsafe(nt => (name = nt.name.toUpperCase, price = nt.price * 2): NamePrice)
        )(env)
        .payload,
      expectedDoubled,
    )

    val expectedPrice = basketRecord(
      basket.copy(items = List(Order("x", 2.0, 1), Order("y", 4.0, 2)))
    )
    val iorOk = {
      val out = outer.modify { (r: IndexedRecord) =>
        fieldsT.modify(nt => (name = nt.name, price = nt.price * 2): NamePrice)(r) match
          case Ior.Right(v)   => v
          case Ior.Both(_, v) => v
          case Ior.Left(_)    => r
      }(env)
      recordsEqual(out.payload, expectedPrice)
    }

    // Diagnostic case: a basket whose first element is a record under a schema lacking `price`,
    // so the per-element multi-field walk surfaces PathMissing(price).
    val noPriceSchema = {
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "name",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      fields.add(
        new org.apache.avro.Schema.Field(
          "qty",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Order", null, "eo.avro.test", false, fields)
    }
    val noPriceRec = new GenericData.Record(noPriceSchema)
    noPriceRec.put(0, "x")
    noPriceRec.put(1, Integer.valueOf(1))
    val brokenItems: Seq[AnyRef] = Seq(noPriceRec, orderRecord(Order("y", 2.0, 2)))
    val brokenBasket = basketRoot(brokenItems)
    val diagOk =
      fieldsT.modify(nt => (name = nt.name, price = nt.price): NamePrice)(brokenBasket) match
        case Ior.Both(c, _) =>
          c.toList.contains(AvroFailure.PathMissing(PathStep.Field("price"))) === true
        case other => ko(s"expected Ior.Both, got $other")

    (unsafeOk === true).and(iorOk === true).and(diagOk)
  }

  /** Compare two records via apache-avro's structural compare. Mirrors the equivalent helpers in
    * the per-spec test files (`AvroPrismSpec.recordsEqual`, etc.); the public [[cats.Eq]] given for
    * `IndexedRecord` does the same job but the per-spec helper keeps these comparisons independent
    * of the cats `===` vs structural-`===` distinction.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

end CrossCarrierCompositionSpec

object CrossCarrierCompositionSpec:

  /** Outer carrier for scenario (1) — a single-field case class wrapping an Avro record. The
    * generics `lens[Box](_.payload)` macro returns a `BijectionIso` for full-cover 1-arity case
    * classes (per CLAUDE.md), so scenario (1) uses the explicit `Lens.apply` constructor instead.
    */
  case class Box(payload: IndexedRecord)

  /** Outer carrier for scenarios (2)–(5) — a tagged envelope around an Avro record. Two fields so
    * the generics `lens[Envelope](_.payload)` macro returns a `SimpleLens` (the partial-cover shape
    * that the cross-carrier scenarios need).
    */
  case class Envelope(tag: String, payload: IndexedRecord)

  // The two NamedTuple types the `.fields(_.name, _.age)` and `.fields(_.name, _.price)` macros
  // synthesise. Kept at the spec-object level so the spec body can ascribe values to them.

  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]

  given AvroEncoder[NameAge] = AvroEncoder.derived
  given AvroDecoder[NameAge] = AvroDecoder.derived
  given AvroSchemaFor[NameAge] = AvroSchemaFor.derived

  given AvroEncoder[NamePrice] = AvroEncoder.derived
  given AvroDecoder[NamePrice] = AvroDecoder.derived
  given AvroSchemaFor[NamePrice] = AvroSchemaFor.derived

end CrossCarrierCompositionSpec
