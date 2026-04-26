package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import io.circe.Json

/** Public surface forwarder for the two Json optic carriers, [[JsonPrism]] and [[JsonTraversal]].
  *
  * '''2026-04-26 unification.''' This trait used to support FOUR carriers (`JsonPrism`,
  * `JsonFieldsPrism`, `JsonTraversal`, `JsonFieldsTraversal`); after the [[JsonFocus]] rethink the
  * single-field / multi-field axis is internal to the focus and the four classes collapse to two.
  * The trait still factors the `modify` / `transform` / `place` / `transfer` (and `*Unsafe`)
  * forwarders out of the per-class bodies — both `JsonPrism` and `JsonTraversal` rely on it to
  * thread the `parseInput*` step uniformly.
  *
  * Concrete classes supply six per-Json hooks (three Ior-bearing, three silent); the trait wires up
  * the public surface in terms of those hooks.
  */
private[circe] trait JsonOpticOps[A]:

  // ---- Abstract members supplied by each carrier ---------------------

  protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json]
  protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json]
  protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json]

  protected def modifyImpl(json: Json, f: A => A): Json
  protected def transformImpl(json: Json, f: Json => Json): Json
  protected def placeImpl(json: Json, a: A): Json

  // ---- Default (Ior-bearing) surface --------------------------------

  def modify(f: A => A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => modifyIor(j, f))

  def transform(f: Json => Json): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => transformIor(j, f))

  def place(a: A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, a))

  def transfer[C](f: C => A): (Json | String) => C => Ior[Chain[JsonFailure], Json] =
    input => c => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, f(c)))

  // ---- *Unsafe (silent) escape hatches -------------------------------

  def modifyUnsafe(f: A => A): (Json | String) => Json =
    input => modifyImpl(JsonFailure.parseInputUnsafe(input), f)

  def transformUnsafe(f: Json => Json): (Json | String) => Json =
    input => transformImpl(JsonFailure.parseInputUnsafe(input), f)

  def placeUnsafe(a: A): (Json | String) => Json =
    input => placeImpl(JsonFailure.parseInputUnsafe(input), a)

  def transferUnsafe[C](f: C => A): (Json | String) => C => Json =
    input => c => placeImpl(JsonFailure.parseInputUnsafe(input), f(c))

/** Per-element accumulator for [[JsonTraversal]] — folds an `Ior`-bearing element rewrite over a
  * `Vector[Json]` and lifts the (chain, vector) pair to a single `Ior`.
  */
private[circe] object JsonTraversalAccumulator:

  /** Apply `readElement` to every element, accumulating the per-element `Ior`s into
    * `(Vector[A], Chain[JsonFailure])` and lifting at the end. `Ior.Right(a)` adds `a`,
    * `Ior.Both(c, a)` adds both, `Ior.Left(c)` drops the element and adds `c`.
    */
  def collectIor[A](
      arr: Vector[Json],
      readElement: Json => Ior[Chain[JsonFailure], A],
  ): Ior[Chain[JsonFailure], Vector[A]] =
    val (chain, result) =
      arr.foldLeft((Chain.empty[JsonFailure], Vector.empty[A])) {
        case ((chain, acc), elem) =>
          readElement(elem) match
            case Ior.Right(a)   => (chain, acc :+ a)
            case Ior.Left(c)    => (chain ++ c, acc)
            case Ior.Both(c, a) => (chain ++ c, acc :+ a)
      }
    if chain.isEmpty then Ior.Right(result) else Ior.Both(chain, result)

  /** Per-element rewrite with failure accumulation. Elements whose `elemUpdate` returns `Ior.Left`
    * are left unchanged in the output Vector; their failure(s) contribute to the accumulated chain.
    */
  def mapElementsIor(
      arr: Vector[Json],
      elemUpdate: Json => Ior[Chain[JsonFailure], Json],
  ): (Vector[Json], Chain[JsonFailure]) =
    arr.foldLeft((Vector.empty[Json], Chain.empty[JsonFailure])) {
      case ((acc, chain), elem) =>
        elemUpdate(elem) match
          case Ior.Right(j)   => (acc :+ j, chain)
          case Ior.Both(c, j) => (acc :+ j, chain ++ c)
          case Ior.Left(c)    => (acc :+ elem, chain ++ c)
    }
