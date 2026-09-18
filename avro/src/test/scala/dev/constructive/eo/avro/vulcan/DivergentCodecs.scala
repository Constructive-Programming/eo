package dev.constructive.eo.avro.vulcan

import scala.jdk.CollectionConverters.*

import _root_.vulcan.Codec as VCodec
import cats.syntax.all.*
import dev.constructive.eo.avro.AvroCodec
import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

/** Hand-written vulcan codecs whose Avro schema is NOT positionally 1:1 with the case class — the
  * population the position-resolution design (issue #35) silently mis-targets.
  *
  * This is the standing gap these fixtures close: every fixture in `AvroSpecFixtures` is
  * kindlings-derived, hence 1:1 by construction, so no existing example could have seen the hazard.
  * `vulcan.Codec` is the realistic carrier for the divergent shapes: `Codec.record`'s field list
  * names each schema field independently of the case field it accesses, so order, arity and naming
  * can all diverge — and the bridge (`AvroVulcan.codec`) can recover none of it.
  *
  * Evidence rule for every fixture: the payload is produced by the codec ITSELF, so the value in
  * schema field `f` is by construction whatever the codec decided to write there. That makes SLOT
  * TRUTH (not `copy(...)`-truth) checkable without guessing.
  */
object DivergentCodecs:

  def encodeBytes[A](a: A)(using c: AvroCodec[A]): Array[Byte] =
    AvroCodec.encodeValue(a).fold(f => throw new RuntimeException(f.toString), identity)

  def recordOf(bytes: Array[Byte], schema: Schema): GenericRecord =
    AvroCodec
      .decodeRecord(bytes, schema)
      .fold(f => throw new RuntimeException(f.toString), _.asInstanceOf[GenericRecord])

  /** Every top-level schema slot of `bytes`, stringified, keyed by SCHEMA field name. The ground
    * truth a write is scored against: exactly one slot may change, and it must be the intended one.
    */
  def slots(bytes: Array[Byte], schema: Schema): Map[String, String] =
    AvroCodec
      .decodeRecord(bytes, schema)
      .fold(
        f => Map("DECODE-FAILED" -> f.toString),
        r => schema.getFields.asScala.map(f => f.name -> String.valueOf(r.get(f.pos))).toMap,
      )

end DivergentCodecs

// ---- shape 1b: a COMPUTED schema field BETWEEN the case fields ------------------------------
// schema {a, computed, b, c} vs case class {a, b, c}. The reporter's motivating case (issue #95).
final case class Three(a: String, b: String, c: Long)

object Three:

  given VCodec[Three] = VCodec.record("Three", "eo.hazard") { f =>
    (
      f("a", _.a),
      f("computed", (t: Three) => t.a + "|" + t.b),
      f("b", _.b),
      f("c", _.c),
    ).mapN((a, _, b, c) => Three(a, b, c))
  }

/** NamedTuple codec for the `.fields(_.a, _.b)` shape — kindlings-derived, since there is no
  * `vulcan.Codec` for a NamedTuple. Its own schema is 1:1 with the tuple; only the PARENT (`Three`)
  * diverges, which is exactly the shape that corrupts two slots per grouped write.
  */
object ThreeNt:

  type AB = NamedTuple.NamedTuple[("a", "b"), (String, String)]

  given AvroEncoder[AB] = AvroEncoder.derived
  given AvroDecoder[AB] = AvroDecoder.derived
  given AvroSchemaFor[AB] = AvroSchemaFor.derived

end ThreeNt

// ---- shape 2: schema field ORDER reversed, both fields String --------------------------------
// Equal arity and both leaves the same type, so nothing about the shape or a decode failure can
// betray the mis-aim: only the NAMES can.
final case class Pair(alpha: String, beta: String)

object Pair:

  given VCodec[Pair] = VCodec.record("Pair", "eo.hazard") { f =>
    (f("beta", _.beta), f("alpha", _.alpha)).mapN((b, a) => Pair(a, b))
  }

// ---- tier-2 shape: a name TRANSFORM *and* a computed field -----------------------------------
// schema {click_id, computed, landing_page_id} vs case class {clickId, landingPageId}.
// No literal Scala name survives in the schema, so exact-name resolution alone cannot see it.
final case class SnakeComputed(clickId: String, landingPageId: Long)

object SnakeComputed:

  given VCodec[SnakeComputed] = VCodec.record("SnakeComputed", "eo.hazard") { f =>
    (
      f("click_id", _.clickId),
      f("computed", (s: SnakeComputed) => s.clickId + "!"),
      f("landing_page_id", _.landingPageId),
    ).mapN((c, _, l) => SnakeComputed(c, l))
  }

// ---- shape 7c / 11: the INNER record carries the computed field ------------------------------
// The outer record is 1:1; the divergence is one hop down, and reachable both by `.field(_.inner)`
// and by a traversal suffix (`.field(_.items).each`).
final case class Inner(x: String, y: Long)

object Inner:

  given VCodec[Inner] = VCodec.record("Inner", "eo.hazard") { f =>
    (
      f("c", (i: Inner) => i.x + "!"),
      f("x", _.x),
      f("y", _.y),
    ).mapN((_, x, y) => Inner(x, y))
  }

final case class Outer(id: String, inner: Inner)

object Outer:

  given VCodec[Outer] = VCodec.record("Outer", "eo.hazard") { f =>
    (f("id", _.id), f("inner", _.inner)).mapN(Outer.apply)
  }

final case class Basket(items: List[Inner])

object Basket:

  given VCodec[Basket] = VCodec.record("Basket", "eo.hazard") { f =>
    f("items", _.items).map(Basket.apply)
  }

// ---- FALSE-POSITIVE CONTROL: vulcan rename map only, ORDER PRESERVED -------------------------
// Issue #35's population. The names carry no recoverable relation to the Scala names, so only
// POSITION can resolve them; any mechanism that breaks this row has broken #35's motivating case.
final case class NPair(alpha: String, beta: String)

object NPair:

  given VCodec[NPair] = VCodec.record("NPair", "eo.hazard") { f =>
    (f("alpha_name", _.alpha), f("beta_name", _.beta)).mapN(NPair.apply)
  }

/** FALSE-POSITIVE CONTROL — a kindlings-derived codec under a deliberately hostile custom name
  * transform (`n => "x_" + n.reverse`). The #35 worst case: the schema names bear no recoverable
  * relation to the Scala names, so position must stay in charge.
  */
object HostileTransform:

  given AvroConfig = AvroConfig().withTransformFieldNames(n => "x_" + n.reverse)

  final case class Hostile(alpha: String, beta: Long)

  object Hostile:
    given AvroEncoder[Hostile] = AvroEncoder.derived
    given AvroDecoder[Hostile] = AvroDecoder.derived
    given AvroSchemaFor[Hostile] = AvroSchemaFor.derived

end HostileTransform

// ---- PINNED REGRESSION shape: a PERMUTING rename ---------------------------------------------
// The codec writes case field `alpha` into the schema field literally named "beta" and vice versa.
// Position happens to be right here, and any name-first rule is wrong. No name TRANSFORM can
// produce this shape (a transform is a function of the name alone); only a hand-written field list
// can.
final case class PermPair(alpha: String, beta: String)

object PermPair:

  given VCodec[PermPair] = VCodec.record("PermPair", "eo.hazard") { f =>
    (f("beta", _.alpha), f("alpha", _.beta)).mapN((a, b) => PermPair(a, b))
  }

// ---- PINNED RESIDUAL shape 5a: a vulcan override map AND a reorder ---------------------------
// Equal arity, no name hit, so neither the names nor the shape can see the mis-aim.
final case class RPair(alpha: String, beta: String)

object RPair:

  given VCodec[RPair] = VCodec.record("RPair", "eo.hazard") { f =>
    (f("beta_name", _.beta), f("alpha_name", _.alpha)).mapN((b, a) => RPair(a, b))
  }
