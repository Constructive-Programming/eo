package dev.constructive.eo.avro

import scala.util.control.NonFatal

import java.io.InputStream
import org.apache.avro.Schema
import org.apache.avro.io.{BinaryData, Decoder, DecoderFactory}

/** Internal byte-offset locator for the slice/graft surface on [[AvroPrism]].
  *
  * Given the binary encoding of a record under `rootSchema`, [[locate]] resolves an optic path to
  * the byte span `[fieldStart, end)` of the focused field's encoded value — WITHOUT materialising
  * an `IndexedRecord`. The walk drives apache-avro's `directBinaryDecoder` (the non-buffering
  * variant, so stream position == logical decode position) over a position-counting
  * [[InputStream]], skipping non-target fields in schema order via a hand-rolled skip switch over
  * `Schema.Type`.
  *
  * '''Scope (Phase 1 of the zero-copy plan).''' Only the offset machinery needed by `sliceBytes` /
  * `graftBytes` lives here — [[PathStep.Field]] descent through records and
  * [[PathStep.UnionBranch]] resolution. [[PathStep.Index]] is deliberately unsupported (array
  * elements sit inside length-prefixed blocks whose framing a graft would have to rewrite) and
  * surfaces as [[AvroFailure.UnsupportedSpanStep]]. A full byte-native read surface (no
  * `IndexedRecord` at all) is a later project; the read/modify path keeps parsing to
  * `IndexedRecord`.
  *
  * Every failure is structured ([[AvroFailure]]), never thrown: truncated / malformed bytes arrive
  * as [[AvroFailure.BinaryParseFailed]], schema misses as [[AvroFailure.PathMissing]] /
  * [[AvroFailure.NotARecord]] / [[AvroFailure.UnionResolutionFailed]].
  */
private[avro] object AvroBinaryCursor:

  /** Byte span of a focused field's encoding inside a binary-encoded record.
    *
    * When the focused position is a union branch (terminal [[PathStep.UnionBranch]]), the span
    * separates the branch-index bytes from the value bytes: `fieldStart` opens the zigzag branch
    * index, `valueStart` opens the branch value, and `branchOrdinal` carries the ordinal of the
    * path's REQUESTED branch (which is what a graft re-synthesises — not necessarily the runtime
    * branch of these bytes when located leniently). Otherwise `valueStart == fieldStart` and
    * `branchOrdinal` is `None` — for a union-TYPED field focused without a `.union[Branch]` step,
    * the branch index is part of the value span (the focus type's encoding IS index + value).
    *
    * @param fieldStart
    *   start (inclusive) of the focused field's full encoding
    * @param valueStart
    *   start (inclusive) of the value bytes (`== fieldStart` unless union-branch-focused)
    * @param end
    *   end (exclusive) of the focused field's encoding
    * @param valueSchema
    *   schema of the value bytes (the branch schema when union-branch-focused)
    * @param branchOrdinal
    *   ordinal of the path's requested union branch; `None` when the terminal step isn't a
    *   [[PathStep.UnionBranch]]
    */
  final case class BinarySpan(
      fieldStart: Int,
      valueStart: Int,
      end: Int,
      valueSchema: Schema,
      branchOrdinal: Option[Int],
  )

  /** Resolve `path` to the byte span of the focused value inside `bytes` (the binary encoding of a
    * record under `rootSchema`).
    *
    * @param strictTerminalUnion
    *   when the TERMINAL step is a [[PathStep.UnionBranch]]: `true` (slice) requires the runtime
    *   branch of `bytes` to match the requested branch (mismatch →
    *   [[AvroFailure.UnionResolutionFailed]]); `false` (graft) tolerates a mismatch — the span
    *   covers whatever branch the bytes currently carry, and `branchOrdinal` still reports the
    *   REQUESTED branch so the graft can re-synthesise its index. Non-terminal union steps are
    *   always strict (the walk cannot descend a branch that isn't there).
    */
  def locate(
      bytes: Array[Byte],
      rootSchema: Schema,
      path: Array[PathStep],
      strictTerminalUnion: Boolean,
  ): Either[AvroFailure, BinarySpan] =
    try locateUncaught(bytes, rootSchema, path, strictTerminalUnion)
    catch case NonFatal(t) => Left(AvroFailure.BinaryParseFailed(t))

  /** Zigzag-varint encoding of `n` (Avro `int` wire form) — used by graft to synthesise a union
    * branch index. Delegates to apache-avro's [[BinaryData.encodeInt]].
    */
  def zigZagInt(n: Int): Array[Byte] =
    val buf = new Array[Byte](5)
    val len = BinaryData.encodeInt(n, buf, 0)
    if len == 5 then buf else java.util.Arrays.copyOf(buf, len)

  /** Assemble prefix + (synthesised branch index, when `span` addresses a union branch) + fragment
    * + suffix — the splice step shared by [[AvroPrism.graftBytes]] and [[AvroBytesPrism]].
    */
  def splice(bytes: Array[Byte], span: BinarySpan, fragment: Array[Byte]): Array[Byte] =
    val index = span.branchOrdinal match
      case Some(ordinal) => zigZagInt(ordinal)
      case None          => Array.emptyByteArray
    val suffixLen = bytes.length - span.end
    val out = new Array[Byte](span.fieldStart + index.length + fragment.length + suffixLen)
    System.arraycopy(bytes, 0, out, 0, span.fieldStart)
    System.arraycopy(index, 0, out, span.fieldStart, index.length)
    System.arraycopy(fragment, 0, out, span.fieldStart + index.length, fragment.length)
    System.arraycopy(
      bytes,
      span.end,
      out,
      span.fieldStart + index.length + fragment.length,
      suffixLen,
    )
    out

  /** The walk body — may throw (EOF on truncated input, avro decode complaints); [[locate]] wraps
    * every NonFatal throw into [[AvroFailure.BinaryParseFailed]].
    */
  private def locateUncaught(
      bytes: Array[Byte],
      rootSchema: Schema,
      path: Array[PathStep],
      strictTerminalUnion: Boolean,
  ): Either[AvroFailure, BinarySpan] =
    val in = new CountingInputStream(bytes)
    val d = DecoderFactory.get().directBinaryDecoder(in, null)
    var schema = rootSchema
    var failure: AvroFailure | Null = null
    // Populated iff the terminal step is a UnionBranch (index bytes already consumed).
    var unionSpan: BinarySpan | Null = null
    var i = 0
    while i < path.length && failure == null do
      path(i) match
        case step @ PathStep.Field(name) =>
          if schema.getType != Schema.Type.RECORD then failure = AvroFailure.NotARecord(step)
          else
            val field = schema.getField(name)
            if field == null then failure = AvroFailure.PathMissing(step)
            else
              val fields = schema.getFields
              var j = 0
              while j < field.pos do
                skipValue(fields.get(j).schema, d)
                j += 1
              schema = field.schema

        case step @ PathStep.UnionBranch(branchName) =>
          if schema.getType != Schema.Type.UNION then
            failure = AvroFailure.UnionResolutionFailed(declaredBranches(schema), step)
          else
            val requested = branchOrdinalOf(schema, branchName)
            if requested < 0 then
              failure = AvroFailure.UnionResolutionFailed(declaredBranches(schema), step)
            else
              val isTerminal = i == path.length - 1
              val indexStart = in.position
              val runtime = d.readIndex()
              val runtimeSchema = schema.getTypes.get(runtime)
              if runtime != requested && (strictTerminalUnion || !isTerminal) then
                failure = AvroFailure.UnionResolutionFailed(declaredBranches(schema), step)
              else if isTerminal then
                unionSpan = BinarySpan(
                  fieldStart = indexStart,
                  valueStart = in.position,
                  end = 0, // patched below once the branch value is skipped
                  valueSchema = schema.getTypes.get(requested),
                  branchOrdinal = Some(requested),
                )
                schema = runtimeSchema
              else schema = runtimeSchema

        case step @ PathStep.Index(_) =>
          failure = AvroFailure.UnsupportedSpanStep(step)
      i += 1
    if failure != null then Left(failure)
    else
      unionSpan match
        case span: BinarySpan =>
          // Terminal union: index bytes consumed; skip the runtime branch value to find the end.
          skipValue(schema, d)
          Right(span.copy(end = in.position))
        case null =>
          // Terminal field (or empty path = whole record): span opens here.
          val start = in.position
          skipValue(schema, d)
          Right(BinarySpan(start, start, in.position, schema, None))

  /** Skip one encoded value of `schema` — the hand-rolled switch over `Schema.Type`. Records
    * recurse field-by-field, arrays / maps consume block framing (negative block counts carry a
    * byte size that avro's `skipArray` / `skipMap` fast-forward directly), unions read the branch
    * index then skip the branch.
    */
  private def skipValue(schema: Schema, d: Decoder): Unit =
    schema.getType match
      case Schema.Type.NULL    => d.readNull()
      case Schema.Type.BOOLEAN => d.readBoolean(): Unit
      case Schema.Type.INT     => d.readInt(): Unit
      case Schema.Type.LONG    => d.readLong(): Unit
      case Schema.Type.FLOAT   => d.readFloat(): Unit
      case Schema.Type.DOUBLE  => d.readDouble(): Unit
      case Schema.Type.STRING  => d.skipString()
      case Schema.Type.BYTES   => d.skipBytes()
      case Schema.Type.FIXED   => d.skipFixed(schema.getFixedSize)
      case Schema.Type.ENUM    => d.readEnum(): Unit
      case Schema.Type.UNION   => skipValue(schema.getTypes.get(d.readIndex()), d)
      case Schema.Type.RECORD  =>
        val fields = schema.getFields
        val n = fields.size
        var i = 0
        while i < n do
          skipValue(fields.get(i).schema, d)
          i += 1
      case Schema.Type.ARRAY =>
        val elem = schema.getElementType
        var l = d.skipArray()
        while l > 0 do
          var i = 0L
          while i < l do
            skipValue(elem, d)
            i += 1
          l = d.skipArray()
      case Schema.Type.MAP =>
        val value = schema.getValueType
        var l = d.skipMap()
        while l > 0 do
          var i = 0L
          while i < l do
            d.skipString()
            skipValue(value, d)
            i += 1
          l = d.skipMap()

  /** Ordinal of the union alternative whose full name is `branchName`, or -1. Matches the naming
    * convention of [[PathStep.UnionBranch]] (schema full names — `"long"` for primitives,
    * `"my.ns.Record"` for records).
    */
  private def branchOrdinalOf(union: Schema, branchName: String): Int =
    val types = union.getTypes
    val n = types.size
    var i = 0
    var found = -1
    while i < n && found < 0 do
      if types.get(i).getFullName == branchName then found = i
      i += 1
    found

  /** Declared alternative names of a union schema (empty for non-unions) — the diagnostic payload
    * of [[AvroFailure.UnionResolutionFailed]].
    */
  private def declaredBranches(schema: Schema): List[String] =
    if schema.getType != Schema.Type.UNION then Nil
    else
      val types = schema.getTypes
      List.tabulate(types.size)(i => types.get(i).getFullName)

  /** Position-counting [[InputStream]] over an `Array[Byte]` — the substrate that makes the direct
    * (non-buffering) binary decoder's consumption observable as byte offsets. All three read shapes
    * plus `skip` keep [[position]] exact.
    */
  final private class CountingInputStream(bytes: Array[Byte]) extends InputStream:

    private var pos: Int = 0

    def position: Int = pos

    override def read(): Int =
      if pos >= bytes.length then -1
      else
        val b = bytes(pos) & 0xff
        pos += 1
        b

    override def read(b: Array[Byte], off: Int, len: Int): Int =
      if pos >= bytes.length then -1
      else
        val n = math.min(len, bytes.length - pos)
        System.arraycopy(bytes, pos, b, off, n)
        pos += n
        n

    override def skip(n: Long): Long =
      val k = math.min(n, (bytes.length - pos).toLong).max(0L)
      pos += k.toInt
      k

    override def available(): Int = bytes.length - pos

  end CountingInputStream

end AvroBinaryCursor
