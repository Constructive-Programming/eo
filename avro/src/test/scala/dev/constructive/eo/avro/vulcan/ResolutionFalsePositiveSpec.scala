package dev.constructive.eo.avro.vulcan

import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions
import scala.util.control.NonFatal

import _root_.vulcan.Codec as VCodec
import cats.syntax.all.*
import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import hearth.kindlings.avroderivation.{AvroConfig, AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification

// ==============================================================================================
// LEGITIMATE, CURRENTLY-WORKING user code. Every fixture in this file is code a real user can
// write today and get CORRECT behaviour from. It is the counterweight to the repro corpus: a
// resolver that fixes the hazard by refusing or re-aiming ordinary codecs has not fixed anything.
// Any cell that stops being CORRECT here is a FALSE POSITIVE (a loud refusal on working code) or
// an INTRODUCED CORRUPTION (a silently different slot) — the latter is what killed the per-field
// nominal rung, which resolved FP2 below onto the wrong column.
// ==============================================================================================

// ---- FP1: abbreviated schema names + a TRAILING server-added column ---------------------------
// The single most common hand-written Avro shape: short wire names, plus one derived/audit field
// the case class does not carry. Arity 3 vs 2, and no schema name resembles a Scala name. Both
// leaves resolve by position and are CORRECT, because the extra field is at the TAIL and shifts
// nothing — which is why a global arity gate was rejected: it refuses this.
final case class FpClick(clickId: String, landingPageId: Long)

object FpClick:

  given VCodec[FpClick] = VCodec.record("FpClick", "eo.fp") { f =>
    (
      f("id", _.clickId),
      f("lp", _.landingPageId),
      f("ingested_at", (c: FpClick) => c.clickId.length.toLong),
    ).mapN((i, l, _) => FpClick(i, l))
  }

// ---- FP2: legacy column names that COLLIDE with a sibling's Scala name -------------------------
// Arity 2 vs 2, order preserved, so position is right. `userId` is written to `uid`; `user_id` is
// a DIFFERENT, older column holding `user`. A per-field nominal rung matches `userId` against
// `user_id` and lands on the wrong column — an INTRODUCED corruption. The all-or-nothing rule
// abstains instead, because `user` maps to no schema field so the mapping is not total.
final case class FpVisit(userId: String, user: String)

object FpVisit:

  given VCodec[FpVisit] = VCodec.record("FpVisit", "eo.fp") { f =>
    (f("uid", _.userId), f("user_id", _.user)).mapN(FpVisit.apply)
  }

// ---- FP3: the codec DROPS a derived/transient field --------------------------------------------
// "don't serialise the cache" — schema arity 2, case arity 3, abbreviated names.
final case class FpRow(id: String, name: String, cachedHash: Long)

object FpRow:

  given VCodec[FpRow] = VCodec.record("FpRow", "eo.fp") { f =>
    (f("i", _.id), f("n", _.name)).mapN((i, n) => FpRow(i, n, 0L))
  }

// ---- FP4 (control): vulcan rename map only, order preserved ------------------------------------
final case class FpKeep(alpha: String, beta: String)

object FpKeep:

  given VCodec[FpKeep] = VCodec.record("FpKeep", "eo.fp") { f =>
    (f("alpha_name", _.alpha), f("beta_name", _.beta)).mapN(FpKeep.apply)
  }

// ---- FP5: `.each.field` under an element codec with a trailing computed field -------------------
final case class FpItem(sku: String, qty: Long)

object FpItem:

  given VCodec[FpItem] = VCodec.record("FpItem", "eo.fp") { f =>
    (
      f("s", _.sku),
      f("q", _.qty),
      f("chk", (i: FpItem) => i.sku.length.toLong),
    ).mapN((s, q, _) => FpItem(s, q))
  }

final case class FpCart(owner: String, items: List[FpItem])

object FpCart:

  given VCodec[FpCart] = VCodec.record("FpCart", "eo.fp") { f =>
    (f("owner", _.owner), f("items", _.items)).mapN(FpCart.apply)
  }

// ---- FP6: nested record whose INNER codec carries a trailing computed field ---------------------
final case class FpInner(x: String, y: Long)

object FpInner:

  given VCodec[FpInner] = VCodec.record("FpInner", "eo.fp") { f =>
    (
      f("x0", _.x),
      f("y0", _.y),
      f("digest", (i: FpInner) => i.x + "!"),
    ).mapN((x, y, _) => FpInner(x, y))
  }

final case class FpOuter(id: String, inner: FpInner)

object FpOuter:

  given VCodec[FpOuter] = VCodec.record("FpOuter", "eo.fp") { f =>
    (f("id", _.id), f("inner", _.inner)).mapN(FpOuter.apply)
  }

// ---- kindlings-derived controls: the issue-#35 population ----------------------------------------
object FpSnake:

  given AvroConfig = AvroConfig().withSnakeCaseFieldNames

  final case class SnakeClick(clickId: String, landingPageId: Long)

  object SnakeClick:
    given AvroEncoder[SnakeClick] = AvroEncoder.derived
    given AvroDecoder[SnakeClick] = AvroDecoder.derived
    given AvroSchemaFor[SnakeClick] = AvroSchemaFor.derived

end FpSnake

object FpHostile:

  // Deliberately unrecognisable, letter-scrambling transform: the #35 worst case.
  given AvroConfig = AvroConfig().withTransformFieldNames(n => "x_" + n.reverse)

  final case class HostileClick(clickId: String, landingPageId: Long)

  object HostileClick:
    given AvroEncoder[HostileClick] = AvroEncoder.derived
    given AvroDecoder[HostileClick] = AvroDecoder.derived
    given AvroSchemaFor[HostileClick] = AvroSchemaFor.derived

end FpHostile

object FpPlain:

  final case class Bag(tags: Map[String, Long], total: Int)

  object Bag:
    given AvroEncoder[Bag] = AvroEncoder.derived
    given AvroDecoder[Bag] = AvroDecoder.derived
    given AvroSchemaFor[Bag] = AvroSchemaFor.derived

end FpPlain

/** The false-positive tripwire for schema-field resolution (issue #95). 28 cells, each a
  * legitimate call site that behaves CORRECTLY on published 0.15.1; the whole safety argument for
  * changing the resolver is that this scorecard does not move.
  *
  * Verdicts are scored against SLOT TRUTH — which schema slot a write actually touched — so a
  * write that lands on the wrong column is distinguishable from one that lands on the right column
  * and leaves a derived sibling stale.
  *
  * One cell is expected to change: `hatch-record-probe-absent`, where `.fieldNamed` on a name the
  * reader schema does not carry used to return `None` at runtime and is now refused at
  * construction. That is the deliberate behaviour change; everything else must stay CORRECT.
  */
class ResolutionFalsePositiveSpec extends Specification:

  private val verdicts = scala.collection.mutable.LinkedHashMap.empty[String, String]
  private val report = scala.collection.mutable.ListBuffer.empty[String]

  private def slots(bytes: Array[Byte], schema: Schema): Map[String, String] =
    AvroCodec
      .decodeRecord(bytes, schema)
      .fold(
        f => Map("DECODE-FAILED" -> f.toString),
        r => schema.getFields.asScala.map(f => f.name -> String.valueOf(r.get(f.pos))).toMap,
      )

  /** Which top-level schema slots a write actually touched. Never `copy(...)`, which cannot
    * distinguish a wrong slot from a stale derived one.
    */
  private def changed(before: Array[Byte], after: Array[Byte], schema: Schema): String =
    val b = slots(before, schema)
    val a = slots(after, schema)
    val d = b.keySet.filter(k => b(k) != a.getOrElse(k, "<absent>")).toList.sorted
    if d.isEmpty then "<none>" else d.mkString(",")

  private def bytesOf[A](a: A)(using c: AvroCodec[A]): Array[Byte] =
    AvroCodec.encodeValue(a).fold(f => throw new RuntimeException(f.toString), identity)

  private def cell(id: String, expect: String)(thunk: => String): Unit =
    val got =
      try thunk
      catch case NonFatal(t) => "LOUD:" + t.getMessage.take(240).replace('\n', ' ')
    val verdict =
      if got == expect then "CORRECT"
      else if got.startsWith("LOUD") then "LOUD-REFUSAL"
      else if got == "None" || got == "<none>" then "SILENT-MISS"
      else "SILENT-WRONG"
    verdicts += (id -> verdict)
    report += s"FP-PROBE\t$id\t$verdict\texpect=$expect\tgot=$got"

  // ---- FP1 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpClick]]
    val v = FpClick("C0", 7L)
    val b = bytesOf(v)
    cell("fp1-read-clickId", "Some(C0)")(codecPrism[FpClick].field(_.clickId).getOption(b).toString)
    cell("fp1-read-lp", "Some(7)")(codecPrism[FpClick].field(_.landingPageId).getOption(b).toString)
    cell("fp1-write-clickId", "id")(
      changed(b, codecPrism[FpClick].field(_.clickId).replace("C1")(b), c.schema)
    )
    cell("fp1-write-lp", "lp")(
      changed(b, codecPrism[FpClick].field(_.landingPageId).replace(99L)(b), c.schema)
    )
    cell("fp1-record-read-clickId", "Some(C0)") {
      val rec = c.encode(v).asInstanceOf[GenericRecord]
      codecPrism[FpClick].field(_.clickId).record.getOptionUnsafe(rec).toString
    }
  }

  // ---- FP2 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpVisit]]
    val b = bytesOf(FpVisit("U0", "NAME0"))
    cell("fp2-read-userId", "Some(U0)")(codecPrism[FpVisit].field(_.userId).getOption(b).toString)
    cell("fp2-read-user", "Some(NAME0)")(codecPrism[FpVisit].field(_.user).getOption(b).toString)
    cell("fp2-write-userId", "uid")(
      changed(b, codecPrism[FpVisit].field(_.userId).replace("U1")(b), c.schema)
    )
    cell("fp2-write-user", "user_id")(
      changed(b, codecPrism[FpVisit].field(_.user).replace("NAME1")(b), c.schema)
    )
  }

  // ---- FP3 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpRow]]
    val b = bytesOf(FpRow("I0", "N0", 5L))
    cell("fp3-read-id", "Some(I0)")(codecPrism[FpRow].field(_.id).getOption(b).toString)
    cell("fp3-read-name", "Some(N0)")(codecPrism[FpRow].field(_.name).getOption(b).toString)
    cell("fp3-write-id", "i")(changed(b, codecPrism[FpRow].field(_.id).replace("I1")(b), c.schema))
    cell("fp3-write-name", "n")(
      changed(b, codecPrism[FpRow].field(_.name).replace("N1")(b), c.schema)
    )
  }

  // ---- FP4 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpKeep]]
    val b = bytesOf(FpKeep("A0", "B0"))
    cell("fp4-read-alpha", "Some(A0)")(codecPrism[FpKeep].field(_.alpha).getOption(b).toString)
    cell("fp4-write-alpha", "alpha_name")(
      changed(b, codecPrism[FpKeep].field(_.alpha).replace("A1")(b), c.schema)
    )
  }

  // ---- FP5 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpCart]]
    val v = FpCart("ada", List(FpItem("S0", 1L), FpItem("S1", 2L)))
    val b = bytesOf(v)
    cell("fp5-each-read-sku", "Some(Vector(S0, S1))") {
      val rec = c.encode(v).asInstanceOf[GenericRecord]
      codecPrism[FpCart].field(_.items).each.field(_.sku).record.getAll(rec).toOption.toString
    }
    cell("fp5-each-write-sku", "items") {
      changed(b, codecPrism[FpCart].field(_.items).each.field(_.sku).modify(_ + "!")(b), c.schema)
    }
  }

  // ---- FP6 ------------------------------------------------------------------------------------
  locally {
    val c = summon[AvroCodec[FpOuter]]
    val b = bytesOf(FpOuter("O0", FpInner("X0", 3L)))
    cell("fp6-read-inner-x", "Some(X0)")(
      codecPrism[FpOuter].field(_.inner).field(_.x).getOption(b).toString
    )
    cell("fp6-write-inner-x", "inner")(
      changed(b, codecPrism[FpOuter].field(_.inner).field(_.x).replace("X1")(b), c.schema)
    )
  }

  // ---- kindlings controls -----------------------------------------------------------------------
  locally {
    import FpSnake.*
    val c = summon[AvroCodec[SnakeClick]]
    val b = bytesOf(SnakeClick("C0", 7L))
    cell("ctl-snake-read", "Some(C0)")(
      codecPrism[SnakeClick].field(_.clickId).getOption(b).toString
    )
    cell("ctl-snake-write", "click_id")(
      changed(b, codecPrism[SnakeClick].field(_.clickId).replace("C1")(b), c.schema)
    )
  }

  locally {
    import FpHostile.*
    val c = summon[AvroCodec[HostileClick]]
    val b = bytesOf(HostileClick("C0", 7L))
    cell("ctl-hostile-read", "Some(C0)")(
      codecPrism[HostileClick].field(_.clickId).getOption(b).toString
    )
    cell("ctl-hostile-write", "x_dIkcilc")(
      changed(b, codecPrism[HostileClick].field(_.clickId).replace("C1")(b), c.schema)
    )
  }

  // ---- map keys and the `.fieldNamed` hatch --------------------------------------------------
  locally {
    import FpPlain.*
    val v = Bag(Map("t1" -> 1L, "t2" -> 2L), 9)
    val b = bytesOf(v)
    val bagRec = summon[AvroCodec[Bag]].encode(v).asInstanceOf[GenericRecord]
    // Map KEYS are data, not schema fields — `.fieldNamed` is the only way to address one, and the
    // existence check must carve them out entirely.
    cell("hatch-map-key-present", "Some(1)")(
      codecPrism[Bag].field(_.tags).fieldNamed[Long]("t1").record.getOptionUnsafe(bagRec).toString
    )
    cell("hatch-map-key-absent", "None")(
      codecPrism[Bag].field(_.tags).fieldNamed[Long]("t9").record.getOptionUnsafe(bagRec).toString
    )
    cell("hatch-map-key-write", "{t2=2, t1=9}") {
      val out = codecPrism[Bag]
        .field(_.tags)
        .fieldNamed[Long]("t1")
        .record
        .placeUnsafe(9L)(bagRec)
        .asInstanceOf[GenericRecord]
      String.valueOf(out.get("tags"))
    }
    cell("hatch-record-probe-present", "Some(9)")(
      codecPrism[Bag].fieldNamed[Int]("total").getOption(b).toString
    )
    // The ONE deliberate change: a record-level `.fieldNamed` for a name the reader schema does not
    // carry. `None` on 0.15.1 (the shape the hatch exists to avoid), a construction-time refusal
    // after commit C.
    cell("hatch-record-probe-absent", "None")(
      codecPrism[Bag].fieldNamed[Long]("no_such_field").getOption(b).toString
    )
  }

  private lazy val scorecard: String =
    verdicts.values.groupBy(identity).map((k, v) => s"$k=${v.size}").toList.sorted.mkString(" ")

  "TRIPWIRE: no legitimate call site is silently mis-targeted" >> {
    report.foreach(println)
    println(s"FP-PROBE\tSCORECARD\t${verdicts.size} cells\t$scorecard")
    verdicts.collect { case (id, "SILENT-WRONG") => id }.toList === Nil
  }

  "TRIPWIRE: no legitimate call site silently stops resolving" >> {
    verdicts.collect { case (id, "SILENT-MISS") => id }.toList === Nil
  }

  "every cell keeps the verdict it had on published 0.15.1" >> {
    (verdicts.size === 28)
      .and(verdicts.collect { case (id, v) if v != "CORRECT" => id }.toList === Nil)
  }

end ResolutionFalsePositiveSpec
