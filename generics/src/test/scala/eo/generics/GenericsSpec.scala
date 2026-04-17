package eo
package generics

import eo.optics.Optic.*
import eo.data.Forgetful.given

import eo.generics.samples.{Employee, Person, Shape, Tree}
import eo.generics.samples.given

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behavioural specs for macro-derived Lens and Prism. The derived
  * optics must be observationally identical to hand-written ones, so
  * we exercise the three Lens laws (get-set / set-get / set-set) and
  * the three Prism laws (partial-round-trip / round-trip /
  * reverseGet-then-getOption) directly rather than leaning on the
  * cross-module law harness. Keeping the checks in-module means
  * `eo-generics` stays independent of `cats-eo-laws` at runtime.
  *
  * Test ADTs live in [[eo.generics.samples]] so the macro splice can
  * emit `new T(...)` without tripping over inner-class outer accessors.
  */
class GenericsSpec extends Specification with ScalaCheck:

  // ---------- Product-type Lens derivation ----------

  val ageL  = lens[Person](_.age)
  val nameL = lens[Person](_.name)

  "derived Lens has the correct getter" >> forAll { (p: Person) =>
    ageL.get(p) == p.age && nameL.get(p) == p.name
  }

  "derived Lens obeys the set-get law" >> forAll { (p: Person, a: Int, s: String) =>
    ageL.get(ageL.replace(a)(p)) == a &&
      nameL.get(nameL.replace(s)(p)) == s
  }

  "derived Lens obeys the get-set law" >> forAll { (p: Person) =>
    ageL.replace(ageL.get(p))(p) == p &&
      nameL.replace(nameL.get(p))(p) == p
  }

  "derived Lens obeys the set-set law" >> forAll { (p: Person, a1: Int, a2: Int) =>
    ageL.replace(a2)(ageL.replace(a1)(p)) == ageL.replace(a2)(p)
  }

  "derived Lens.modify matches replace ∘ get" >> forAll { (p: Person) =>
    ageL.modify(_ + 1)(p) == ageL.replace(ageL.get(p) + 1)(p)
  }

  // ---------- place / transfer on a derived Lens ----------
  //
  // The macro emits a `SimpleLens[Person, Int, String]` which
  // carries `place` / `transfer` directly on its class body — no
  // external evidence needed. Callers that reach those through the
  // generic `Optic` extensions instead of the class methods pick up
  // the `S => (o.X, A)` given that `SimpleLens`'s companion publishes
  // (derived from `to`).

  "derived Lens.place overwrites the focus, leaving the complement alone" >> forAll { (p: Person, a: Int) =>
    import ageL.given
    import nameL.given
    ageL.place(a)(p) == Person(p.name, a) &&
      nameL.place("Bob")(p) == Person("Bob", p.age)
  }

  "derived Lens.transfer lifts a C => A into a focus-replacer" >> forAll { (p: Person, d: Double) =>
    import ageL.given
    // For a home-grown `C => A` (`Double => Int`), transfer should
    // equal place(f(c)) -- i.e. converting the value and replacing.
    val toInt: Double => Int = _.toInt
    ageL.transfer(toInt)(p)(d) == Person(p.name, d.toInt)
  }

  "derived Lens.transfer is curried form of place ∘ f" >> forAll { (p: Person, c: Int) =>
    import ageL.given
    val f: Int => Int = _ + 1
    ageL.transfer(f)(p)(c) == ageL.place(f(c))(p)
  }

  // ---------- N-field (>=3) Lens derivation ----------
  //
  // `Employee(id: Long, name: String, salary: Double, department:
  // String)` has four fields. The macro synthesises the complement
  // `XA` as a `scala.Tuple` of the three non-focused field types and
  // threads field values through `Tuple.apply(i).asInstanceOf[T_i]`
  // at the combine site. Every lens below exercises a different
  // position (head / middle / tail) so an off-by-one in the index
  // bookkeeping surfaces.

  val empIdL         = lens[Employee](_.id)
  val empNameL       = lens[Employee](_.name)
  val empSalaryL     = lens[Employee](_.salary)
  val empDepartmentL = lens[Employee](_.department)

  "N-field Lens get reads the right field" >> forAll { (e: Employee) =>
    empIdL.get(e)         == e.id         &&
    empNameL.get(e)       == e.name       &&
    empSalaryL.get(e)     == e.salary     &&
    empDepartmentL.get(e) == e.department
  }

  "N-field Lens set-get law" >> forAll {
      (e: Employee, i: Long, n: String, s: Double, d: String) =>
    empIdL.get(empIdL.replace(i)(e))                 == i &&
      empNameL.get(empNameL.replace(n)(e))           == n &&
      empSalaryL.get(empSalaryL.replace(s)(e))       == s &&
      empDepartmentL.get(empDepartmentL.replace(d)(e)) == d
  }

  "N-field Lens get-set law" >> forAll { (e: Employee) =>
    empIdL.replace(empIdL.get(e))(e)                 == e &&
      empNameL.replace(empNameL.get(e))(e)           == e &&
      empSalaryL.replace(empSalaryL.get(e))(e)       == e &&
      empDepartmentL.replace(empDepartmentL.get(e))(e) == e
  }

  "N-field Lens preserves non-focused fields under replace" >> forAll {
      (e: Employee, n: String) =>
    val after = empNameL.replace(n)(e)
    after.id == e.id &&
      after.name == n &&
      after.salary == e.salary &&
      after.department == e.department
  }

  "N-field Lens.modify runs the function on the focus" >> forAll {
      (e: Employee) =>
    empSalaryL.modify(_ * 1.1)(e) == e.copy(salary = e.salary * 1.1)
  }

  // ---------- Sum-type Prism derivation ----------

  val circleP: eo.optics.Optic[Shape, Shape, Shape.Circle, Shape.Circle, Either] =
    prism[Shape, Shape.Circle]

  "derived Prism round-trips on matching variants" >> forAll { (r: Double) =>
    val c = Shape.Circle(r)
    circleP.to(c) == Right(c)
  }

  "derived Prism returns Left on non-matching variants" >> {
    val sq: Shape = Shape.Square(2.0)
    circleP.to(sq) == Left(sq)
    val tr: Shape = Shape.Triangle(1.0, 2.0)
    circleP.to(tr) == Left(tr)
  }

  "derived Prism.reverseGet widens variant to parent" >> forAll { (r: Double) =>
    val c: Shape.Circle = Shape.Circle(r)
    circleP.reverseGet(c) == (c: Shape)
  }

  "derived Prism obeys reverseGet-then-getOption == Some" >> forAll { (r: Double) =>
    val c: Shape.Circle = Shape.Circle(r)
    circleP.to(circleP.reverseGet(c)) == Right(c)
  }

  "derived Prism obeys partial-round-trip on any Shape" >> forAll { (s: Shape) =>
    circleP.to(s) match
      case Right(c) => circleP.reverseGet(c) == s
      case Left(s2) => s2 == s
  }

  // ---------- Scala 3 union-type Prism derivation ----------
  //
  // Union types (`A | B`) have no nominal subtype declaration, but
  // `A <: (A | B)` and `B <: (A | B)` both hold, so `prism[A | B, A]`
  // and `prism[A | B, B]` are well-typed. Hearth's `Enum.parse[A | B]`
  // recognises the union as a sum with `directChildren = {A, B}`,
  // and `Enum.matchOn` generates a pattern match that picks the
  // requested alternative.

  val intInUnionP: eo.optics.Optic[Int | String, Int | String, Int, Int, Either] =
    prism[Int | String, Int]

  val stringInUnionP: eo.optics.Optic[Int | String, Int | String, String, String, Either] =
    prism[Int | String, String]

  "derived Prism on a union type focuses the Int alternative" >> forAll { (i: Int) =>
    val u: Int | String = i
    intInUnionP.to(u) == Right(i) &&
      stringInUnionP.to(u) == Left(u)
  }

  "derived Prism on a union type focuses the String alternative" >> forAll { (s: String) =>
    val u: Int | String = s
    stringInUnionP.to(u) == Right(s) &&
      intInUnionP.to(u) == Left(u)
  }

  "derived Prism on a union type round-trips on each alternative" >> forAll { (i: Int, s: String) =>
    intInUnionP.reverseGet(i) == (i: Int | String) &&
      stringInUnionP.reverseGet(s) == (s: Int | String)
  }

  "derived Prism on a union type obeys the partial-round-trip law" >> {
    forAll { (i: Int) =>
      val u: Int | String = i
      intInUnionP.to(u) match
        case Right(j) => intInUnionP.reverseGet(j) == u
        case Left(_)  => false
    } &&
    forAll { (s: String) =>
      val u: Int | String = s
      intInUnionP.to(u) match
        case Right(_) => false
        case Left(u2) => u2 == u
    }
  }

  // ---------- Recursive-type Lens / Prism derivation ----------
  //
  // The macros are first-order (no recursion into field types), but
  // the *types* they're applied to may be recursive. These tests pin
  // that down: a Lens onto a `Tree[N]`-typed child of `Branch`, and a
  // Prism between `Tree[Int]` and one of its variants.

  val leafValueL   = lens[Tree.Leaf[Int]](_.value)
  val branchLeftL  = lens[Tree.Branch[Int]](_.left)
  val branchRightL = lens[Tree.Branch[Int]](_.right)

  val leafP   = prism[Tree[Int], Tree.Leaf[Int]]
  val branchP = prism[Tree[Int], Tree.Branch[Int]]

  "derived Lens on a recursive type's leaf reads the carried value" >> forAll { (n: Int) =>
    leafValueL.get(Tree.Leaf(n)) == n
  }

  "derived Lens on a recursive type's branch reads the named subtree" >> forAll { (n: Int, m: Int) =>
    val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
    branchLeftL.get(b) == Tree.Leaf(n) &&
      branchRightL.get(b) == Tree.Leaf(m)
  }

  "derived Lens on a recursive type can replace a deeply nested subtree" >> forAll { (n: Int, m: Int, k: Int) =>
    val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
    val replacement: Tree[Int] = Tree.Branch(Tree.Leaf(k), Tree.Leaf(k))
    branchLeftL.replace(replacement)(b) == Tree.Branch(replacement, Tree.Leaf(m))
  }

  "derived Lens on a recursive type obeys the set-set law" >> forAll { (n: Int, m: Int, k: Int) =>
    val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
    val r1: Tree[Int] = Tree.Leaf(k)
    val r2: Tree[Int] = Tree.Leaf(k + 1)
    branchLeftL.replace(r2)(branchLeftL.replace(r1)(b)) == branchLeftL.replace(r2)(b)
  }

  "derived Prism on a recursive ADT picks the matching variant" >> forAll { (n: Int) =>
    val l: Tree[Int] = Tree.Leaf(n)
    val b: Tree[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(n))
    leafP.to(l) == Right(Tree.Leaf(n)) &&
      leafP.to(b) == Left(b) &&
      branchP.to(b).isRight &&
      branchP.to(l) == Left(l)
  }

  "derived Prism on a recursive ADT round-trips on its own variant" >> forAll { (n: Int) =>
    val leaf: Tree.Leaf[Int] = Tree.Leaf(n)
    leafP.to(leafP.reverseGet(leaf)) == Right(leaf)
  }
