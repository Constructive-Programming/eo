package dev.constructive.eo.circe

import scala.language.implicitConversions

import cats.data.Ior
import io.circe.syntax.*
import org.specs2.mutable.Specification

/** Index-bounds discrimination for the array walk: the existing specs exercise out-of-range-high on
  * a 1-element array and index 0 on non-arrays, but never a SUCCESSFUL walk at indices 0 and >0 of
  * one array, nor a negative index — so operator mutants on the `idx < 0 || idx >= arr.length`
  * check survived.
  */
class JsonIndexBoundsSpec extends Specification:

  import JsonSpecFixtures.*

  // covers: JsonWalk.walkStep Index bounds (JsonWalk.scala:62), every operator variant —
  // `idx < 0` weakened to `> 0` breaks at(1), to `<= 0` breaks at(0); a weakened
  // `idx >= length` lets at(3)/at(-1) walk off the array.
  "indices 0 and 1 read their elements; -1 and length fail IndexOutOfRange" >> {
    val basket = Basket("Alice", Vector(Order("A"), Order("B"), Order("C"))).asJson
    def run(i: Int) = codecPrism[Basket].items.at(i).modify(identity)(basket)
    val valid = (run(0), run(1)) match
      case (Ior.Right(_), Ior.Right(_)) => true
      case _                            => false
    def oob(i: Int) = run(i) match
      case Ior.Both(chain, _) =>
        chain.headOption.get == JsonFailure.IndexOutOfRange(PathStep.Index(i), 3)
      case _ => false
    (valid, oob(-1), oob(3)) must beEqualTo((true, true, true))
  }
