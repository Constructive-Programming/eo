package dev.constructive.eo

// =====================================================================
//  The Grate sub-shape grid — the companion to CompositionMatrixSpec.
//
//  CompositionMatrixSpec's `trav` / `fold` rows describe
//  `MultiFocus[PSVec]` (the `Traversal` class, `each`, `Plated`). The
//  other shipped MultiFocus sub-shape — the Grate, i.e.
//  `MultiFocus[Function1[X0, *]]` over the Naperian factories
//  (`MultiFocus.tuple` / `representable` / `representableAt` / `apply`)
//  — has a materially NARROWER composition footprint. This spec pins it,
//  so the QA page can show it and a future bridge cannot silently move a
//  cell.
//
//  Why the ✗ cells are structural, not missing plumbing:
//    - a Lens / Traversal write-back would have to pick one focus out of a
//      Naperian bundle => needs `Foldable[Function1[X0, *]]`: no instance
//      (and no lawful one — a function's codomain is not enumerable)
//    - a Prism / Optional miss would need `Alternative[Function1[X0, *]]`
//      (`empty`, i.e. `X0 => A` with no `A`): impossible
//    - a Getter / AffineFold / Fold read-collapse would have to enumerate
//      the codomain: no lawful fold
//    - cross-`F` MultiFocus composition (PSVec ∘ Function1) needs a per-`F`
//      natural transformation: documented workaround only
//  Same doctrine as CompositionMatrixSpec: no expected-type ascription and
//  no `given` imports — a cell that starts needing either goes red.
// =====================================================================

import scala.compiletime.testing.typeChecks

import org.specs2.mutable.Specification

import optics.*
import data.MultiFocus

object GrateFixtures:
  case class Box[A](a: A)

  // Column direction: outers whose focus IS the Grate's source (`Int => Int`).
  val o_iso = Iso[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](_.a, Box(_))
  val o_lens = Lens[Box[Int => Int], Int => Int](_.a, (s, m) => Box(m))
  val o_prism = Prism[Box[Int => Int], Int => Int](b => Right(b.a), Box(_))
  val o_optional =
    Optional[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](b => Right(b.a), sb => Box(sb._2))
  val o_trav = Traversal.each[List, Int => Int]
  val o_getter = Getter[Box[Int => Int], Int => Int](_.a)
  val o_affold = AffineFold[Box[Int => Int], Int => Int](b => Some(b.a))
  val o_fold = Fold[List, Int => Int]
  val o_modify =
    Modify[Box[Int => Int], Box[Int => Int], Int => Int, Int => Int](f => b => Box(f(b.a)))
  val o_review = Review[Box[Int => Int], Int => Int](Box(_))
  val o_unfold = Unfold((xs: List[Int => Int]) => Box(xs.head))
  val i_grate = MultiFocus.apply[Function1[Int, *], Int]

  // Row direction: the Grate as outer, inners sourced on its focus.
  val g_box = MultiFocus.apply[Function1[Int, *], Box[Int]]
  val g_list = MultiFocus.apply[Function1[Int, *], List[Int]]
  val g_fun = MultiFocus.apply[Function1[Int, *], Int => Int]

  val i_iso = Iso[Box[Int], Box[Int], Int, Int](_.a, Box(_))
  val i_lens = Lens[Box[Int], Int](_.a, (s, m) => Box(m))
  val i_prism = Prism[Box[Int], Int](b => Right(b.a), Box(_))
  val i_optional = Optional[Box[Int], Box[Int], Int, Int](b => Right(b.a), sb => Box(sb._2))
  val i_getter = Getter[Box[Int], Int](_.a)
  val i_affold = AffineFold[Box[Int], Int](b => Some(b.a))
  val i_modify = Modify[Box[Int], Box[Int], Int, Int](f => b => Box(f(b.a)))
  val i_review = Review[Box[Int], Int](Box(_))
  val i_unfold = Unfold((xs: List[Box[Int]]) => Box(xs.head))
  val i_each = Traversal.each[List, Int]
  val i_fold = Fold[List, Int]

class GrateShapeSpec extends Specification:
  import GrateFixtures.*

  "Grate sub-shape — family ∘ grate (the Grate as inner)" >> {
    "iso ∘ grate → MultiFocus[Function1[Int, *]]" >> {
      typeChecks("o_iso.andThen(i_grate)") must beTrue
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
    "modify ∘ grate → ModifyF" >> {
      typeChecks("o_modify.andThen(i_grate)") must beTrue
    }
    "review ∘ grate must not compile" >> {
      typeChecks("o_review.andThen(i_grate)") must beFalse
    }
    "unfold ∘ grate must not compile" >> {
      typeChecks("o_unfold.andThen(i_grate)") must beFalse
    }
  }

  "Grate sub-shape — grate ∘ family (the Grate as outer)" >> {
    "grate ∘ iso → MultiFocus[Function1[Int, *]]" >> {
      typeChecks("g_box.andThen(i_iso)") must beTrue
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

  "Grate sub-shape — grate ∘ grate (same carrier)" >> {
    "grate ∘ grate → MultiFocus[Function1[Int, *]]" >> {
      typeChecks("g_fun.andThen(i_grate)") must beTrue
    }
  }
