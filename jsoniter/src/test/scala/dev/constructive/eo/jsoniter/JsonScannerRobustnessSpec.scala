package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Grammar-sweep and robustness specs for [[JsonPathScanner]] — truncation-safety across every
  * prefix of a generated document, literal/number/object/array skip grammars, and `findAll` fan-out
  * over mixed steps. Complements [[JsoniterPrismSpec]] / [[JsoniterTraversalSpec]], which exercise
  * the optic-level surface rather than the scanner's byte-level dispatch. See per-block `covers`
  * comments for `JsonPathScanner.scala` line ranges targeted.
  */
class JsonScannerRobustnessSpec extends Specification with ScalaCheck:

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  private def text(b: Array[Byte], span: JsonPathScanner.Span): String =
    new String(b.slice(span.start, span.end), "UTF-8")

  // ----- 1: truncation safety -------------------------------------------

  private def genValue(depth: Int): Gen[String] =
    val leaves = Gen.oneOf(
      Gen.const("true"),
      Gen.const("false"),
      Gen.const("null"),
      Gen.choose(-100, 100).map(_.toString),
      Gen.oneOf("a", "bb", "").map(s => s""""$s""""),
    )
    if depth <= 0 then leaves
    else
      Gen.oneOf(
        leaves,
        Gen
          .choose(0, 3)
          .flatMap(n => Gen.listOfN(n, genValue(depth - 1)))
          .map(_.mkString("[", ",", "]")),
        Gen
          .choose(0, 3)
          .flatMap(n => Gen.listOfN(n, genValue(depth - 1)))
          .map(vs => vs.zipWithIndex.map((v, i) => s""""k$i":$v""").mkString("{", ",", "}")),
      )

  private val genDoc: Gen[String] = genValue(3)

  private val repPaths: List[List[PathStep]] = List(
    Nil,
    List(PathStep.Field("k0")),
    List(PathStep.Index(0)),
    List(PathStep.Wildcard),
    List(PathStep.Field("k0"), PathStep.Index(1)),
  )

  "find/findAll never throw on any prefix of a generated document" >> {
    // covers: every skip*/find* function in JsonPathScanner.scala — a mid-document cut must
    // resolve to Miss/partial-Nil at any byte boundary, never an exception (bounds checks at
    // lines 142/166/226/260/278/300/etc).
    Prop.forAll(genDoc) { doc =>
      val docBytes = bytes(doc)
      @tailrec def loop(len: Int): Boolean =
        if len > docBytes.length then true
        else
          val prefix = docBytes.take(len)
          val stepsOk = repPaths.forall { p =>
            scala.util.Try(JsonPathScanner.find(prefix, p)).isSuccess &&
            scala.util.Try(JsonPathScanner.findAll(prefix, p)).isSuccess
          }
          if !stepsOk then false else loop(len + 1)
      loop(0)
    }
  }

  // ----- 2: literal sweep -------------------------------------------------

  private val literalCases: List[(String, Boolean)] = List(
    ("""{"a":true,"b":1}""", true),
    ("""{"a":false,"b":1}""", true),
    ("""{"a":null,"b":1}""", true),
    ("""{"a":tru""", false),
    ("""{"a":fals""", false),
    ("""{"a":nul""", false),
    ("""{"a":txue,"b":1}""", false),
    ("""{"a":falze,"b":1}""", false),
    ("""{"a":nudl,"b":1}""", false),
    ("""{"a":true ,"b":1}trailing""", true),
  )

  "literal skip: true/false/null valid/truncated/wrong-char/trailing, never throws" >> {
    // covers: skipLiteral dispatch + width checks (lines 312-317).
    val allOk = literalCases.forall {
      case (doc, expectHit) =>
        val r = scala.util.Try(JsonPathScanner.find(bytes(doc), List(PathStep.Field("b"))))
        r.isSuccess && r.get.isHit == expectHit
    }
    allOk must beTrue
  }

  // ----- 3: number-format sweep -------------------------------------------

  private val numberGen: Gen[String] =
    for
      sign <- Gen.oneOf("", "-")
      intPart <- Gen.oneOf("0", "7", "123")
      frac <- Gen.oneOf("", ".5", ".0")
      exp <- Gen.oneOf("", "e1", "e+1", "e-1", "E2", "E+2", "E-2")
    yield s"$sign$intPart$frac$exp"

  "number skip: sign/fraction/exponent grammar sweep — $.after always Hits" >> {
    // covers: skipNumber sign/frac/exponent-sign branches (lines 322-334).
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

  // Malformed or truncated scalar/object inputs that must resolve to a no-throw Miss.
  // (Missing-]/-array truncation is already exercised by the section-1 truncation property,
  // which drives k0[idx] into every prefix of a generated array — skipArray + findArray guards.)
  private val malformedNoThrowCases: List[(String, List[PathStep])] = List(
    // truncated numbers — skipNumber/skipDigits end-of-buffer guards (lines 322-338)
    ("""{"n":1e+""", List(PathStep.Field("after"))),
    ("""{"n":1.""", List(PathStep.Field("after"))),
    ("""{"n":-""", List(PathStep.Field("after"))),
    ("""{"n":12e""", List(PathStep.Field("after"))),
    // unquoted key / missing colon — findFieldValueLoop quote/colon guards (lines 168, 173)
    ("""{a:1,"target":2}""", List(PathStep.Field("target"))),
    ("""{"a" 1,"target":2}""", List(PathStep.Field("target"))),
  )

  "malformed number/object inputs never throw, resolve to Miss" >> {
    val allOk = malformedNoThrowCases.forall {
      case (doc, path) =>
        val r = scala.util.Try(JsonPathScanner.find(bytes(doc), path))
        r.isSuccess && !r.get.isHit
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
    // covers: findArrayElementLoop (lines 220-234), skipArray (lines 276-292).
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

  "small examples: root Nil-path, negative number, unterminated string, key-length mismatch" >> {
    // covers: find() start boundary (lines 49-51), skipNumber '-' branch (line 323), skipString
    // unterminated guard (line 308), stringEqualsAscii length guard (lines 197-198).
    val root = bytes("""{"a":1}""")
    val rootHit = JsonPathScanner.find(root, Nil).isHit must beTrue

    val neg = bytes("""{"x":-5,"y":1}""")
    val negHit = JsonPathScanner.find(neg, List(PathStep.Field("y"))).isHit must beTrue

    val unterminated = bytes("""{"a":"oops""")
    val untermR = scala.util.Try(JsonPathScanner.find(unterminated, List(PathStep.Field("a"))))
    val untermOk = (untermR.isSuccess must beTrue).and(untermR.get.isHit must beFalse)

    val lenMismatch = bytes("""{"ab":1,"target":2}""")
    val lenOk = JsonPathScanner.find(lenMismatch, List(PathStep.Field("abc"))).isHit must beFalse

    rootHit.and(negHit).and(untermOk).and(lenOk)
  }
