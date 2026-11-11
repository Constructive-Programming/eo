package dev.constructive.eo

import data.{Affine, Direct, ModifyF, MultiFocus, PSVec}
import optics.{Getter, Iso, Lens, Modify, Optic, Optional, Prism, Traversal}

/** The small, hand-built optics the law matrix and the behaviour specs share. Framework-free (core
  * only), so the munit [[OpticLawsSuite]] — which runs on every platform — and the specs2 specs —
  * JVM and Scala.js only — read one definition.
  */
object OpticFixtures:

  /** Tuple swap `(Int, String) <-> (String, Int)`. */
  val swapIso: Optic[(Int, String), (Int, String), (String, Int), (String, Int), Direct] =
    Iso[(Int, String), (Int, String), (String, Int), (String, Int)](
      _.swap,
      _.swap,
    )

  val firstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
    Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

  val secondLens: Optic[(Int, String), (Int, String), String, String, Tuple2] =
    Lens[(Int, String), String](_._2, (s, a) => (s._1, a))

  /** Even Int, isomorphic to its half: `getOrModify(n) = Right(n / 2)` when even, else `Left(n)`;
    * `reverseGet(k) = k * 2`. Its laws hold on the whole Int range only if the domain is restricted
    * so the doubling never overflows (the suite restricts it to ±10000).
    */
  val evenPrism: Optic[Int, Int, Int, Int, Either] =
    Prism[Int, Int](
      n => if n % 2 == 0 then Right(n / 2) else Left(n),
      k => k * 2,
    )

  /** Head of a non-empty pair `(Int, List[Int])`. */
  val headOptional: Optic[(Int, List[Int]), (Int, List[Int]), Int, Int, Affine] =
    Optional[(Int, List[Int]), (Int, List[Int]), Int, Int](
      { case (h, _) => Right(h) },
      { case ((_, hs), h2) => (h2, hs) },
    )

  /** Maps `f` over both sides of a pair. */
  val pairModify: Optic[(Int, Int), (Int, Int), Int, Int, ModifyF] =
    Modify[(Int, Int), (Int, Int), Int, Int](f => { case (a, b) => (f(a), f(b)) })

  val listTraversal: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
    Traversal.each[List, Int]

  val firstGetter: Optic[(Int, String), Unit, Int, Unit, Direct] =
    Getter[(Int, String), Int](_._1)
