package eo

import optics.Fold

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Minimal in-core spec whose job is to keep `sbt-stryker4s` useful
  * after the core / laws / tests split: stryker's sbt plugin runs
  * `core/test` (not `tests/test`), and the whole project has exactly
  * one stryker-reachable runtime expression — `Option(_).filter(p)` in
  * `Fold.select`. Pinning it here ensures the `filter -> filterNot`
  * mutation is detected. The richer behavioural/law suite lives in
  * `cats-eo-tests`.
  */
class FoldMutationSpec extends Specification with ScalaCheck:

  "Fold.select keeps matching values and drops the rest" >> {
    val evenFold = Fold.select[Int](_ % 2 == 0)
    forAll((n: Int) =>
      evenFold.to(n) == (if n % 2 == 0 then Some(n) else None)
    )
  }
