package dev.constructive.eo

import cats.instances.function.given
import org.specs2.mutable.Specification

import data.MultiFocus

/** `MultiFocus.zipWith` — the pointwise combine a Grate can express and a Traversal cannot.
  *
  * A Traversal visits one focus at a time with no access to a second structure, so nothing built
  * from `modify` can merge two containers; a Grate sees every focus while rebuilding, which is what
  * makes this definable at all.
  */
class GrateZipSpec extends Specification:

  type BoolFn[A] = Boolean => A

  "zipWith over Representable[Function1]" should {
    val defaults: BoolFn[Int] = b => if b then 1 else 2
    val overrides: BoolFn[Int] = b => if b then 10 else 20

    "combine pointwise at every representation point" >> {
      val summed = MultiFocus.zipWith(defaults, overrides)(_ + _)
      (summed(true) === 11).and(summed(false) === 22)
    }
    "change the result type" >> {
      val rendered = MultiFocus.zipWith(defaults, overrides)((d, o) => s"$d/$o")
      (rendered(true) === "1/10").and(rendered(false) === "2/20")
    }
    "zip: the pairing special case" >> {
      val paired = MultiFocus.zip(defaults, overrides)
      (paired(true) === ((1, 10))).and(paired(false) === ((2, 20)))
    }
    "agree with map when both sides are the same container" >> {
      val doubledViaZip = MultiFocus.zipWith(defaults, defaults)(_ + _)
      (doubledViaZip(true) === 2).and(doubledViaZip(false) === 4)
    }
    "merge two configurations field-by-field — the motivating use" >> {
      // Representable index = the field name; `orElse` gives override-wins-if-present semantics.
      type Field[A] = String => A
      val base: Field[Option[String]] = {
        case "host" => Some("localhost")
        case "port" => Some("80")
        case _      => None
      }
      val patch: Field[Option[String]] = {
        case "port" => Some("8080")
        case _      => None
      }
      val merged = MultiFocus.zipWith(patch, base)(_ orElse _)
      (merged("host") === Some("localhost"))
        .and(merged("port") === Some("8080"))
        .and(merged("missing") === None)
    }
  }

  "zipWith and the representable Grate optic" should {
    "agree: zipping a container with itself is the optic's own modify" >> {
      val src: BoolFn[Int] = b => if b then 5 else 7
      val viaZip = MultiFocus.zipWith(src, src)((a, _) => a + 1)
      val viaOptic = MultiFocus.representable[BoolFn, Int].modify(_ + 1)(src)
      (viaZip(true) === viaOptic(true)).and(viaZip(false) === viaOptic(false))
    }
  }
