package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import io.circe.Json

/** Shared public surface for the four Json optic carriers — [[JsonPrism]], [[JsonFieldsPrism]],
  * [[JsonTraversal]], and [[JsonFieldsTraversal]]. Every one of those classes exposes the same
  * `modify` / `transform` / `place` / `transfer` Ior-bearing entry points and the matching
  * `*Unsafe` escape hatches; this trait holds those forwarders once and demands the per-class
  * `*Ior` / `*Impl` internals as abstract members.
  *
  * The trait deliberately ''does not'' include `get` / `getAll` / `getOption*` — those return
  * different result types (single `A` vs `Vector[A]` vs `Option[A]`) and live on the concrete
  * classes.
  *
  * Each forwarder is a one-line delegate that runs `JsonFailure.parseInput*` on the input and
  * threads the resulting `Json` through the per-class `*Ior` / `*Impl` body. There is no
  * behavioural change relative to the pre-refactor hand-written copies in each class.
  */
private[circe] trait JsonOpticOps[A]:

  // ---- Abstract members supplied by each carrier ---------------------

  /** Ior-bearing focus rewrite — drives the default `modify` surface. */
  protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json]

  /** Ior-bearing raw-Json rewrite — drives the default `transform` surface. */
  protected def transformIor(json: Json, f: Json => Json): Ior[Chain[JsonFailure], Json]

  /** Ior-bearing constant-place — drives the default `place` and `transfer` surfaces. */
  protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json]

  /** Silent (input-on-failure) focus rewrite — drives the `modifyUnsafe` escape hatch. */
  protected def modifyImpl(json: Json, f: A => A): Json

  /** Silent raw-Json rewrite — drives the `transformUnsafe` escape hatch. */
  protected def transformImpl(json: Json, f: Json => Json): Json

  /** Silent constant-place — drives the `placeUnsafe` and `transferUnsafe` escape hatches. */
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
