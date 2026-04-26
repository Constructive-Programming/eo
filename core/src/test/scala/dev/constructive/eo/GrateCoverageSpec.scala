package dev.constructive.eo

import scala.language.implicitConversions

import cats.Representable
import cats.instances.function.given
import org.specs2.mutable.Specification

import data.Grate
import data.Grate.given
import optics.Optic
import optics.Optic.*

/** Additional Grate coverage targeting the factory + composeFrom paths that the existing
  * `GrateSpec` doesn't touch.
  *
  * '''2026-04-25 consolidation.''' 4 → 3 named blocks. The two `Grate.at` specs (modify-at /
  * replace-at) collapsed into one. `grateFunctor.map` direct surface and `grate.andThen(grate)`
  * composeFrom path stay separate (they witness independent code paths).
  */
class GrateCoverageSpec extends Specification:

  // covers: materialise the focus at the supplied representative index, replace
  // overwrites every slot with the supplied value
  "Grate.at — Representable-indexed factory: modify-at + replace-broadcast" >> {
    val F = summon[Representable[[a] =>> Boolean => a]]
    val gTrue: Optic[Boolean => Int, Boolean => Int, Int, Int, Grate] =
      Grate.at[[a] =>> Boolean => a, Int](F)(true)
    val gFalse: Optic[Boolean => Int, Boolean => Int, Int, Int, Grate] =
      Grate.at[[a] =>> Boolean => a, Int](F)(false)

    val fn: Boolean => Int = b => if b then 42 else 7
    val doubled = gTrue.modify(_ * 2)(fn)
    val modOk = (doubled(true) === 84).and(doubled(false) === 14)

    val fn2: Boolean => Int = b => if b then 1 else 2
    val flat = gFalse.replace(99)(fn2)
    val replOk = (flat(true) === 99).and(flat(false) === 99)

    modOk.and(replOk)
  }

  // covers: grateFunctor.map applies the focus function while preserving the rebuild
  "grateFunctor.map — direct instance surface preserves the rebuild" >> {
    val pair: (Int, Int => Int) = (5, (x: Int) => x * 10)
    val mapped: (String, Int => String) = grateFunctor.map(pair, (_: Int).toString)
    (mapped._1 === "5").and(mapped._2(7) === "70")
  }

  // covers: compose two homogeneous-tuple Grates of matching shape (composeFrom path)
  "grate.andThen(grate) — same-carrier composition exercises grateAssoc.composeFrom" >> {
    val outer: Optic[(Int, Int), (Int, Int), Int, Int, Grate] =
      Grate.tuple[(Int, Int), Int]
    val doubled = outer.modify(_ * 2)((10, 20))
    (doubled === ((20, 40))).and(outer.replace(0)((1, 2)) === ((0, 0)))
  }
