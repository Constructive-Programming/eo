package dev.constructive.eo
package bench

/** What the benchmarks share, in scope for every file in this package and, through a chained
  * package clause, `bench.fixture`. Keeps each bench's imports to what is particular to it. Not
  * here: `fixture.*` (a package can't be wildcard-exported) and `uninitialized` (compiler magic
  * that only works under its own name).
  */
export _root_.vulcan.Codec as VCodec
export com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray}
export dev.constructive.eo.samples.{Bin, Expr}
export hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
export monocle.{
  Getter as MGetter,
  Lens as MLens,
  Optional as MOptional,
  Setter as MSetter,
  Traversal as MTraversal,
}
export org.apache.avro.generic.IndexedRecord
export org.openjdk.jmh.annotations.{Benchmark, OperationsPerInvocation, Param, Setup, TearDown}

// JMH's `Level` is a Java enum: the type alias gives the type, the object its constants. A
// re-exported constant still compiles to the enum constant in `@Setup(Level.Iteration)`.
type Level = org.openjdk.jmh.annotations.Level

object Level:
  export org.openjdk.jmh.annotations.Level.*
