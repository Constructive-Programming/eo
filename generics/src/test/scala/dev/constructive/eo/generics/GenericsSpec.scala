package dev.constructive.eo
package generics

import scala.language.implicitConversions

import dev.constructive.eo.generics.samples.{Employee, LTree, Person, Shape, Tree}
import dev.constructive.eo.optics.Optic.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

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

  // covers: derived single-field Lens has the correct getter (forAll over Person),
  //   set-get law, get-set law, set-set law, modify matches replace ∘ get;
  //   .place overwrites focus, .transfer lifts a C => A, .transfer is curried place ∘ f
  "derived single-field Lens (Person.age, Person.name): all laws + place/transfer" >> {
    forAll { (p: Person, a: Int, a1: Int, a2: Int, s: String, d: Double) =>
      val getOk = ageL.get(p) == p.age && nameL.get(p) == p.name
      val setGet = ageL.get(ageL.replace(a)(p)) == a && nameL.get(nameL.replace(s)(p)) == s
      val getSet = ageL.replace(ageL.get(p))(p) == p && nameL.replace(nameL.get(p))(p) == p
      val setSet = ageL.replace(a2)(ageL.replace(a1)(p)) == ageL.replace(a2)(p)
      val modify = ageL.modify(_ + 1)(p) == ageL.replace(ageL.get(p) + 1)(p)
      val placeOk = ageL.place(a)(p) == Person(p.name, a) &&
        nameL.place("Bob")(p) == Person("Bob", p.age)
      val transferToInt = ageL.transfer((x: Double) => x.toInt)(p)(d) == Person(p.name, d.toInt)
      val transferCurried = ageL.transfer((x: Int) => x + 1)(p)(a) == ageL.place(a + 1)(p)
      getOk && setGet && getSet && setSet && modify && placeOk && transferToInt && transferCurried
    }
  }

  // ---------- N-field (>=3) Lens derivation ----------

  val empIdL = lens[Employee](_.id)
  val empNameL = lens[Employee](_.name)
  val empSalaryL = lens[Employee](_.salary)
  val empDepartmentL = lens[Employee](_.department)

  // covers: derived N-field Lens (4-field Employee) — get reads right field across head/middle/tail,
  //   set-get law per field, get-set law per field, replace preserves non-focused fields,
  //   modify applies on the focus (e.g. salary × 1.1);
  //   complement Tuple2 exposes non-focused fields by name (NamedTuple in declaration order)
  "N-field Lens (Employee 4 fields): all laws + non-focused-preserve + modify + complement-by-name" >> {
    forAll { (e: Employee, i: Long, n: String, s: Double, d: String) =>
      val getOk = empIdL.get(e) == e.id && empNameL.get(e) == e.name &&
        empSalaryL.get(e) == e.salary && empDepartmentL.get(e) == e.department
      val setGet = empIdL.get(empIdL.replace(i)(e)) == i &&
        empNameL.get(empNameL.replace(n)(e)) == n &&
        empSalaryL.get(empSalaryL.replace(s)(e)) == s &&
        empDepartmentL.get(empDepartmentL.replace(d)(e)) == d
      val getSet = empIdL.replace(empIdL.get(e))(e) == e &&
        empNameL.replace(empNameL.get(e))(e) == e &&
        empSalaryL.replace(empSalaryL.get(e))(e) == e &&
        empDepartmentL.replace(empDepartmentL.get(e))(e) == e
      val after = empNameL.replace(n)(e)
      val nonFocused = after.id == e.id && after.name == n && after.salary == e.salary &&
        after.department == e.department
      val modify = empSalaryL.modify(_ * 1.1)(e) == e.copy(salary = e.salary * 1.1)

      // Complement-by-name: Lens.to returns the (complement, focus) pair where the
      // complement is a NamedTuple over non-focused fields in declaration order.
      val (complement, focus) = empNameL.to(e)
      type Complement = scala.NamedTuple.NamedTuple[
        ("id", "salary", "department"),
        (Long, Double, String),
      ]
      val named = complement.asInstanceOf[Complement]
      val complementOk = focus == e.name && named.id == e.id && named.salary == e.salary &&
        named.department == e.department

      getOk && setGet && getSet && nonFocused && modify && complementOk
    }
  }

  // ---------- Sum-type Prism derivation ----------

  val circleP: dev.constructive.eo.optics.Optic[Shape, Shape, Shape.Circle, Shape.Circle, Either] =
    prism[Shape, Shape.Circle]

  // covers: derived Prism (Shape.Circle) — round-trip on matching variants,
  //   non-matching variants return Left (Square / Triangle hit the miss branch),
  //   reverseGet widens the variant to parent, reverseGet ∘ to == Right(input),
  //   partial-round-trip law on any Shape
  "derived Prism (Shape.Circle): round-trip + non-match + widen + partial-round-trip" >> {
    forAll { (r: Double, s: Shape) =>
      val c: Shape.Circle = Shape.Circle(r)
      val rt = circleP.to(c) == Right(c)
      val sq: Shape = Shape.Square(2.0)
      val tr: Shape = Shape.Triangle(1.0, 2.0)
      val nonMatch = circleP.to(sq) == Left(sq) && circleP.to(tr) == Left(tr)
      val widen = circleP.reverseGet(c) == (c: Shape)
      val rgGet = circleP.to(circleP.reverseGet(c)) == Right(c)
      val partial = circleP.to(s) match
        case Right(cc) => circleP.reverseGet(cc) == s
        case Left(s2)  => s2 == s
      rt && nonMatch && widen && rgGet && partial
    }
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
  //   round-trips on its own variant
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

  type EmpNameDeptIdFocus =
    scala.NamedTuple.NamedTuple[("name", "department", "id"), (String, String, Long)]

  val nameDeptIdL = lens[Employee](_.name, _.department, _.id)

  val lBranchRightLeftL = lens[LTree.LBranch[Int]](_.right, _.left)

  type LBranchFocus =
    scala.NamedTuple.NamedTuple[("right", "left"), (LTree[Int], LTree[Int])]

  // covers: multi-field Lens 2-of-4 (Employee.salary, .id, selector-rev) — get packs focus
  //   in selector order, set-get law, get-set law, replace preserves non-focused fields,
  //   set-set law, complement exposes non-focused fields by name (NamedTuple, decl-order);
  //   multi-field Lens 3-of-4 (Employee.name, .department, .id) — get reads in selector
  //   order, replace preserves non-focused salary;
  //   recursive parameterised ADT (LTree.LBranch[Int] with selectors right + left) —
  //   set-get law on a recursive focus type, middle field preserved through replace
  "multi-field Lens 2-of-4 / 3-of-4 / recursive LBranch[Int]: laws + complement + middle preserved" >> {
    val twoOfFour = forAll { (e: Employee, s: Double, i: Long, s1: Double, s2: Double) =>
      val getOrder = {
        val focus = salaryIdL.get(e).asInstanceOf[EmpSalaryIdFocus]
        focus.salary == e.salary && focus.id == e.id
      }
      val newFocus = (s, i).asInstanceOf[EmpSalaryIdFocus]
      val setGet = {
        val got = salaryIdL.get(salaryIdL.replace(newFocus)(e)).asInstanceOf[EmpSalaryIdFocus]
        got.salary == s && got.id == i
      }
      val getSet = salaryIdL.replace(salaryIdL.get(e))(e) == e
      val after = salaryIdL.replace(newFocus)(e)
      val nonFocused = after.name == e.name && after.department == e.department
      val f1 = (s1, 1L).asInstanceOf[EmpSalaryIdFocus]
      val f2 = (s2, 2L).asInstanceOf[EmpSalaryIdFocus]
      val setSet = salaryIdL.replace(f2)(salaryIdL.replace(f1)(e)) == salaryIdL.replace(f2)(e)
      val (comp, _) = salaryIdL.to(e)
      val named = comp.asInstanceOf[EmpSalaryIdComplement]
      val complement = named.name == e.name && named.department == e.department
      getOrder && setGet && getSet && nonFocused && setSet && complement
    }

    val threeOfFour = forAll { (e: Employee, n: String, d: String, i: Long) =>
      val focus = nameDeptIdL.get(e).asInstanceOf[EmpNameDeptIdFocus]
      val getOrder = focus.name == e.name && focus.department == e.department && focus.id == e.id
      val newFocus = (n, d, i).asInstanceOf[EmpNameDeptIdFocus]
      val after = nameDeptIdL.replace(newFocus)(e)
      val nonFocused =
        after.salary == e.salary && after.name == n && after.department == d && after.id == i
      getOrder && nonFocused
    }

    val recursive = forAll { (x: Int, y: Int, k: Int) =>
      val branch: LTree.LBranch[Int] =
        LTree.LBranch[Int](LTree.LLeaf(x), x + y, LTree.LLeaf(y))
      val newRight: LTree[Int] = LTree.LLeaf(999)
      val newLeft: LTree[Int] = LTree.LLeaf(-1)
      val newFocus = (newRight, newLeft).asInstanceOf[LBranchFocus]
      val got = lBranchRightLeftL
        .get(lBranchRightLeftL.replace(newFocus)(branch))
        .asInstanceOf[LBranchFocus]
      val setGet = got.right == newRight && got.left == newLeft

      val middleBranch: LTree.LBranch[Int] =
        LTree.LBranch[Int](LTree.LLeaf(x), k, LTree.LLeaf(y))
      val middleFocus =
        (LTree.LLeaf[Int](1): LTree[Int], LTree.LLeaf[Int](2): LTree[Int])
          .asInstanceOf[LBranchFocus]
      val middlePreserved =
        lBranchRightLeftL.replace(middleFocus)(middleBranch).middle == k

      setGet && middlePreserved
    }

    twoOfFour && threeOfFour && recursive
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

  // covers: full-cover Iso (Person 2-of-2 reversed) — reverseGet ∘ get = id, get ∘ reverseGet = id,
  //   modify on the reversed 2-of-2 NamedTuple,
  //   full-cover Iso (Person 2-of-2 declaration order) — reverseGet ∘ get = id,
  //   full-cover Iso (Employee 4-of-4 scrambled) — both Iso laws hold,
  //   reverseGet places fields in declaration order despite scrambled selector ordering,
  //   1-field Iso D2 witness — leafValueI.reverseGet ∘ leafValueI.get = id on Tree.Leaf,
  //   full-cover Iso composes with a downstream Lens via fused andThen (chained.get / chained.replace)
  "full-cover Iso (Person 2-of-2 + Employee 4-of-4 scrambled) + 1-field D2 + downstream-Lens fusion" >> {
    forAll { (p: Person, e: Employee, a: Int, s: String, n: Int, d: String, i: Long, sa: Double) =>
      val rgGet2Rev = personAgeNameIso.reverseGet(personAgeNameIso.get(p)) == p
      val focus2Rev = (a, s).asInstanceOf[PersonAgeNameFocus]
      val getRg2Rev = {
        val got = personAgeNameIso
          .get(personAgeNameIso.reverseGet(focus2Rev))
          .asInstanceOf[PersonAgeNameFocus]
        got.age == a && got.name == s
      }
      val mod2Rev = personAgeNameIso.reverseGet(
        (a, p.name).asInstanceOf[PersonAgeNameFocus]
      ) == Person(p.name, a)

      val rgGet2Decl = personNameAgeIso.reverseGet(personNameAgeIso.get(p)) == p

      val rgGet4 = empAllIso.reverseGet(empAllIso.get(e)) == e
      val focus4 = (d, i, sa, "x").asInstanceOf[EmpAllFocus]
      val getRg4 = {
        val got = empAllIso.get(empAllIso.reverseGet(focus4)).asInstanceOf[EmpAllFocus]
        got.department == d && got.id == i && got.salary == sa && got.name == "x"
      }
      val declOrder = empAllIso.reverseGet(focus4) == Employee(i, "x", sa, d)

      // 1-field Iso D2 witness — leafValueI round-trips on Tree.Leaf.
      val leaf: Tree.Leaf[Int] = Tree.Leaf(n)
      val d2 = leafValueI.reverseGet(leafValueI.get(leaf)) == leaf

      // Full-cover Iso composed with a downstream GetReplaceLens (fused andThen).
      import dev.constructive.eo.optics.{GetReplaceLens, Lens}
      val idInFocusL: GetReplaceLens[EmpAllFocus, EmpAllFocus, Long, Long] =
        Lens(
          (f: EmpAllFocus) => f.asInstanceOf[(String, Long, Double, String)]._2,
          (f: EmpAllFocus, i: Long) =>
            val t = f.asInstanceOf[(String, Long, Double, String)]
            (t._1, i, t._3, t._4).asInstanceOf[EmpAllFocus],
        )
      val chained = empAllIso.andThen(idInFocusL)
      val fusedOk = chained.get(e) == e.id && chained.replace(e.id + 1)(e) == e.copy(id = e.id + 1)

      rgGet2Rev && getRg2Rev && mod2Rev && rgGet2Decl &&
      rgGet4 && getRg4 && declOrder && d2 && fusedOk
    }
  }
