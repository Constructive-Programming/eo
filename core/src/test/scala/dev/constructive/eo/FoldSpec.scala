package dev.constructive.eo

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import optics.Fold

/** In-core smoke spec that pins down `Fold.select`'s predicate semantics. The fuller behaviour /
  * law suites live in cats-eo-tests, but keeping this one-property spec in core lets `core/test`
  * stand on its own and keeps the `Option(_).filter(p)` line in `Fold.select` exercised without
  * needing the downstream test module.
  */
class FoldSpec extends Specification with ScalaCheck:

  "Fold.select keeps matching values and drops the rest" >> {
    val evenFold = Fold.select[Int](_ % 2 == 0)
    forAll((n: Int) => evenFold.to(n) == (if n % 2 == 0 then Some(n) else None))
  }
