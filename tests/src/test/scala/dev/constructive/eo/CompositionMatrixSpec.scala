package dev.constructive.eo

// =====================================================================
//  GENERATED-THEN-MAINTAINED — regenerate with tools from the matrix
//  harness lineage if the family list changes, otherwise edit in place.
//
//  The composition matrix: every (outer family ∘ inner family) pair is
//  asserted to either compose — WITHOUT a type ascription, landing at
//  the expected strength — or to NOT compile (semantically void cells:
//  building through non-invertible optics, writing through read-only
//  ones, reading through write-only ones).
//
//  Deliberately NO `data.*.given` imports: the matrix pins the
//  out-of-the-box UX. If a cell starts requiring an import or an
//  expected-type ascription to compose, this spec goes red.
// =====================================================================

import scala.compiletime.testing.typeChecks

import org.specs2.mutable.Specification

import optics.*
import data.{Affine, Direct, Forget, ModifyF, MultiFocus, PSVec}

object MatrixFixtures:
  case class Box[A](a: A)
  val o_iso = Iso[Box[Box[Int]], Box[Box[Int]], Box[Int], Box[Int]](_.a, Box(_))
  val o_isoL = Iso[Box[List[Int]], Box[List[Int]], List[Int], List[Int]](_.a, Box(_))
  val o_lens = Lens[Box[Box[Int]], Box[Int]](_.a, (s, m) => Box(m))
  val o_lensL = Lens[Box[List[Int]], List[Int]](_.a, (s, m) => Box(m))
  val o_prism = Prism[Box[Box[Int]], Box[Int]](b => Right(b.a), Box(_))
  val o_prismL = Prism[Box[List[Int]], List[Int]](b => Right(b.a), Box(_))

  val o_optional = Optional[Box[Box[Int]], Box[Box[Int]], Box[Int], Box[Int]](
    b => Right(b.a),
    sb => Box(sb._2),
  )

  val o_optionalL = Optional[Box[List[Int]], Box[List[Int]], List[Int], List[Int]](
    b => Right(b.a),
    sb => Box(sb._2),
  )

  val o_trav = Traversal.each[List, Box[Int]]
  val o_travL = Traversal.each[List, List[Int]]
  val o_getter = Getter[Box[Box[Int]], Box[Int]](_.a)
  val o_getterL = Getter[Box[List[Int]], List[Int]](_.a)
  val o_affold = AffineFold[Box[Box[Int]], Box[Int]](b => Some(b.a))
  val o_affoldL = AffineFold[Box[List[Int]], List[Int]](b => Some(b.a))
  val o_fold = Fold[List, Box[Int]]
  val o_foldL = Fold[List, List[Int]]
  val o_modify = Modify[Box[Box[Int]], Box[Box[Int]], Box[Int], Box[Int]](f => b => Box(f(b.a)))

  val o_modifyL =
    Modify[Box[List[Int]], Box[List[Int]], List[Int], List[Int]](f => b => Box(f(b.a)))

  val o_review = Review[Box[Box[Int]], Box[Int]](Box(_))
  val o_reviewL = Review[Box[List[Int]], List[Int]](Box(_))
  val i_iso = Iso[Box[Int], Box[Int], Int, Int](_.a, Box(_))
  val i_lens = Lens[Box[Int], Int](_.a, (s, m) => Box(m))
  val i_prism = Prism[Box[Int], Int](b => Right(b.a), Box(_))
  val i_optional = Optional[Box[Int], Box[Int], Int, Int](b => Right(b.a), sb => Box(sb._2))
  val i_trav = Traversal.each[List, Int]
  val i_getter = Getter[Box[Int], Int](_.a)
  val i_affold = AffineFold[Box[Int], Int](b => Some(b.a))
  val i_fold = Fold[List, Int]
  val i_modify = Modify[Box[Int], Box[Int], Int, Int](f => b => Box(f(b.a)))
  val i_review = Review[Box[Int], Int](Box(_))
  val o_unfold = Unfold((xs: List[Box[Int]]) => Box(Box(xs.map(_.a).sum)))
  val i_unfold = Unfold((xs: List[Int]) => Box(xs.sum))

class CompositionMatrixSpec extends Specification:
  import MatrixFixtures.*

  "iso (outer) composition row" >> {
    "iso ∘ iso → Optic" >> {
      typeChecks("o_iso.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Direct] = o_iso.andThen(i_iso)"
      ) must beTrue
    }
    "iso ∘ lens → Optic" >> {
      typeChecks("o_iso.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Tuple2] = o_iso.andThen(i_lens)"
      ) must beTrue
    }
    "iso ∘ prism → Optic" >> {
      typeChecks("o_iso.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Either] = o_iso.andThen(i_prism)"
      ) must beTrue
    }
    "iso ∘ optional → Optic" >> {
      typeChecks("o_iso.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_iso.andThen(i_optional)"
      ) must beTrue
    }
    "iso ∘ trav → Optic" >> {
      typeChecks("o_isoL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Box[List[Int]], Int, Int, MultiFocus[PSVec]] = o_isoL.andThen(i_trav)"
      ) must beTrue
    }
    "iso ∘ getter → Getter" >> {
      typeChecks("o_iso.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks("val r: Getter[Box[Box[Int]], Int] = o_iso.andThen(i_getter)") must beTrue
    }
    "iso ∘ affold → AffineFold" >> {
      typeChecks("o_iso.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_iso.andThen(i_affold)"
      ) must beTrue
    }
    "iso ∘ fold → Optic" >> {
      typeChecks("o_isoL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_isoL.andThen(i_fold)"
      ) must beTrue
    }
    "iso ∘ modify → Optic" >> {
      typeChecks("o_iso.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_iso.andThen(i_modify)"
      ) must beTrue
    }
    "iso ∘ review → Review" >> {
      typeChecks("o_iso.andThen(i_review)") must beTrue // resolves with no expected type
      typeChecks("val r: Review[Box[Box[Int]], Int] = o_iso.andThen(i_review)") must beTrue
    }
    "iso ∘ unfold → Unfold" >> {
      typeChecks("o_iso.andThen(i_unfold)") must beTrue // resolves with no expected type
      typeChecks("val r: Unfold[Box[Box[Int]], Int, List] = o_iso.andThen(i_unfold)") must beTrue
    }
  }

  "lens (outer) composition row" >> {
    "lens ∘ iso → Optic" >> {
      typeChecks("o_lens.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Tuple2] = o_lens.andThen(i_iso)"
      ) must beTrue
    }
    "lens ∘ lens → Optic" >> {
      typeChecks("o_lens.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Tuple2] = o_lens.andThen(i_lens)"
      ) must beTrue
    }
    "lens ∘ prism → Optic" >> {
      typeChecks("o_lens.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_lens.andThen(i_prism)"
      ) must beTrue
    }
    "lens ∘ optional → Optic" >> {
      typeChecks("o_lens.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_lens.andThen(i_optional)"
      ) must beTrue
    }
    "lens ∘ trav → Optic" >> {
      typeChecks("o_lensL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Box[List[Int]], Int, Int, MultiFocus[PSVec]] = o_lensL.andThen(i_trav)"
      ) must beTrue
    }
    "lens ∘ getter → Getter" >> {
      typeChecks("o_lens.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks("val r: Getter[Box[Box[Int]], Int] = o_lens.andThen(i_getter)") must beTrue
    }
    "lens ∘ affold → AffineFold" >> {
      typeChecks("o_lens.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_lens.andThen(i_affold)"
      ) must beTrue
    }
    "lens ∘ fold → Optic" >> {
      typeChecks("o_lensL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_lensL.andThen(i_fold)"
      ) must beTrue
    }
    "lens ∘ modify → Optic" >> {
      typeChecks("o_lens.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_lens.andThen(i_modify)"
      ) must beTrue
    }
    "lens ∘ review must not compile" >> {
      typeChecks("o_lens.andThen(i_review)") must beFalse
    }
    "lens ∘ unfold must not compile" >> {
      typeChecks("o_lens.andThen(i_unfold)") must beFalse
    }
  }

  "prism (outer) composition row" >> {
    "prism ∘ iso → Optic" >> {
      typeChecks("o_prism.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Either] = o_prism.andThen(i_iso)"
      ) must beTrue
    }
    "prism ∘ lens → Optic" >> {
      typeChecks("o_prism.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_prism.andThen(i_lens)"
      ) must beTrue
    }
    "prism ∘ prism → Optic" >> {
      typeChecks("o_prism.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Either] = o_prism.andThen(i_prism)"
      ) must beTrue
    }
    "prism ∘ optional → Optic" >> {
      typeChecks("o_prism.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_prism.andThen(i_optional)"
      ) must beTrue
    }
    "prism ∘ trav → Optic" >> {
      typeChecks("o_prismL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Box[List[Int]], Int, Int, MultiFocus[PSVec]] = o_prismL.andThen(i_trav)"
      ) must beTrue
    }
    "prism ∘ getter → AffineFold" >> {
      typeChecks("o_prism.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_prism.andThen(i_getter)"
      ) must beTrue
    }
    "prism ∘ affold → AffineFold" >> {
      typeChecks("o_prism.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_prism.andThen(i_affold)"
      ) must beTrue
    }
    "prism ∘ fold → Optic" >> {
      typeChecks("o_prismL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_prismL.andThen(i_fold)"
      ) must beTrue
    }
    "prism ∘ modify → Optic" >> {
      typeChecks("o_prism.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_prism.andThen(i_modify)"
      ) must beTrue
    }
    "prism ∘ review → Review" >> {
      typeChecks("o_prism.andThen(i_review)") must beTrue // resolves with no expected type
      typeChecks("val r: Review[Box[Box[Int]], Int] = o_prism.andThen(i_review)") must beTrue
    }
    "prism ∘ unfold → Unfold" >> {
      typeChecks("o_prism.andThen(i_unfold)") must beTrue // resolves with no expected type
      typeChecks("val r: Unfold[Box[Box[Int]], Int, List] = o_prism.andThen(i_unfold)") must beTrue
    }
  }

  "optional (outer) composition row" >> {
    "optional ∘ iso → Optic" >> {
      typeChecks("o_optional.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_optional.andThen(i_iso)"
      ) must beTrue
    }
    "optional ∘ lens → Optic" >> {
      typeChecks("o_optional.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_optional.andThen(i_lens)"
      ) must beTrue
    }
    "optional ∘ prism → Optic" >> {
      typeChecks("o_optional.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_optional.andThen(i_prism)"
      ) must beTrue
    }
    "optional ∘ optional → Optic" >> {
      typeChecks("o_optional.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, Affine] = o_optional.andThen(i_optional)"
      ) must beTrue
    }
    "optional ∘ trav → Optic" >> {
      typeChecks("o_optionalL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Box[List[Int]], Int, Int, MultiFocus[PSVec]] = o_optionalL.andThen(i_trav)"
      ) must beTrue
    }
    "optional ∘ getter → AffineFold" >> {
      typeChecks("o_optional.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_optional.andThen(i_getter)"
      ) must beTrue
    }
    "optional ∘ affold → AffineFold" >> {
      typeChecks("o_optional.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_optional.andThen(i_affold)"
      ) must beTrue
    }
    "optional ∘ fold → Optic" >> {
      typeChecks("o_optionalL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_optionalL.andThen(i_fold)"
      ) must beTrue
    }
    "optional ∘ modify → Optic" >> {
      typeChecks("o_optional.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_optional.andThen(i_modify)"
      ) must beTrue
    }
    "optional ∘ review must not compile" >> {
      typeChecks("o_optional.andThen(i_review)") must beFalse
    }
    "optional ∘ unfold must not compile" >> {
      typeChecks("o_optional.andThen(i_unfold)") must beFalse
    }
  }

  "trav (outer) composition row" >> {
    "trav ∘ iso → Optic" >> {
      typeChecks("o_trav.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], List[Box[Int]], Int, Int, MultiFocus[PSVec]] = o_trav.andThen(i_iso)"
      ) must beTrue
    }
    "trav ∘ lens → Optic" >> {
      typeChecks("o_trav.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], List[Box[Int]], Int, Int, MultiFocus[PSVec]] = o_trav.andThen(i_lens)"
      ) must beTrue
    }
    "trav ∘ prism → Optic" >> {
      typeChecks("o_trav.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], List[Box[Int]], Int, Int, MultiFocus[PSVec]] = o_trav.andThen(i_prism)"
      ) must beTrue
    }
    "trav ∘ optional → Optic" >> {
      typeChecks("o_trav.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], List[Box[Int]], Int, Int, MultiFocus[PSVec]] = o_trav.andThen(i_optional)"
      ) must beTrue
    }
    "trav ∘ trav → Optic" >> {
      typeChecks("o_travL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[List[Int]], List[List[Int]], Int, Int, MultiFocus[PSVec]] = o_travL.andThen(i_trav)"
      ) must beTrue
    }
    "trav ∘ getter → Optic" >> {
      typeChecks("o_trav.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_trav.andThen(i_getter)"
      ) must beTrue
    }
    "trav ∘ affold → Optic" >> {
      typeChecks("o_trav.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_trav.andThen(i_affold)"
      ) must beTrue
    }
    "trav ∘ fold → Optic" >> {
      typeChecks("o_travL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[List[Int]], Unit, Int, Unit, Forget[List]] = o_travL.andThen(i_fold)"
      ) must beTrue
    }
    "trav ∘ modify → Optic" >> {
      typeChecks("o_trav.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], List[Box[Int]], Int, Int, ModifyF] = o_trav.andThen(i_modify)"
      ) must beTrue
    }
    "trav ∘ review must not compile" >> {
      typeChecks("o_trav.andThen(i_review)") must beFalse
    }
    "trav ∘ unfold must not compile" >> {
      typeChecks("o_trav.andThen(i_unfold)") must beFalse
    }
  }

  "getter (outer) composition row" >> {
    "getter ∘ iso → Getter" >> {
      typeChecks("o_getter.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks("val r: Getter[Box[Box[Int]], Int] = o_getter.andThen(i_iso)") must beTrue
    }
    "getter ∘ lens → Getter" >> {
      typeChecks("o_getter.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks("val r: Getter[Box[Box[Int]], Int] = o_getter.andThen(i_lens)") must beTrue
    }
    "getter ∘ prism → AffineFold" >> {
      typeChecks("o_getter.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_getter.andThen(i_prism)"
      ) must beTrue
    }
    "getter ∘ optional → AffineFold" >> {
      typeChecks("o_getter.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_getter.andThen(i_optional)"
      ) must beTrue
    }
    "getter ∘ trav → Optic" >> {
      typeChecks("o_getterL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_getterL.andThen(i_trav)"
      ) must beTrue
    }
    "getter ∘ getter → Getter" >> {
      typeChecks("o_getter.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks("val r: Getter[Box[Box[Int]], Int] = o_getter.andThen(i_getter)") must beTrue
    }
    "getter ∘ affold → AffineFold" >> {
      typeChecks("o_getter.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_getter.andThen(i_affold)"
      ) must beTrue
    }
    "getter ∘ fold → Optic" >> {
      typeChecks("o_getterL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_getterL.andThen(i_fold)"
      ) must beTrue
    }
    "getter ∘ modify must not compile" >> {
      typeChecks("o_getter.andThen(i_modify)") must beFalse
    }
    "getter ∘ review must not compile" >> {
      typeChecks("o_getter.andThen(i_review)") must beFalse
    }
    "getter ∘ unfold must not compile" >> {
      typeChecks("o_getter.andThen(i_unfold)") must beFalse
    }
  }

  "affold (outer) composition row" >> {
    "affold ∘ iso → AffineFold" >> {
      typeChecks("o_affold.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_iso)"
      ) must beTrue
    }
    "affold ∘ lens → AffineFold" >> {
      typeChecks("o_affold.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_lens)"
      ) must beTrue
    }
    "affold ∘ prism → AffineFold" >> {
      typeChecks("o_affold.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_prism)"
      ) must beTrue
    }
    "affold ∘ optional → AffineFold" >> {
      typeChecks("o_affold.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_optional)"
      ) must beTrue
    }
    "affold ∘ trav → Optic" >> {
      typeChecks("o_affoldL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_affoldL.andThen(i_trav)"
      ) must beTrue
    }
    "affold ∘ getter → AffineFold" >> {
      typeChecks("o_affold.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_getter)"
      ) must beTrue
    }
    "affold ∘ affold → AffineFold" >> {
      typeChecks("o_affold.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Unit, Int, Unit, Affine] = o_affold.andThen(i_affold)"
      ) must beTrue
    }
    "affold ∘ fold → Optic" >> {
      typeChecks("o_affoldL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Unit, Int, Unit, Forget[List]] = o_affoldL.andThen(i_fold)"
      ) must beTrue
    }
    "affold ∘ modify must not compile" >> {
      typeChecks("o_affold.andThen(i_modify)") must beFalse
    }
    "affold ∘ review must not compile" >> {
      typeChecks("o_affold.andThen(i_review)") must beFalse
    }
    "affold ∘ unfold must not compile" >> {
      typeChecks("o_affold.andThen(i_unfold)") must beFalse
    }
  }

  "fold (outer) composition row" >> {
    "fold ∘ iso → Optic" >> {
      typeChecks("o_fold.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_iso)"
      ) must beTrue
    }
    "fold ∘ lens → Optic" >> {
      typeChecks("o_fold.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_lens)"
      ) must beTrue
    }
    "fold ∘ prism → Optic" >> {
      typeChecks("o_fold.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_prism)"
      ) must beTrue
    }
    "fold ∘ optional → Optic" >> {
      typeChecks("o_fold.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_optional)"
      ) must beTrue
    }
    "fold ∘ trav → Optic" >> {
      typeChecks("o_foldL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[List[Int]], Unit, Int, Unit, Forget[List]] = o_foldL.andThen(i_trav)"
      ) must beTrue
    }
    "fold ∘ getter → Optic" >> {
      typeChecks("o_fold.andThen(i_getter)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_getter)"
      ) must beTrue
    }
    "fold ∘ affold → Optic" >> {
      typeChecks("o_fold.andThen(i_affold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[Box[Int]], Unit, Int, Unit, Forget[List]] = o_fold.andThen(i_affold)"
      ) must beTrue
    }
    "fold ∘ fold → Optic" >> {
      typeChecks("o_foldL.andThen(i_fold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[List[List[Int]], Unit, Int, Unit, Forget[List]] = o_foldL.andThen(i_fold)"
      ) must beTrue
    }
    "fold ∘ modify must not compile" >> {
      typeChecks("o_fold.andThen(i_modify)") must beFalse
    }
    "fold ∘ review must not compile" >> {
      typeChecks("o_fold.andThen(i_review)") must beFalse
    }
    "fold ∘ unfold must not compile" >> {
      typeChecks("o_fold.andThen(i_unfold)") must beFalse
    }
  }

  "modify (outer) composition row" >> {
    "modify ∘ iso → Optic" >> {
      typeChecks("o_modify.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_modify.andThen(i_iso)"
      ) must beTrue
    }
    "modify ∘ lens → Optic" >> {
      typeChecks("o_modify.andThen(i_lens)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_modify.andThen(i_lens)"
      ) must beTrue
    }
    "modify ∘ prism → Optic" >> {
      typeChecks("o_modify.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_modify.andThen(i_prism)"
      ) must beTrue
    }
    "modify ∘ optional → Optic" >> {
      typeChecks("o_modify.andThen(i_optional)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_modify.andThen(i_optional)"
      ) must beTrue
    }
    "modify ∘ trav → Optic" >> {
      typeChecks("o_modifyL.andThen(i_trav)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[List[Int]], Box[List[Int]], Int, Int, ModifyF] = o_modifyL.andThen(i_trav)"
      ) must beTrue
    }
    "modify ∘ getter must not compile" >> {
      typeChecks("o_modify.andThen(i_getter)") must beFalse
    }
    "modify ∘ affold must not compile" >> {
      typeChecks("o_modify.andThen(i_affold)") must beFalse
    }
    "modify ∘ fold must not compile" >> {
      typeChecks("o_modifyL.andThen(i_fold)") must beFalse
    }
    "modify ∘ modify → Optic" >> {
      typeChecks("o_modify.andThen(i_modify)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Optic[Box[Box[Int]], Box[Box[Int]], Int, Int, ModifyF] = o_modify.andThen(i_modify)"
      ) must beTrue
    }
    "modify ∘ review must not compile" >> {
      typeChecks("o_modify.andThen(i_review)") must beFalse
    }
    "modify ∘ unfold must not compile" >> {
      typeChecks("o_modify.andThen(i_unfold)") must beFalse
    }
  }

  "review (outer) composition row" >> {
    "review ∘ iso → Review" >> {
      typeChecks("o_review.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks("val r: Review[Box[Box[Int]], Int] = o_review.andThen(i_iso)") must beTrue
    }
    "review ∘ lens must not compile" >> {
      typeChecks("o_review.andThen(i_lens)") must beFalse
    }
    "review ∘ prism → Review" >> {
      typeChecks("o_review.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks("val r: Review[Box[Box[Int]], Int] = o_review.andThen(i_prism)") must beTrue
    }
    "review ∘ optional must not compile" >> {
      typeChecks("o_review.andThen(i_optional)") must beFalse
    }
    "review ∘ trav must not compile" >> {
      typeChecks("o_reviewL.andThen(i_trav)") must beFalse
    }
    "review ∘ getter must not compile" >> {
      typeChecks("o_review.andThen(i_getter)") must beFalse
    }
    "review ∘ affold must not compile" >> {
      typeChecks("o_review.andThen(i_affold)") must beFalse
    }
    "review ∘ fold must not compile" >> {
      typeChecks("o_reviewL.andThen(i_fold)") must beFalse
    }
    "review ∘ modify must not compile" >> {
      typeChecks("o_review.andThen(i_modify)") must beFalse
    }
    "review ∘ review → Review" >> {
      typeChecks("o_review.andThen(i_review)") must beTrue // resolves with no expected type
      typeChecks("val r: Review[Box[Box[Int]], Int] = o_review.andThen(i_review)") must beTrue
    }
    "review ∘ unfold → Unfold" >> {
      typeChecks("o_review.andThen(i_unfold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Unfold[Box[Box[Int]], Int, List] = o_review.andThen(i_unfold)"
      ) must beTrue
    }
  }

  "unfold (outer) composition row" >> {
    "unfold ∘ iso → Unfold" >> {
      typeChecks("o_unfold.andThen(i_iso)") must beTrue // resolves with no expected type
      typeChecks("val r: Unfold[Box[Box[Int]], Int, List] = o_unfold.andThen(i_iso)") must beTrue
    }
    "unfold ∘ lens must not compile" >> {
      typeChecks("o_unfold.andThen(i_lens)") must beFalse
    }
    "unfold ∘ prism → Unfold" >> {
      typeChecks("o_unfold.andThen(i_prism)") must beTrue // resolves with no expected type
      typeChecks("val r: Unfold[Box[Box[Int]], Int, List] = o_unfold.andThen(i_prism)") must beTrue
    }
    "unfold ∘ optional must not compile" >> {
      typeChecks("o_unfold.andThen(i_optional)") must beFalse
    }
    "unfold ∘ trav must not compile" >> {
      typeChecks("o_unfold.andThen(i_trav)") must beFalse
    }
    "unfold ∘ getter must not compile" >> {
      typeChecks("o_unfold.andThen(i_getter)") must beFalse
    }
    "unfold ∘ affold must not compile" >> {
      typeChecks("o_unfold.andThen(i_affold)") must beFalse
    }
    "unfold ∘ fold must not compile" >> {
      typeChecks("o_unfold.andThen(i_fold)") must beFalse
    }
    "unfold ∘ modify must not compile" >> {
      typeChecks("o_unfold.andThen(i_modify)") must beFalse
    }
    "unfold ∘ review → Unfold" >> {
      typeChecks("o_unfold.andThen(i_review)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Unfold[Box[Box[Int]], Int, List] = o_unfold.andThen(i_review)"
      ) must beTrue
    }
    "unfold ∘ unfold → Unfold" >> {
      typeChecks("o_unfold.andThen(i_unfold)") must beTrue // resolves with no expected type
      typeChecks(
        "val r: Unfold[Box[Box[Int]], Int, List] = o_unfold.andThen(i_unfold)"
      ) must beTrue
    }
  }
