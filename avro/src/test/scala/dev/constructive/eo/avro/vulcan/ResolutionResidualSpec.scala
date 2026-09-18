package dev.constructive.eo.avro.vulcan

import scala.language.implicitConversions

import _root_.vulcan.Codec as VCodec
import cats.syntax.all.*
import dev.constructive.eo.avro.codecPrism
import org.apache.avro.{LogicalTypes, Schema}
import org.specs2.mutable.Specification

// ==============================================================================================
// KNOWN LIMITATIONS, pinned as executable examples rather than prose. Every codec below is still
// resolved to the WRONG schema field after issue #95's fix. They are not regressions — each one is
// wrong on published 0.15.1 too — but the next change to this resolver has to look at them, and a
// reader deciding whether to trust `.field(_.x)` against a hand-written codec deserves the exact
// shapes spelled out.
//
// Two distinct causes, and the distinction matters when choosing a follow-up mechanism:
//
//   (1) NO NAME SIGNAL — the schema names are unrecoverable AND the field list is permuted, so the
//       nominal rung abstains and position decides, wrongly. Reachable only by a differential probe
//       of the codec itself (encode sentinels, observe which slot they land in) or an explicit
//       declaration; no amount of name matching helps.
//   (2) A MISLEADING NAME SIGNAL — a schema field BEARS a case field's name but HOLDS a different
//       value (a derived public id, a stale legacy column). The name map is total and injective, so
//       the resolver trusts it. Same verdict as 0.15.1, with one real cost: the corrupt value is
//       now more PLAUSIBLE (`pub-REAL-ID` rather than a digest length).
// ==============================================================================================

// ---- b2: two same-physical-type columns, renamed AND reversed (cause 1) ------------------------
// A logical type is an annotation on the same physical `long`, so it does not disambiguate.
final case class Ev(occurredAt: Long, seqNo: Long)

object Ev:

  private val tsLong: Schema =
    LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG))

  given VCodec[Ev] = VCodec.record("Ev", "eo.residual") { f =>
    (
      f("seq_no", _.seqNo),
      f("event_ts", _.occurredAt, default = None),
    ).mapN((s, t) => Ev(t, s))
  }

  def tsSchema: Schema = tsLong

// ---- b6: COMPENSATING arity — one case field dropped, one computed added (cause 1) -------------
// 3 case fields, 3 schema fields, so no count discrepancy exists; the transform hides every name;
// and position is wrong from the insertion point on.
final case class Comp(a: String, b: String, c: String)

object Comp:

  given VCodec[Comp] = VCodec.record("Comp", "eo.residual") { f =>
    (
      f("x_a", _.a),
      f("x_digest", (m: Comp) => m.a + "/" + m.b),
      f("x_c", _.c),
    ).mapN((a, _, c) => Comp(a, "B-NOT-STORED", c))
  }

// ---- at1: a schema field literally named after a case field, holding something else (cause 2) --
// `id` in the SCHEMA is a derived public identifier; the case field `id`'s real value is in
// `raw_ident`. The map {id -> id, payload -> payload} is total and injective, so it is trusted.
final case class Doc(id: String, payload: String)

object Doc:

  given VCodec[Doc] = VCodec.record("Doc", "eo.residual") { f =>
    (
      f("digest", (d: Doc) => d.payload.length.toString),
      f("id", (d: Doc) => "pub-" + d.id),
      f("raw_ident", _.id),
      f("payload", _.payload),
    ).mapN((_, _, i, p) => Doc(i, p))
  }

// ---- at2: the same, reached through the separator/case-insensitive rung (cause 2) --------------
// `USER_ID` is a stale legacy column; the live value is in `user_ident`.
final case class Acct(userId: String, balance: Long)

object Acct:

  given VCodec[Acct] = VCodec.record("Acct", "eo.residual") { f =>
    (
      f("USER_ID", (_: Acct) => "STALE-LEGACY"),
      f("user_ident", _.userId),
      f("balance", _.balance),
    ).mapN((_, u, b) => Acct(u, b))
  }

// ---- at3: TWO schema fields normalise alike — ambiguity is no signal, so position decides ------
final case class Tw(userId: String, tag: String)

object Tw:

  given VCodec[Tw] = VCodec.record("Tw", "eo.residual") { f =>
    (
      f("userId_", (_: Tw) => "STALE-DUP"),
      f("user_id", _.userId),
    ).mapN((_, u) => Tw(u, "TAG-DEFAULT"))
  }

/** The residual-risk ledger for issue #95's resolution change. Each example asserts what the
  * shipped resolver ACTUALLY does on a shape it cannot resolve — so the limitation is a fact under
  * test, not a claim in a doc comment.
  */
class ResolutionResidualSpec extends Specification:

  import DivergentCodecs.*

  // ---- cause 1: no name signal, position decides and is wrong -------------------------------

  "RESIDUAL 5a — a vulcan rename map PLUS a reorder still mis-targets" >> {
    // {beta_name, alpha_name} vs {alpha, beta}: equal arity, no recoverable name, permuted.
    val bytes = encodeBytes(RPair("ALPHA0", "BETA0"))
    codecPrism[RPair].field(_.alpha).getOption(bytes) must beSome("BETA0")
  }

  "RESIDUAL b2 — two same-physical-type columns, renamed and reversed, still mis-target" >> {
    // `occurredAt` reads back the sequence number. A timestamp-millis logical type annotates the
    // same physical `long`, so it cannot disambiguate either.
    val bytes = encodeBytes(Ev(1700000000000L, 7L))
    (codecPrism[Ev].field(_.occurredAt).getOption(bytes) must beSome(7L))
      .and(codecPrism[Ev].field(_.seqNo).getOption(bytes) must beSome(1700000000000L))
      .and(Ev.tsSchema.getLogicalType.getName === "timestamp-millis")
  }

  "RESIDUAL b6 — compensating arity (one field dropped, one computed added) still mis-targets" >> {
    // The field COUNTS agree, so nothing about the shape is suspicious; only `b` is wrong.
    val bytes = encodeBytes(Comp("A0", "B0", "C0"))
    (codecPrism[Comp].field(_.a).getOption(bytes) must beSome("A0"))
      .and(codecPrism[Comp].field(_.b).getOption(bytes) must beSome("A0/B0"))
      .and(codecPrism[Comp].field(_.c).getOption(bytes) must beSome("C0"))
  }

  "RESIDUAL at3 — two schema fields normalising alike is ambiguity, so position decides" >> {
    // `userId_` and `user_id` both normalise to `userid`. An ambiguous name signal is no signal:
    // the nominal rung disqualifies itself rather than guessing, and position lands on the stale
    // duplicate. A refusal here would be defensible; today it is silent.
    val bytes = encodeBytes(Tw("U-REAL", "T0"))
    codecPrism[Tw].field(_.userId).getOption(bytes) must beSome("STALE-DUP")
  }

  // ---- cause 2: a misleading name signal, trusted because the map is total -------------------

  "RESIDUAL at1 — a schema field bearing a case field's name but holding a derived value" >> {
    // {digest, id, raw_ident, payload}: `id` is a public identifier, the case field's real value is
    // in `raw_ident`. Wrong on 0.15.1 too (it read `digest`), but the wrong value is now PLAUSIBLE.
    // Note `payload` is FIXED by the change — it read `id` before.
    val bytes = encodeBytes(Doc("REAL-ID", "P0"))
    (codecPrism[Doc].field(_.id).getOption(bytes) must beSome("pub-REAL-ID"))
      .and(codecPrism[Doc].field(_.payload).getOption(bytes) must beSome("P0"))
  }

  "RESIDUAL at2 — the same, reached through the separator/case-insensitive rung" >> {
    // {USER_ID, user_ident, balance}: `USER_ID` normalises to `userid` and is a stale legacy copy.
    val bytes = encodeBytes(Acct("U-REAL", 10L))
    (codecPrism[Acct].field(_.userId).getOption(bytes) must beSome("STALE-LEGACY"))
      .and(codecPrism[Acct].field(_.balance).getOption(bytes) must beSome(10L))
  }

  // ---- the one deliberate BEHAVIOUR CHANGE ---------------------------------------------------

  "REGRESSION (accepted) — a PERMUTING rename was right by position and is now wrong by name" >> {
    // The codec writes case field `alpha` into the schema field literally NAMED "beta". No name
    // transform can produce this shape — a transform is a function of the name alone — but a
    // hand-written field list can, and this is the one place the change makes things worse.
    val bytes = encodeBytes(PermPair("ALPHA0", "BETA0"))
    (codecPrism[PermPair].field(_.alpha).getOption(bytes) must beSome("BETA0"))
      .and(codecPrism[PermPair].field(_.beta).getOption(bytes) must beSome("ALPHA0"))
  }

  // ---- and the hatch that reaches every one of them ------------------------------------------

  "every residual above is reachable with `.fieldNamed`, which bypasses resolution entirely" >> {
    val rpairBytes = encodeBytes(RPair("ALPHA0", "BETA0"))
    val docBytes = encodeBytes(Doc("REAL-ID", "P0"))
    val acctBytes = encodeBytes(Acct("U-REAL", 10L))
    val compBytes = encodeBytes(Comp("A0", "B0", "C0"))
    (codecPrism[RPair].fieldNamed[String]("alpha_name").getOption(rpairBytes) must beSome("ALPHA0"))
      .and(
        codecPrism[Doc].fieldNamed[String]("raw_ident").getOption(docBytes) must beSome("REAL-ID")
      )
      .and(
        codecPrism[Acct].fieldNamed[String]("user_ident").getOption(acctBytes) must beSome("U-REAL")
      )
      // `Comp.b` is genuinely not stored, so no name reaches it — the hatch cannot invent data.
      .and(
        codecPrism[Comp].fieldNamed[String]("x_digest").getOption(compBytes) must beSome("A0/B0")
      )
  }

end ResolutionResidualSpec
