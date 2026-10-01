package dev.constructive.eo
package optics

import org.specs2.mutable.Specification

import data.Direct

/** Pins `Getter.andThen` overload resolution (the C8 fix). The pre-fix `Getter` carried a
  * class-level any-carrier member + a re-homed read-only twin, which made
  * `getter.andThen(DirectCarriedCitizen)` — e.g. the schemes' `cata`/`ana`/`hylo` — a documented
  * three-way tie (E051). With only the trait members in the overload set, dotty resolves by
  * specificity; this spec pins the three routes and their result types.
  */
class GetterAndThenResolutionSpec extends Specification:

  // --- fixtures --------------------------------------------------------------

  case class Doc(id: Int, tag: String, tree: Bin)

  enum Bin:
    case Leaf(n: Int)
    case Branch(l: Bin, r: Bin)

  val binTree = Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))

  val leafSum: Getter[Bin, Int] = Getter[Bin, Int](leafSumFold)

  private def leafSumFold(s: Bin): Int = s match
    case Bin.Leaf(n)      => n
    case Bin.Branch(l, r) => leafSumFold(l) + leafSumFold(r)

  val treePick = PickFold[Bin, String] {
    case Bin.Leaf(_)      => Some("leaf")
    case Bin.Branch(_, _) => None
  } // read-only, OTHER carrier (Affine) — T = Unit but G = Affine

  val getter = Getter[Doc, Bin](_.tree)

  // --- the three routes ------------------------------------------------------

  "getter.andThen(getter) resolves to the fused member → a plain Getter" >> {
    val g = getter.andThen(leafSum)
    (g: Getter[Doc, Int]).get(Doc(7, "t", binTree)) === 6 // 1 + 2 + 3
  }

  "getter.andThen(writable lens inner) resolves via the trait's read-only member → rc.Out" >> {
    val g =
      Getter[Doc, Bin](_.tree).andThen(treePick) // read-only inner, other carrier → trait overload
    (g.pick(Doc(7, "t", binTree)) === None)
      .and(g.pick(Doc(7, "t", Bin.Leaf(9))) === Some("leaf"))
  }

  "getter.andThen(Direct-carried read-only citizen) resolves — was the C8 tie" >> {
    // A Direct-carried Getter-shaped optic that is NOT a concrete `Getter` — the exact shape the
    // schemes' zoo citizens have. Worn as the erased trait type so the static `Getter` fast path
    // is defeated and the trait member route is exercised.
    val citizen: Optic[Bin, Unit, Int, Unit, Direct] =
      new Optic[Bin, Unit, Int, Unit, Direct]:
        type X = Nothing
        def to(s: Bin): Direct[X, Int] = Direct(leafSumFold(s))
        def from(d: Direct[X, Unit]): Unit = ()

    val g = Getter[Doc, Bin](_.tree).andThen(citizen)
    (g: Getter[Doc, Int]).get(Doc(7, "t", binTree)) === 6 // 1 + 2 + 3
  }
