package dev.constructive.eo.jsoniter.avro

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, JsonValueCodec}
import dev.constructive.eo.avro.AvroCodec
import dev.constructive.eo.avro.jsoniter.{AvroBytes, JsoniterBytes}
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.jsoniter.{JsoniterPrism, JsoniterTraversal}
import dev.constructive.eo.optics.{Getter, Optic}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.EncoderFactory

/** JSON-bytes → Avro-bytes direction of the jsoniter bridge — the mirror of
  * `dev.constructive.eo.avro.jsoniter.AvroJsoniter`, living on the jsoniter side because it extends
  * [[dev.constructive.eo.jsoniter.JsoniterPrism]] (each `.face` extension lives with the optic it
  * extends; cats-eo-avro is an `Optional` dependency of `cats-eo-jsoniter` for this sub-package
  * alone).
  *
  * Unlike `AvroJsoniter`'s structural walk, this direction is '''typed through the root''': a JSON
  * document has no schema of its own, so the whole-document conversion decodes the root `S` via its
  * `JsonValueCodec` and re-encodes through its `AvroCodec` — see [[JsoniterAvro.jsonToAvro]].
  * `JsoniterPrism` erases the root type when drilling, which is why the [[avro]] flip restates it:
  * `JsoniterPrism[Person].name.avro[Person]`.
  */
object JsoniterAvro:

  /** Whole-document JSON bytes → Avro binary bytes — the reverse of
    * `AvroJsoniter.bytesToJson(schema)`, typed through the root `S`: decode `S` with the
    * `JsonValueCodec`, encode with the `AvroCodec`, write binary under `codec.schema`.
    *
    * Total only for documents that decode as `S` — anything else throws at the jsoniter decode
    * (same eager-parse contract as the `.json` face's byte parse).
    * @group optic
    */
  def jsonToAvro[S](using
      jCodec: JsonValueCodec[S],
      aCodec: AvroCodec[S],
  ): Getter[JsoniterBytes, AvroBytes] =
    new Getter(json => writeBinary(aCodec.encode(readFromArray[S](json)), aCodec.schema))

  /** Encode a generic Avro runtime value to its binary wire form under `schema`. Fresh
    * writer/encoder per call (`GenericDatumWriter` construction is cheap; the encoder is not
    * thread-safe).
    */
  private def writeBinary(datum: Any, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[Any](schema).write(datum, encoder)
    encoder.flush()
    out.toByteArray

end JsoniterAvro

/** Avro-carried face of a drilled [[dev.constructive.eo.jsoniter.JsoniterPrism]] — the mirror of
  * `AvroPrism`'s `.json` face: JSON bytes in, '''Avro binary bytes out'''. Drill with the full
  * cursor sugar (`.field(_.x)` / `.at(i)` / Dynamic selection) and flip last, restating the root
  * type (the prism erases it while drilling):
  *
  * {{{
  *   import dev.constructive.eo.jsoniter.JsoniterPrism
  *   import dev.constructive.eo.jsoniter.avro.*
  *
  *   JsoniterPrism[Person].name.avro[Person].modify(_.toUpperCase)(jsonBytes)
  *   // → AvroBytes of the WHOLE doc, name uppercased
  * }}}
  *
  * Reads (`.getOption`) still yield the typed focus `A` straight off the JSON bytes (span scan, no
  * root decode); every write splices the JSON payload exactly like the plain prism, then converts
  * the whole spliced document through [[JsoniterAvro.jsonToAvro]] — which '''is''' a root-typed
  * decode, so `S` must be the type the prism was rooted at (a mismatch throws at the decode; the
  * compiler cannot check it). A path / decode Miss converts the document unchanged; malformed JSON
  * throws at the conversion.
  */
extension [A](p: JsoniterPrism[A])

  def avro[S](using
      JsonValueCodec[S],
      AvroCodec[S],
  ): Optic[JsoniterBytes, AvroBytes, A, A, Affine] =
    Optic
      .outerProfunctor[A, A, Affine]
      .dimap(p)(identity[JsoniterBytes])(JsoniterAvro.jsonToAvro[S].get)

/** Avro-carried face of a drilled [[dev.constructive.eo.jsoniter.JsoniterTraversal]] — `.each`'s
  * multi-focus counterpart of the prism extension above: `.foldMap` / `.all` read the typed
  * elements off the JSON bytes, `.modify` splices every element and converts the whole document to
  * Avro binary. Same root-type caveat.
  */
extension [A](t: JsoniterTraversal[A])

  def avro[S](using
      JsonValueCodec[S],
      AvroCodec[S],
  ): Optic[JsoniterBytes, AvroBytes, A, A, MultiFocus[PSVec]] =
    Optic
      .outerProfunctor[A, A, MultiFocus[PSVec]]
      .dimap(t)(identity[JsoniterBytes])(JsoniterAvro.jsonToAvro[S].get)
