package dev.constructive.eo.avro.vulcan

import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

import dev.constructive.eo.avro.{AvroCodec, AvroWalk, NominalResolutionOracle}
import java.util.ArrayList
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** The DOCTRINE gate for issue #103: the pre-index rung must return the SAME verdict as the scan it
  * replaces, for every input — not merely "the suite is still green".
  *
  * A performance rewrite of a resolution rule is only safe if it is verdict-identical, and "the
  * examples still pass" cannot show that: an example pins one cell, while the rung's semantics are
  * a function over (schema, case-field list, declaration index). So this spec diffs the LIVE
  * `AvroWalk.totalNominalIndex` against [[NominalResolutionOracle]] — the frozen pre-#103
  * implementation, kept verbatim — over two corpora:
  *
  *   1. EXHAUSTIVE SYNTHETIC. Every ordered schema-field list of length 1-3 drawn from a pool of
  *      deliberately confusable names (`a`, `A`, `_a`, `a_b`, `ab`, `aB`, `b` — exact collisions,
  *      case collisions, separator collisions), crossed with every case-name list of length 0-3
  *      over that pool plus an unmatched name, crossed with every declaration index from -1 to
  *      arity. That covers each semantic the rewrite could lose, by construction rather than by
  *      hoping an example happens to hit it: exact-beats-normalised, two-normalised-hits-is-none,
  *      the `arity > fields.size` short-circuit, whole-list injectivity, the empty `caseNames`
  *      (NamedTuple) abstention and the out-of-range `declIdx` abstention.
  *   1. REAL FIXTURES. Every codec behind the three #98 specs, so the diff is anchored to the
  *      shapes the doctrine was actually written for, not only to synthetic ones.
  *
  * The printed table is the artifact: verdict per (fixture, case field), plus the mismatch count,
  * which must be zero.
  */
class NominalResolutionParitySpec extends Specification:

  // ---- corpus 1: exhaustive synthetic -------------------------------------------------------

  /** Exact (`a` vs `a`), case (`a` vs `A`), separator (`a_b` vs `ab`) and cross (`aB` vs `a_b`)
    * collisions all live in this pool, so the corpus contains records whose fields normalise alike
    * and records where an exact hit and a normalised hit point at different positions.
    */
  private val pool = List("a", "A", "_a", "a_b", "ab", "aB", "b")

  /** `c` matches nothing in the pool — the "not total" leg. */
  private val caseNamePool = pool :+ "c"

  private def record(names: List[String]): Schema =
    val fields = new ArrayList[Schema.Field](names.size)
    names.foreach(n =>
      fields.add(new Schema.Field(n, Schema.create(Schema.Type.STRING), null, null))
    )
    Schema.createRecord(
      "P" + names.mkString("_").replace("-", "").replace(".", ""),
      null,
      "eo.p",
      false,
      fields
    )

  /** Ordered selections without repetition — a record cannot declare one name twice. */
  private val schemas: List[(String, Schema)] =
    (1 to 3)
      .toList
      .flatMap(k => pool.combinations(k).flatMap(_.permutations).toList)
      .map(ns => (ns.mkString("{", ",", "}"), record(ns)))

  /** Sequences WITH repetition — a duplicated case name is the sharpest injectivity probe. */
  private def sequences(k: Int): List[List[String]] =
    if k == 0 then List(Nil)
    else caseNamePool.flatMap(h => sequences(k - 1).map(h :: _))

  private val caseLists: List[List[String]] = (0 to 3).toList.flatMap(sequences)

  // ---- corpus 2: the real #98 fixtures ------------------------------------------------------

  private def entry[A](label: String, names: String*)(using
      c: AvroCodec[A]
  ): (String, Schema, List[String]) =
    (label, c.schema, names.toList)

  private val fixtures: List[(String, Schema, List[String])] =
    List(
      entry[Three]("Three", "a", "b", "c"),
      entry[Pair]("Pair", "alpha", "beta"),
      entry[SnakeComputed]("SnakeComputed", "clickId", "landingPageId"),
      entry[Inner]("Inner", "x", "y"),
      entry[Outer]("Outer", "id", "inner"),
      entry[Basket]("Basket", "items"),
      entry[NPair]("NPair", "alpha", "beta"),
      entry[PermPair]("PermPair", "alpha", "beta"),
      entry[RPair]("RPair", "alpha", "beta"),
      entry[HostileTransform.Hostile]("Hostile", "alpha", "beta"),
      entry[ThreeNt.AB]("ThreeNt(NamedTuple)"),
      entry[FpClick]("FpClick", "clickId", "landingPageId"),
      entry[FpVisit]("FpVisit", "userId", "user"),
      entry[FpRow]("FpRow", "id", "name", "cachedHash"),
      entry[FpKeep]("FpKeep", "alpha", "beta"),
      entry[FpItem]("FpItem", "sku", "qty"),
      entry[FpCart]("FpCart", "owner", "items"),
      entry[FpInner]("FpInner", "x", "y"),
      entry[FpOuter]("FpOuter", "id", "inner"),
      entry[FpSnake.SnakeClick]("SnakeClick", "clickId", "landingPageId"),
      entry[FpHostile.HostileClick]("HostileClick", "clickId", "landingPageId"),
      entry[FpPlain.Bag]("Bag", "tags", "total"),
      entry[Ev]("Ev", "occurredAt", "seqNo"),
      entry[Comp]("Comp", "a", "b", "c"),
      entry[Doc]("Doc", "id", "payload"),
      entry[Acct]("Acct", "userId", "balance"),
      entry[Tw]("Tw", "userId", "tag"),
    )

  private def verdictName(schema: Schema, idx: Int): String =
    if idx < 0 then "ABSTAIN" else schema.getFields.get(idx).name

  // ---- the diff -----------------------------------------------------------------------------

  "every SYNTHETIC verdict is identical to the frozen pre-index rung" >> {
    val mismatches =
      for
        (label, schema) <- schemas
        names <- caseLists
        i <- -1 to names.size
        live = AvroWalk.totalNominalIndex(schema, names, i)
        was = NominalResolutionOracle.totalNominalIndex(schema, names, i)
        if live != was
      yield s"$label case=${names.mkString("[", ",", "]")} declIdx=$i: was $was, now $live"
    val cells = schemas.size * caseLists.size
    println(
      s"=== nominal verdict diff, synthetic: ${schemas.size} schemas x ${caseLists.size}"
        + s" case-name lists ($cells pairs, every declIdx in [-1..arity]) ==="
    )
    println(s"  mismatches: ${mismatches.size}")
    mismatches.take(20).foreach(m => println(s"  DIFF $m"))
    mismatches must beEmpty
  }

  "every REAL-FIXTURE verdict is identical to the frozen pre-index rung" >> {
    println("=== nominal verdict diff, real fixtures (case field -> schema field, or ABSTAIN) ===")
    val mismatches =
      fixtures.flatMap: (label, schema, names) =>
        val schemaFields = schema.getFields.asScala.map(_.name).mkString(",")
        println(f"  $label%-20s schema{$schemaFields}")
        names
          .zipWithIndex
          .flatMap: (n, i) =>
            val live = AvroWalk.totalNominalIndex(schema, names, i)
            val was = NominalResolutionOracle.totalNominalIndex(schema, names, i)
            println(
              f"    $n%-16s was ${verdictName(schema, was)}%-16s now ${verdictName(schema, live)}%-16s"
                + (if live == was then "same" else "*** DIFF ***")
            )
            if live == was then Nil
            else List(s"$label.$n: was $was, now $live")
    println(s"  mismatches: ${mismatches.size}")
    mismatches must beEmpty
  }

  // ---- the semantics, pinned directly -------------------------------------------------------
  // Each of these is covered by the exhaustive sweep above; they are spelled out separately so a
  // failure NAMES the rule that broke instead of printing a synthetic triple.

  private val tricky = record(List("a", "A", "b"))

  "an EXACT name wins over a normalised match elsewhere" >> {
    // `a` and `A` normalise alike; an exact hit must still answer, and answer with its OWN slot.
    (AvroWalk.totalNominalIndex(tricky, List("a", "b"), 0) === 0)
      .and(AvroWalk.totalNominalIndex(tricky, List("A", "b"), 0) === 1)
  }

  "MORE than one normalised match is ambiguity, never first-wins" >> {
    // `_a` is exact for neither `a` nor `A`, and normalises to both: no signal, so the whole
    // mapping fails and every field abstains.
    AvroWalk.totalNominalIndex(tricky, List("_a", "b"), 1) === -1
  }

  "arity > fields.size short-circuits before any lookup" >> {
    AvroWalk.totalNominalIndex(record(List("a")), List("a", "b"), 0) === -1
  }

  "injectivity is checked across the WHOLE case-field list" >> {
    // Case fields `ab` and `aB` normalise to the same key, and that key names ONE schema field,
    // `a_b`: two case fields aimed at one slot, so the map is not injective. The rung then abstains
    // for EVERY field — the second case field does not merely lose slot 0 to the first.
    val r = record(List("a_b", "b"))
    (AvroWalk.totalNominalIndex(r, List("ab", "aB"), 0) === -1)
      .and(AvroWalk.totalNominalIndex(r, List("ab", "aB"), 1) === -1)
  }

  "an empty caseNames (a NamedTuple parent) abstains" >> {
    AvroWalk.totalNominalIndex(tricky, Nil, 0) === -1
  }

  "a declIdx outside [0, arity) abstains" >> {
    (AvroWalk.totalNominalIndex(tricky, List("a", "b"), -1) === -1)
      .and(AvroWalk.totalNominalIndex(tricky, List("a", "b"), 2) === -1)
  }

end NominalResolutionParitySpec
