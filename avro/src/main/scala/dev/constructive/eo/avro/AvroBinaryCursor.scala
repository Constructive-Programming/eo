package dev.constructive.eo.avro

import scala.annotation.tailrec
import scala.util.control.NonFatal

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.{Arrays, List as JList}
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.io.{BinaryData, Decoder, DecoderFactory}

/** Internal byte-offset locator behind [[AvroPrism]]'s byte-carried optic (`to`/`from`) and its
  * slice/graft surface.
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
  * surfaces as [[AvroFailure.UnsupportedSpanStep]] — the byte optic Misses on such paths; the
  * record face (`.record`) keeps supporting Index steps through the parsed walk.
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
    locateFrom(bytes, rootSchema, path, strictTerminalUnion, startPos = 0)

  /** [[locate]] starting at an interior offset — `bytes(startPos)` must open an encoding of
    * `schema`. Backs the per-element focus walk of [[locateElements]].
    */
  def locateFrom(
      bytes: Array[Byte],
      schema: Schema,
      path: Array[PathStep],
      strictTerminalUnion: Boolean,
      startPos: Int,
  ): Either[AvroFailure, BinarySpan] =
    try locateUncaught(bytes, schema, path, strictTerminalUnion, startPos)
    catch case NonFatal(t) => Left(AvroFailure.BinaryParseFailed(t))

  /** One array element's boundaries plus its focused sub-span. `focus = None` when the suffix walk
    * refused this element (path miss, branch mismatch, index step) — or, later, when the slice
    * decode refused: the traversal demotes those so spans and foci stay 1:1.
    */
  final case class ElementSpan(start: Int, end: Int, focus: Option[BinarySpan])

  /** The focused byte spans of an array-terminal walk — the traversal counterpart of [[locate]].
    *
    * Resolves `prefix` to an array-typed field, walks the array's block framing, and resolves
    * `suffix` inside EVERY element. Whole-walk failures (bad prefix, non-array terminal, truncated
    * bytes) surface as `Left`.
    *
    * `canonicalFraming = false` flags payloads whose array used the negative-count (byte-sized)
    * block framing: in-place splices would leave the recorded block byte-sizes stale, so write
    * paths must go through [[reframeArray]] instead of [[spliceAll]].
    */
  final case class ElementSpans(
      array: BinarySpan,
      elements: Vector[ElementSpan],
      canonicalFraming: Boolean,
  )

  def locateElements(
      bytes: Array[Byte],
      rootSchema: Schema,
      prefix: Array[PathStep],
      suffix: Array[PathStep],
  ): Either[AvroFailure, ElementSpans] =
    locate(bytes, rootSchema, prefix, strictTerminalUnion = true).flatMap { arr =>
      if arr.valueSchema.getType != Schema.Type.ARRAY then
        Left(AvroFailure.NotAnArray(AvroWalk.terminalOf(prefix)))
      else
        try Right(walkElementsUncaught(bytes, arr, suffix))
        catch case NonFatal(t) => Left(AvroFailure.BinaryParseFailed(t))
    }

  private def walkElementsUncaught(
      bytes: Array[Byte],
      arr: BinarySpan,
      suffix: Array[PathStep],
  ): ElementSpans =
    val elemSchema = arr.valueSchema.getElementType
    val in = new CountingInputStream(bytes, arr.valueStart)
    val d = DecoderFactory.get().directBinaryDecoder(in, null)
    val elements = Vector.newBuilder[ElementSpan]
    val canonical =
      walkElementBlocks(bytes, elemSchema, suffix, in, d, elements, d.readLong(), true)
    ElementSpans(arr, elements.result(), canonical)

  /** Walk `remaining` elements of one array block: skip each element's encoding, then run the
    * per-element focus locate (`suffix`) on a fresh cursor at the element start, recording an
    * [[ElementSpan]]. A focus-walk failure keeps the element (marked focus-less) so its bytes
    * survive a re-frame.
    */
  @tailrec private def walkElementItems(
      bytes: Array[Byte],
      elemSchema: Schema,
      suffix: Array[PathStep],
      in: CountingInputStream,
      d: Decoder,
      elements: scala.collection.mutable.Builder[ElementSpan, Vector[ElementSpan]],
      remaining: Long,
  ): Unit =
    if remaining > 0L then
      val elemStart = in.position
      skipValue(elemSchema, d)
      val focus = locateFrom(
        bytes,
        elemSchema,
        suffix,
        strictTerminalUnion = true,
        startPos = elemStart,
      ).toOption
      elements += ElementSpan(elemStart, in.position, focus)
      walkElementItems(bytes, elemSchema, suffix, in, d, elements, remaining - 1L)

  /** Walk one array's `d.readLong()`-framed block structure from an already-read leading `count`,
    * threading whether the framing has stayed canonical (positive counts only — negative byte-sized
    * blocks make in-place splices unsafe). Returns the final canonical flag; companion of
    * [[walkElementItems]].
    */
  @tailrec private def walkElementBlocks(
      bytes: Array[Byte],
      elemSchema: Schema,
      suffix: Array[PathStep],
      in: CountingInputStream,
      d: Decoder,
      elements: scala.collection.mutable.Builder[ElementSpan, Vector[ElementSpan]],
      count: Long,
      canonical: Boolean,
  ): Boolean =
    if count == 0L then canonical
    else
      // -Long.MinValue == Long.MinValue: negating leaves it negative, the item loop would be
      // skipped, and the walk would confidently misparse element data as block framing.
      val negative = count < 0L
      if negative && count == Long.MinValue then
        throw new java.io.IOException("array block count Long.MinValue is not a valid framing")
      val n = if negative then -count else count
      if negative then
        d.readLong(): Unit // block byte-size — goes stale across a size-changing splice
      walkElementItems(bytes, elemSchema, suffix, in, d, elements, n)
      walkElementBlocks(
        bytes,
        elemSchema,
        suffix,
        in,
        d,
        elements,
        d.readLong(),
        canonical && !negative,
      )

  /** Zigzag-varint encoding of `n` (Avro `long` wire form) — the block count emitted by
    * [[reframeArray]].
    */
  def zigZagLong(n: Long): Array[Byte] =
    val buf = new Array[Byte](10)
    val len = BinaryData.encodeLong(n, buf, 0)
    if len == 10 then buf else Arrays.copyOf(buf, len)

  /** Rebuild the whole array region with CANONICAL framing (one positive-count block + zero
    * terminator), applying per-element focus replacements — the write path for payloads whose
    * original framing carried block byte-sizes that an in-place splice would falsify.
    *
    * `encodedFoci` aligns 1:1, in order, with the elements whose `focus` is defined; `None` entries
    * (encode failures) keep that element's original bytes.
    */
  def reframeArray(
      bytes: Array[Byte],
      plan: ElementSpans,
      encodedFoci: Vector[Option[Array[Byte]]],
  ): Array[Byte] =
    val out = new ByteArrayOutputStream(bytes.length + 16)
    out.write(bytes, 0, plan.array.valueStart)
    if plan.elements.nonEmpty then
      val countBytes = zigZagLong(plan.elements.length.toLong)
      out.write(countBytes, 0, countBytes.length)
    var k = 0
    plan.elements.foreach { e =>
      e.focus match
        case Some(f) =>
          val enc = encodedFoci(k)
          k += 1
          enc match
            case Some(newBytes) =>
              out.write(bytes, e.start, f.fieldStart - e.start)
              // Re-synthesise the requested union branch index when the element focus is a
              // `.each.union[B]` branch (branchOrdinal = Some) — mirrors spliceAll. For a leaf /
              // fields element focus branchOrdinal is None and this is a no-op.
              f.branchOrdinal.foreach { o =>
                val idx = zigZagInt(o)
                out.write(idx, 0, idx.length)
              }
              out.write(newBytes, 0, newBytes.length)
              out.write(bytes, f.end, e.end - f.end)
            case None => out.write(bytes, e.start, e.end - e.start)
        case None => out.write(bytes, e.start, e.end - e.start)
    }
    out.write(0) // zero-count block terminator
    out.write(bytes, plan.array.end, bytes.length - plan.array.end)
    out.toByteArray

  /** Decode the value slice addressed by `span` through `codec` — [[AvroCodec.readDatum]] under the
    * span's resolved schema, then the codec's Any→A side. Shared by [[AvroPrism]]'s `to` and
    * [[AvroTraversal]]'s per-element reads.
    */
  def decodeSlice[A](
      bytes: Array[Byte],
      span: BinarySpan,
      codec: AvroCodec[A],
  ): Either[Throwable, A] =
    try
      codec.decodeEither(
        AvroCodec.readDatum(
          bytes,
          span.valueStart,
          span.end - span.valueStart,
          span.valueSchema,
          span.valueSchema,
          threadLocalStorage = true,
        )
      )
    catch case NonFatal(t) => Left(t)

  /** Encode `a` under the span's resolved schema — the codec's A→Any side, then
    * [[AvroCodec.writeDatum]]. Mirror of [[decodeSlice]]; shared by the prism and traversal write
    * paths for LEAF focuses (the codec's runtime shape matches the span schema exactly).
    */
  def encodeValue[A](
      a: A,
      span: BinarySpan,
      codec: AvroCodec[A],
  ): Either[Throwable, Array[Byte]] =
    try Right(AvroCodec.writeDatum(codec.encode(a), span.valueSchema))
    catch case NonFatal(t) => Left(t)

  /** Encode step for FIELDS focuses — a `.fields(...)` span addresses the PARENT record, and the
    * NamedTuple's runtime record (selected fields, selector order) must NOT be written under the
    * parent schema: avro's `GenericDatumWriter` fetches datum fields by POSITION, so a partial
    * cover blows past the NT arity and a reordered same-typed full cover silently swaps values.
    * Instead: decode the parent slice, overlay the NT fields BY NAME (the same
    * [[AvroFocus.Fields.writeFields]] overlay the record face uses), and re-encode the parent.
    */
  def encodeFieldsOverlay[A](
      bytes: Array[Byte],
      span: BinarySpan,
      fields: AvroFocus.Fields[A],
      a: A,
  ): Either[Throwable, Array[Byte]] =
    try
      val parent = AvroCodec
        .readDatum(
          bytes,
          span.valueStart,
          span.end - span.valueStart,
          span.valueSchema,
          span.valueSchema,
          threadLocalStorage = true,
        )
        .asInstanceOf[IndexedRecord]
      Right(AvroCodec.writeDatum(fields.writeFields(parent, a), span.valueSchema))
    catch case NonFatal(t) => Left(t)

  /** Zigzag-varint encoding of `n` (Avro `int` wire form) — used by graft to synthesise a union
    * branch index. Delegates to apache-avro's [[BinaryData.encodeInt]].
    */
  def zigZagInt(n: Int): Array[Byte] =
    val buf = new Array[Byte](5)
    val len = BinaryData.encodeInt(n, buf, 0)
    if len == 5 then buf else Arrays.copyOf(buf, len)

  /** Assemble prefix + (synthesised branch index, when `span` addresses a union branch) + fragment
    * + suffix — the splice step shared by [[AvroPrism.graftBytes]] and the byte optics' `from`.
    */
  def splice(bytes: Array[Byte], span: BinarySpan, fragment: Array[Byte]): Array[Byte] =
    spliceAll(bytes, Vector((span, fragment)))

  /** One-pass multi-span [[splice]]: `replacements` must be sorted by `fieldStart` and
    * non-overlapping (guaranteed by construction for [[locateElements]] output). Each span's branch
    * index is re-synthesised exactly as in the single-span case.
    */
  def spliceAll(
      bytes: Array[Byte],
      replacements: Vector[(BinarySpan, Array[Byte])],
  ): Array[Byte] =
    var total = bytes.length
    replacements.foreach { (span, enc) =>
      val idxLen = span.branchOrdinal.fold(0)(o => zigZagInt(o).length)
      total += idxLen + enc.length - (span.end - span.fieldStart)
    }
    val out = new Array[Byte](total)
    var src = 0
    var dst = 0
    replacements.foreach { (span, enc) =>
      val pre = span.fieldStart - src
      System.arraycopy(bytes, src, out, dst, pre)
      dst += pre
      span.branchOrdinal.foreach { o =>
        val idx = zigZagInt(o)
        System.arraycopy(idx, 0, out, dst, idx.length)
        dst += idx.length
      }
      System.arraycopy(enc, 0, out, dst, enc.length)
      dst += enc.length
      src = span.end
    }
    System.arraycopy(bytes, src, out, dst, bytes.length - src)
    out

  /** The walk body — may throw (EOF on truncated input, avro decode complaints); [[locate]] wraps
    * every NonFatal throw into [[AvroFailure.BinaryParseFailed]].
    */
  private def locateUncaught(
      bytes: Array[Byte],
      rootSchema: Schema,
      path: Array[PathStep],
      strictTerminalUnion: Boolean,
      startPos: Int,
  ): Either[AvroFailure, BinarySpan] =
    val in = new CountingInputStream(bytes, startPos)
    val d = DecoderFactory.get().directBinaryDecoder(in, null)
    // `unionSpan` is populated iff the terminal step is a UnionBranch (index bytes already read).
    val (failure, schema, unionSpan) =
      locateLoop(path, d, in, strictTerminalUnion, 0, rootSchema, null)
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

  /** The path-walk step machine behind [[locateUncaught]] — advances `schema`/`unionSpan` step by
    * step, skipping non-target siblings via the decoder, until the path is exhausted or a step
    * fails. Returns `(failure, terminalSchema, unionSpan)`; a non-null first component is the
    * structured failure.
    */
  @tailrec private def locateLoop(
      path: Array[PathStep],
      d: Decoder,
      in: CountingInputStream,
      strictTerminalUnion: Boolean,
      i: Int,
      schema: Schema,
      unionSpan: BinarySpan | Null,
  ): (AvroFailure | Null, Schema, BinarySpan | Null) =
    if i >= path.length then (null, schema, unionSpan)
    else
      path(i) match
        case step @ PathStep.Field(name) =>
          if schema.getType != Schema.Type.RECORD then
            (AvroFailure.NotARecord(step), schema, unionSpan)
          else
            val field = schema.getField(name)
            if field == null then (AvroFailure.PathMissing(step), schema, unionSpan)
            else
              skipFieldsBefore(schema.getFields, d, 0, field.pos)
              locateLoop(path, d, in, strictTerminalUnion, i + 1, field.schema, unionSpan)

        case step @ PathStep.UnionBranch(branchName) =>
          if schema.getType != Schema.Type.UNION then
            (AvroFailure.UnionResolutionFailed(declaredBranches(schema), step), schema, unionSpan)
          else
            val requested = branchOrdinalOf(schema, branchName)
            if requested < 0 then
              (
                AvroFailure.UnionResolutionFailed(declaredBranches(schema), step),
                schema,
                unionSpan,
              )
            else
              val isTerminal = i == path.length - 1
              val indexStart = in.position
              val runtime = d.readIndex()
              val runtimeSchema = schema.getTypes.get(runtime)
              if runtime != requested && (strictTerminalUnion || !isTerminal) then
                (
                  AvroFailure.UnionResolutionFailed(declaredBranches(schema), step),
                  schema,
                  unionSpan,
                )
              else if isTerminal then
                val span = BinarySpan(
                  fieldStart = indexStart,
                  valueStart = in.position,
                  end = 0, // patched below once the branch value is skipped
                  valueSchema = schema.getTypes.get(requested),
                  branchOrdinal = Some(requested),
                )
                locateLoop(path, d, in, strictTerminalUnion, i + 1, runtimeSchema, span)
              else locateLoop(path, d, in, strictTerminalUnion, i + 1, runtimeSchema, unionSpan)

        case step @ PathStep.Index(_) =>
          (AvroFailure.UnsupportedSpanStep(step), schema, unionSpan)

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
        skipFieldsBefore(fields, d, 0, fields.size)
      case Schema.Type.ARRAY => skipArrayBlocks(schema.getElementType, d, d.skipArray())
      case Schema.Type.MAP   => skipMapBlocks(schema.getValueType, d, d.skipMap())

  /** Skip the encodings of `fields` in the index range `[j, upTo)` — the leading siblings before a
    * `Field` step's target position (and, with `upTo = size`, a whole record).
    */
  @tailrec private def skipFieldsBefore(
      fields: JList[Schema.Field],
      d: Decoder,
      j: Int,
      upTo: Int,
  ): Unit =
    if j < upTo then
      skipValue(fields.get(j).schema, d)
      skipFieldsBefore(fields, d, j + 1, upTo)

  /** Skip `remaining` array elements of `elem`. */
  @tailrec private def skipArrayItems(elem: Schema, d: Decoder, remaining: Long): Unit =
    if remaining > 0L then
      skipValue(elem, d)
      skipArrayItems(elem, d, remaining - 1L)

  /** Skip a whole `d.skipArray()`-framed array of `elem`, block by block (avro's `skipArray` folds
    * negative byte-sized framing into a plain positive element count).
    */
  @tailrec private def skipArrayBlocks(elem: Schema, d: Decoder, count: Long): Unit =
    if count > 0L then
      skipArrayItems(elem, d, count)
      skipArrayBlocks(elem, d, d.skipArray())

  /** Skip `remaining` map entries (string key + `value` value). */
  @tailrec private def skipMapItems(value: Schema, d: Decoder, remaining: Long): Unit =
    if remaining > 0L then
      d.skipString()
      skipValue(value, d)
      skipMapItems(value, d, remaining - 1L)

  /** Skip a whole `d.skipMap()`-framed map of `value`, block by block. */
  @tailrec private def skipMapBlocks(value: Schema, d: Decoder, count: Long): Unit =
    if count > 0L then
      skipMapItems(value, d, count)
      skipMapBlocks(value, d, d.skipMap())

  /** Ordinal of the union alternative whose full name is `branchName`, or -1. Matches the naming
    * convention of [[PathStep.UnionBranch]] (schema full names — `"long"` for primitives,
    * `"my.ns.Record"` for records).
    */
  private def branchOrdinalOf(union: Schema, branchName: String): Int =
    val types = union.getTypes
    val n = types.size
    @tailrec def loop(i: Int): Int =
      if i >= n then -1
      else if types.get(i).getFullName == branchName then i
      else loop(i + 1)
    loop(0)

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
  final private class CountingInputStream(bytes: Array[Byte], startPos: Int = 0)
      extends InputStream:

    private var pos: Int = startPos

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
