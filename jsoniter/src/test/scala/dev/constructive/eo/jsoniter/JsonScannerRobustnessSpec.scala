package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Grammar-sweep and robustness specs for [[JsonPathScanner]] — a structural scanner property that
  * checks every prefix of a generated document against a model (absolute spans, in-bounds spans,
  * prefix/full agreement), plus literal/number/object/array skip grammars and `findAll` fan-out
  * over mixed steps. Complements [[JsoniterPrismSpec]] / [[JsoniterTraversalSpec]], which exercise
  * the optic-level surface rather than the scanner's byte-level dispatch. See per-block `covers`
  * comments for `JsonPathScanner.scala` line ranges targeted.
  */
class JsonScannerRobustnessSpec extends Specification with ScalaCheck:

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  private def text(b: Array[Byte], span: JsonPathScanner.Span): String =
    new String(b.slice(span.start, span.end), "UTF-8")

  // ----- 1: structural scanner property ----------------------------------

  /** A generated document together with the rendered text of its top-level children. Carrying the
    * children lets the oracle state what each top-level step MUST resolve to without re-parsing —
    * an absolute check that a purely differential prefix-vs-full oracle cannot make, because a
    * uniformly-wrong scanner agrees with itself.
    */
  final private case class DocModel(text: String, kind: Char, kids: List[String])

  private val genLeaf: Gen[String] = Gen.oneOf(
    Gen.const("true"),
    Gen.const("false"),
    Gen.const("null"),
    Gen.choose(-100, 100).map(_.toString),
    Gen.oneOf("a", "bb", "").map(s => s""""$s""""),
  )

  // Child counts are frequency-weighted rather than uniform so the two structural boundaries both
  // occur often: 0 children puts `{}` / `[]` in VALUE position (the empty-container fast paths at
  // JsonPathScanner 130/159/206/245), >= 2 children force a sibling to be SKIPPED before the
  // target is reached (the find*Loop / skip*Loop advance arms).
  private val genKidCount: Gen[Int] = Gen.frequency((2, 0), (3, 1), (3, 2), (2, 3))

  // Members and elements are joined with ", " — the space after the comma is load-bearing: it is
  // the only way a prefix cut can leave `skipObjectLoop` at a position whose whitespace run reaches
  // end-of-input (line 264 advances to `np + 1` without a skipWs, unlike line 181).
  private def genModel(depth: Int): Gen[DocModel] =
    val leaf = genLeaf.map(DocModel(_, 'l', Nil))
    if depth <= 0 then leaf
    else
      val kids = genKidCount.flatMap(n => Gen.listOfN(n, genModel(depth - 1).map(_.text)))
      Gen.oneOf(
        leaf,
        kids.map(ks => DocModel(ks.mkString("[", ", ", "]"), 'a', ks)),
        kids.map(ks =>
          DocModel(
            ks.zipWithIndex.map((v, i) => s""""k$i":$v""").mkString("{", ", ", "}"),
            'o',
            ks,
          )
        ),
      )

  private val repPaths: List[List[PathStep]] = List(
    Nil,
    List(PathStep.Field("k0")),
    List(PathStep.Field("k1")),
    List(PathStep.Index(0)),
    List(PathStep.Wildcard),
    List(PathStep.Field("k0"), PathStep.Index(1)),
  )

  /** Absolute oracle: what the scanner must return for the WHOLE document, stated from the model
    * rather than from a second run of the scanner.
    */
  private def absoluteViolations(model: DocModel, full: Array[Byte]): List[String] =
    val root = JsonPathScanner.find(full, Nil)
    val rootV =
      if root == JsonPathScanner.Span(0, full.length) then Nil
      else List(s"root: $root != Span(0,${full.length})")
    val kidV = model.kind match
      case 'o' =>
        val hits = model.kids.zipWithIndex.flatMap { (kid, i) =>
          val sp = JsonPathScanner.find(full, List(PathStep.Field(s"k$i")))
          if sp.isHit && text(full, sp) == kid then Nil else List(s"$$.k$i -> $sp, want '$kid'")
        }
        val absent = JsonPathScanner.find(full, List(PathStep.Field("zz")))
        hits ::: (if absent.isHit then List(s"$$.zz -> $absent, want Miss") else Nil)
      case 'a' =>
        val hits = model.kids.zipWithIndex.flatMap { (kid, i) =>
          val sp = JsonPathScanner.find(full, List(PathStep.Index(i)))
          if sp.isHit && text(full, sp) == kid then Nil else List(s"$$[$i] -> $sp, want '$kid'")
        }
        val oob = List(model.kids.length, -1)
          .filter(i => JsonPathScanner.find(full, List(PathStep.Index(i))).isHit)
          .map(i => s"$$[$i] hit, want Miss")
        val starTexts =
          JsonPathScanner.findAll(full, List(PathStep.Wildcard)).map(s => text(full, s))
        val starV =
          if starTexts == model.kids then Nil else List(s"$$[*] -> $starTexts, want ${model.kids}")
        hits ::: oob ::: starV
      case _ =>
        val f = JsonPathScanner.find(full, List(PathStep.Field("k0")))
        val i = JsonPathScanner.find(full, List(PathStep.Index(0)))
        (if f.isHit then List(s"leaf $$.k0 -> $f, want Miss") else Nil) :::
          (if i.isHit then List(s"leaf $$[0] -> $i, want Miss") else Nil)
    rootV ::: kidV

  /** Bounds + differential oracle over every prefix `full.take(len)`, `len` enumerated exhaustively
    * so byte-exact cuts (the last byte of a literal, the space after a comma) are straddled by
    * construction rather than sampled.
    *
    *   - bounds: every returned span satisfies `0 <= start <= end <= len`, on BOTH surfaces.
    *   - R1: a prefix hit is a truncation of the full hit (same start, end no further).
    *   - R2: when the full span fits entirely inside the prefix, the prefix answer IS the full
    *     answer. (The converse — "must be a Miss otherwise" — is false: the scanner is permissive
    *     about truncated numbers, so `{"k0":12` resolves `$.k0` to the Hit `1`.)
    *   - R3: `findAll` agrees on every span that ends strictly inside the prefix.
    */
  private def prefixViolations(full: Array[Byte]): List[String] =
    repPaths.flatMap { p =>
      val sf = JsonPathScanner.find(full, p)
      val af = JsonPathScanner.findAll(full, p)
      @tailrec def loop(len: Int, acc: List[String]): List[String] =
        if len > full.length then acc
        else
          val pre = full.take(len)
          val sp = JsonPathScanner.find(pre, p)
          val ap = JsonPathScanner.findAll(pre, p)
          val spans = (if sp.isHit then List(sp) else Nil) ::: ap
          val v1 =
            if spans.forall(s => s.start >= 0 && s.start <= s.end && s.end <= len) then Nil
            else List(s"bounds len=$len p=$p sp=$sp ap=$ap")
          val v2 =
            if !sp.isHit || (sf.isHit && sp.start == sf.start && sp.end <= sf.end) then Nil
            else List(s"R1 len=$len p=$p sp=$sp sf=$sf")
          val v3 =
            if !(sf.isHit && sf.end <= len) || sp == sf then Nil
            else List(s"R2 len=$len p=$p sp=$sp sf=$sf")
          val v4 =
            if ap.filter(_.end < len) == af.filter(_.end < len) then Nil
            else List(s"R3 len=$len p=$p ap=$ap af=$af")
          loop(len + 1, v4 ::: v3 ::: v2 ::: v1 ::: acc)
      loop(0, Nil)
    }

  "scanner: absolute spans, in-bounds spans, and prefix/full agreement at every cut" >> {
    // covers: JsonPathScanner.scala 104:12 (walkAll pushes a Span(pos,-1)), 245:8 + 245:14
    // (skipObject's `{}` fast path), 254:29 (skipObjectLoop's `||` un-short-circuited at
    // end-of-input), 348:28 + 348:48 (`matches` width guard) — plus every skip*/find* bounds guard
    // the previous `Try(...).isSuccess` oracle could only observe as "did not throw". An exception
    // now fails the property directly, so the no-throw guarantee is kept for free.
    Prop.forAll(genModel(3)) { model =>
      val full = bytes(model.text)
      val all = absoluteViolations(model, full) ::: prefixViolations(full)
      Prop.propBoolean(all.isEmpty) :| all.take(3).mkString(" | ")
    }
  }

  // ----- 2: literal sweep -------------------------------------------------

  // Valid and truncated literals are reached by the section-1 generator (literal leaves, every
  // prefix); a CORRUPTED interior byte is not a prefix of any well-formed document, so these rows
  // stay as pinned examples.
  private val literalCases: List[(String, Boolean)] = List(
    ("""{"a":txue,"b":1}""", false),
    ("""{"a":falze,"b":1}""", false),
    ("""{"a":nudl,"b":1}""", false),
    ("""{"a":true ,"b":1}trailing""", true),
  )

  "literal skip: wrong-char literals Miss, whitespace-before-comma and trailing bytes Hit" >> {
    // covers: skipLiteral dispatch + width checks (lines 303-309).
    val allOk = literalCases.forall {
      case (doc, expectHit) =>
        val r = scala.util.Try(JsonPathScanner.find(bytes(doc), List(PathStep.Field("b"))))
        r.isSuccess && r.get.isHit == expectHit
    }
    allOk must beTrue
  }

  // ----- 3: number-format sweep -------------------------------------------

  // "e" / "E" with no exponent digits: malformed, but the scanner is documented as permissive and
  // must not derail — it is what forces the exponent-sign guard (line 323) to be read.
  private val numberGen: Gen[String] =
    for
      sign <- Gen.oneOf("", "-")
      intPart <- Gen.oneOf("0", "7", "123")
      frac <- Gen.oneOf("", ".5", ".0")
      exp <- Gen.oneOf("", "e1", "e+1", "e-1", "E2", "E+2", "E-2", "e", "E")
    yield s"$sign$intPart$frac$exp"

  "number skip: sign/fraction/exponent grammar sweep — $.after always Hits" >> {
    // covers: skipNumber sign/frac/exponent-sign branches (lines 314-326), incl. 323:12.
    Prop.forAll(numberGen) { num =>
      val doc = bytes(s"""{"n":$num,"after":1}""")
      JsonPathScanner.find(doc, List(PathStep.Field("after"))).isHit
    }
  }

  // ----- 4: object sweep ---------------------------------------------------

  private def buildObject(count: Int, targetIdx: Option[Int]): (Array[Byte], Option[String]) =
    val value = "999"
    val fields: Vector[(String, String)] = (0 until count).map { i =>
      if targetIdx.contains(i) then ("target", value) else (s"f$i", i.toString)
    }.toVector
    (bytes(fields.map((k, v) => s""""$k":$v""").mkString("{", ",", "}")), targetIdx.map(_ => value))

  // (count, target index) — enumerated so every count x position combo that makes sense occurs.
  private val objectScenarios: List[(Int, Option[Int])] = List(
    (0, None),
    (1, Some(0)),
    (1, None),
    (2, Some(0)),
    (2, Some(1)),
    (2, None),
    (5, Some(0)),
    (5, Some(2)),
    (5, Some(4)),
    (5, None),
  )

  "object scan: member-count x target-position sweep — Hit iff present, span decodes correctly" >> {
    // covers: findFieldValueLoop key-match / skip-and-advance (lines 165-182).
    Prop.forAll(Gen.oneOf(objectScenarios)) {
      case (count, targetIdx) =>
        val (doc, expected) = buildObject(count, targetIdx)
        val span = JsonPathScanner.find(doc, List(PathStep.Field("target")))
        val hitOk = span.isHit == expected.isDefined
        val valueOk = expected.forall(v => text(doc, span) == v)
        hitOk && valueOk
    }
  }

  // Malformed inputs a document generator cannot produce: each has broken syntax whose bytes
  // nonetheless spell a resolvable member/element, so the mis-parse a dropped guard causes lands
  // exactly on the probed path instead of being rejected by a later guard.
  private val malformedNoThrowCases: List[(String, List[PathStep])] = List(
    // truncated numbers — skipNumber/skipDigits end-of-buffer guards (lines 314-331)
    ("""{"n":1e+""", List(PathStep.Field("after"))),
    ("""{"n":1.""", List(PathStep.Field("after"))),
    ("""{"n":-""", List(PathStep.Field("after"))),
    ("""{"n":12e""", List(PathStep.Field("after"))),
    // unquoted key / missing colon — findFieldValueLoop quote/colon guards (lines 168, 173)
    ("""{a:1,"target":2}""", List(PathStep.Field("target"))),
    ("""{"a" 1,"target":2}""", List(PathStep.Field("target"))),
  )

  "malformed number/object inputs never throw, resolve to Miss on both surfaces" >> {
    val allOk = malformedNoThrowCases.forall {
      case (doc, path) =>
        val b = bytes(doc)
        val one = scala.util.Try(JsonPathScanner.find(b, path))
        val many = scala.util.Try(JsonPathScanner.findAll(b, path))
        one.isSuccess && !one.get.isHit && many.isSuccess && many.get.isEmpty
    }
    allOk must beTrue
  }

  // ----- 5: array sweep -----------------------------------------------------

  private def buildArrayDoc(len: Int): Array[Byte] =
    val elems = (0 until len).map(i => (i * 10).toString).mkString(",")
    bytes(s"""{"arr":[$elems],"after":1}""")

  // (length, target index) — includes first/mid/last/just-past/negative-oob.
  private val arraySwScenarios: List[(Int, Int)] =
    List((0, 0), (1, 0), (1, 1), (2, 0), (2, 1), (2, 2), (5, 0), (5, 2), (5, 4), (5, 5), (5, -1))

  "array scan: length x index sweep — Hit iff 0<=idx<len, correct span; $.after Hits (skipArray)" >> {
    // covers: findArrayElementLoop (lines 212-226), skipArray (lines 268-284).
    Prop.forAll(Gen.oneOf(arraySwScenarios)) {
      case (len, idx) =>
        val doc = buildArrayDoc(len)
        val span = JsonPathScanner.find(doc, List(PathStep.Field("arr"), PathStep.Index(idx)))
        val expectHit = idx >= 0 && idx < len
        val hitOk = span.isHit == expectHit
        val valueOk = !expectHit || text(doc, span) == (idx * 10).toString
        val afterOk = JsonPathScanner.find(doc, List(PathStep.Field("after"))).isHit
        hitOk && valueOk && afterOk
    }
  }

  // ----- 6: findAll mixed-step -----------------------------------------------

  private val mixedDoc = bytes(
    """{"rows":[{"xs":[1,2]},{"xs":[3,4,5]},{"xs":[]},{"xs":[9,BAD,7]}]}"""
  )

  "findAll: mixed Index/Wildcard/Field steps resolve exact spans in document order" >> {
    // covers: walkAll Wildcard branch + walkAllArrayElementsLoop (lines 117-118, 136-150).
    val xsFirstSteps = PathParser.parse("$.rows[*].xs[0]").toOption.get
    val values: List[String] =
      JsonPathScanner.findAll(mixedDoc, xsFirstSteps).map(s => text(mixedDoc, s))
    val valuesOk = values === List("1", "3", "9") // row2's xs is empty, so it contributes nothing

    val row0Steps = PathParser.parse("$.rows[0]").toOption.get
    val row0Spans = JsonPathScanner.findAll(mixedDoc, row0Steps)
    val row0Ok = (row0Spans.length === 1).and(text(mixedDoc, row0Spans.head) === """{"xs":[1,2]}""")

    val topArr = bytes("[10,20,30]")
    val topSteps = PathParser.parse("$[0]").toOption.get
    val topSpans = JsonPathScanner.findAll(topArr, topSteps)
    val topOk = (topSpans.length === 1).and(text(topArr, topSpans.head) === "10")

    val malformedSteps = PathParser.parse("$.rows[*].xs[1]").toOption.get
    val noThrow = scala.util.Try(JsonPathScanner.findAll(mixedDoc, malformedSteps)).isSuccess

    valuesOk.and(row0Ok).and(topOk).and(noThrow must beTrue)
  }

  // ----- 8: small pinned examples --------------------------------------------

  "small example: a key-length mismatch must not match" >> {
    // covers: stringEqualsAscii length guard (line 197). The generator's keys and probes are all
    // two bytes wide, so the length half of that guard never fires with a mismatch under it.
    val lenMismatch = bytes("""{"ab":1,"target":2}""")
    JsonPathScanner.find(lenMismatch, List(PathStep.Field("abc"))).isHit must beFalse
  }
