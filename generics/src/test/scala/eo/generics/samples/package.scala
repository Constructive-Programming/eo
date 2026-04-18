package eo
package generics

import org.scalacheck.{Arbitrary, Gen}

/** Top-level test fixtures used by [[eo.generics.GenericsSpec]].
  *
  * The ADTs here are intentionally NOT defined inside the spec class. Hearth's
  * `CaseClass.construct` emits `new T(...)` rather than `s.copy(...)` (so it can handle Scala 3
  * enum cases that lack a `.copy`), but `new InnerClass(...)` from a macro splice loses its
  * outer-accessor wiring and crashes the back-end with "missing outer accessor in class
  * GenericsSpec". Hoisting these fixtures to a top-level package sidesteps that entirely.
  */
package object samples:

  // Plain product type for Lens tests.
  final case class Person(name: String, age: Int)

  object Person:

    given Arbitrary[Person] =
      Arbitrary(for
        n <- Gen.alphaNumStr
        a <- Gen.choose(-1000, 1000)
      yield Person(n, a))

  // Scala 3 enum (sum) with parameterised cases for Prism tests.
  enum Shape:
    case Circle(radius: Double)
    case Square(side: Double)
    case Triangle(base: Double, height: Double)

  object Shape:

    given Arbitrary[Shape] =
      Arbitrary(
        Gen.oneOf(
          Gen.posNum[Double].map(Circle(_)),
          Gen.posNum[Double].map(Square(_)),
          Gen.zip(Gen.posNum[Double], Gen.posNum[Double]).map((b, h) => Triangle(b, h)),
        )
      )

  // Recursive parameterised ADT used to exercise that the macros
  // cope with `Tree[N]`-shaped types -- both the `Branch` constructor
  // (whose field type is the recursive `Tree[N]`) and the `Tree[Int]`
  // -> `Leaf[Int]` Prism.
  enum Tree[+N]:
    case Leaf(value: N)
    case Branch(left: Tree[N], right: Tree[N])

  // 4-field case class for the N-field (>= 3) Lens derivation. The
  // macro must synthesise an `XA = T1 *: T2 *: T3 *: EmptyTuple`
  // complement and round-trip it through `_.apply(i).asInstanceOf[t]`
  // at combine time.
  final case class Employee(
      id: Long,
      name: String,
      salary: Double,
      department: String,
  )

  object Employee:

    given Arbitrary[Employee] =
      Arbitrary(for
        i <- Gen.choose(1L, 1_000_000L)
        n <- Gen.alphaNumStr
        s <- Gen.choose(0.0, 500_000.0)
        d <- Gen.alphaStr
      yield Employee(i, n, s, d))
