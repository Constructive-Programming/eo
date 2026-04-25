package dev.constructive.eo.circe

import cats.data.{Chain, Ior}
import io.circe.{Encoder, Json, JsonObject}

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

/** Shared field-level helpers for the multi-field Json optic carriers — [[JsonFieldsPrism]]
  * and [[JsonFieldsTraversal]]. Both classes carry `fieldNames` (the SELECTOR-order field
  * list) and `encoder: Encoder[A]` (where `A` is a Scala 3 NamedTuple), and both perform the
  * same per-field walks: read (`readFields`), assemble-with-Null-fallback (`buildSubObject`),
  * write through the encoder (`writeFields`), and overlay a foreign sub-object
  * (`overlayFields`).
  *
  * Routing through this trait collapses four near-identical hand-written helpers per class.
  */
private[circe] trait JsonFieldsBuilder[A]:

  /** SELECTOR-order list of field names this multi-field carrier focuses.
    *
    * `private[circe]` matches the access level the implementing classes already use for
    * their constructor `val`s — a `protected` declaration here would force them to widen
    * their access modifier, breaking the existing intra-package API.
    */
  private[circe] def fieldNames: Array[String]

  /** Codec for the synthesised NamedTuple `A`. Same access-level rationale as
    * [[fieldNames]].
    */
  private[circe] def encoder: Encoder[A]

  /** Read each selected field from `obj`. Returns either the assembled sub-object (for
    * decoder feed) or a Chain of per-field failures. Missing fields accumulate as
    * `JsonFailure.PathMissing`.
    */
  protected def readFields(obj: JsonObject): Either[Chain[JsonFailure], JsonObject] =
    val (chain, sub) = fieldNames.toVector.foldLeft((Chain.empty[JsonFailure], JsonObject.empty)) {
      case ((chain, sub), name) =>
        obj(name) match
          case None    => (chain :+ JsonFailure.PathMissing(PathStep.Field(name)), sub)
          case Some(v) => (chain, sub.add(name, v))
    }
    Either.cond(chain.isEmpty, sub, chain)

  /** Pick the selected fields out of `obj` into a fresh JsonObject. Missing fields become
    * `Json.Null` entries — the caller decides whether that's acceptable (the decode-path
    * checks for missing-ness explicitly via `readFields`).
    */
  protected def buildSubObject(obj: JsonObject): JsonObject =
    fieldNames.foldLeft(JsonObject.empty) { (sub, name) =>
      sub.add(name, obj(name).getOrElse(Json.Null))
    }

  /** Given a computed NamedTuple `a`, produce a new parent JsonObject with each selected
    * field overlaid. The encoder runs once; the resulting JsonObject is split back into
    * individual `(name, Json)` pairs that overlay onto `parent` via successive
    * `JsonObject.add` calls. Encoders that silently omit a field leave the corresponding
    * entry on `parent` untouched (matches the pre-FP body's silent tolerance).
    */
  protected def writeFields(parent: JsonObject, a: A): JsonObject =
    val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
    fieldNames.foldLeft(parent) { (out, name) =>
      encoded(name).fold(out)(v => out.add(name, v))
    }

  /** Overlay the fields of `newSub` onto `parent`. Same shape as `writeFields` but starting
    * from a `JsonObject` produced by `f` in `transform` (rather than re-encoding an `A`).
    */
  protected def overlayFields(parent: JsonObject, newSub: JsonObject): JsonObject =
    fieldNames.foldLeft(parent) { (out, name) =>
      newSub(name).fold(out)(v => out.add(name, v))
    }

/** Free-floating utility for the per-element accumulation used by both [[JsonTraversal]]
  * and [[JsonFieldsTraversal]]. Both `getAllIor` bodies look the same after the per-class
  * prefix walk: `arr.foldLeft((empty Chain, empty Vector)) { ... readElement ... }` and a
  * post-fold `Ior.Right` / `Ior.Both` lift. Routing through this helper deduplicates the
  * fold + lift pair.
  */
private[circe] object JsonTraversalAccumulator:

  /** Apply `readElement` to every element of `arr`, accumulating the per-element `Ior`s
    * into a `(Vector[A], Chain[JsonFailure])` and lifting the pair to a single
    * `Ior[Chain[JsonFailure], Vector[A]]` at the end.
    *
    *   - `Ior.Right(a)` → element contributes `a`, no failure.
    *   - `Ior.Both(c, a)` → element contributes `a`, also adds `c` to the chain.
    *   - `Ior.Left(c)` → element drops out of the result Vector, adds `c` to the chain.
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

  /** Per-element rewrite with failure accumulation. Elements whose `elemUpdate` returns
    * `Ior.Left` are left unchanged in the output Vector; their failure(s) contribute to the
    * accumulated chain. Used by both `JsonTraversal.modifyIor` / `…transformIor` /
    * `…placeIor` and `JsonFieldsTraversal`'s equivalents.
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
