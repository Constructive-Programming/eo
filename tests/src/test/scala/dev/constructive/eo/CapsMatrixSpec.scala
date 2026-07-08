package dev.constructive.eo

// =====================================================================
//  The capability matrix: optic family × capability trait.
//
//  Each AVAILABLE cell is asserted through BOTH routes:
//    - mixin: the concrete optic class IS the capability (subtype), and
//      implicit search resolves the concrete given to the instance
//      itself (`beTheSameAs`) — the hot path, no wrapper;
//    - derivation: the same optic bound at the generic `Optic[…, F]`
//      type derives the capability through the companion given.
//  VOID cells (carrier lacks the gate typeclass) are pinned with
//  `typeChecks` so a new instance/given import can't silently open them.
//
//  The derivation cells double as the SIP-64 clause-order regression:
//  several `Accessor`/`Forgetful*` instances are always in scope
//  (Tuple2 AND Direct, …), so a derived given resolves only because its
//  optic parameter pins the carrier BEFORE the gate is searched. If
//  someone reorders a companion given's using clause — or rewrites it
//  as a context bound on F — these cells go red.
//
//  Per the drilled-optic house rule (SeamLaws), the Lens column is also
//  exercised on a composed (drilled) optic, not just flat constructors.
// =====================================================================

import scala.compiletime.testing.typeChecks

import org.specs2.mutable.Specification

import optics.*
import data.{Affine, Direct, Forget, ModifyF, MultiFocus, PSVec}

object CapsFixtures:
  case class Address(street: String, zip: Int)
  case class Person(name: String, address: Address)

  val addr = Address("Main St", 12345)
  val person = Person("Ada", addr)

  val streetLens: GetReplaceLens[Address, Address, String, String] =
    Lens[Address, String](_.street, (a, s) => a.copy(street = s))

  val addressLens: GetReplaceLens[Person, Person, Address, Address] =
    Lens[Person, Address](_.address, (p, a) => p.copy(address = a))

  val drilled: GetReplaceLens[Person, Person, String, String] =
    addressLens.andThen(streetLens)

  val firstLens: SimpleLens[(String, Int), String, Int] = Lens.first[String, Int]

  val pairIso: BijectionIso[Address, Address, (String, Int), (String, Int)] =
    Iso[Address, Address, (String, Int), (String, Int)](
      a => (a.street, a.zip),
      t => Address(t._1, t._2),
    )

  val somePrism: MendTearPrism[Option[Int], Option[Int], Int, Int] =
    Prism[Option[Int], Int](o => o.toRight(o), Some(_))

  val pickPrism: PickMendPrism[Option[Int], Int, Int] =
    Prism.optional[Option[Int], Int](identity, Some(_))

  val zipOptional: Optional[Address, Address, Int, Int] =
    Optional[Address, Address, Int, Int, Affine](
      a => if a.zip > 0 then Right(a.zip) else Left(a),
      sb => sb._1.copy(zip = sb._2),
    )

  val zipAffineFold: PickFold[Address, Int] =
    AffineFold[Address, Int](a => Option.when(a.zip > 0)(a.zip))

  val listFold: ForgetFold[List[Int], List, Int] = Fold[List, Int]
  val eachTraversal = Traversal.each[List, Int]
  val streetGetter: Getter[Address, String] = Getter[Address, String](_.street)
  val addressReview: Review[Address, String] = Review[Address, String](s => Address(s, 0))

  val streetModify: Modify[Address, Address, String, String] =
    Modify[Address, Address, String, String](f => a => a.copy(street = f(a.street)))

class CapsMatrixSpec extends Specification:
  import CapsFixtures.*

  // ------------------------------------------------------------- CanGet
  "CanGet" >> {
    "mixin: Lens / SimpleLens / Iso / Getter serve the capability directly" >> {
      (streetLens: CanGet[Address, String]).get(addr) === "Main St"
      (firstLens: CanGet[(String, Int), String]).get(("a", 1)) === "a"
      (pairIso: CanGet[Address, (String, Int)]).get(addr) === (("Main St", 12345))
      (streetGetter: CanGet[Address, String]).get(addr) === "Main St"
      (drilled: CanGet[Person, String]).get(person) === "Main St"
    }
    "resolution: a concrete given resolves to the instance itself (no wrapper)" >> {
      given GetReplaceLens[Address, Address, String, String] = streetLens
      summon[CanGet[Address, String]] must beTheSameAs(streetLens)
    }
    "derivation: a generic Tuple2 / Direct optic given derives the capability" >> {
      {
        given Optic[Person, Person, String, String, Tuple2] = drilled
        summon[CanGet[Person, String]].get(person) === drilled.get(person)
      }
      {
        given Optic[Address, Unit, String, Unit, Direct] = streetGetter
        summon[CanGet[Address, String]].get(addr) === "Main St"
      }
    }
    "void: an Either-carrier optic cannot serve CanGet" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           given Optic[Option[Int], Option[Int], Int, Int, Either] = CapsFixtures.somePrism
           summon[CanGet[Option[Int], Int]]"""
      ) must beFalse
    }
  }

  // ------------------------------------------------------- CanGetOption
  "CanGetOption" >> {
    "mixin: Prism (both shapes) / Optional / AffineFold serve directly" >> {
      (somePrism: CanGetOption[Option[Int], Int]).getOption(Some(3)) === Some(3)
      (pickPrism: CanGetOption[Option[Int], Int]).getOption(None) === None
      (zipOptional: CanGetOption[Address, Int]).getOption(addr) === Some(12345)
      (zipAffineFold: CanGetOption[Address, Int]).getOption(addr) === Some(12345)
    }
    "resolution: concrete given is the instance itself" >> {
      given MendTearPrism[Option[Int], Option[Int], Int, Int] = somePrism
      summon[CanGetOption[Option[Int], Int]] must beTheSameAs(somePrism)
    }
    "derivation: generic Either / Affine optic givens derive" >> {
      {
        given Optic[Option[Int], Option[Int], Int, Int, Either] = somePrism
        summon[CanGetOption[Option[Int], Int]].getOption(Some(3)) === Some(3)
      }
      {
        given Optic[Address, Address, Int, Int, Affine] = zipOptional
        summon[CanGetOption[Address, Int]].getOption(addr) === Some(12345)
      }
    }
    "void: a Tuple2-carrier optic cannot serve CanGetOption" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           given Optic[CapsFixtures.Address, CapsFixtures.Address, String, String, Tuple2] =
             CapsFixtures.streetLens
           summon[CanGetOption[CapsFixtures.Address, String]]"""
      ) must beFalse
    }
  }

  // ------------------------------------------------------ CanReverseGet
  "CanReverseGet" >> {
    "mixin: Iso / Prism (both shapes) / Review serve directly" >> {
      (pairIso: CanReverseGet[Address, (String, Int)]).reverseGet(("Elm", 1)) === Address("Elm", 1)
      (somePrism: CanReverseGet[Option[Int], Int]).reverseGet(3) === Some(3)
      (pickPrism: CanReverseGet[Option[Int], Int]).reverseGet(3) === Some(3)
      (addressReview: CanReverseGet[Address, String]).reverseGet("Elm") === Address("Elm", 0)
    }
    "resolution: concrete given is the instance itself" >> {
      given Review[Address, String] = addressReview
      summon[CanReverseGet[Address, String]] must beTheSameAs(addressReview)
    }
    "derivation: generic Either / Direct optic givens derive" >> {
      {
        given Optic[Option[Int], Option[Int], Int, Int, Either] = somePrism
        summon[CanReverseGet[Option[Int], Int]].reverseGet(3) === Some(3)
      }
      {
        given Optic[Unit, Address, Unit, String, Direct] = addressReview
        summon[CanReverseGet[Address, String]].reverseGet("Elm") === Address("Elm", 0)
      }
    }
    "void: an Affine-carrier optic cannot serve CanReverseGet" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           import dev.constructive.eo.data.Affine
           given Optic[CapsFixtures.Address, CapsFixtures.Address, Int, Int, Affine] =
             CapsFixtures.zipOptional
           summon[CanReverseGet[CapsFixtures.Address, Int]]"""
      ) must beFalse
    }
  }

  // ---------------------------------------------------------- CanModify
  "CanModifyP / CanModify" >> {
    "mixin: every writable concrete family serves directly (modify + replace)" >> {
      (streetLens: CanModify[Address, String]).modify(_.toUpperCase)(addr) ===
        Address("MAIN ST", 12345)
      (streetLens: CanModify[Address, String]).replace("Elm")(addr) === Address("Elm", 12345)
      (firstLens: CanModify[(String, Int), String]).modify(_ + "!")(("a", 1)) === (("a!", 1))
      (pairIso: CanModifyP[Address, Address, (String, Int), (String, Int)])
        .modify((s, z) => (s, z + 1))(addr) === Address("Main St", 12346)
      (somePrism: CanModify[Option[Int], Int]).modify(_ + 1)(Some(3)) === Some(4)
      (pickPrism: CanModifyP[Option[Int], Option[Int], Int, Int]).replace(9)(None) === None
      (zipOptional: CanModify[Address, Int]).modify(_ + 1)(addr) === Address("Main St", 12346)
      (streetModify: CanModify[Address, String]).modify(_.toLowerCase)(addr) ===
        Address("main st", 12345)
      (drilled: CanModify[Person, String]).modify(_.reverse)(person) ===
        Person("Ada", Address("tS niaM", 12345))
    }
    "resolution: concrete given is the instance itself" >> {
      given Modify[Address, Address, String, String] = streetModify
      summon[CanModify[Address, String]] must beTheSameAs(streetModify)
    }
    "derivation: generic optic givens derive — incl. Traversal (anonymous Optic)" >> {
      {
        given Optic[Person, Person, String, String, Tuple2] = drilled
        summon[CanModify[Person, String]].modify(_.reverse)(person) ===
          drilled.modify(_.reverse)(person)
      }
      {
        given Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] = eachTraversal
        summon[CanModify[List[Int], Int]].modify(_ + 1)(List(1, 2, 3)) === List(2, 3, 4)
      }
    }
    "doctrine: a generic consumer binds late through the alias" >> {
      def shout[T](using cm: CanModify[T, String]): T => T = cm.modify(_.toUpperCase)
      shout[Address](using streetLens)(addr) === Address("MAIN ST", 12345)
      shout[Person](using drilled)(person) === Person("Ada", Address("MAIN ST", 12345))
    }
  }

  // ------------------------------------------------------------ CanFold
  "CanFold" >> {
    "mixin: readable families serve directly; helpers ride the kernel" >> {
      (streetGetter: CanFold[Address, String]).foldMap(_.length)(addr) === 7
      (streetLens: CanFold[Address, String]).foci(addr) === List("Main St")
      (pairIso: CanFold[Address, (String, Int)]).length(addr) === 1
      (somePrism: CanFold[Option[Int], Int]).foldMap(identity)(Some(3)) === 3
      (somePrism: CanFold[Option[Int], Int]).foldMap(identity)(None) === 0
      (pickPrism: CanFold[Option[Int], Int]).headOption(None) === None
      (zipOptional: CanFold[Address, Int]).exists(_ > 9999)(addr) === true
      (zipAffineFold: CanFold[Address, Int]).headOption(addr) === Some(12345)
      (listFold: CanFold[List[Int], Int]).foldMap(identity)(List(1, 2, 3)) === 6
      (listFold: CanFold[List[Int], Int]).foci(List(1, 2, 3)) === List(1, 2, 3)
      (drilled: CanFold[Person, String]).headOption(person) === Some("Main St")
    }
    "resolution: concrete given is the instance itself" >> {
      given ForgetFold[List[Int], List, Int] = listFold
      summon[CanFold[List[Int], Int]] must beTheSameAs(listFold)
    }
    "derivation: generic Forget / MultiFocus optic givens derive" >> {
      {
        given Optic[List[Int], Unit, Int, Unit, Forget[List]] = listFold
        summon[CanFold[List[Int], Int]].foldMap(identity)(List(1, 2, 3)) === 6
      }
      {
        given Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] = eachTraversal
        summon[CanFold[List[Int], Int]].foci(List(1, 2, 3)) === List(1, 2, 3)
      }
    }
    "void: a ModifyF-carrier optic cannot serve CanFold" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           import dev.constructive.eo.data.ModifyF
           given Optic[CapsFixtures.Address, CapsFixtures.Address, String, String, ModifyF] =
             CapsFixtures.streetModify
           summon[CanFold[CapsFixtures.Address, String]]"""
      ) must beFalse
    }
  }

  // ------------------------------------------------------------- CanPut
  "CanPutP / CanPut" >> {
    "derivation: Direct-carrier optics (Iso, Review) derive — no mixin, wrapper route" >> {
      {
        given Optic[Address, Address, (String, Int), (String, Int), Direct] = pairIso
        summon[CanPutP[Address, (String, Int), (String, Int)]]
          .put((s, z) => (s.toUpperCase, z))(("main", 7)) === Address("MAIN", 7)
      }
      {
        given Optic[Unit, Address, Unit, String, Direct] = addressReview
        summon[CanPutP[Address, Unit, String]].put(_ => "Elm")(()) === Address("Elm", 0)
      }
    }
    "void: a Tuple2-carrier optic cannot serve CanPutP" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           given Optic[CapsFixtures.Address, CapsFixtures.Address, String, String, Tuple2] =
             CapsFixtures.streetLens
           summon[CanPutP[CapsFixtures.Address, String, String]]"""
      ) must beFalse
    }
  }

  // --------------------------------------------------------- CanModifyF
  "CanModifyFP / CanModifyF" >> {
    "derivation: Tuple2 is the only carrier traversing under bare Functor" >> {
      given Optic[Address, Address, String, String, Tuple2] = streetLens
      val cm = summon[CanModifyF[Address, String]]
      // Id-functor round-trip through the S => G[T] shape
      cm.modifyF[[x] =>> x](_.toUpperCase)(addr) === Address("MAIN ST", 12345)
    }
    "void: an Either-carrier optic cannot serve CanModifyFP" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           given Optic[Option[Int], Option[Int], Int, Int, Either] = CapsFixtures.somePrism
           summon[CanModifyFP[Option[Int], Option[Int], Int, Int]]"""
      ) must beFalse
    }
  }

  // --------------------------------------------------------- CanModifyA
  "CanModifyAP / CanModifyA" >> {
    "derivation: Tuple2 / Either / Affine / MultiFocus carriers derive" >> {
      {
        given Optic[Address, Address, String, String, Tuple2] = streetLens
        summon[CanModifyA[Address, String]]
          .modifyA(s => Option(s.toUpperCase))(addr) === Some(Address("MAIN ST", 12345))
      }
      {
        given Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] = eachTraversal
        summon[CanModifyA[List[Int], Int]]
          .modifyA(n => Option.when(n > 0)(n + 1))(List(1, 2)) === Some(List(2, 3))
      }
    }
    "void: a Direct-carrier optic cannot serve CanModifyAP (ForgetfulTraverse[Direct, *] is Invariant-only)" >> {
      typeChecks(
        """import dev.constructive.eo.*, dev.constructive.eo.optics.Optic
           import dev.constructive.eo.data.Direct
           given Optic[CapsFixtures.Address, CapsFixtures.Address, (String, Int), (String, Int), Direct] =
             CapsFixtures.pairIso
           summon[CanModifyAP[CapsFixtures.Address, CapsFixtures.Address, (String, Int), (String, Int)]]"""
      ) must beFalse
    }
  }

  // ----------------------------------------------- CanPlace/CanTransform
  "CanPlace / CanTransform (explicit constructors — no derived given by design)" >> {
    "CanPlace.from lifts a SimpleLens via its transformEvidence" >> {
      import optics.SimpleLens.transformEvidence
      given SimpleLens[(String, Int), String, Int] = firstLens
      val cp = CanPlace.from(firstLens)
      cp.place("z")(("a", 1)) === (("z", 1))
      cp.transfer[Int](_.toString)(("a", 1))(42) === (("42", 1))
    }
    "CanTransform.from lifts a SimpleLens" >> {
      import optics.SimpleLens.transformEvidence
      given SimpleLens[(String, Int), String, Int] = firstLens
      val ct: CanTransform[(String, Int), String, String] = CanTransform.from(firstLens)
      ct.transform(_.toUpperCase)(("a", 1)) === (("A", 1))
    }
  }
