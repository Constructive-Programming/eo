package dev.constructive.eo
package avro
package vulcan

import cats.syntax.all.*
import org.apache.avro.LogicalTypes

// ==============================================================================================
// THE RESIDUAL LEDGER, after issue #100's differential codec probe. Every codec below defeated
// issue #95's name-then-position resolver, and the reason it did is the same in each case: names
// and arity are INFERENCE, and these shapes carry no recoverable signal in either.
//
// The probe is measurement instead: it encodes two sentinels of the case class that differ in one
// field and reads which schema slot actually moved. Cells that used to be pinned as WRONG are now
// pinned as RIGHT, and the two that remain are pinned as UNCLOSEABLE — with the reason stated, so
// nobody re-opens them looking for a better heuristic.
//
// What the probe closes (all asserted below):
//
//   (1) NO NAME SIGNAL — a field list renamed beyond recognition AND permuted (5a), two
//       same-physical-type columns renamed and reversed (b2), and a PERMUTING hand-written rename,
//       where position happened to be right and the name rung made it wrong.
//   (2) A MISLEADING NAME SIGNAL — a schema field that BEARS a case field's name but HOLDS a
//       different value: a derived public id (at1), a stale legacy column reached through the
//       separator/case-insensitive rung (at2), and two columns normalising alike (at3). The name
//       map is total and injective, so the old rung trusted it; the probe measures the slot instead.
//
// What it cannot close, and why (also asserted below): a field the codec NEVER WRITES. b6 drops `b`
// and adds a computed column, `Tw` drops `tag` — no slot holds the value, so the probe finds no
// slot whose content is the field's and abstains, and resolution falls back to position exactly as
// before. That is not a gap in the probe: there is no column to aim at, and writing `b` into the
// digest column would be worse than reading the digest.
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
// and position is wrong from the insertion point on. `b` is genuinely not stored: no probe can
// find a column that the codec never wrote.
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
// `raw_ident`. The map {id -> id, payload -> payload} is total and injective, so it was trusted.
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

// ---- at3: TWO schema fields normalise alike — ambiguity is no signal, so position decided ------
final case class Tw(userId: String, tag: String)

object Tw:

  given VCodec[Tw] = VCodec.record("Tw", "eo.residual") { f =>
    (
      f("userId_", (_: Tw) => "STALE-DUP"),
      f("user_id", _.userId),
    ).mapN((_, u) => Tw(u, "TAG-DEFAULT"))
  }

/** The residual-risk ledger for schema-field resolution, re-scored after issue #100.
  *
  * Each example asserts what the shipped resolver ACTUALLY does, so the claim in the docs is a fact
  * under test. Read the header for which cells the differential probe closed and which two it
  * provably cannot.
  */
class ResolutionResidualSpec extends Specification:

  import DivergentCodecs.*

  /** The schema field a drilled prism will actually read, i.e. what resolution decided. */
  private def resolved[A](optic: dev.constructive.eo.avro.AvroPrism[A]): String =
    optic.path.last match
      case PathStep.Field(name) => name
      case other                => other.toString

  // ---- cause 1: no name signal, closed by the probe ----------------------------------------

  "CLOSED 5a — a vulcan rename map PLUS a reorder now resolves to the field's own column" >> {
    // {beta_name, alpha_name} vs {alpha, beta}: equal arity, no recoverable name, permuted. Neither
    // name nor position could see it; the probe sees the slot move.
    val bytes = encodeBytes(RPair("ALPHA0", "BETA0"))
    (codecPrism[RPair].field(_.alpha).getOption(bytes) must beSome("ALPHA0"))
      .and(codecPrism[RPair].field(_.beta).getOption(bytes) must beSome("BETA0"))
      .and(resolved(codecPrism[RPair].field(_.alpha)) === "alpha_name")
      .and(resolved(codecPrism[RPair].field(_.beta)) === "beta_name")
  }

  "CLOSED b2 — two same-physical-type columns, renamed and reversed, now resolve correctly" >> {
    // A timestamp-millis logical type annotates the same physical `long`, so it still cannot
    // disambiguate — the probe does not need it to.
    val bytes = encodeBytes(Ev(1700000000000L, 7L))
    (codecPrism[Ev].field(_.occurredAt).getOption(bytes) must beSome(1700000000000L))
      .and(codecPrism[Ev].field(_.seqNo).getOption(bytes) must beSome(7L))
      .and(resolved(codecPrism[Ev].field(_.occurredAt)) === "event_ts")
      .and(Ev.tsSchema.getLogicalType.getName === "timestamp-millis")
  }

  "RESIDUAL b6 — a field the codec never writes: the probe abstains and position decides" >> {
    // The field COUNTS agree and `b` is not stored anywhere: `x_digest` moves when `b` changes, but
    // it holds `a + "/" + b`, not `b`. No probe can aim at a column that does not exist, so the
    // answer stays what it was — and `a` / `c`, which ARE stored, are confirmed correct.
    val bytes = encodeBytes(Comp("A0", "B0", "C0"))
    (codecPrism[Comp].field(_.a).getOption(bytes) must beSome("A0"))
      .and(codecPrism[Comp].field(_.b).getOption(bytes) must beSome("A0/B0"))
      .and(codecPrism[Comp].field(_.c).getOption(bytes) must beSome("C0"))
      .and(resolved(codecPrism[Comp].field(_.a)) === "x_a")
      // the only slot that moves with `b` — and it is the digest, not `b`
      .and(resolved(codecPrism[Comp].field(_.b)) === "x_digest")
      .and(resolved(codecPrism[Comp].field(_.c)) === "x_c")
  }

  "CLOSED at3 — two schema fields normalising alike now resolve to the live column" >> {
    // `userId_` and `user_id` both normalise to `userid`. An ambiguous name signal is no signal —
    // and the probe does not need one: only `user_id` holds `userId`'s value.
    val bytes = encodeBytes(Tw("U-REAL", "T0"))
    (codecPrism[Tw].field(_.userId).getOption(bytes) must beSome("U-REAL"))
      .and(resolved(codecPrism[Tw].field(_.userId)) === "user_id")
  }

  "RESIDUAL at3b — a DROPPED field (`tag`) still resolves by position, as before" >> {
    // `tag` is written nowhere, so the probe abstains; position lands on `user_id`, which at least
    // decodes. There is no correct answer to find.
    val bytes = encodeBytes(Tw("U-REAL", "T0"))
    (resolved(codecPrism[Tw].field(_.tag)) === "user_id")
      .and(codecPrism[Tw].field(_.tag).getOption(bytes) must beSome("U-REAL"))
  }

  // ---- cause 2: a misleading name signal, closed by the probe ------------------------------

  "CLOSED at1 — a schema field bearing a case field's name but holding a derived value" >> {
    // {digest, id, raw_ident, payload}: `id` is a public identifier, the case field's real value is
    // in `raw_ident`. The name map is total and injective, so the old rung trusted it; the probe
    // requires the slot to HOLD the field's value, which `id` does not.
    val bytes = encodeBytes(Doc("REAL-ID", "P0"))
    (codecPrism[Doc].field(_.id).getOption(bytes) must beSome("REAL-ID"))
      .and(codecPrism[Doc].field(_.payload).getOption(bytes) must beSome("P0"))
      .and(resolved(codecPrism[Doc].field(_.id)) === "raw_ident")
      .and(resolved(codecPrism[Doc].field(_.payload)) === "payload")
  }

  "CLOSED at2 — the same, reached through the separator/case-insensitive rung" >> {
    // {USER_ID, user_ident, balance}: `USER_ID` normalises to `userid` and is a stale legacy copy.
    val bytes = encodeBytes(Acct("U-REAL", 10L))
    (codecPrism[Acct].field(_.userId).getOption(bytes) must beSome("U-REAL"))
      .and(codecPrism[Acct].field(_.balance).getOption(bytes) must beSome(10L))
      .and(resolved(codecPrism[Acct].field(_.userId)) === "user_ident")
  }

  // ---- the retired behaviour change ---------------------------------------------------------

  "RETIRED REGRESSION — a PERMUTING rename is CORRECT again" >> {
    // The codec writes case field `alpha` into the schema field literally NAMED "beta". Position
    // happened to be right and the name rung made it wrong; the probe reads the slot, so both
    // fields are right again. The one shape issue #95 made worse is now better than 0.15.1.
    val bytes = encodeBytes(PermPair("ALPHA0", "BETA0"))
    (resolved(codecPrism[PermPair].field(_.alpha)) === "beta")
      .and(resolved(codecPrism[PermPair].field(_.beta)) === "alpha")
      .and(codecPrism[PermPair].field(_.alpha).getOption(bytes) must beSome("ALPHA0"))
      .and(codecPrism[PermPair].field(_.beta).getOption(bytes) must beSome("BETA0"))
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
