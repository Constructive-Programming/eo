package dev.constructive.eo.avro

import scala.util.control.NonFatal

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import java.io.ByteArrayOutputStream
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

/** Factory for the byte-carried Avro optic: re-carrier an [[AvroPrism]] onto the raw binary
  * payload, so the WIRE FORM is the source — `Optic[Array[Byte], Array[Byte], A, A, Affine]`.
  *
  * This is the optic-shaped face of the slice/graft machinery ([[AvroBinaryCursor]]): where the
  * prism's `sliceBytes` / `graftBytes` are plain methods trafficking in encoded fragments, this
  * factory packs the same locate → decode / encode → splice round-trip into `to` / `from`, so the
  * whole capability-gated extension surface lights up on the bytes themselves — `.getOption`,
  * `.modify`, `.replace`, `.foldMap`, `.andThen`, … — no `IndexedRecord` materialised.
  *
  * Mirrors [[dev.constructive.eo.jsoniter.JsoniterPrism]] shape-for-shape:
  *
  *   - Carrier [[Affine]], `type X = (Array[Byte], (Array[Byte], BinarySpan))`:
  *     - `Fst[X] = Array[Byte]` — original payload; `Miss` carries it for pass-through (parse
  *       failure, path miss, union-branch mismatch, decode failure).
  *     - `Snd[X] = (Array[Byte], BinarySpan)` — payload + located span; `Hit` carries them so
  *       `from` can splice the re-encoded focus back in place.
  *   - `to` locates the focused field's byte span via [[AvroBinaryCursor.locate]] (strict on the
  *     terminal union branch, like `sliceBytes`) and decodes the value slice through the prism's
  *     [[AvroCodec]].
  *   - `from` on a `Hit` encodes the focus under the span's resolved schema and splices `prefix ++
  *     (synthesised branch index) ++ encoded ++ suffix` — three `arraycopy`s, no record decode. On
  *     a `Miss` (or an encode failure) the original bytes pass through unchanged.
  *
  * Same scope limits as the cursor: [[PathStep.Index]] paths are unsupported (they `Miss`), and a
  * `.fields(...)` prism addresses the enclosing PARENT record's span.
  */
object AvroBytesPrism:

  /** The byte-carried counterpart of `prism` — same focus, `Array[Byte]` on both ends. Also
    * reachable as [[AvroPrism.bytes]]:
    *
    * {{{
    *   val ageBytes = codecPrism[Person].field(_.age).bytes
    *   ageBytes.modify(_ + 1)(payload)   // Array[Byte] => Array[Byte], no IndexedRecord
    * }}}
    */
  def apply[A](prism: AvroPrism[A]): Optic[Array[Byte], Array[Byte], A, A, Affine] =
    new Optic[Array[Byte], Array[Byte], A, A, Affine]:
      type X = (Array[Byte], (Array[Byte], AvroBinaryCursor.BinarySpan))

      def to(bytes: Array[Byte]): Affine[X, A] = scan(bytes)
      def from(aff: Affine[X, A]): Array[Byte] = splice(aff)

      private def scan(bytes: Array[Byte]): Affine[X, A] =
        AvroBinaryCursor.locate(
          bytes,
          prism.rootSchemaCached,
          prism.path,
          strictTerminalUnion = true,
        ) match
          case Left(_)     => new Affine.Miss[X, A](bytes)
          case Right(span) =>
            decodeSlice(bytes, span) match
              case Right(a) => new Affine.Hit[X, A]((bytes, span), a)
              case Left(_)  => new Affine.Miss[X, A](bytes)

      private def decodeSlice(
          bytes: Array[Byte],
          span: AvroBinaryCursor.BinarySpan,
      ): Either[Throwable, A] =
        try
          val reader = new GenericDatumReader[Any](span.valueSchema)
          val decoder = DecoderFactory
            .get()
            .binaryDecoder(bytes, span.valueStart, span.end - span.valueStart, null)
          prism.codec.decodeEither(reader.read(null, decoder))
        catch case NonFatal(t) => Left(t)

      private def splice(aff: Affine[X, A]): Array[Byte] =
        aff match
          case m: Affine.Miss[X, A] => m.fst
          case h: Affine.Hit[X, A]  =>
            val (src, span) = h.snd
            encodeValue(h.b, span) match
              // ponytail: silent pass-through on encode failure — from has no failure channel;
              // callers needing diagnostics use the prism's Ior-bearing byte surface
              case Left(_)        => src
              case Right(encoded) => AvroBinaryCursor.splice(src, span, encoded)

      private def encodeValue(
          a: A,
          span: AvroBinaryCursor.BinarySpan,
      ): Either[Throwable, Array[Byte]] =
        try
          val out = new ByteArrayOutputStream()
          val writer = new GenericDatumWriter[Any](span.valueSchema)
          val encoder = EncoderFactory.get().binaryEncoder(out, null)
          writer.write(prism.codec.encode(a), encoder)
          encoder.flush()
          Right(out.toByteArray)
        catch case NonFatal(t) => Left(t)

end AvroBytesPrism
