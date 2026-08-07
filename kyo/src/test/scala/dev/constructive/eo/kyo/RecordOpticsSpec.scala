package dev.constructive.eo
package kyo

import _root_.kyo.*
import org.specs2.mutable.Specification

// Record fixtures live at top level: specs2's mutable DSL has its own String `~`
// (HTML links) that shadows kyo's field constructor inside the Specification body.
private type PersonNT = (name: String, age: Int)
private type PersonR = "name" ~ String & "age" ~ Int
private type FullName = "first" ~ String & "last" ~ String

private val recFixture: Record[PersonR] = "name" ~ "Alice" & "age" ~ 30
private val widerFixture: Record[PersonR] = Record.widen(recFixture & "city" ~ "Paris")
private val fullNameFixture: Record[FullName] = "first" ~ "Ada" & "last" ~ "Lovelace"

// 25 fields — beyond the TupleN accessor range, so the macro's TupleXXL paths
// (`Tuples.apply` reads, `Tuples.fromIArray` build) are the ones under test.
private type WideNT = (
    f1: Int,
    f2: Int,
    f3: Int,
    f4: Int,
    f5: Int,
    f6: Int,
    f7: Int,
    f8: Int,
    f9: Int,
    f10: Int,
    f11: Int,
    f12: Int,
    f13: Int,
    f14: Int,
    f15: Int,
    f16: Int,
    f17: Int,
    f18: Int,
    f19: Int,
    f20: Int,
    f21: Int,
    f22: Int,
    f23: Int,
    f24: Int,
    f25: Int,
)

private val wideNt: WideNT = (
  f1 = 1,
  f2 = 2,
  f3 = 3,
  f4 = 4,
  f5 = 5,
  f6 = 6,
  f7 = 7,
  f8 = 8,
  f9 = 9,
  f10 = 10,
  f11 = 11,
  f12 = 12,
  f13 = 13,
  f14 = 14,
  f15 = 15,
  f16 = 16,
  f17 = 17,
  f18 = 18,
  f19 = 19,
  f20 = 20,
  f21 = 21,
  f22 = 22,
  f23 = 23,
  f24 = 24,
  f25 = 25,
)

// Case-class fixtures live at top level too: the macro rebuilds via `new T(...)`,
// which carries no outer accessor for spec-nested classes (same rule as eo-generics).
private case class PersonCc(name: String, age: Int)
private case class BoxCc[A](value: A, label: String)

private enum ShapeCc:
  case Circle(radius: Double)

class RecordOpticsSpec extends Specification:

  val nt: PersonNT = (name = "Alice", age = 30)
  val rec: Record[PersonR] = recFixture

  val personI = Record.iso[PersonNT]
  val ageL = Record.lens[PersonR]("age")

  "Record.iso" should {
    "get builds the record from the NamedTuple" >> {
      val r = personI.get(nt)
      (r.name === "Alice").and(r.age === 30)
    }
    "reverseGet reads the NamedTuple back" >> (personI.reverseGet(rec) === nt)
    "roundtrip NamedTuple → Record → NamedTuple is identity" >>
      (personI.reverseGet(personI.get(nt)) === nt)
    "roundtrip Record → NamedTuple → Record is identity on exact records" >>
      (personI.get(personI.reverseGet(rec)) === rec)
    "roundtrip on a wider record compacts to the named shape" >>
      (personI.get(personI.reverseGet(widerFixture)) === widerFixture.compact)
  }

  "Record.iso beyond 22 fields" should {
    val wideI = Record.iso[WideNT]
    "get builds the record" >> {
      val r = wideI.get(wideNt)
      (r.f1 === 1).and(r.f23 === 23).and(r.f25 === 25)
    }
    "roundtrip NamedTuple → Record → NamedTuple is identity" >>
      (wideI.reverseGet(wideI.get(wideNt)) === wideNt)
    "roundtrip Record → NamedTuple → Record is identity" >> {
      val r = wideI.get(wideNt)
      wideI.get(wideI.reverseGet(r)) === r
    }
  }

  "Record.iso on case classes" should {
    val ccI = Record.iso[PersonCc]
    val cc = PersonCc("Alice", 30)
    "get builds the record from the case class" >> {
      val r = ccI.get(cc)
      (r.name === "Alice").and(r.age === 30)
    }
    "reverseGet rebuilds the case class" >> (ccI.reverseGet(ccI.get(cc)) === cc)
    "roundtrip Record → case class → Record is identity" >>
      (ccI.get(ccI.reverseGet(rec)) === rec)
    "produce the same record as the NamedTuple iso" >> (ccI.get(cc) === personI.get(nt))
    "handle generic case classes at concrete instantiations" >> {
      val boxI = Record.iso[BoxCc[Int]]
      val box = BoxCc(42, "answer")
      (boxI.get(box).value === 42).and(boxI.reverseGet(boxI.get(box)) === box)
    }
    "handle enum cases" >> {
      val circleI = Record.iso[ShapeCc.Circle]
      val c = new ShapeCc.Circle(2.5) // enum `apply` widens to ShapeCc; `new` keeps the case type
      (circleI.get(c).radius === 2.5).and(circleI.reverseGet(circleI.get(c)) === c)
    }
    "drill into a field through andThen" >> {
      val ageInCc = ccI.andThen(ageL)
      (ageInCc.get(cc) === 30).and(ageInCc.modify(_ + 1)(cc) === PersonCc("Alice", 31))
    }
  }

  "Record.lens" should {
    "get the field" >> (ageL.get(rec) === 30)
    "put-get" >> (ageL.get(ageL.replace(31)(rec)) === 31)
    "get-put observes identically" >> (ageL.replace(ageL.get(rec))(rec) === rec)
    "put-put" >> (ageL.get(ageL.replace(2)(ageL.replace(1)(rec))) === 2)
    "leave sibling fields untouched" >> (ageL.replace(31)(rec).name === "Alice")
    "distinguish same-typed fields by name" >> {
      val out = Record.lens[FullName]("first").replace("Grace")(fullNameFixture)
      (out.first === "Grace").and(out.last === "Lovelace")
    }
  }

  "composition" should {
    "drill NamedTuple → Record → field through andThen" >> {
      val ageInNt = personI.andThen(ageL)
      val bumped: PersonNT = (name = "Alice", age = 31)
      (ageInNt.get(nt) === 30).and(ageInNt.modify(_ + 1)(nt) === bumped)
    }
    "serve capability-consuming code" >> {
      def bump[S](s: S)(using m: CanModify[S, Int]): S = m.modify(_ + 12)(s)
      bump(rec)(using ageL).age === 42
    }
  }
