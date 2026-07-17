package dev.constructive.eo.jsoniter

import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Round-trip and boundary specs for [[PathParser]] — full grammar coverage via render/reparse, an
  * exhaustive ASCII ident-start/-part sweep, and pinned error-path examples. See per-block `covers`
  * comments for `PathParser.scala` line ranges targeted.
  */
class PathParserBoundarySpec extends Specification with ScalaCheck:

  private val identGen: Gen[String] =
    for
      head <- Gen.oneOf(('A' to 'Z') ++ ('a' to 'z') :+ '_')
      tailLen <- Gen.choose(0, 5)
      tail <- Gen.listOfN(tailLen, Gen.oneOf(('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') :+ '_'))
    yield (head +: tail).mkString

  private val stepGen: Gen[PathStep] = Gen.oneOf(
    identGen.map(PathStep.Field.apply),
    Gen.choose(0, 999).map(PathStep.Index.apply),
    Gen.const(PathStep.Wildcard),
  )

  private def render(step: PathStep): String = step match
    case PathStep.Field(name) => s".$name"
    case PathStep.Index(i)    => s"[$i]"
    case PathStep.Wildcard    => "[*]"

  private val pathGen: Gen[List[PathStep]] =
    Gen.choose(0, 4).flatMap(n => Gen.listOfN(n, stepGen))

  "round-trip: render(steps) parses back to the same steps" >> {
    // covers: parseSteps/parseField/parseIndex full dispatch (lines 32-77).
    Prop.forAll(pathGen) { steps =>
      val rendered = "$" + steps.map(render).mkString
      val parsed: Either[String, List[PathStep]] = PathParser.parse(rendered)
      parsed == Right(steps)
    }
  }

  "ASCII ident-boundary sweep: every char 0..127 accepted as ident-start iff [A-Za-z_]" >> {
    // covers: isIdentStart (lines 79-80) — off-by-one at range edges ('@','[','`','{').
    val allOk = (0 until 128).forall { c =>
      val ch = c.toChar
      val expected = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_'
      PathParser.parse(s"$$.${ch}x").isRight == expected
    }
    allOk must beTrue
  }

  "error-path examples: never throw; exact Left messages where specified" >> {
    // covers: parseField empty-ident guard (line 49), parseIndex end-of-input guards
    // (lines 65-66, 73-74), PathParser.parse leading-'$' guard (lines 27-29).
    def safeParse(s: String) = scala.util.Try(PathParser.parse(s))

    val dotOnly = safeParse("$.")
    val bracketOnly = safeParse("$[")
    val bracketDigits = safeParse("$[42")
    val bracketBadIdx = safeParse("$[x]")
    val noDollar = safeParse("a.foo")
    val all = List(dotOnly, bracketOnly, bracketDigits, bracketBadIdx, noDollar)

    val allSucceeded = all.forall(_.isSuccess)
    val allLeft = all.forall(_.get.isLeft)
    val badIdxMsgOk = bracketBadIdx.get == Left("expected integer or '*' at position 2")
    val noDollarMsgOk = noDollar.get == Left("path must start with '$' (got 'a')")

    (allSucceeded must beTrue)
      .and(allLeft must beTrue)
      .and(badIdxMsgOk must beTrue)
      .and(noDollarMsgOk must beTrue)
  }
