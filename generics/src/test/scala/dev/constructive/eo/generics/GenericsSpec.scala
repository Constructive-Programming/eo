package dev.constructive.eo
package generics

import dev.constructive.eo.generics.samples.{Employee, LTree, Person, Shape, Tree}
import dev.constructive.eo.optics.Optic.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.language.implicitConversions

/** Behavioural specs for macro-derived Lens and Prism.
  *
  * '''2026-04-25 consolidation.''' 50 → 13 named blocks, organised as one composite block per macro
  * shape. Each block exercises every law for that shape in a single Result. Discipline-equivalent
  * law coverage preserved per the consolidation contract.
  */
class GenericsSpec extends Specification with ScalaCheck:

  // ---------- Product-type Lens derivation ----------

  val ageL = lens[Person](_.age)
  val nameL = lens[Person](_.name)

  // covers: derived Lens has the correct getter, set-get law, get-set law,
  // set-set law, modify matches replace ∘ get
  "derived single-field Lens (Person.age, Person.name): get + set-get + get-set + set-set + modify" >> {
    val getOk = forAll { (p: Person) => ageL.get(p) == p.age && nameL.get(p) == p.name }
    val setGet = forAll { (p: Person, a: Int, s: String) =>
      ageL.get(ageL.replace(a)(p)) == a && nameL.get(nameL.replace(s)(p)) == s
    }
    val getSet = forAll { (p: Person) =>
      ageL.replace(ageL.get(p))(p) == p && nameL.replace(nameL.get(p))(p) == p
    }
    val setSet = forAll { (p: Person, a1: Int, a2: Int) =>
      ageL.replace(a2)(ageL.replace(a1)(p)) == ageL.replace(a2)(p)
    }
    val modify = forAll { (p: Person) =>
      ageL.modify(_ + 1)(p) == ageL.replace(ageL.get(p) + 1)(p)
    }
    getOk && setGet && getSet && setSet && modify
  }

  // covers: place overwrites focus, transfer lifts a C => A, transfer is curried
  // form of place ∘ f
  "derived Lens.place / .transfer: overwrite focus + lift C=>A + curried place∘f" >> {
    val placeOk = forAll { (p: Person, a: Int) =>
      ageL.place(a)(p) == Person(p.name, a) && nameL.place("Bob")(p) == Person("Bob", p.age)
    }
    val transferToInt = forAll { (p: Person, d: Double) =>
      val toInt: Double => Int = _.toInt
      ageL.transfer(toInt)(p)(d) == Person(p.name, d.toInt)
    }
    val transferCurried = forAll { (p: Person, c: Int) =>
      val f: Int => Int = _ + 1
      ageL.transfer(f)(p)(c) == ageL.place(f(c))(p)
    }
    placeOk && transferToInt && transferCurried
  }

  // ---------- N-field (>=3) Lens derivation ----------

  val empIdL = lens[Employee](_.id)
  val empNameL = lens[Employee](_.name)
  val empSalaryL = lens[Employee](_.salary)
  val empDepartmentL = lens[Employee](_.department)

  // covers: get reads the right field across head/middle/tail, set-get law,
  // get-set law, replace preserves non-focused fields, modify runs on the focus
  "N-field Lens (Employee 4 fields): get + set-get + get-set + non-focused-preserve + modify" >> {
    val getOk = forAll { (e: Employee) =>
      empIdL.get(e) == e.id && empNameL.get(e) == e.name &&
      empSalaryL.get(e) == e.salary && empDepartmentL.get(e) == e.department
    }
    val setGet = forAll { (e: Employee, i: Long, n: String, s: Double, d: String) =>
      empIdL.get(empIdL.replace(i)(e)) == i &&
      empNameL.get(empNameL.replace(n)(e)) == n &&
      empSalaryL.get(empSalaryL.replace(s)(e)) == s &&
      empDepartmentL.get(empDepartmentL.replace(d)(e)) == d
    }
    val getSet = forAll { (e: Employee) =>
      empIdL.replace(empIdL.get(e))(e) == e &&
      empNameL.replace(empNameL.get(e))(e) == e &&
      empSalaryL.replace(empSalaryL.get(e))(e) == e &&
      empDepartmentL.replace(empDepartmentL.get(e))(e) == e
    }
    val nonFocused = forAll { (e: Employee, n: String) =>
      val after = empNameL.replace(n)(e)
      after.id == e.id && after.name == n && after.salary == e.salary &&
      after.department == e.department
    }
    val modify = forAll { (e: Employee) =>
      empSalaryL.modify(_ * 1.1)(e) == e.copy(salary = e.salary * 1.1)
    }
    getOk && setGet && getSet && nonFocused && modify
  }

  // covers: derived N-field Lens exposes complement fields by name
  "derived N-field Lens exposes complement fields by name" >> forAll { (e: Employee) =>
    val (complement, focus) = empNameL.to(e)
    type Complement = scala.NamedTuple.NamedTuple[
      ("id", "salary", "department"),
      (Long, Double, String),
    ]
    val named = complement.asInstanceOf[Complement]
    focus == e.name && named.id == e.id && named.salary == e.salary &&
    named.department == e.department
  }

  // ---------- Sum-type Prism derivation ----------

  val circleP: dev.constructive.eo.optics.Optic[Shape, Shape, Shape.Circle, Shape.Circle, Either] =
    prism[Shape, Shape.Circle]

  // covers: round-trips on matching variants, returns Left on non-matching variants,
  // reverseGet widens variant to parent, reverseGet-then-getOption == Some,
  // partial-round-trip on any Shape
  "derived Prism (Shape.Circle): round-trip + non-match Left + widen + partial-round-trip" >> {
    val roundTrip = forAll { (r: Double) =>
      val c = Shape.Circle(r)
      circleP.to(c) == Right(c)
    }
    val nonMatch = {
      val sq: Shape = Shape.Square(2.0)
      val tr: Shape = Shape.Triangle(1.0, 2.0)
      circleP.to(sq) == Left(sq) && circleP.to(tr) == Left(tr)
    }
    val widen = forAll { (r: Double) =>
      val c: Shape.Circle = Shape.Circle(r)
      circleP.reverseGet(c) == (c: Shape)
    }
    val rgGet = forAll { (r: Double) =>
      val c: Shape.Circle = Shape.Circle(r)
      circleP.to(circleP.reverseGet(c)) == Right(c)
    }
    val partial = forAll { (s: Shape) =>
      circleP.to(s) match
        case Right(c) => circleP.reverseGet(c) == s
        case Left(s2) => s2 == s
    }
    roundTrip && nonMatch && widen && rgGet && partial
  }

  // ---------- Scala 3 union-type Prism derivation ----------

  val intInUnionP: dev.constructive.eo.optics.Optic[Int | String, Int | String, Int, Int, Either] =
    prism[Int | String, Int]

  val stringInUnionP
      : dev.constructive.eo.optics.Optic[Int | String, Int | String, String, String, Either] =
    prism[Int | String, String]

  // covers: focuses Int alternative, focuses String alternative, round-trips on
  // each alternative, partial-round-trip law on each
  "derived Prism on Int | String union: focus int / focus string / round-trip / partial-round-trip" >> {
    val focusInt = forAll { (i: Int) =>
      val u: Int | String = i
      intInUnionP.to(u) == Right(i) && stringInUnionP.to(u) == Left(u)
    }
    val focusString = forAll { (s: String) =>
      val u: Int | String = s
      stringInUnionP.to(u) == Right(s) && intInUnionP.to(u) == Left(u)
    }
    val roundTrip = forAll { (i: Int, s: String) =>
      intInUnionP.reverseGet(i) == (i: Int | String) &&
      stringInUnionP.reverseGet(s) == (s: Int | String)
    }
    val partialInt = forAll { (i: Int) =>
      val u: Int | String = i
      intInUnionP.to(u) match
        case Right(j) => intInUnionP.reverseGet(j) == u
        case Left(_)  => false
    }
    val partialString = forAll { (s: String) =>
      val u: Int | String = s
      intInUnionP.to(u) match
        case Right(_) => false
        case Left(u2) => u2 == u
    }
    focusInt && focusString && roundTrip && partialInt && partialString
  }

  // ---------- Recursive-type Lens / Prism derivation ----------

  val leafValueI = lens[Tree.Leaf[Int]](_.value)
  type LeafFocus = scala.NamedTuple.NamedTuple[Tuple1["value"], Tuple1[Int]]
  val branchLeftL = lens[Tree.Branch[Int]](_.left)
  val branchRightL = lens[Tree.Branch[Int]](_.right)
  val leafP = prism[Tree[Int], Tree.Leaf[Int]]
  val branchP = prism[Tree[Int], Tree.Branch[Int]]

  // covers: 1-field recursive leaf reads carried value, round-trips through
  // reverseGet, branch reads named subtree, can replace deeply nested subtree,
  // obeys set-set law, 1-field Iso get ∘ reverseGet = id (D2 witness)
  "derived Lens / Iso on recursive Tree[Int]: leaf read + branch read/replace + set-set + D2 witness" >> {
    val leafRead = forAll { (n: Int) =>
      val focus = leafValueI.get(Tree.Leaf(n)).asInstanceOf[LeafFocus]
      focus.value == n
    }
    val leafRoundTrip = forAll { (n: Int) =>
      val leaf: Tree.Leaf[Int] = Tree.Leaf(n)
      leafValueI.reverseGet(leafValueI.get(leaf)) == leaf
    }
    val branchRead = forAll { (n: Int, m: Int) =>
      val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
      branchLeftL.get(b) == Tree.Leaf(n) && branchRightL.get(b) == Tree.Leaf(m)
    }
    val branchReplace = forAll { (n: Int, m: Int, k: Int) =>
      val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
      val replacement: Tree[Int] = Tree.Branch(Tree.Leaf(k), Tree.Leaf(k))
      branchLeftL.replace(replacement)(b) == Tree.Branch(replacement, Tree.Leaf(m))
    }
    val branchSetSet = forAll { (n: Int, m: Int, k: Int) =>
      val b: Tree.Branch[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(m))
      val r1: Tree[Int] = Tree.Leaf(k)
      val r2: Tree[Int] = Tree.Leaf(k + 1)
      branchLeftL.replace(r2)(branchLeftL.replace(r1)(b)) == branchLeftL.replace(r2)(b)
    }
    val d2Inv = forAll { (n: Int) =>
      val focus = Tuple1(n).asInstanceOf[LeafFocus]
      val got = leafValueI.get(leafValueI.reverseGet(focus)).asInstanceOf[LeafFocus]
      got.value == n
    }
    leafRead && leafRoundTrip && branchRead && branchReplace && branchSetSet && d2Inv
  }

  // covers: derived Prism on a recursive ADT picks the matching variant,
  // round-trips on its own variant
  "derived Prism on Tree[Int]: pick variant + round-trip own variant" >> {
    val pick = forAll { (n: Int) =>
      val l: Tree[Int] = Tree.Leaf(n)
      val b: Tree[Int] = Tree.Branch(Tree.Leaf(n), Tree.Leaf(n))
      leafP.to(l) == Right(Tree.Leaf(n)) && leafP.to(b) == Left(b) &&
      branchP.to(b).isRight && branchP.to(l) == Left(l)
    }
    val rt = forAll { (n: Int) =>
      val leaf: Tree.Leaf[Int] = Tree.Leaf(n)
      leafP.to(leafP.reverseGet(leaf)) == Right(leaf)
    }
    pick && rt
  }

  // ---------- Multi-field Lens (partial cover) ----------

  type EmpSalaryIdFocus = scala.NamedTuple.NamedTuple[("salary", "id"), (Double, Long)]

  type EmpSalaryIdComplement =
    scala.NamedTuple.NamedTuple[("name", "department"), (String, String)]

  val salaryIdL = lens[Employee](_.salary, _.id)

  // covers: get packs focus in selector order, set-get law (2-of-4 selector ≠
  // decl-order), get-set law, replace preserves non-focused fields, set-set law,
  // complement exposes non-focused fields by name
  "multi-field Lens 2-of-4 (Employee.salary, .id selector-rev): all laws + complement-by-name" >> {
    val getOrder = forAll { (e: Employee) =>
      val focus = salaryIdL.get(e).asInstanceOf[EmpSalaryIdFocus]
      focus.salary == e.salary && focus.id == e.id
    }
    val setGet = forAll { (e: Employee, s: Double, i: Long) =>
      val newFocus = (s, i).asInstanceOf[EmpSalaryIdFocus]
      val got = salaryIdL.get(salaryIdL.replace(newFocus)(e)).asInstanceOf[EmpSalaryIdFocus]
      got.salary == s && got.id == i
    }
    val getSet = forAll { (e: Employee) => salaryIdL.replace(salaryIdL.get(e))(e) == e }
    val nonFocused = forAll { (e: Employee, i: Long, s: Double) =>
      val newFocus = (s, i).asInstanceOf[EmpSalaryIdFocus]
      val after = salaryIdL.replace(newFocus)(e)
      after.name == e.name && after.department == e.department
    }
    val setSet = forAll { (e: Employee, s1: Double, s2: Double) =>
      val f1 = (s1, 1L).asInstanceOf[EmpSalaryIdFocus]
      val f2 = (s2, 2L).asInstanceOf[EmpSalaryIdFocus]
      salaryIdL.replace(f2)(salaryIdL.replace(f1)(e)) == salaryIdL.replace(f2)(e)
    }
    val complement = forAll { (e: Employee) =>
      val (comp, _) = salaryIdL.to(e)
      val named = comp.asInstanceOf[EmpSalaryIdComplement]
      named.name == e.name && named.department == e.department
    }
    getOrder && setGet && getSet && nonFocused && setSet && complement
  }

  type EmpNameDeptIdFocus =
    scala.NamedTuple.NamedTuple[("name", "department", "id"), (String, String, Long)]

  val nameDeptIdL = lens[Employee](_.name, _.department, _.id)

  // covers: 3-of-4 get reads in selector order, replace preserves non-focused salary
  "multi-field Lens 3-of-4 (Employee.name, .department, .id): get-order + non-focused-salary preserved" >> {
    val getOrder = forAll { (e: Employee) =>
      val focus = nameDeptIdL.get(e).asInstanceOf[EmpNameDeptIdFocus]
      focus.name == e.name && focus.department == e.department && focus.id == e.id
    }
    val nonFocused = forAll { (e: Employee, n: String, d: String, i: Long) =>
      val newFocus = (n, d, i).asInstanceOf[EmpNameDeptIdFocus]
      val after = nameDeptIdL.replace(newFocus)(e)
      after.salary == e.salary && after.name == n && after.department == d && after.id == i
    }
    getOrder && nonFocused
  }

  // ---------- Recursive parameterised ADT, multi-field partial cover ----

  val lBranchRightLeftL = lens[LTree.LBranch[Int]](_.right, _.left)

  type LBranchFocus =
    scala.NamedTuple.NamedTuple[("right", "left"), (LTree[Int], LTree[Int])]

  // covers: recursive parameterised ADT set-get law, preserves middle
  "multi-field Lens on recursive LTree.LBranch[Int] (right, left): set-get + middle preserved" >> {
    val setGet = forAll { (x: Int, y: Int) =>
      val branch: LTree.LBranch[Int] =
        LTree.LBranch[Int](LTree.LLeaf(x), x + y, LTree.LLeaf(y))
      val newRight: LTree[Int] = LTree.LLeaf(999)
      val newLeft: LTree[Int] = LTree.LLeaf(-1)
      val newFocus = (newRight, newLeft).asInstanceOf[LBranchFocus]
      val got = lBranchRightLeftL
        .get(lBranchRightLeftL.replace(newFocus)(branch))
        .asInstanceOf[LBranchFocus]
      got.right == newRight && got.left == newLeft
    }
    val middle = forAll { (x: Int, y: Int, k: Int) =>
      val branch: LTree.LBranch[Int] =
        LTree.LBranch[Int](LTree.LLeaf(x), k, LTree.LLeaf(y))
      val newFocus =
        (LTree.LLeaf[Int](1): LTree[Int], LTree.LLeaf[Int](2): LTree[Int])
          .asInstanceOf[LBranchFocus]
      val after = lBranchRightLeftL.replace(newFocus)(branch)
      after.middle == k
    }
    setGet && middle
  }

  // ---------- Full-cover Iso derivation ----------

  type PersonAgeNameFocus = scala.NamedTuple.NamedTuple[("age", "name"), (Int, String)]
  val personAgeNameIso = lens[Person](_.age, _.name)
  type PersonNameAgeFocus = scala.NamedTuple.NamedTuple[("name", "age"), (String, Int)]
  val personNameAgeIso = lens[Person](_.name, _.age)

  type EmpAllFocus = scala.NamedTuple.NamedTuple[
    ("department", "id", "salary", "name"),
    (String, Long, Double, String),
  ]

  val empAllIso = lens[Employee](_.department, _.id, _.salary, _.name)

  // covers: full-cover Iso reverseGet ∘ get = id on 2-of-2 reversed, get ∘
  // reverseGet = id on 2-of-2 reversed, modify on 2-of-2 reversed, reverseGet
  // ∘ get = id on 2-of-2 declaration order, reverseGet ∘ get = id on 4-of-4
  // scrambled, get ∘ reverseGet = id on 4-of-4 scrambled, reverseGet places
  // fields in declaration order
  "full-cover Iso (Person 2-of-2 + Employee 4-of-4 scrambled): all Iso laws + decl-order placement" >> {
    val rgGet2Rev = forAll { (p: Person) =>
      personAgeNameIso.reverseGet(personAgeNameIso.get(p)) == p
    }
    val getRg2Rev = forAll { (a: Int, s: String) =>
      val focus = (a, s).asInstanceOf[PersonAgeNameFocus]
      val got = personAgeNameIso
        .get(personAgeNameIso.reverseGet(focus))
        .asInstanceOf[PersonAgeNameFocus]
      got.age == a && got.name == s
    }
    val mod2Rev = forAll { (p: Person, a: Int) =>
      val focus = (a, p.name).asInstanceOf[PersonAgeNameFocus]
      personAgeNameIso.reverseGet(focus) == Person(p.name, a)
    }
    val rgGet2Decl = forAll { (p: Person) =>
      personNameAgeIso.reverseGet(personNameAgeIso.get(p)) == p
    }
    val rgGet4 = forAll { (e: Employee) => empAllIso.reverseGet(empAllIso.get(e)) == e }
    val getRg4 = forAll { (d: String, i: Long, s: Double, n: String) =>
      val focus = (d, i, s, n).asInstanceOf[EmpAllFocus]
      val got = empAllIso.get(empAllIso.reverseGet(focus)).asInstanceOf[EmpAllFocus]
      got.department == d && got.id == i && got.salary == s && got.name == n
    }
    val declOrder = forAll { (d: String, i: Long, s: Double, n: String) =>
      val focus = (d, i, s, n).asInstanceOf[EmpAllFocus]
      empAllIso.reverseGet(focus) == Employee(i, n, s, d)
    }
    rgGet2Rev && getRg2Rev && mod2Rev && rgGet2Decl && rgGet4 && getRg4 && declOrder
  }

  // covers: 1-field Iso obeys reverseGet ∘ get = id (D2 witness)
  "1-field Iso D2 witness — leaf round-trip" >> forAll { (n: Int) =>
    val leaf: Tree.Leaf[Int] = Tree.Leaf(n)
    leafValueI.reverseGet(leafValueI.get(leaf)) == leaf
  }

  // covers: full-cover Iso composes with a downstream Lens (fused andThen)
  "full-cover Iso composes with a downstream Lens (fused andThen)" >> forAll { (e: Employee) =>
    import dev.constructive.eo.optics.{GetReplaceLens, Lens}
    val idInFocusL: GetReplaceLens[EmpAllFocus, EmpAllFocus, Long, Long] =
      Lens(
        (f: EmpAllFocus) => f.asInstanceOf[(String, Long, Double, String)]._2,
        (f: EmpAllFocus, i: Long) =>
          val t = f.asInstanceOf[(String, Long, Double, String)]
          (t._1, i, t._3, t._4).asInstanceOf[EmpAllFocus],
      )
    val chained = empAllIso.andThen(idInFocusL)
    chained.get(e) == e.id && chained.replace(e.id + 1)(e) == e.copy(id = e.id + 1)
  }
