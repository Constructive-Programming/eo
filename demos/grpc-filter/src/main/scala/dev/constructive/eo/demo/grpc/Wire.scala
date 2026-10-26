package dev.constructive.eo.demo.grpc

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.jsoniter.{JsonSlice, JsoniterPrism}
import dev.constructive.eo.optics.Optic
import fs2.Chunk

/** A read-write optic whose focus may be missing: eo-jsoniter's prisms, and [[Model]]'s chains of a
  * prism and lenses.
  */
type Focus[S, A] = Optic[S, S, A, A, Affine]

/** eo-jsoniter optics over a message as it arrives: JSON bytes. Each one scans the bytes for its
  * path and decodes only what it focuses, so no `Issue` is ever built.
  *
  * Classifying and filtering takes two steps. [[detail]] finds the message's `detail` (one scan,
  * past the 60 fields in front of it) and hands out that object's own bytes: a view of them, where
  * they lie in the message. The givens then read the few leaves the policy needs from those few
  * hundred bytes. Every optic here is in place (`JsoniterPrism.inPlace`): nothing is copied, from
  * the socket's read to the leaf.
  *
  * The paths are typed: `.field(_.impact)` is checked against the model when this file compiles, so
  * renaming a field breaks the build rather than the filter.
  */
object Wire:

  /** A message's `detail` object: its own JSON, a view of it where it lies in the message (and the
    * message where it lies in the socket's read). A separate type, so the detail's optics can't be
    * pointed at a whole message. Like any slice it shares the read's array: judge it, don't keep it
    * ([[bytes]] copies it out).
    */
  opaque type DetailJson = JsonSlice

  // Typing a path needs a codec for every object on it; only the leaf's ever decodes anything.
  private given JsonValueCodec[Detail.Bug] = JsonCodecMaker.make
  private given JsonValueCodec[Detail.Incident] = JsonCodecMaker.make
  private given JsonValueCodec[Detail.Request] = JsonCodecMaker.make
  private given JsonValueCodec[Trace] = JsonCodecMaker.make
  private given JsonValueCodec[List[Frame]] = JsonCodecMaker.make
  private given JsonValueCodec[Frame] = JsonCodecMaker.make
  private given JsonValueCodec[Impact] = JsonCodecMaker.make
  private given JsonValueCodec[Service] = JsonCodecMaker.make
  private given JsonValueCodec[Customer] = JsonCodecMaker.make
  private given JsonValueCodec[Contract] = JsonCodecMaker.make

  /** A message as the optics read it: a view of its bytes where they lie, nothing copied (fs2's
    * `toByteBuffer` wraps an array slice's own array).
    */
  def json(message: Chunk.ArraySlice[Byte]): JsonSlice = JsonSlice(message.toByteBuffer)

  /** Message ⇀ its detail's bytes, as they lie in the message: nothing decoded, nothing copied. */
  val detail: Focus[JsonSlice, DetailJson] = JsoniterPrism.inPlace[Issue].field(_.detail).raw

  /** Which kind of issue: the `"type"` field that names `Detail`'s case. */
  given kind: Focus[DetailJson, Kind] =
    // The one string path: `"type"` is the codec's tag, not a field of the model. A `Left` would be
    // a typo in this line, and every test reads this optic.
    JsoniterPrism.fromPath[Kind]("$.type").fold(e => throw IllegalArgumentException(e), _.inPlace)

  // Each case's leaves: the path from the case's own root, read off its own fields.
  given severity: Focus[DetailJson, Severity] =
    JsoniterPrism.inPlace[Detail.Incident].field(_.severity)

  given tier: Focus[DetailJson, Tier] =
    JsoniterPrism.inPlace[Detail.Incident].field(_.impact).field(_.service).field(_.tier)

  given module: Focus[DetailJson, Module] =
    JsoniterPrism.inPlace[Detail.Bug].field(_.trace).field(_.frames).at(0).field(_.module)

  given plan: Focus[DetailJson, Plan] =
    JsoniterPrism.inPlace[Detail.Request].field(_.requester).field(_.contract).field(_.plan)

  given votes: Focus[DetailJson, Votes] = JsoniterPrism.inPlace[Detail.Request].field(_.votes)

  /** A copy of the bytes, in an array of their own. */
  extension (detail: DetailJson) def bytes: Array[Byte] = detail.toArray
