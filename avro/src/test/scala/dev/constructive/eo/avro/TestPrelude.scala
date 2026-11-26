package dev.constructive.eo
package avro

/** What the avro specs share on top of `SpecPrelude` (spec-kit), in scope for every file in this
  * package and, through a chained package clause, its sub-packages. Keeps each spec's imports to
  * what is particular to it.
  */
export cats.data.{Chain, Ior}
export dev.constructive.eo.laws.discipline.{OptionalTests, SeamTests}
export dev.constructive.eo.laws.{OptionalLaws, SeamLaws}
export dev.constructive.eo.optics.{Lens, Optic}
export dev.constructive.eo.optics.Optic.*
export hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
export java.util.ArrayList
export org.apache.avro.generic.{GenericRecord, IndexedRecord}
export org.scalacheck.Prop.forAll

// Java classes: an `export` of the class alone gives the type but not its statics
// (`Schema.create`, `new GenericData.Record(…)`), so each pairs a type alias with an object that
// re-exports the statics.
type Schema = org.apache.avro.Schema

object Schema:
  export org.apache.avro.Schema.*

  // a nested Java enum's constants are statics too, so they need the same treatment
  object Type:
    export org.apache.avro.Schema.Type.*

type GenericData = org.apache.avro.generic.GenericData

object GenericData:
  export org.apache.avro.generic.GenericData.*
