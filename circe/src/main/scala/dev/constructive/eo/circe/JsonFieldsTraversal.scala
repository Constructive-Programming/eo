package dev.constructive.eo.circe

import scala.language.dynamics

import cats.data.{Chain, Ior}
import io.circe.{Decoder, Encoder, Json, JsonObject}

/** Multi-field sibling of [[JsonTraversal]] — iterates an array and focuses a Scala 3 NamedTuple on
  * every element. Produced by `JsonTraversal.fields(_.a, _.b, ...)` (see the `.fields` extension in
  * the [[JsonTraversal]] companion).
  *
  * Storage:
  *
  * {{{
  *   prefix     : Array[PathStep]  // root → iterated array
  *   elemParent : Array[PathStep]  // per-element root → sub-object carrying the selected fields
  *                                  // (empty when elements themselves carry the fields, which is
  *                                  //  the common case)
  *   fieldNames : Array[String]    // selected fields in SELECTOR order
  *   encoder    : Encoder[A]       // codec for the NamedTuple A
  *   decoder    : Decoder[A]
  * }}}
  *
  * Accumulation semantics (D4): per-element NamedTuple atomicity, same as [[JsonFieldsPrism]]. If
  * element 5 has two selected fields missing, element 5 is left unchanged in the output and *two*
  * JsonFailures contribute to the chain. Across elements, the chain accumulates naturally. A single
  * call on a size-N array with k failing fields per bad element yields a Chain of length Σ (failing
  * fields per bad element).
  *
  * Prefix failures return `Ior.Left(chain-of-one)` — nothing to iterate.
  */
final class JsonFieldsTraversal[A] private[circe] (
    private[circe] val prefix: Array[PathStep],
    private[circe] val elemParent: Array[PathStep],
    private[circe] val fieldNames: Array[String],
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends JsonOpticOps[A],
      Dynamic:

  // ---- Read surface (multi-focus specific) --------------------------

  def getAll(input: Json | String): Ior[Chain[JsonFailure], Vector[A]] =
    JsonFailure.parseInputIor(input).flatMap(getAllIor)

  inline def getAllUnsafe(input: Json | String): Vector[A] =
    getAllUnsafeImpl(JsonFailure.parseInputUnsafe(input))

  // ---- Shared helpers ---------------------------------------------

  /** Walk `prefix` to the iterated array. Returns the array paired with the parents Vector
    * collected along the way (used by [[rebuildRoot]] to splice the new array back through).
    */
  private def walkPrefix(json: Json): Either[JsonFailure, (Vector[Json], Vector[AnyRef])] =
    JsonWalk.walkPath(json, prefix).flatMap {
      case (cur, parents) =>
        cur
          .asArray
          .toRight(JsonFailure.NotAnArray(JsonWalk.terminalOf(prefix)))
          .map(arr => (arr, parents))
    }

  /** Walk elemParent on a single element to the field-bearing parent JsonObject. Returns the parent
    * paired with the per-step parents Vector for the rebuild side.
    */
  private def walkElemParent(
      elemJson: Json
  ): Either[JsonFailure, (JsonObject, Vector[AnyRef])] =
    JsonWalk.walkPath(elemJson, elemParent).flatMap {
      case (cur, parents) =>
        cur
          .asObject
          .toRight(JsonFailure.NotAnObject(JsonWalk.terminalOf(elemParent)))
          .map(obj => (obj, parents))
    }

  /** Read each selected field of `obj` into a Chain (failures) or a sub-JsonObject (success). */
  private def readFields(obj: JsonObject): Either[Chain[JsonFailure], JsonObject] =
    val (chain, sub) = fieldNames.toVector.foldLeft((Chain.empty[JsonFailure], JsonObject.empty)) {
      case ((chain, sub), name) =>
        obj(name) match
          case None    => (chain :+ JsonFailure.PathMissing(PathStep.Field(name)), sub)
          case Some(v) => (chain, sub.add(name, v))
    }
    Either.cond(chain.isEmpty, sub, chain)

  private def buildSubObject(obj: JsonObject): JsonObject =
    fieldNames.foldLeft(JsonObject.empty) { (sub, name) =>
      sub.add(name, obj(name).getOrElse(Json.Null))
    }

  private def writeFields(parent: JsonObject, a: A): JsonObject =
    val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
    fieldNames.foldLeft(parent) { (out, name) =>
      encoded(name).fold(out)(v => out.add(name, v))
    }

  /** Overlay the fields of `newSub` onto `parent`. Same shape as [[writeFields]] but starting from
    * a `JsonObject` produced by `f` in `transform`.
    */
  private def overlayFields(parent: JsonObject, newSub: JsonObject): JsonObject =
    fieldNames.foldLeft(parent) { (out, name) =>
      newSub(name).fold(out)(v => out.add(name, v))
    }

  /** Rebuild the elem after updating the terminal JsonObject at the end of `elemParent`. When
    * `elemParent` is empty the new parent IS the new elem, no fold needed.
    */
  private def rebuildElem(newParent: JsonObject, parents: Vector[AnyRef]): Json =
    JsonWalk.rebuildPath(parents, elemParent, Json.fromJsonObject(newParent))

  /** Rebuild the root after updating the iterated array. */
  private def rebuildRoot(newArr: Vector[Json], parents: Vector[AnyRef]): Json =
    JsonWalk.rebuildPath(parents, prefix, Json.fromValues(newArr))

  // ---- *Unsafe impls -----------------------------------------------

  override protected def modifyImpl(json: Json, f: A => A): Json =
    walkPrefix(json) match
      case Left(_)                     => json
      case Right((arr, prefixParents)) =>
        rebuildRoot(arr.map(updateElementDecodedUnsafe(_, f)), prefixParents)

  override protected def transformImpl(json: Json, f: Json => Json): Json =
    walkPrefix(json) match
      case Left(_)                     => json
      case Right((arr, prefixParents)) =>
        rebuildRoot(arr.map(updateElementRawUnsafe(_, f)), prefixParents)

  override protected def placeImpl(json: Json, a: A): Json =
    walkPrefix(json) match
      case Left(_)                     => json
      case Right((arr, prefixParents)) =>
        rebuildRoot(arr.map(placeElementUnsafe(_, a)), prefixParents)

  private def getAllUnsafeImpl(json: Json): Vector[A] =
    walkPrefix(json) match
      case Left(_)         => Vector.empty
      case Right((arr, _)) => arr.flatMap(readElementUnsafe)

  private def updateElementDecodedUnsafe(elemJson: Json, f: A => A): Json =
    walkElemParent(elemJson)
      .flatMap {
        case (obj, parents) =>
          readFields(obj).left.map(_ => Chain.empty).flatMap { sub =>
            Json
              .fromJsonObject(sub)
              .as[A](using decoder)
              .left
              .map(_ => Chain.empty[JsonFailure])
              .map(a => rebuildElem(writeFields(obj, f(a)), parents))
          }
      }
      .getOrElse(elemJson)

  private def updateElementRawUnsafe(elemJson: Json, f: Json => Json): Json =
    walkElemParent(elemJson) match
      case Left(_)               => elemJson
      case Right((obj, parents)) =>
        f(Json.fromJsonObject(buildSubObject(obj)))
          .asObject
          .map(newSub => rebuildElem(overlayFields(obj, newSub), parents))
          .getOrElse(elemJson)

  private def placeElementUnsafe(elemJson: Json, a: A): Json =
    walkElemParent(elemJson) match
      case Left(_)               => elemJson
      case Right((obj, parents)) => rebuildElem(writeFields(obj, a), parents)

  private def readElementUnsafe(elemJson: Json): Option[A] =
    walkElemParent(elemJson).toOption.flatMap {
      case (obj, _) =>
        readFields(obj)
          .toOption
          .flatMap(sub => Json.fromJsonObject(sub).as[A](using decoder).toOption)
    }

  // ---- Ior-bearing impls ------------------------------------------

  override protected def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    walkPrefix(json) match
      case Left(failure)               => Ior.Left(Chain.one(failure))
      case Right((arr, prefixParents)) =>
        val (newArr, chain) = mapElementsIor(arr, updateElementDecodedIor(_, f))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  override protected def transformIor(
      json: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    walkPrefix(json) match
      case Left(failure)               => Ior.Left(Chain.one(failure))
      case Right((arr, prefixParents)) =>
        val (newArr, chain) = mapElementsIor(arr, updateElementRawIor(_, f))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  override protected def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    walkPrefix(json) match
      case Left(failure)               => Ior.Left(Chain.one(failure))
      case Right((arr, prefixParents)) =>
        val (newArr, chain) = mapElementsIor(arr, placeElementIor(_, a))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  private def getAllIor(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    walkPrefix(json) match
      case Left(failure)   => Ior.Left(Chain.one(failure))
      case Right((arr, _)) =>
        val (chain, result) =
          arr.foldLeft((Chain.empty[JsonFailure], Vector.empty[A])) {
            case ((chain, acc), elem) =>
              readElementIor(elem) match
                case Ior.Right(a)   => (chain, acc :+ a)
                case Ior.Left(c)    => (chain ++ c, acc)
                case Ior.Both(c, a) => (chain ++ c, acc :+ a)
          }
        if chain.isEmpty then Ior.Right(result) else Ior.Both(chain, result)

  /** Per-element update with failure accumulation. Elements whose update fails (walk / read /
    * decode) are left unchanged in the output; their failure(s) contribute to the outer chain.
    */
  private def mapElementsIor(
      arr: Vector[Json],
      elemUpdate: Json => Ior[Chain[JsonFailure], Json],
  ): (Vector[Json], Chain[JsonFailure]) =
    val (acc, chain) = arr.foldLeft((Vector.empty[Json], Chain.empty[JsonFailure])) {
      case ((acc, chain), elem) =>
        elemUpdate(elem) match
          case Ior.Right(j)   => (acc :+ j, chain)
          case Ior.Both(c, j) => (acc :+ j, chain ++ c)
          case Ior.Left(c)    => (acc :+ elem, chain ++ c)
    }
    (acc, chain)

  private def updateElementDecodedIor(
      elemJson: Json,
      f: A => A,
  ): Ior[Chain[JsonFailure], Json] =
    walkElemParent(elemJson) match
      case Left(failure)         => Ior.Both(Chain.one(failure), elemJson)
      case Right((obj, parents)) =>
        readFields(obj) match
          case Left(chain) => Ior.Both(chain, elemJson)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(rebuildElem(writeFields(obj, f(a)), parents))
              case Left(df) =>
                Ior.Both(
                  Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(elemParent), df)),
                  elemJson,
                )

  private def updateElementRawIor(
      elemJson: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    walkElemParent(elemJson) match
      case Left(failure)         => Ior.Both(Chain.one(failure), elemJson)
      case Right((obj, parents)) =>
        f(Json.fromJsonObject(buildSubObject(obj))).asObject match
          case None =>
            Ior.Both(
              Chain.one(JsonFailure.NotAnObject(JsonWalk.terminalOf(elemParent))),
              elemJson,
            )
          case Some(newSub) =>
            Ior.Right(rebuildElem(overlayFields(obj, newSub), parents))

  private def placeElementIor(
      elemJson: Json,
      a: A,
  ): Ior[Chain[JsonFailure], Json] =
    walkElemParent(elemJson) match
      case Left(failure)         => Ior.Both(Chain.one(failure), elemJson)
      case Right((obj, parents)) => Ior.Right(rebuildElem(writeFields(obj, a), parents))

  private def readElementIor(elemJson: Json): Ior[Chain[JsonFailure], A] =
    walkElemParent(elemJson) match
      case Left(failure)   => Ior.Left(Chain.one(failure))
      case Right((obj, _)) =>
        readFields(obj) match
          case Left(chain) => Ior.Left(chain)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(a)
              case Left(df) =>
                Ior.Left(
                  Chain.one(JsonFailure.DecodeFailed(JsonWalk.terminalOf(elemParent), df))
                )
