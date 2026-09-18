package dev.constructive.eo.avro

import scala.util.control.NonFatal

import java.util.ArrayList
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** The ALL-OR-NOTHING nominal rung, stated as a declarative oracle and checked EXHAUSTIVELY.
  *
  * `AvroWalk.fieldNameAt` resolves `.field(_.x)` by NAME when the whole case-field list maps to
  * DISTINCT schema fields (exact, else uniquely up to `_` / `-` / `.` and case), and abstains to
  * position otherwise. Four clauses — totality, injectivity, exact-beats-normalised, ambiguity ⇒
  * abstain — and before this spec only totality was pinned. Issue #104 recorded the cost: an
  * experimental alias rung silently re-aimed a case the rung gets right while every avro test
  * stayed green. A MIS-TARGETED optic satisfies get-put, put-get, put-put and modify fusion
  * perfectly, so no optic law family can see this; the contract is a naming contract on a
  * resolution function, which is why this is a property and not a `checkAll` registration.
  *
  * The oracle below is written from the DOCTRINE, deliberately not as a frozen copy of the
  * implementation: parity against a frozen copy of the code cannot detect a wrong doctrine, which
  * is exactly the #104 failure mode.
  *
  * That is the division of labour with [[NominalResolutionOracle]] and
  * `vulcan.NominalResolutionParitySpec`, which #107 added alongside its index rewrite. That oracle
  * is the OLD ALGORITHM frozen verbatim, and its spec asks "does the cached-index rung still agree
  * with the scan it replaced" — a refactor gate, which cannot fire if the algorithm was already
  * resolving to the wrong field. This one asks "does the rung agree with what the rung is FOR", and
  * is written without reading the implementation. Both are wanted: #107's rewrite deleted
  * `sameFieldName` and the fuzzy per-field scan outright, and the fact that this doctrine oracle
  * still agrees cell for cell is the evidence that the rewrite preserved the CONTRACT and not just
  * the code path.
  *
  * The corpus is an exhaustive enumeration rather than a `Gen`: the discriminating cells are
  * threshold cells (a duplicate resolution landing on slot 0, a fuzzy ambiguity whose FIRST hit is
  * at index 0 versus at index >= 1, `declIdx` exactly equal to the schema's field count) that a
  * default generator would essentially never produce.
  *
  * Subsumes the deleted `"AvroWalk.fieldNameAt: non-record parent and out-of-range declIdx both
  * throw loudly"` example, which tested `declIdx = 99` against a 2-field schema — a cell where the
  * `>=` guard and a `>` mutant agree.
  *
  * covers (against post-#107 `AvroWalk`, which replaced the fuzzy per-field scan with
  * `normalisedName` + a cached `normalisedNameIndex`): `if exact != null` in `totalNominalIndex`
  * (exact-beats-normalised fast path), `j < 0` and the `&&` in its `seen` injectivity scan, the
  * `idx < 0` verdict on a `null` or `Ambiguous` index hit (ambiguity => abstain), the char
  * classification and `Character.toLowerCase` in `normalisedName`, the four disjuncts of the
  * all-or-nothing precondition, and `declIdx >= fields.size` in `fieldNameAt` at the exact
  * boundary. Line numbers are deliberately not quoted: the pre-#107 ones this spec was written
  * against are already gone.
  */
class AvroNominalDoctrineSpec extends Specification:

  // ---- Corpus -------------------------------------------------------------
  //
  // Every pool entry is BOTH a legal Avro field name (`[A-Za-z_][A-Za-z0-9_]*`) and a legal Scala
  // identifier, and ASCII-only so `toLowerCase` agrees with the per-char `Character.toLowerCase`
  // the implementation uses. `-` and `.` are therefore absent by construction: those two `skip`
  // arms are unreachable from any legal schema paired with any legal Scala identifier.
  private val namePool: List[String] = List("a", "A", "_a", "b", "a_b", "aB")

  /** The sub-pool used for arity-3 case lists — keeps the corpus at ~10^2 case shapes. */
  private val smallPool: List[String] = List("a", "A", "_a", "b")

  private def seqsWithRep(pool: List[String], len: Int): List[List[String]] =
    if len <= 0 then List(Nil)
    else
      for
        h <- pool
        t <- seqsWithRep(pool, len - 1)
      yield h :: t

  private def injectiveSeqs(pool: List[String], len: Int): List[List[String]] =
    if len <= 0 then List(Nil)
    else
      for
        h <- pool
        t <- injectiveSeqs(pool.filterNot(_ == h), len - 1)
      yield h :: t

  private def recordOf(names: List[String], tag: Int): Schema =
    val fields = new ArrayList[Schema.Field]()
    names.foreach(n =>
      fields.add(new Schema.Field(n, Schema.create(Schema.Type.STRING), null, null))
    )
    Schema.createRecord(s"R$tag", null, "eo.avro.test", false, fields)

  /** All injective ordered field lists of length 1..3 — Avro forbids duplicate field names. */
  private val schemas: List[(List[String], Schema)] =
    val lists = (1 to 3).toList.flatMap(injectiveSeqs(namePool, _))
    lists.zipWithIndex.map((ns, i) => (ns, recordOf(ns, i)))

  /** Case-field lists WITH repetition — two identical case names is the degenerate injectivity
    * failure — plus the empty list (a NamedTuple parent, which has no case fields).
    */
  private val caseCorpus: List[List[String]] =
    Nil :: (seqsWithRep(namePool, 1) ++ seqsWithRep(namePool, 2) ++ seqsWithRep(smallPool, 3))

  // ---- The declarative oracle --------------------------------------------

  private def norm(s: String): String =
    s.filter(c => c != '_' && c != '-' && c != '.').toLowerCase

  /** Exact name wins; otherwise the UNIQUE normalised match; otherwise no signal. */
  private def oracleIdx(fields: List[String], name: String): Int =
    val exact = fields.indexOf(name)
    if exact >= 0 then exact
    else
      val hits = fields.indices.filter(i => norm(fields(i)) == norm(name))
      if hits.sizeIs == 1 then hits.head else -1

  /** What the DOCTRINE says `fieldNameAt` returns, as either a name or a `throw:<class>` tag. */
  private def oracle(fields: List[String], caseNames: List[String], declIdx: Int): String =
    def positional: String =
      if declIdx >= fields.size then "throw:IllegalArgumentException"
      else if declIdx < 0 then "throw:IndexOutOfBoundsException"
      else fields(declIdx)
    val arity = caseNames.size
    if arity == 0 || declIdx < 0 || declIdx >= arity || arity > fields.size then positional
    else
      val ix = caseNames.map(oracleIdx(fields, _))
      val total = ix.forall(_ >= 0)
      val injective = ix.distinct.sizeIs == ix.size
      if total && injective then fields(ix(declIdx)) else positional

  private def observed(schema: Schema, caseNames: List[String], declIdx: Int): String =
    try
      AvroWalk.fieldNameAt(
        schema,
        caseNames.lift(declIdx).getOrElse("?"),
        declIdx,
        caseNames,
        "test",
      )
    catch case NonFatal(e) => "throw:" + e.getClass.getSimpleName

  private val disagreements: List[String] =
    for
      (fieldNames, schema) <- schemas
      caseNames <- caseCorpus
      declIdx <- -1 to (caseNames.size + 1)
      want = oracle(fieldNames, caseNames, declIdx)
      got = observed(schema, caseNames, declIdx)
      if want != got
    yield s"schema=$fieldNames case=$caseNames declIdx=$declIdx want=$want got=$got"

  /** The corpus says nothing about non-record parents — they are the one input shape the oracle
    * does not model, because resolution never begins. Checked here rather than in a block of its
    * own: it is the same function's same contract, and the guard is on the parent's schema TYPE, so
    * it must hold at every declIdx.
    */
  private val nonRecordOutcomes: List[String] =
    val stringSchema = Schema.create(Schema.Type.STRING)
    val parents =
      List(stringSchema, Schema.createArray(stringSchema), Schema.createMap(stringSchema))
    for
      parent <- parents
      declIdx <- List(0, 1)
    yield observed(parent, List("a", "b"), declIdx)

  "AvroWalk.fieldNameAt agrees with the nominal-resolution doctrine over the whole corpus" >> {
    val shown: List[String] = disagreements.take(10)
    val cells: Int = schemas.size * caseCorpus.size
    (shown === List.empty[String])
      .and(disagreements.size === 0)
      .and(cells must be_>(10000))
      .and(nonRecordOutcomes === List.fill(6)("throw:IllegalArgumentException"))
  }

end AvroNominalDoctrineSpec
