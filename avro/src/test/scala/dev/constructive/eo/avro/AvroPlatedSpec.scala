package dev.constructive.eo
package avro

import scala.annotation.tailrec

import dev.constructive.eo.optics.Plated
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.{GenericData, IndexedRecord}
import org.specs2.mutable.Specification

/** `Plated[IndexedRecord]` — recursive walk over a nested-record Avro tree. Concrete examples (an
  * `Arbitrary[IndexedRecord]` generator would need a self-referential schema, which the universal
  * combinator laws on `Plated[Expr]` / `Plated[Json]` already cover); here we pin the Avro-specific
  * behaviour: children = directly-record-valued fields, transform recurses, structure preserved.
  */
class AvroPlatedSpec extends Specification:

  // Inner { label: string };  Outer { left: Inner, right: Inner, tag: string }
  private val innerSchema =
    SchemaBuilder.record("Inner").fields().requiredString("label").endRecord()

  private val outerSchema =
    SchemaBuilder
      .record("Outer")
      .fields()
      .name("left")
      .`type`(innerSchema)
      .noDefault()
      .name("right")
      .`type`(innerSchema)
      .noDefault()
      .requiredString("tag")
      .endRecord()

  private def inner(label: String): IndexedRecord =
    val r = new GenericData.Record(innerSchema)
    r.put("label", label)
    r

  private def outer(l: String, r: String, tag: String): IndexedRecord =
    val rec = new GenericData.Record(outerSchema)
    rec.put("left", inner(l))
    rec.put("right", inner(r))
    rec.put("tag", tag)
    rec

  // Uppercase a record's `label` field if it has one; leave every other record alone.
  private val upperLabel: IndexedRecord => IndexedRecord = rec =>
    val schema = rec.getSchema
    val field = schema.getField("label")
    if field == null then rec
    else
      val copy = new GenericData.Record(schema)
      val n = schema.getFields.size
      @tailrec def copyFields(i: Int): Unit =
        if i < n then
          copy.put(i, rec.get(i))
          copyFields(i + 1)
      copyFields(0)
      copy.put(field.pos, rec.get(field.pos).toString.toUpperCase)
      copy

  "children are exactly the directly-record-valued fields (left, right — not the String tag)" >> {
    Plated.children(outer("a", "b", "t")).length == 2
  }

  "universe enumerates the outer record and both nested records" >> {
    Plated.universe(outer("a", "b", "t")).length == 3
  }

  "transform recurses into every nested record, leaving the non-record `tag` skeleton intact" >> {
    val result = Plated.transform(upperLabel)(outer("a", "b", "keep"))
    val left = result.get(0).asInstanceOf[IndexedRecord]
    val right = result.get(1).asInstanceOf[IndexedRecord]
    left.get(0).toString == "A" &&
    right.get(0).toString == "B" &&
    result.get(2).toString == "keep"
  }

  "transform(identity) round-trips the record unchanged (structural Eq)" >> {
    val before = outer("a", "b", "t")
    val after = Plated.transform(identity[IndexedRecord])(before)
    summon[cats.Eq[IndexedRecord]].eqv(before, after)
  }
