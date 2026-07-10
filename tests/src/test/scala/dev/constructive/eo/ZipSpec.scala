package dev.constructive.eo

import org.specs2.mutable.Specification

import optics.{Getter, Lens, Optic, Optional}
import optics.Optic.*
import data.Affine

/** Behavioural coverage for the fanout family: [[optics.Optic.zip]] (writeable), plus the
  * capability-surface [[CanGet.zip]] / [[CanGetOption.zip]] / [[CanModifyP.zip]]. Includes the
  * "unsound" guard — proof that zipping overlapping foci violates a lens law, which is exactly what
  * `ZipLaws` catches.
  */
class ZipSpec extends Specification:

  case class Person(name: String, age: Int, city: String)

  val nameL: Optic[Person, Person, String, String, Tuple2] =
    Lens[Person, String](_.name, (p, n) => p.copy(name = n))

  val ageL: Optic[Person, Person, Int, Int, Tuple2] =
    Lens[Person, Int](_.age, (p, a) => p.copy(age = a))

  val ada = Person("Ada", 36, "London")

  val nameAge: Optic[Person, Person, (String, Int), (String, Int), Tuple2] = nameL.zip(ageL)

  "Optic.zip reads both foci as a pair" >> {
    nameAge.get(ada) === (("Ada", 36))
  }

  "Optic.zip writes both foci (disjoint → lawful)" >> {
    nameAge.replace(("Grace", 45))(ada) === Person("Grace", 45, "London")
  }

  "Optic.zip modify runs the pair function" >> {
    nameAge.modify((n, a) => (n.toUpperCase, a + 1))(ada) === Person("ADA", 37, "London")
  }

  "Optic.zip over Affine legs — pair present iff both Optionals hit" >> {
    val adultAge: Optic[Person, Person, Int, Int, Affine] =
      Optional[Person, Person, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = (p, a) => p.copy(age = a),
      )
    val namedName: Optic[Person, Person, String, String, Affine] =
      Optional[Person, Person, String, String, Affine](
        getOrModify = p => Either.cond(p.name.nonEmpty, p.name, p),
        reverseGet = (p, n) => p.copy(name = n),
      )
    val z = adultAge.zip(namedName)
    val minor = Person("Ada", 10, "London")

    (z.getOption(ada) === Some((36, "Ada")))
      .and(z.getOption(minor) === None)
      .and( // age leg misses
        z.replace((40, "Grace"))(ada) === Person("Grace", 40, "London")
      )
      .and(z.replace((40, "Grace"))(minor) === minor) // miss → source unchanged
  }

  "Optic.zip result derives capability evidence" >> {
    given Optic[Person, Person, (String, Int), (String, Int), Tuple2] = nameAge
    def bump[T](t: T)(using cm: CanModify[T, (String, Int)]): T =
      cm.modify((n, a) => (n, a + 1))(t)
    bump(ada) === Person("Ada", 37, "London")
  }

  "unsound: zipping a lens with itself violates replace-get" >> {
    // both legs write age; the sequential write lands leg-2, so get reads (x, x) != (x, y)
    val bad = ageL.zip(ageL)
    bad.get(bad.replace((1, 2))(ada)) !== ((1, 2))
  }

  "CanGet.zip is a pure read fanout" >> {
    val g1: CanGet[Person, String] = Getter[Person, String](_.name)
    val g2: CanGet[Person, Int] = Getter[Person, Int](_.age)
    g1.zip(g2).get(ada) === (("Ada", 36))
  }

  "CanGetOption.zip is present iff both legs hit" >> {
    val evenGO: CanGetOption[Int, Int] = n => if n % 2 == 0 then Some(n) else None
    val posGO: CanGetOption[Int, Int] = n => if n > 0 then Some(n) else None
    val both: CanGetOption[Int, (Int, Int)] = evenGO.zip(posGO)
    (both.getOption(4), both.getOption(3), both.getOption(-2)) ===
      ((Some((4, 4)), None, None))
  }

  "CanModifyP.zip — read-modify-write fanout over the CanGet & CanModify intersection" >> {
    val nl: CanGet[Person, String] & CanModify[Person, String] =
      Lens[Person, String](_.name, (p, n) => p.copy(name = n))
    val al: CanGet[Person, Int] & CanModify[Person, Int] =
      Lens[Person, Int](_.age, (p, a) => p.copy(age = a))
    val z = nl.zip(al)
    (z.get(ada) === (("Ada", 36)))
      .and(z.modify((n, a) => (n.toUpperCase, a + 1))(ada) === Person("ADA", 37, "London"))
  }
