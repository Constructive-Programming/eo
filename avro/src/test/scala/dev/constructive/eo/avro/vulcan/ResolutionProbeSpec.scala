package dev.constructive.eo.avro.vulcan

import scala.compiletime.summonFrom
import scala.language.implicitConversions

import _root_.vulcan.Codec as VCodec
import cats.syntax.all.*
import dev.constructive.eo.avro.{
  codecPrism,
  AvroCodec,
  AvroPrism,
  AvroProbe,
  AvroTraversal,
  AvroWalk,
  PathStep,
}
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.specs2.mutable.Specification

// ==============================================================================================
// The DIFFERENTIAL CODEC PROBE (issue #100), end to end and at the driver.
//
// The probe is measurement where issue #95's resolver was inference: encode two sentinels of the
// parent case class that differ in exactly the target field, diff the encoded record's slots, and
// the slot whose content IS the field's value is the field's column. `ResolutionResidualSpec` is
// the ledger of codecs that defeated the old resolver; this spec is the machinery's own contract:
//
//   - it must never re-aim a call site that is already correct (the false-positive control),
//   - it must abstain — and leave the old answer in place — when a sentinel cannot be synthesised,
//     when the codec rejects one, or when a field has no column at all,
//   - it must probe the codec of the record it is resolving in, one hop down included,
//   - and it must cost nothing per operation (one cached verdict per codec and case field).
// ==============================================================================================

// ---- a nested hop: the INNER codec is the one that is renamed and reordered --------------------
// {q_y, q_x} vs case class {px, py}: neither the names nor the position reach `px`.
final case class NInner(px: String, py: Long)

object NInner:

  given VCodec[NInner] = VCodec.record("NInner", "eo.probe") { f =>
    (f("q_y", _.py), f("q_x", _.px)).mapN((y, x) => NInner(x, y))
  }

final case class NOuter(tag: String, body: NInner)

object NOuter:

  given VCodec[NOuter] = VCodec.record("NOuter", "eo.probe") { f =>
    (f("t", _.tag), f("b", _.body)).mapN(NOuter.apply)
  }

// ---- a codec that REFUSES the sentinels --------------------------------------------------------
// The encoder throws for anything that is not an `OK-` code — a validating hand-written codec. The
// probe's only correct response is to abstain, so resolution keeps its name/position answer.
final case class Guarded(code: String, note: String)

object Guarded:

  given VCodec[Guarded] = VCodec.record("Guarded", "eo.probe") { f =>
    (
      f(
        "c",
        (g: Guarded) =>
          if g.code.startsWith("OK-") then g.code
          else throw new IllegalArgumentException(s"bad code: ${g.code}"),
      ),
      f("n", _.note),
    ).mapN(Guarded.apply)
  }

// ---- a RECURSIVE ADT: no finite sentinel exists -------------------------------------------------
// `AvroProbe[RecNode]` needs an `AvroProbe[Option[RecNode]]`, which needs an `AvroProbe[RecNode]`: the
// derivation diverges, so `Slots[RecNode]` never materialises and the probe is not even offered.
final case class RecNode(value: String, next: Option[RecNode])

object RecNode:

  given AvroEncoder[RecNode] = AvroEncoder.derived
  given AvroDecoder[RecNode] = AvroDecoder.derived
  given AvroSchemaFor[RecNode] = AvroSchemaFor.derived

// ---- `.fields` over a renamed AND reordered codec -----------------------------------------------
object RPairNt:

  // The NamedTuple codec triplet is the only way to spell a `.fields` focus; the same five lines
  // sit beside every such fixture (see `DivergentCodecs.ThreeNt`).
  type AB = NamedTuple.NamedTuple[("alpha", "beta"), (String, String)]

  given AvroEncoder[AB] = AvroEncoder.derived
  given AvroDecoder[AB] = AvroDecoder.derived
  given AvroSchemaFor[AB] = AvroSchemaFor.derived

end RPairNt

class ResolutionProbeSpec extends Specification:

  import DivergentCodecs.*

  /** The schema field a drilled optic will actually read — resolution's verdict, observed. */
  private def resolved[A](optic: AvroPrism[A]): String = stepName(optic.path)

  /** The same for a traversal's per-element suffix. */
  private def resolved[A](optic: AvroTraversal[A]): String = stepName(optic.suffix)

  private def stepName(steps: Array[PathStep]): String =
    steps.last match
      case PathStep.Field(name) => name
      case other                => other.toString

  /** What the PRE-probe rule (name, else declaration position) decides for `caseName`. The
    * false-positive control: wherever that rule is already right, the probe must agree with it.
    */
  private def oldRule[A](schema: Schema, caseName: String, declIdx: Int, caseNames: List[String]) =
    AvroWalk.fieldNameAt(schema, caseName, declIdx, caseNames, "spec", _ => None)

  /** The strict verdict for `declIdx`, through the driver the optic itself calls. */
  private def probeName[A](
      codec: AvroCodec[A],
      fieldCodec: AvroCodec[?],
      slots: => Option[AvroProbe.Slots[A]],
      declIdx: Int,
  ): Option[String] =
    AvroProbe.fieldName(codec, fieldCodec, slots, declIdx, codec.schema)

  /** The parent's synthesiser when the derivation resolves, `None` otherwise — the compile-time
    * half of the all-or-nothing rule, observed rather than assumed.
    */
  private inline def slotsOf[A]: Option[AvroProbe.Slots[A]] =
    summonFrom {
      case s: AvroProbe.Slots[A] => Some(s)
      case _                     => None
    }

  // ---- the false-positive control ------------------------------------------------------------

  "the probe never re-aims a call site the name/position rule already resolves correctly" >> {
    // Every fixture here is one the old rule gets RIGHT — a derived 1:1 codec, a name transform, a
    // trailing computed column, a legacy column set, a nested record. If the probe disagrees with
    // the old rule on any of them it has introduced a corruption, which is the failure this
    // machinery must never have.
    val three = summon[AvroCodec[Three]].schema
    val pair = summon[AvroCodec[Pair]].schema
    val npair = summon[AvroCodec[NPair]].schema
    val snake = summon[AvroCodec[SnakeComputed]].schema
    val inner = summon[AvroCodec[Inner]].schema
    val click = summon[AvroCodec[FpClick]].schema
    val visit = summon[AvroCodec[FpVisit]].schema
    val row = summon[AvroCodec[FpRow]].schema
    val cart = summon[AvroCodec[FpCart]].schema
    val item = summon[AvroCodec[FpItem]].schema

    val threeNames = List("a", "b", "c")
    val pairNames = List("alpha", "beta")
    val snakeNames = List("clickId", "landingPageId")
    val innerNames = List("x", "y")
    val clickNames = List("clickId", "landingPageId")
    val visitNames = List("userId", "user")
    val rowNames = List("id", "name", "cachedHash")
    val cartNames = List("owner", "items")
    val itemNames = List("sku", "qty")

    val cells = List(
      (resolved(codecPrism[Three].field(_.a)), oldRule(three, "a", 0, threeNames)),
      (resolved(codecPrism[Three].field(_.b)), oldRule(three, "b", 1, threeNames)),
      (resolved(codecPrism[Three].field(_.c)), oldRule(three, "c", 2, threeNames)),
      (resolved(codecPrism[Pair].field(_.alpha)), oldRule(pair, "alpha", 0, pairNames)),
      (resolved(codecPrism[NPair].field(_.alpha)), oldRule(npair, "alpha", 0, pairNames)),
      (
        resolved(codecPrism[SnakeComputed].field(_.clickId)),
        oldRule(snake, "clickId", 0, snakeNames)
      ),
      (
        resolved(codecPrism[SnakeComputed].field(_.landingPageId)),
        oldRule(snake, "landingPageId", 1, snakeNames),
      ),
      (resolved(codecPrism[Inner].field(_.x)), oldRule(inner, "x", 0, innerNames)),
      (resolved(codecPrism[Inner].field(_.y)), oldRule(inner, "y", 1, innerNames)),
      (resolved(codecPrism[FpClick].field(_.clickId)), oldRule(click, "clickId", 0, clickNames)),
      (
        resolved(codecPrism[FpClick].field(_.landingPageId)),
        oldRule(click, "landingPageId", 1, clickNames),
      ),
      (resolved(codecPrism[FpVisit].field(_.userId)), oldRule(visit, "userId", 0, visitNames)),
      (resolved(codecPrism[FpVisit].field(_.user)), oldRule(visit, "user", 1, visitNames)),
      (resolved(codecPrism[FpRow].field(_.id)), oldRule(row, "id", 0, rowNames)),
      (resolved(codecPrism[FpRow].field(_.name)), oldRule(row, "name", 1, rowNames)),
      (resolved(codecPrism[FpCart].field(_.owner)), oldRule(cart, "owner", 0, cartNames)),
      // the traversal suffix probes the ELEMENT codec, one hop down
      (
        resolved(codecPrism[FpCart].field(_.items).each.field(_.sku)),
        oldRule(item, "sku", 0, itemNames),
      ),
      (
        resolved(codecPrism[FpCart].field(_.items).each.field(_.qty)),
        oldRule(item, "qty", 1, itemNames),
      ),
    )
    val mismatches = cells.filter((probe, old) => probe != old)
    (cells.size === 18).and(mismatches === Nil)
  }

  // ---- what the probe adds -------------------------------------------------------------------

  "the probe reads the slot the codec writes, where neither names nor position can" >> {
    // {beta_name, alpha_name} vs {alpha, beta}: equal arity, no name hit, permuted — position would
    // aim `alpha` at `beta_name`. `ResolutionResidualSpec` pins the values read through these.
    // `ResolutionResidualSpec` asserts the same two verdicts for `RPair`.
    (resolved(codecPrism[RPair].field(_.alpha)) === "alpha_name")
      .and(resolved(codecPrism[RPair].field(_.beta)) === "beta_name")
  }

  "a nested hop probes the INNER codec, not the outer one" >> {
    val bytes = encodeBytes(NOuter("T0", NInner("X0", 7L)))
    val optic = codecPrism[NOuter].field(_.body).field(_.px)
    (optic.getOption(bytes) must beSome("X0"))
      .and(resolved(codecPrism[NOuter].field(_.body)) === "b")
      .and(resolved(optic) === "q_x")
      // position would have aimed at `q_y` (the Long) and failed to decode at all
      .and(codecPrism[NOuter].field(_.body).field(_.py).getOption(bytes) must beSome(7L))
  }

  "the two verdict rules differ exactly where the field has no column of its own" >> {
    // `Comp.b` is never written: the only slot that moves with it is the digest, which holds
    // `a + "/" + b` and not `b`. The strict rule (which has the field's own codec and can demand
    // the slot HOLD the value) refuses; the change-only rule (`.fields`, which cannot) accepts the
    // single moved slot. Both are conservative — the strict rule is what keeps a derived column
    // from claiming a field.
    val codec = summon[AvroCodec[Comp]]
    val slots = summon[AvroProbe.Slots[Comp]]
    val string = summon[AvroCodec[String]]
    val strict = probeName(codec, string, Some(slots), 1)
    val changeOnly = AvroProbe.fieldNameByChange(codec, Some(slots), 1, codec.schema)
    val stored = probeName(codec, string, Some(slots), 0)
    (strict must beNone).and(changeOnly must beSome("x_digest")).and(stored must beSome("x_a"))
  }

  "`.fields` resolves every selector through the probe" >> {
    import RPairNt.given
    val bytes = encodeBytes(RPair("ALPHA0", "BETA0"))
    // position would have read (beta_name, alpha_name) — the two values swapped
    codecPrism[RPair].fields(_.alpha, _.beta).getOption(bytes) must
      beSome((alpha = "ALPHA0", beta = "BETA0"): RPairNt.AB)
  }

  // ---- graceful degradation -------------------------------------------------------------------

  "a recursive ADT has no finite sentinel: the probe abstains, and `.field` still resolves" >> {
    val codec = summon[AvroCodec[RecNode]]
    val string = summon[AvroCodec[String]]
    val bytes = encodeBytes(RecNode("V0", None))
    val verdict = probeName(codec, string, slotsOf[RecNode], 0)
    // The derivation needs an `AvroProbe[Option[RecNode]]`, which needs an `AvroProbe[RecNode]`:
    // the search diverges, so there is no synthesiser to offer at all.
    (slotsOf[RecNode] must beNone)
      .and(verdict must beNone)
      .and(resolved(codecPrism[RecNode].field(_.value)) === "value")
      .and(codecPrism[RecNode].field(_.value).getOption(bytes) must beSome("V0"))
  }

  "a codec that REFUSES the sentinels abstains; resolution keeps its positional answer" >> {
    val codec = summon[AvroCodec[Guarded]]
    val slots = summon[AvroProbe.Slots[Guarded]]
    val probe = probeName(codec, summon[AvroCodec[String]], Some(slots), 0)
    val bytes = encodeBytes(Guarded("OK-1", "N0"))
    // the probe has no verdict, so the optic still builds and still resolves the old way...
    (probe must beNone)
      .and(resolved(codecPrism[Guarded].field(_.code)) === "c")
      .and(resolved(codecPrism[Guarded].field(_.note)) === "n")
      // ...and a payload the codec accepts still round-trips through it
      .and(codecPrism[Guarded].field(_.code).getOption(bytes) must beSome("OK-1"))
  }

  "no synthesiser at all is the same abstention — `.field` still builds and still resolves" >> {
    val bytes = encodeBytes(RecNode("V0", None))
    val optic = codecPrism[RecNode].field(_.value)
    (resolved(optic) === "value").and(optic.getOption(bytes) must beSome("V0"))
  }

  // ---- cost -----------------------------------------------------------------------------------

  "the verdict is cached per (codec, case field): one pair of encodes, then lookups" >> {
    val counting = new CountingCodec(summon[AvroCodec[Ev]])
    val slots = summon[AvroProbe.Slots[Ev]]
    val long = summon[AvroCodec[Long]]
    val first = AvroProbe.fieldName(counting, long, Some(slots), 0, counting.schema)
    val afterFirst = counting.encodes
    val second = AvroProbe.fieldName(counting, long, Some(slots), 0, counting.schema)
    val sibling = AvroProbe.fieldName(counting, long, Some(slots), 1, counting.schema)
    (first must beSome("event_ts"))
      .and(second === first)
      .and(sibling must beSome("seq_no"))
      // baseline + perturbed for the first verdict, and nothing at all for the repeat
      .and(afterFirst === 2)
      .and(counting.encodes === 4)
  }

  final private class CountingCodec[A](inner: AvroCodec[A]) extends AvroCodec[A]:

    var encodes: Int = 0

    def schema: Schema = inner.schema

    def encode(a: A): Any =
      encodes += 1
      inner.encode(a)

    def decodeEither(any: Any): Either[Throwable, A] = inner.decodeEither(any)

end ResolutionProbeSpec
