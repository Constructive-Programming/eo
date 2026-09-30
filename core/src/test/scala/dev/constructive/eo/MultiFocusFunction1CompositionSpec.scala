package dev.constructive.eo

import scala.language.implicitConversions

import cats.instances.function.given
import dev.constructive.eo.compose.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.{Direct, MultiFocus, RepresentativeIndex}
import data.MultiFocus.at
import optics.*
import optics.Optic.*

/** Same-carrier composition on the `MultiFocus[Function1[X0, *]]` (grate-shaped) carrier, where the
  * two sides of a composed optic do NOT have the same shape:
  *
  *   - a *positional* (tabulating) optic reads a real position of its source —
  *     `MultiFocus.representable` / `tuple`;
  *   - an *index-free* (broadcast) optic reads one value at every index — every optic built by
  *     `forgetful2multifocusFunction1` (the Iso shim), and any composite whose outer is one.
  *
  * `mfAssocFunction1` picks a read and a write shape per side; these blocks pin the shapes that are
  * observable at the composite's boundary through `.modify` / `.replace` / `.at`:
  *
  *   - positional-outer ∘ index-free-inner — the write stays per-position, so `modify(identity)` is
  *     the identity and every source position keeps its own value;
  *   - index-free-outer ∘ positional-inner — the read is tabulated, the write collapses to the one
  *     value the outer rebuilds with;
  *   - nested compositions re-derive both decisions at every level.
  *
  * The positional blocks are the observable half of the design question: a kernel that samples the
  * outer's read at an invented index, or collapses the write to one broadcast value, fails them.
  */
class MultiFocusFunction1CompositionSpec extends Specification with ScalaCheck:

  /** The Iso shim: `S ≅ A`, broadcast to the `Function1[X0, *]` carrier with `X = Unit`. Every
    * index reads the same `A` — the shape the carrier cannot otherwise express. The bridge needs a
    * [[RepresentativeIndex]] for `X0` (the one bundle read it owns; `Int` / `Boolean` / `Unit` /
    * singletons ship instances), so it is threaded through here.
    */
  private def broadcastOuter[X0, S, T, A, B](
      iso: Optic[S, T, A, B, Direct]
  )(using RepresentativeIndex[X0]): Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] =
    summon[Composer[Direct, MultiFocus[Function1[X0, *]]]].to(iso)

  // covers: positional outer ∘ index-free inner, same-carrier `.andThen` through
  // mfAssocFunction1.composeFrom. The outer reads a real tuple position; the inner rebuilds ONE
  // value per position. `modify(identity)` must therefore be the identity, and a pointwise modify
  // must reach every slot with its own value.
  "MultiFocus.tuple.andThen(<iso shim>): per-position write survives the composition" >> {
    val outer: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val inner: Optic[Int, Int, String, String, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, String, String](
        Iso[Int, Int, String, String](_.toString, _.toInt)
      )
    val g: Optic[(Int, Int), (Int, Int), String, String, MultiFocus[Function1[Int, *]]] =
      outer.andThen(inner)

    val identityWrite = forAll { (l: Int, r: Int) =>
      val out = g.modify(identity[String])((l, r))
      out._1 == l && out._2 == r
    }
    val pointwise = forAll { (l: Int, r: Int) =>
      val out = g.modify(s => (s.toInt + 1).toString)((l, r))
      out._1 == l + 1 && out._2 == r + 1
    }
    identityWrite && pointwise
  }

  // covers: index-free outer ∘ positional inner — the read is tabulated (each index reads its own
  // slot) while the write goes back through the outer's single-value rebuild. `.replace`
  // broadcasts the written value to every position because the outer owns exactly one focus.
  "<iso shim>.andThen(MultiFocus.tuple): tabulated read + single-value write" >> {
    val pairIso: Optic[(Int, Int), (Int, Int), (Int, Int), (Int, Int), Direct] =
      Iso[(Int, Int), (Int, Int), (Int, Int), (Int, Int)](identity, identity)
    val outer
        : Optic[(Int, Int), (Int, Int), (Int, Int), (Int, Int), MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, (Int, Int), (Int, Int), (Int, Int), (Int, Int)](pairIso)
    val inner: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val g: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      outer.andThen(inner)

    val pointwise = forAll { (l: Int, r: Int) =>
      val out = g.modify(_ + 1)((l, r))
      out._1 == l + 1 && out._2 == r + 1
    }
    val broadcast = forAll { (l: Int, r: Int, v: Int) =>
      val out = g.replace(v)((l, r))
      out._1 == v && out._2 == v
    }
    pointwise && broadcast
  }

  // covers: two levels of same-carrier `.andThen` — an index-free inner under a composite whose own
  // outer read is POSITIONAL. Each level re-derives its read/write shape; reading one position twice
  // (or collapsing the pair to a single value) is observable here.
  "(<iso shim> ∘ MultiFocus.representable).andThen(<iso shim>): nested composition stays positional" >> {
    // (Int, Int) ≅ (Boolean => Int) — a lawful iso, so a representable factory can sit under it.
    val pairIso: Optic[(Int, Int), (Int, Int), Boolean => Int, Boolean => Int, Direct] =
      Iso[(Int, Int), (Int, Int), Boolean => Int, Boolean => Int](
        p => b => if b then p._1 else p._2,
        f => (f(true), f(false)),
      )
    val mid: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Boolean, *]]] =
      broadcastOuter[Boolean, (Int, Int), (Int, Int), Boolean => Int, Boolean => Int](pairIso)
        .andThen(MultiFocus.representable[[a] =>> Boolean => a, Int])
    val inner: Optic[Int, Int, String, String, MultiFocus[Function1[Boolean, *]]] =
      broadcastOuter[Boolean, Int, Int, String, String](
        Iso[Int, Int, String, String](_.toString, _.toInt)
      )
    val g = mid.andThen(inner)

    val positional = forAll { (l: Int, r: Int) =>
      val out = g.modify(s => (s.toInt + 1).toString)((l, r))
      out._1 == l + 1 && out._2 == r + 1
    }
    val indexRead = forAll { (l: Int, r: Int) =>
      g.at(true)((l, r)) == l.toString && g.at(false)((l, r)) == r.toString
    }
    positional && indexRead
  }

  // covers: the sum itself — `.broadcast` distinguishes an index-free bundle from a tabulation, and
  // `.foci` stays total for both (the broadcast half's lifted `F[A]`). Composition propagates the
  // shape by data: chaining broadcast optics keeps the composite index-free.
  "bundle shape: `.broadcast` / `.foci` on index-free vs tabulating bundles, and through a chain" >> {
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](Iso[Int, Int, Int, Int](_ + 1, _ - 1))
    val tuple: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]

    // Index-free: the value is available outright, and `.foci` is the constant image of it.
    val shimBundle = shim.to(7)
    val shimOk: Boolean =
      shimBundle.broadcast == Some(8) && shimBundle.foci(0) == 8 && shimBundle.foci(1) == 8

    // Tabulating: no index-free value, and `.foci` is the real per-index read.
    val tupleBundle = tuple.to((3, 4))
    val tupleOk: Boolean =
      tupleBundle.broadcast.isEmpty && tupleBundle.foci(0) == 3 && tupleBundle.foci(1) == 4

    // The public constructor is the same shape, for optics that broadcast without an Iso.
    val manual = MultiFocus.broadcast[Boolean, Unit, String]((), "v")
    val manualOk: Boolean = manual.broadcast == Some("v") && manual.foci(true) == "v"

    // Broadcast ∘ broadcast stays index-free (the inner's half is reused verbatim).
    val chained = shim.andThen(shim)
    val chainOk: Boolean = chained.to(7).broadcast == Some(9)

    shimOk && tupleOk && manualOk && chainOk
  }

  /** A *positional* F1-carried optic over an `Int` whose leftover is real (`X = Int`) and whose
    * write reads it back — the shape that makes a borrowed/absent write context observable: a null
    * leftover would show up as a wrong value, and an absent one as an NPE. `from(to(s)) == s`, so
    * `modify(identity)` is the identity on it.
    */
  private def positionalProbe: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
    new Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]]:
      type X = Int
      def to(s: Int): MultiFocus[Function1[Int, *]][X, Int] = MultiFocus(s, (i: Int) => i * 10)
      def from(mf: MultiFocus[Function1[Int, *]][X, Int]): Int = mf.context + mf.foci(0)

  // covers: right-associated same-carrier `.andThen` — the inner is itself a composite, so its own
  // write context must be the one its read produced. A kernel that invents the inner leftover hands
  // it a null `AssocF1Z` and NPEs on the first write.
  "<iso shim>.andThen(<iso shim>.andThen(<iso shim>)): right-associated chain keeps its own context" >> {
    val idIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](identity, identity)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](idIso)
    val g = shim.andThen(shim.andThen(shim))

    val pointwise = forAll { (n: Int) => g.modify(_ + 1)(n) == n + 1 }
    val identityWrite = forAll { (n: Int) => g.modify(identity[Int])(n) == n }
    pointwise && identityWrite
  }

  // covers: right-associated composition where the innermost optic is a *positional* optic with a
  // real leftover it reads on write, under an index-free outer. The composite inner must be handed
  // the context its own read produced, and the outer's single-value write must thread through it.
  "<iso shim>.andThen(<positional probe>.andThen(<iso shim>)): leftover survives both levels" >> {
    val idIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](identity, identity)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](idIso)
    val plusOne: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](Iso[Int, Int, Int, Int](_ + 1, _ - 1))

    val inner = positionalProbe.andThen(plusOne)
    val innerOk = forAll { (n: Int) => inner.modify(identity[Int])(n) == n }

    val g = shim.andThen(inner)
    val outerOk = forAll { (n: Int) =>
      g.modify(identity[Int])(n) == n && g.modify(_ + 1)(n) == n + 1
    }
    innerOk && outerOk
  }

  /** Probe source for the `fromLensF` block: a carried function plus a component the write has to
    * restore from the leftover (a null leftover would show up as a dropped/`null` component).
    */
  private type FnPair = (Int => Int, String)

  // covers: a `fromLensF`-built inner carries a *real* leftover that its write reads back. An
  // invented one (null) makes the lens's rebuild NPE / drop the carried component; the kernel now
  // threads the leftover the read observed.
  "fromLensF inner under an iso shim: the lens's leftover survives the write" >> {
    val lens: Optic[FnPair, FnPair, Int => Int, Int => Int, Tuple2] =
      Lens[FnPair, Int => Int](p => p._1, (p, f) => (f, p._2))
    val inner: Optic[FnPair, FnPair, Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.fromLensF[Function1[Int, *], FnPair, FnPair, Int, Int](lens)
    val pairIso: Optic[FnPair, FnPair, FnPair, FnPair, Direct] =
      Iso[FnPair, FnPair, FnPair, FnPair](identity, identity)
    val outer: Optic[FnPair, FnPair, FnPair, FnPair, MultiFocus[Function1[Int, *]]] =
      broadcastOuter(pairIso)
    val g = outer.andThen(inner)

    val src: FnPair = ((i: Int) => i * 2, "keep")
    val out = g.modify(_ + 1)(src)
    (out._2 == "keep") && (out._1(3) == 7)
  }

  // covers: right-associated composition through an iso shim under a *tabulating* outer, where the
  // inner composite is written one position at a time — every position needs the leftover its own
  // read produced.
  "MultiFocus.tuple.andThen(<iso shim>.andThen(<iso shim>)): per-position write through a composite" >> {
    val tuple: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val idIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](identity, identity)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](idIso)
    val g = tuple.andThen(shim.andThen(shim))

    val replaceOk = forAll { (l: Int, r: Int, v: Int) => g.replace(v)((l, r)) == ((v, v)) }
    val modifyOk = forAll { (l: Int, r: Int) => g.modify(_ + 1)((l, r)) == ((l + 1, r + 1)) }
    replaceOk && modifyOk
  }

  // covers: tabulating ∘ tabulating — the composite reads one position per index (the shared index
  // set makes the two sides advance together), so its write must be the exact inverse of that read:
  // every position keeps what it was read at, and only the addressed position takes the new value.
  // This is the one composition whose read is not a bijection; it is lawful because `C =:= D` here,
  // which `Optic.andThen` detects and routes to the kernel's exact-inverse write. (On main the same
  // expression returned `((1, 2), (1, 2))` — the first row broadcast everywhere.)
  "MultiFocus.tuple.andThen(MultiFocus.tuple): diagonal read, in-place write (lawful)" >> {
    val outer: Optic[
      ((Int, Int), (Int, Int)),
      ((Int, Int), (Int, Int)),
      (Int, Int),
      (Int, Int),
      MultiFocus[Function1[Int, *]],
    ] = MultiFocus.tuple[((Int, Int), (Int, Int)), (Int, Int)]
    val inner: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val g = outer.andThen(inner)

    // MF1: the composite's write inverts its own read.
    val identityOk = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g.modify(identity[Int])(((a, b), (c, d))) == (((a, b), (c, d)))
    }
    // MF2: two modifies compose, and each write only touches the position it was given.
    val composeOk = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      val f: Int => Int = _ + 1
      val h: Int => Int = _ * 3
      g.modify(h)(g.modify(f)(((a, b), (c, d)))) == g.modify(f.andThen(h))(((a, b), (c, d)))
    }
    val inPlace = forAll { (a: Int, b: Int, c: Int, d: Int, v: Int) =>
      g.replace(v)(((a, b), (c, d))) == (((v, b), (c, v)))
    }
    // The diagonal is what the composite reads, and it is also all it writes back.
    val diagonal = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g.at(0)(((a, b), (c, d))) == a && g.at(1)(((a, b), (c, d))) == d
    }
    identityOk && composeOk && inPlace && diagonal
  }

  // covers: associativity of the same-carrier kernel across the shapes that need the two different
  // write entry points — an index-free shim inside (one collapsed value) and tabulating sides
  // (in-place diagonal write). Reads agree; the writes must land on the same values either way.
  "same-carrier composition is associative across shim / tabulating mixes" >> {
    val tuple2: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val pairs: Optic[
      ((Int, Int), (Int, Int)),
      ((Int, Int), (Int, Int)),
      (Int, Int),
      (Int, Int),
      MultiFocus[Function1[Int, *]],
    ] = MultiFocus.tuple[((Int, Int), (Int, Int)), (Int, Int)]
    val idIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](identity, identity)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](idIso)

    val left = pairs.andThen(tuple2.andThen(shim))
    val right = pairs.andThen(tuple2).andThen(shim)

    val sameRead = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      val s = ((a, b), (c, d))
      left.at(0)(s) == right.at(0)(s) && left.at(1)(s) == right.at(1)(s)
    }
    val sameWrite = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      val s = ((a, b), (c, d))
      left.modify(identity[Int])(s) == right.modify(identity[Int])(s) &&
      left.replace(7)(s) == right.replace(7)(s)
    }
    sameRead && sameWrite
  }

  /** A *polymorphic* tabulating F1-carried optic over an `Int`: it reads a real position, and its
    * write changes the focus type (`Int` → `String`), so no `Int => String` exists for a kernel to
    * put read values back with.
    */
  private def polyProbe: Optic[Int, Int, Int, String, MultiFocus[Function1[Int, *]]] =
    new Optic[Int, Int, Int, String, MultiFocus[Function1[Int, *]]]:
      type X = Unit
      def to(s: Int): MultiFocus[Function1[Int, *]][Unit, Int] =
        MultiFocus((), (i: Int) => s + i)
      def from(mf: MultiFocus[Function1[Int, *]][Unit, String]): Int = mf.foci(0).toInt

  // covers: the documented fallback — a tabulating ∘ tabulating composite whose write CHANGES the
  // focus type. The composite reads one position per index, so the read values are `C`, and the write
  // bundle is `D` with no `C => D` in sight; the kernel keeps its per-position value write and cannot
  // put the rest back. Pinned so the boundary between the exact (stable-focus) and lossy
  // (type-changing) writes stays where the docs say it is.
  "type-changing write through a tabulating ∘ tabulating composite: documented lossy fallback" >> {
    val outer: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] = positionalProbe
    val g = outer.andThen(polyProbe)

    // The read is `i => (i * 10) + i` on either index; `.modify(_.toString)` has no reader-side
    // inverse to fall back on, so each position the write touches takes the transformed value and the
    // outer's own rebuild (`context + foci(0)`) decides the result.
    g.modify(_.toString)(5) == 5
  }

  // covers: a *composite* inner whose innermost pair is stable (`tuple ∘ shim`) under a tabulating
  // outer. The stability witness rides on the composite — `Optic.andThen` mixes it in from the call
  // site's `C =:= D` — so the outer's diagonal write is still the exact inverse of its read: MF1
  // holds and `replace` touches the two positions the composite reads, not whole rows.
  "tabulating ∘ (tabulating ∘ shim): the composite inner carries the stability witness" >> {
    val pairs: Optic[
      ((Int, Int), (Int, Int)),
      ((Int, Int), (Int, Int)),
      (Int, Int),
      (Int, Int),
      MultiFocus[Function1[Int, *]],
    ] = MultiFocus.tuple[((Int, Int), (Int, Int)), (Int, Int)]
    val tuple2: Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int), Int]
    val idIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](identity, identity)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](idIso)

    val g = pairs.andThen(tuple2.andThen(shim))

    val identityOk = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g.modify(identity[Int])(((a, b), (c, d))) == (((a, b), (c, d)))
    }
    val inPlace = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g.replace(9)(((a, b), (c, d))) == (((9, b), (c, 9)))
    }
    val diagonal = forAll { (a: Int, b: Int, c: Int, d: Int) =>
      g.at(0)(((a, b), (c, d))) == a && g.at(1)(((a, b), (c, d))) == d
    }
    identityOk && inPlace && diagonal
  }

  // covers: the bespoke surface on the same carrier — bare `.modify` / `.replace` on a positional
  // factory, the `Representable`-gated `.at(i)` read, and both Functor-shaped collects.
  "positional factories: bare modify / replace / at / collect stay intact" >> {
    val tuple: Optic[(Int, Int, Int), (Int, Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int, Int), Int]
    val table: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[[a] =>> Boolean => a, Int]
    val intIso: Optic[Int, Int, Int, Int, Direct] = Iso[Int, Int, Int, Int](_ + 1, _ - 1)
    val shim: Optic[Int, Int, Int, Int, MultiFocus[Function1[Int, *]]] =
      broadcastOuter[Int, Int, Int, Int, Int](intIso)

    val tupleModify = forAll { (a: Int, b: Int, c: Int) =>
      tuple.modify(_ * 2)((a, b, c)) == ((a * 2, b * 2, c * 2))
    }
    val tupleReplace = forAll { (a: Int, b: Int, c: Int, v: Int) =>
      tuple.replace(v)((a, b, c)) == ((v, v, v))
    }
    val atOk = forAll { (l: Int, r: Int) =>
      val f: Boolean => Int = b => if b then l else r
      table.at(true)(f) == l && table.at(false)(f) == r
    }
    val collectOk = forAll { (l: Int, r: Int) =>
      val f: Boolean => Int = b => if b then l else r
      val summed = table.collectMap[Int](fa => fa(true) + fa(false))(f)
      val hinted = table.collectWith(fa => a => a + fa(true))(f)
      summed(true) == l + r && summed(false) == l + r &&
      hinted(true) == l + l && hinted(false) == r + l
    }
    val shimOk = forAll { (n: Int) =>
      shim.modify(_ * 2)(n) == (n + 1) * 2 - 1 && shim.replace(0)(n) == -1
    }

    tupleModify && tupleReplace && atOk && collectOk && shimOk
  }
