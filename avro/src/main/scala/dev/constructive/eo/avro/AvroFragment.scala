package dev.constructive.eo.avro

import org.apache.avro.Schema

/** An encoded byte fragment sliced out of a binary Avro payload by
  * [[AvroPrism.sliceBytes]] — the value bytes of one focused field, ready to be hashed, shipped
  * around, or grafted into another payload via [[AvroPrism.graftBytes]].
  *
  * When the slicing prism focused a union branch (`.union[Branch]`), the branch-index bytes are
  * STRIPPED: `bytes` carries the branch value only, `schema` is the branch schema, and
  * `branchOrdinal` reports the branch's ordinal in the union (graft re-synthesises the index from
  * the receiving prism's own path, so the ordinal here is informational — e.g. for
  * schema-fingerprint bookkeeping). For non-union focuses `branchOrdinal` is `None` and `bytes` is
  * the field's full encoding.
  *
  * @param bytes
  *   the encoded value bytes (union branch index stripped)
  * @param schema
  *   the schema the bytes are encoded under (the resolved field / branch schema)
  * @param branchOrdinal
  *   the union branch ordinal when the focus was a union branch; `None` otherwise
  */
final case class AvroFragment(
    bytes: Array[Byte],
    schema: Schema,
    branchOrdinal: Option[Int],
)
