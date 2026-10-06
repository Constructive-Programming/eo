package dev.constructive.eo

// =====================================================================
//  The Grate sub-shape grid — the companion to CompositionMatrixSpec.
//
//  Indexed is the full-grid successor, not a MultiFocus sub-shape.
//  No cross-family Composer bridges are installed. The generic writable-outer
//  extension still supports a write-only Modify inner. Explicitly constructing an
//  Indexed.iso supplies a lawful Unit axis; it is not an implicit widening.
//  Keep the explicit cell labels so the QA generator can report verified seams.
//  Same doctrine as CompositionMatrixSpec: no expected-type ascription and
//  no `given` imports — a cell that starts needing either goes red.
// =====================================================================

import scala.compiletime.testing.typeChecks

import cats.Representable
import cats.instances.function.*
import org.specs2.mutable.Specification

import optics.*
import data.{Direct, GlassF, ModifyF, MultiFocus}
import compose.{AssociativeFunctor, Composer}

object GrateFixtures:
  case class Box[A](a: A)

  // Column direction: outers whose focus IS the Grate's source (`Int => Int`).
  val o_iso = Iso[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](_.a, Box(_))
  val o_lens = Lens[Box[Int => Int], Int => Int](_.a, (s, m) => Box(m))
  val o_prism = Prism[Box[Int => Int], Int => Int](b => Right(b.a), Box(_))

  val o_optional =
    Optional[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](
      b => Right(b.a),
      sb => Box(sb._2)
    )

  val o_trav = Traversal.each[List, Int => Int]
  val o_getter = Getter[Box[Int => Int], Int => Int](_.a)
  val o_affold = AffineFold[Box[Int => Int], Int => Int](b => Some(b.a))
  val o_fold = Fold[List, Int => Int]

  val o_modify =
    Modify[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](f => b => Box(f(b.a)))

  val o_review = Review[Box[Int => Int], Int => Int](Box(_))
  val o_unfold = Unfold((xs: List[Int => Int]) => Box(xs.head))
  val functionR = summon[Representable.Aux[Function1[Int, *], Int]]
  val i_grate = Indexed.representable[Function1[Int, *], Int, Int](functionR)

  // Row direction: the Grate as outer, inners sourced on its focus.
  val g_box = Indexed.representable[Function1[Int, *], Box[Int], Box[Int]](functionR)
  val g_list = Indexed.representable[Function1[Int, *], List[Int], List[Int]](functionR)
  val g_fun = Indexed.representable[Function1[Int, *], Int => Int, Int => Int](functionR)

  val i_iso = Iso[Box[Int], Box[Int], Int, Int](_.a, Box(_))
  val i_lens = Lens[Box[Int], Int](_.a, (s, m) => Box(m))
  val i_prism = Prism[Box[Int], Int](b => Right(b.a), Box(_))
  val i_optional = Optional[Box[Int], Box[Int], Int, Int](b => Right(b.a), sb => Box(sb._2))
  val i_getter = Getter[Box[Int], Int](_.a)
  val i_affold = AffineFold[Box[Int], Int](b => Some(b.a))
  val i_modify = Modify[Box[Int], Box[Int], Int, Int](f => b => Box(f(b.a)))
  val i_review = Review[Box[Int], Int](Box(_))
  val i_unfold = Unfold((xs: List[Int]) => Box(xs.head))
  val i_each = Traversal.each[List, Int]
  val i_fold = Fold[List, Int]

class GrateShapeSpec extends Specification:
  import GrateFixtures.*

  "Grate sub-shape — family ∘ grate (the Grate as inner)" >> {
    "iso ∘ grate must not compile" >> {
      typeChecks("o_iso.andThen(i_grate)") must beFalse
    }
    "lens ∘ grate must not compile" >> {
      typeChecks("o_lens.andThen(i_grate)") must beFalse
    }
    "prism ∘ grate must not compile" >> {
      typeChecks("o_prism.andThen(i_grate)") must beFalse
    }
    "optional ∘ grate must not compile" >> {
      typeChecks("o_optional.andThen(i_grate)") must beFalse
    }
    "trav ∘ grate must not compile" >> {
      typeChecks("o_trav.andThen(i_grate)") must beFalse
    }
    "getter ∘ grate must not compile" >> {
      typeChecks("o_getter.andThen(i_grate)") must beFalse
    }
    "affold ∘ grate must not compile" >> {
      typeChecks("o_affold.andThen(i_grate)") must beFalse
    }
    "fold ∘ grate must not compile" >> {
      typeChecks("o_fold.andThen(i_grate)") must beFalse
    }
    "modify ∘ grate must not compile" >> {
      typeChecks("o_modify.andThen(i_grate)") must beFalse
    }
    "review ∘ grate must not compile" >> {
      typeChecks("o_review.andThen(i_grate)") must beFalse
    }
    "unfold ∘ grate must not compile" >> {
      typeChecks("o_unfold.andThen(i_grate)") must beFalse
    }
  }

  "Grate sub-shape — grate ∘ family (the Grate as outer)" >> {
    "grate ∘ iso must not compile" >> {
      typeChecks("g_box.andThen(i_iso)") must beFalse
    }
    "grate ∘ lens must not compile" >> {
      typeChecks("g_box.andThen(i_lens)") must beFalse
    }
    "grate ∘ prism must not compile" >> {
      typeChecks("g_box.andThen(i_prism)") must beFalse
    }
    "grate ∘ optional must not compile" >> {
      typeChecks("g_box.andThen(i_optional)") must beFalse
    }
    "grate ∘ trav must not compile" >> {
      typeChecks("g_list.andThen(i_each)") must beFalse
    }
    "grate ∘ getter must not compile" >> {
      typeChecks("g_box.andThen(i_getter)") must beFalse
    }
    "grate ∘ affold must not compile" >> {
      typeChecks("g_box.andThen(i_affold)") must beFalse
    }
    "grate ∘ fold must not compile" >> {
      typeChecks("g_list.andThen(i_fold)") must beFalse
    }
    "grate ∘ modify → ModifyF" >> {
      typeChecks("g_box.andThen(i_modify)") must beTrue
    }
    "grate ∘ review must not compile" >> {
      typeChecks("g_box.andThen(i_review)") must beFalse
    }
    "grate ∘ unfold must not compile" >> {
      typeChecks("g_box.andThen(i_unfold)") must beFalse
    }
  }

  "Full-grid glass — grate ∘ grate (product index)" >> {
    "grate ∘ grate → GlassF[(Int, Int)]" >> {
      typeChecks("g_fun.andThen(i_grate)") must beTrue
    }
  }

  "Full-grid composition retains the concrete product index" >> {
    val composed: Optic[Int => Int => Int, Int => Int => Int, Int, Int, GlassF[(Int, Int)]] =
      g_fun.andThen(i_grate)
    composed.to(i => j => i + j).values((2, 3)) must beEqualTo(5)
  }

  "The writable-outer Modify seam rewrites each represented value" >> {
    val rewritten = g_box.andThen(i_modify).modify(_ + 1)((i: Int) => Box(i))
    rewritten(2) must beEqualTo(Box(3))
    rewritten(9) must beEqualTo(Box(10))
  }

  "Retired Function1 routing stays absent" >> {
    typeChecks("summon[AssociativeFunctor[MultiFocus[Function1[Int, *]], Unit, Unit]]") must beFalse
    typeChecks("summon[Composer[Direct, MultiFocus[Function1[Int, *]]]]") must beFalse
    typeChecks("summon[Composer[GlassF[Int], ModifyF]]") must beFalse
  }
