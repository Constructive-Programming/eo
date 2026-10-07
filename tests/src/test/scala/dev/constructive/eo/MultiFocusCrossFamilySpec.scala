package dev.constructive.eo

import cats.instances.list.given
import dev.constructive.eo.compose.*
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import MatrixFixtures.Box
import data.{Direct, Forget, ModifyF, MultiFocus}
import data.MultiFocus.{collectList, collectMap, collectWith}
import optics.*
import optics.Optic.*

/** The runtime twin of [[CompositionMatrixSpec]]: that spec pins which (outer family ∘ inner
  * family) cells *type*, this one runs `MultiFocus[List]`-carried optics against every other family
  * they can be built from or composed with and checks the behaviour those cells promise.
  *
  *   - One block per inbound provenance (generic factory, Iso, Lens, Prism, Optional) runs the
  *     optic laws (`modify`-identity, compose-modify, replace-idempotent, replace-is-map) together
  *     with the aggregation definitions (`collectMap` / `collectWith` are `F.map` over the foci,
  *     `collectList` supplies a singleton focus vector to reconstruction) and the read surface
  *     (`foldMap` / `headOption` / `length`). A provenance whose write needs something the
  *     aggregation cannot supply fails *here*.
  *   - Both composition directions are swept across the families, with an independently computed
  *     expectation rather than a re-statement of the optic's own definition.
  *   - The *outbound* direction is swept too: every writable provenance projected into `ModifyF`
  *     (writes and laws must match the source optic), the polymorphic factory with a type-changing
  *     write, the generic `Function1` container, and the read-only `Forget[List]` escape over a
  *     `Fold`-sourced optic (available on the `T = Unit` shape).
  *   - Fixed-index full-grid rebuilding is tested separately as `Indexed`, with no implicit
  *     cross-family bridges.
  */
class MultiFocusCrossFamilySpec extends Specification with ScalaCheck:

  private type MFL = MultiFocus[List]

  private given Arbitrary[Box[Int]] = Arbitrary(Arbitrary.arbitrary[Int].map(Box(_)))

  // ------------------------------------------------------------------
  // One `MultiFocus[List]` optic per inbound family, all over a plain `List[Int]` so the collapsing
  // `.collectList` applies (it demands `S =:= List[A]` and `T =:= List[B]`).
  // ------------------------------------------------------------------

  private val fromGeneric: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus.apply[List, Int]

  /** A Lens whose write *reads* its own leftover (the `s.isEmpty` test), which is exactly the shape
    * a forged leftover turns into an NPE.
    */
  private val fromLens: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus.fromLensF[List, List[Int], List[Int], Int, Int](
      Lens[List[Int], List[Int]](identity, (s, m) => if s.isEmpty then m else m)
    )

  /** A "prism" that always hits: lawful, and a `Right` leftover its write has to read to know how
    * the source is put back.
    */
  private val fromPrism: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus.fromPrismF[List, List[Int], List[Int], Int, Int](
      Prism[List[Int], List[Int]](xs => Right(xs), identity)
    )

  private val fromOptional: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus.fromOptionalF[List, List[Int], List[Int], Int, Int](
      Optional[List[Int], List[Int], List[Int], List[Int]](xs => Right(xs), sb => sb._2)
    )

  /** The `Direct` bridge: it keeps the focus type and wraps it in a singleton `List`, so its optic
    * is over `List[Int]` with foci `List[List[Int]]` — a shape of its own, pinned in its own block.
    */
  private val fromIsoShim: Optic[List[Int], List[Int], List[Int], List[Int], MFL] =
    summon[Composer[Direct, MFL]].to[List[Int], List[Int], List[Int], List[Int]](
      Iso[List[Int], List[Int], List[Int], List[Int]](identity, identity)
    )

  /** The laws and definitions every `MultiFocus[List]` optic must satisfy, whatever built it. */
  private def battery(o: Optic[List[Int], List[Int], Int, Int, MFL]): Prop =
    val modifyIdentity = forAll { (xs: List[Int]) => o.modify(identity[Int])(xs) == xs }
    val composeModify = forAll { (xs: List[Int]) =>
      val f: Int => Int = _ + 1
      val g: Int => Int = _ * 3
      o.modify(g)(o.modify(f)(xs)) == o.modify(f.andThen(g))(xs)
    }
    val replaceIdempotent = forAll { (xs: List[Int], b: Int) =>
      o.replace(b)(o.replace(b)(xs)) == o.replace(b)(xs)
    }
    val replaceIsMap = forAll { (xs: List[Int], b: Int) => o.replace(b)(xs) == xs.map(_ => b) }
    val mapShaped = forAll { (xs: List[Int]) =>
      val agg: List[Int] => Int = _.sum
      o.collectMap[Int](agg)(xs) == xs.map(_ => agg(xs)) &&
      o.collectWith(fa => a => agg(fa) + a)(xs) == xs.map(a => agg(xs) + a) &&
      o.collectWith(_ => (a: Int) => a + 1)(xs) == o.modify(_ + 1)(xs)
    }
    val read = forAll { (xs: List[Int]) =>
      o.foldMap[Int](identity)(xs) == xs.sum &&
      o.headOption(xs) == xs.headOption &&
      o.length(xs) == xs.length
    }
    modifyIdentity && composeModify && replaceIdempotent && replaceIsMap && mapShaped && read

  /** A singleton source is guaranteed only for reconstructions that support focus-count changes.
    * Composites and Prism misses preserve context instead; their boundaries are tested below.
    */
  private def collapses(o: Optic[List[Int], List[Int], Int, Int, MFL]): Prop =
    forAll { (xs: List[Int]) => o.collectList(_.sum)(xs) == List(xs.sum) }

  "inbound provenance — generic factory: MF laws + collect* + read surface" >> (battery(
    fromGeneric
  ) && collapses(fromGeneric))

  /** A `MultiFocus[List]` optic that is itself a *composite* of two shipped optics, so its context
    * records per-focus structure (`Z = (Xo, F[(Xi, Int)])`) rather than a single leftover.
    */
  private val fromComposite: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus
      .apply[List, Int]
      .andThen(Lens[Int, Int](identity, (_, m) => m))

  "inbound provenance — composite of two shipped optics: MF laws + collect* + read surface" >>
    battery(fromComposite)

  "collectList boundary — empty composite retains its per-focus context" >> {
    Prop(fromComposite.collectList(_.sum)(Nil) == Nil)
  }

  /** Lawful partial Prism: a hit removes the leading zero and reconstruction puts it back. */
  private val fromPrependZeroPrism: Optic[List[Int], List[Int], Int, Int, MFL] =
    MultiFocus.fromPrismF[List, List[Int], List[Int], Int, Int](
      Prism[List[Int], List[Int]](
        {
          case 0 :: tail => Right(tail)
          case xs        => Left(xs)
        },
        0 :: _
      )
    )

  "collectList boundary — prepend-zero Prism preserves misses, including an empty source" >> {
    val emptyMiss = fromPrependZeroPrism.collectList(_.sum)(Nil) == Nil
    val nonEmptyMiss = forAll { (tail: List[Int]) =>
      val source = 1 :: tail
      fromPrependZeroPrism.collectList(_.sum)(source) == source
    }
    Prop(emptyMiss) && nonEmptyMiss
  }

  "collectList boundary — prepend-zero Prism rebuilds hits around the singleton focus vector" >> {
    val emptyHit = fromPrependZeroPrism.collectList(_.sum)(List(0)) == List(0, 0)
    val hits = forAll { (tail: List[Int]) =>
      fromPrependZeroPrism.collectList(_.sum)(0 :: tail) == List(0, tail.sum)
    }
    Prop(emptyHit) && hits
  }

  // covers: the `Direct` bridge, whose foci are a singleton of the source value — the morph's write
  // picks it back out (documented), so the properties are stated on that shape rather than
  // element-wise.
  "inbound provenance — Iso bridge (Direct): singleton foci obey MF1 / replace / read" >> {
    val identityOk = forAll { (xs: List[Int]) =>
      fromIsoShim.modify(identity[List[Int]])(xs) == xs
    }
    val replaceOk = forAll { (xs: List[Int], b: List[Int]) => fromIsoShim.replace(b)(xs) == b }
    val readOk = forAll { (xs: List[Int]) =>
      fromIsoShim.foldMap[List[Int]](identity)(xs) == xs && fromIsoShim.length(xs) == 1
    }
    identityOk && replaceOk && readOk
  }

  "inbound provenance — Lens bridge (Tuple2): MF laws + collect* + read surface" >> (battery(
    fromLens
  ) && collapses(fromLens))

  "inbound provenance — Prism bridge (Either): MF laws + collect* + read surface" >> (battery(
    fromPrism
  ) && collapses(fromPrism))

  "inbound provenance — Optional bridge (Affine): MF laws + collect* + read surface" >> (battery(
    fromOptional
  ) && collapses(fromOptional))

  // covers: the polymorphic factory, whose read and write types differ — the properties are the
  // pointwise results rather than MF1 (which needs S = T).
  "inbound provenance — polymorphic factory: pointwise modify with a type change" >> {
    val o: Optic[List[Int], List[String], Int, String, MFL] = MultiFocus.pApply[List, Int, String]
    val pointwise = forAll { (xs: List[Int]) => o.modify(_.toString)(xs) == xs.map(_.toString) }
    val collapsing = forAll { (xs: List[Int]) =>
      o.collectList(_.sum.toString)(xs) == List(xs.sum.toString)
    }
    pointwise && collapsing
  }

  // ------------------------------------------------------------------
  // Composition, both directions. Sources and inners are the `Box` shapes `MatrixFixtures`
  // (CompositionMatrixSpec) already uses, so this sweep runs on the cells the matrix pins.
  // ------------------------------------------------------------------

  /** A `Box`-sourced lens whose write reads its leftover, so the `MultiFocus[List]` outer is built
    * by a bridge whose write is not leftover-free either.
    */
  private val boxLens
      : Optic[Box[List[Box[Int]]], Box[List[Box[Int]]], List[Box[Int]], List[Box[Int]], Tuple2] =
    Lens[Box[List[Box[Int]]], List[Box[Int]]](_.a, (s, m) => s.copy(a = m))

  private val mfOuter: Optic[Box[List[Box[Int]]], Box[List[Box[Int]]], Box[Int], Box[Int], MFL] =
    MultiFocus.fromLensF[List, Box[List[Box[Int]]], Box[List[Box[Int]]], Box[Int], Box[Int]](
      boxLens
    )

  // covers: a MultiFocus[List] outer composed with one inner per family it can morph into the carrier
  // (Iso via Direct, Lens via Tuple2, Prism via Either, Optional via Affine) — the MF laws plus an
  // independently computed element-wise expectation and a read-side sum.
  "MultiFocus[List] outer ∘ each family inner: MF laws + element-wise expectation" >> {
    val viaIso = mfOuter.andThen(Iso[Box[Int], Box[Int], Int, Int](_.a, Box(_)))
    val viaLens = mfOuter.andThen(Lens[Box[Int], Int](_.a, (_, m) => Box(m)))
    val viaPrism = mfOuter.andThen(Prism[Box[Int], Int](b => Right(b.a), Box(_)))
    val viaOptional =
      mfOuter.andThen(Optional[Box[Int], Box[Int], Int, Int](b => Right(b.a), sb => Box(sb._2)))

    def pointwise(o: Optic[Box[List[Box[Int]]], Box[List[Box[Int]]], Int, Int, MFL]): Prop =
      forAll { (bs: List[Box[Int]]) =>
        val s = Box(bs)
        o.modify(_ + 1)(s) == Box(bs.map(b => Box(b.a + 1))) &&
        o.modify(identity[Int])(s) == s &&
        o.foldMap[Int](identity)(s) == bs.map(_.a).sum
      }

    pointwise(viaIso) && pointwise(viaLens) && pointwise(viaPrism) && pointwise(viaOptional)
  }

  // ------------------------------------------------------------------
  // Outbound: a MultiFocus optic handed *to* every family it can reach. The provenance blocks above
  // pin "built from a family" (the inbound direction); these pin the other one, and each projection
  // is checked against the source optic so it cannot drift from the behaviour it projects.
  // ------------------------------------------------------------------

  /** `MultiFocus[List] → ModifyF` — the write-only projection, for every writable provenance. */
  private def modifyDirection(o: Optic[List[Int], List[Int], Int, Int, MFL]): Prop =
    val lifted: Optic[List[Int], List[Int], Int, Int, ModifyF] =
      summon[Composer[MFL, ModifyF]].to(o)
    val sameWrites = forAll { (xs: List[Int]) =>
      lifted.modify(_ + 1)(xs) == o.modify(_ + 1)(xs) &&
      lifted.replace(7)(xs) == o.replace(7)(xs)
    }
    val laws = forAll { (xs: List[Int]) =>
      val f: Int => Int = _ + 1
      val g: Int => Int = _ * 3
      lifted.modify(identity[Int])(xs) == xs &&
      lifted.modify(g)(lifted.modify(f)(xs)) == lifted.modify(f.andThen(g))(xs)
    }
    sameWrites && laws

  "outbound — ModifyF projection of the generic factory: writes match the source" >> modifyDirection(
    fromGeneric
  )

  "outbound — ModifyF projection of the Lens bridge: writes match the source" >> modifyDirection(
    fromLens
  )

  "outbound — ModifyF projection of the Prism bridge: writes match the source" >> modifyDirection(
    fromPrism
  )

  "outbound — ModifyF projection of the Optional bridge: writes match the source" >> modifyDirection(
    fromOptional
  )

  "outbound — ModifyF projection of a composite: writes match the source" >> modifyDirection(
    fromComposite
  )

  // covers: the polymorphic factory into ModifyF — the projection has to carry a type-changing write,
  // not just an endomorphism.
  "outbound — ModifyF projection of the polymorphic factory: type-changing write" >> {
    val o: Optic[List[Int], List[String], Int, String, MFL] = MultiFocus.pApply[List, Int, String]
    val lifted: Optic[List[Int], List[String], Int, String, ModifyF] =
      summon[Composer[MFL, ModifyF]].to(o)
    forAll { (xs: List[Int]) =>
      lifted.modify(_.toString)(xs) == o.modify(_.toString)(xs) && lifted.modify(_.toString)(
        xs
      ) == xs.map(_.toString)
    }
  }

  // covers: `MultiFocus[List] → Forget[List]` — the read-only escape, available on the `T = Unit`
  // shape (a Forget-carried optic has no write). Its source is a `Fold`, so this is also the one
  // place the fold family meets the carrier: the escape must read exactly what the fold and the
  // carrier read.
  "outbound — Forget[List] escape over a Fold-sourced optic: read matches the source" >> {
    val mfFold: Optic[List[Int], Unit, Int, Unit, MFL] =
      summon[Composer[Forget[List], MFL]].to(Fold[List, Int])
    val escaped: Optic[List[Int], Unit, Int, Unit, Forget[List]] =
      summon[Composer[MFL, Forget[List]]].to(mfFold)
    forAll { (xs: List[Int]) =>
      escaped.to(xs).value == mfFold.to(xs).foci &&
      escaped.to(xs).value == Fold[List, Int].to(xs).value &&
      escaped.to(xs).value.sum == xs.sum
    }
  }

  // Generic Functor projection remains lawful for a function container; this is not a
  // tabulating Grate factory or a dedicated Function1 composition kernel.
  "outbound — generic Function1 container projection preserves pointwise writes" >> {
    val container = MultiFocus.apply[Function1[Boolean, *], Int]
    val lifted = summon[Composer[MultiFocus[Function1[Boolean, *]], ModifyF]].to(container)
    forAll { (a: Int, b: Int) =>
      val source: Boolean => Int = i => if i then a else b
      List(false, true).forall(i =>
        lifted.modify(_ + 1)(source)(i) == container.modify(_ + 1)(source)(i) &&
          lifted.replace(0)(source)(i) == container.replace(0)(source)(i),
      )
    }
  }

  // covers: the other direction — a family outer with a MultiFocus[List] inner, morphed through the
  // same carrier. Sources are singletons: the inbound `Direct` / `Tuple2` morphs pick the singleton
  // focus out on the way back (documented), so a multi-focus write is out of their contract.
  "each family outer ∘ MultiFocus[List] inner: MF laws + pointwise result" >> {
    val viaIso = Iso[Box[List[Int]], Box[List[Int]], List[Int], List[Int]](_.a, Box(_))
      .andThen(MultiFocus.apply[List, Int])
    val viaLens = Lens[Box[List[Int]], List[Int]](_.a, (_, m) => Box(m))
      .andThen(MultiFocus.apply[List, Int])

    def singleton(o: Optic[Box[List[Int]], Box[List[Int]], Int, Int, MFL]): Prop =
      forAll { (n: Int) =>
        val s = Box(List(n))
        o.modify(_ + 1)(s) == Box(List(n + 1)) &&
        o.modify(identity[Int])(s) == s &&
        o.foldMap[Int](identity)(s) == n
      }

    singleton(viaIso) && singleton(viaLens)
  }
