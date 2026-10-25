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
  * past the 60 fields in front of it) and hands out that object's own bytes. The givens then read
  * the few leaves the policy needs from those few hundred bytes.
  *
  * The paths are typed: `.field(_.impact)` is checked against the model when this file compiles, so
  * renaming a field breaks the build rather than the filter.
  */
object Wire:

  /** A message's `detail` object: its own JSON, sliced out of the message. A separate type, so the
    * detail's optics can't be pointed at a whole message.
    */
  opaque type DetailJson = Array[Byte]

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

  /** A message as the optics read it: a view of its bytes where they lie, nothing copied. */
  def json(message: Chunk.ArraySlice[Byte]): JsonSlice =
    JsonSlice(message.values, message.offset, message.length)

  /** Message ⇀ its detail's bytes, copied verbatim; nothing decoded. The message itself is read in
    * place, as a slice of whatever array holds it (`.onSlice`).
    */
  val detail: Focus[JsonSlice, DetailJson] = JsoniterPrism[Issue].field(_.detail).raw.onSlice

  /** Which kind of issue: the `"type"` field that names `Detail`'s case. */
  given kind: Focus[DetailJson, Kind] =
    // The one string path: `"type"` is the codec's tag, not a field of the model. A `Left` would be
    // a typo in this line, and every test reads this optic.
    JsoniterPrism.fromPath[Kind]("$.type").fold(e => throw IllegalArgumentException(e), identity)

  // Each case's leaves: the path from the case's own root, read off its own fields.
  given severity: Focus[DetailJson, Severity] = JsoniterPrism[Detail.Incident].field(_.severity)

  given tier: Focus[DetailJson, Tier] =
    JsoniterPrism[Detail.Incident].field(_.impact).field(_.service).field(_.tier)

  given module: Focus[DetailJson, Module] =
    JsoniterPrism[Detail.Bug].field(_.trace).field(_.frames).at(0).field(_.module)

  given plan: Focus[DetailJson, Plan] =
    JsoniterPrism[Detail.Request].field(_.requester).field(_.contract).field(_.plan)

  given votes: Focus[DetailJson, Votes] = JsoniterPrism[Detail.Request].field(_.votes)

  /** A copy of the bytes. */
  extension (detail: DetailJson) def bytes: Array[Byte] = detail.clone
