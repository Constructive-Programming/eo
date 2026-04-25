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
) extends Dynamic:

  // ---- Default (Ior-bearing) surface --------------------------------

  def modify(f: A => A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => modifyIor(j, f))

  def transform(f: Json => Json): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => transformIor(j, f))

  def place(a: A): (Json | String) => Ior[Chain[JsonFailure], Json] =
    input => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, a))

  def transfer[C](f: C => A): (Json | String) => C => Ior[Chain[JsonFailure], Json] =
    input => c => JsonFailure.parseInputIor(input).flatMap(j => placeIor(j, f(c)))

  def getAll(input: Json | String): Ior[Chain[JsonFailure], Vector[A]] =
    JsonFailure.parseInputIor(input).flatMap(getAllIor)

  // ---- *Unsafe escape hatches --------------------------------------

  inline def modifyUnsafe(f: A => A): (Json | String) => Json =
    input => modifyImpl(JsonFailure.parseInputUnsafe(input), f)

  inline def transformUnsafe(f: Json => Json): (Json | String) => Json =
    input => transformImpl(JsonFailure.parseInputUnsafe(input), f)

  inline def placeUnsafe(a: A): (Json | String) => Json =
    input => placeImpl(JsonFailure.parseInputUnsafe(input), a)

  inline def transferUnsafe[C](f: C => A): (Json | String) => C => Json =
    input => c => placeImpl(JsonFailure.parseInputUnsafe(input), f(c))

  inline def getAllUnsafe(input: Json | String): Vector[A] =
    getAllUnsafeImpl(JsonFailure.parseInputUnsafe(input))

  // ---- Shared helpers ---------------------------------------------

  /** Walk `prefix` and collect per-step parents. Returns the iterated array on success, or a
    * `JsonFailure` on the first miss.
    */
  private def walkPrefix(
      json: Json,
      parents: Array[AnyRef],
  ): Either[JsonFailure, Vector[Json]] =
    var cur: Json = json
    var i = 0
    while i < prefix.length do
      val step = prefix(i)
      step match
        case PathStep.Field(name) =>
          cur.asObject match
            case None      => return Left(JsonFailure.NotAnObject(step))
            case Some(obj) =>
              parents(i) = obj
              obj(name) match
                case None    => return Left(JsonFailure.PathMissing(step))
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None      => return Left(JsonFailure.NotAnArray(step))
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then
                return Left(JsonFailure.IndexOutOfRange(step, arr.length))
              parents(i) = arr
              cur = arr(idx)
      i += 1
    cur.asArray match
      case Some(arr) => Right(arr)
      case None      =>
        val step =
          if prefix.length == 0 then PathStep.Field("") else prefix(prefix.length - 1)
        Left(JsonFailure.NotAnArray(step))

  /** Walk elemParent on a single element. Returns the parent JsonObject or a JsonFailure. */
  private def walkElemParent(
      elemJson: Json,
      parents: Array[AnyRef],
  ): Either[JsonFailure, JsonObject] =
    var cur: Json = elemJson
    var i = 0
    while i < elemParent.length do
      val step = elemParent(i)
      step match
        case PathStep.Field(name) =>
          cur.asObject match
            case None      => return Left(JsonFailure.NotAnObject(step))
            case Some(obj) =>
              parents(i) = obj
              obj(name) match
                case None    => return Left(JsonFailure.PathMissing(step))
                case Some(c) => cur = c
        case PathStep.Index(idx) =>
          cur.asArray match
            case None      => return Left(JsonFailure.NotAnArray(step))
            case Some(arr) =>
              if idx < 0 || idx >= arr.length then
                return Left(JsonFailure.IndexOutOfRange(step, arr.length))
              parents(i) = arr
              cur = arr(idx)
      i += 1
    cur.asObject match
      case None =>
        val step =
          if elemParent.length == 0 then PathStep.Field("")
          else elemParent(elemParent.length - 1)
        Left(JsonFailure.NotAnObject(step))
      case Some(obj) => Right(obj)

  /** Read each selected field of `obj` into a Chain (failures) or a sub-JsonObject (success). */
  private def readFields(obj: JsonObject): Either[Chain[JsonFailure], JsonObject] =
    var chain: Chain[JsonFailure] = Chain.empty
    var sub: JsonObject = JsonObject.empty
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      obj(name) match
        case None =>
          chain = chain :+ JsonFailure.PathMissing(PathStep.Field(name))
        case Some(v) =>
          sub = sub.add(name, v)
      i += 1
    if chain.isEmpty then Right(sub) else Left(chain)

  private def buildSubObject(obj: JsonObject): JsonObject =
    var sub: JsonObject = JsonObject.empty
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      sub = sub.add(name, obj(name).getOrElse(Json.Null))
      i += 1
    sub

  private def writeFields(parent: JsonObject, a: A): JsonObject =
    val encoded: JsonObject = encoder(a).asObject.getOrElse(JsonObject.empty)
    var out: JsonObject = parent
    var i = 0
    while i < fieldNames.length do
      val name = fieldNames(i)
      encoded(name) match
        case Some(v) => out = out.add(name, v)
        case None    => ()
      i += 1
    out

  /** Rebuild the elem after updating the terminal JsonObject at the end of `elemParent`. */
  private def rebuildElem(newParent: JsonObject, parents: Array[AnyRef]): Json =
    var newChild: Json = Json.fromJsonObject(newParent)
    var j = elemParent.length - 1
    while j >= 0 do
      newChild = rebuildStep(parents(j), elemParent(j), newChild)
      j -= 1
    newChild

  /** Rebuild the root after updating the iterated array. */
  private def rebuildRoot(newArr: Vector[Json], parents: Array[AnyRef]): Json =
    var newChild: Json = Json.fromValues(newArr)
    var j = prefix.length - 1
    while j >= 0 do
      newChild = rebuildStep(parents(j), prefix(j), newChild)
      j -= 1
    newChild

  private inline def rebuildStep(
      parent: AnyRef,
      step: PathStep,
      child: Json,
  ): Json =
    step match
      case PathStep.Field(name) =>
        Json.fromJsonObject(parent.asInstanceOf[JsonObject].add(name, child))
      case PathStep.Index(idx) =>
        Json.fromValues(parent.asInstanceOf[Vector[Json]].updated(idx, child))

  // ---- *Unsafe impls -----------------------------------------------

  private def modifyImpl(json: Json, f: A => A): Json =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(_)    => json
      case Right(arr) =>
        val newArr = arr.map(updateElementDecodedUnsafe(_, f))
        rebuildRoot(newArr, prefixParents)

  private def transformImpl(json: Json, f: Json => Json): Json =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(_)    => json
      case Right(arr) =>
        val newArr = arr.map(updateElementRawUnsafe(_, f))
        rebuildRoot(newArr, prefixParents)

  private def placeImpl(json: Json, a: A): Json =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(_)    => json
      case Right(arr) =>
        val newArr = arr.map(placeElementUnsafe(_, a))
        rebuildRoot(newArr, prefixParents)

  private def getAllUnsafeImpl(json: Json): Vector[A] =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(_)    => Vector.empty
      case Right(arr) =>
        val b = Vector.newBuilder[A]
        b.sizeHint(arr.length)
        var i = 0
        while i < arr.length do
          readElementUnsafe(arr(i)) match
            case Some(a) => b += a
            case None    => ()
          i += 1
        b.result()

  private def updateElementDecodedUnsafe(elemJson: Json, f: A => A): Json =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(_)    => elemJson
      case Right(obj) =>
        readFields(obj) match
          case Left(_)    => elemJson
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Left(_)  => elemJson
              case Right(a) =>
                val newA = f(a)
                val newParent = writeFields(obj, newA)
                if elemParent.length == 0 then Json.fromJsonObject(newParent)
                else rebuildElemWithPrefix(newParent, ep)

  private def updateElementRawUnsafe(elemJson: Json, f: Json => Json): Json =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(_)    => elemJson
      case Right(obj) =>
        val sub = buildSubObject(obj)
        f(Json.fromJsonObject(sub)).asObject match
          case None         => elemJson
          case Some(newSub) =>
            var newParent: JsonObject = obj
            var i = 0
            while i < fieldNames.length do
              val name = fieldNames(i)
              newSub(name) match
                case Some(v) => newParent = newParent.add(name, v)
                case None    => ()
              i += 1
            if elemParent.length == 0 then Json.fromJsonObject(newParent)
            else rebuildElemWithPrefix(newParent, ep)

  private def placeElementUnsafe(elemJson: Json, a: A): Json =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(_)    => elemJson
      case Right(obj) =>
        val newParent = writeFields(obj, a)
        if elemParent.length == 0 then Json.fromJsonObject(newParent)
        else rebuildElemWithPrefix(newParent, ep)

  private def readElementUnsafe(elemJson: Json): Option[A] =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(_)    => None
      case Right(obj) =>
        readFields(obj) match
          case Left(_)    => None
          case Right(sub) =>
            Json.fromJsonObject(sub).as[A](using decoder).toOption

  /** When elemParent.length > 0, splice newParent back into elemJson along the collected parent
    * chain. When elemParent is empty, the caller does the trivial wrap directly. Factored out so
    * the common-case (elements themselves carry fields) avoids an extra method call.
    */
  private def rebuildElemWithPrefix(
      newParent: JsonObject,
      parents: Array[AnyRef],
  ): Json =
    rebuildElem(newParent, parents)

  // ---- Ior-bearing impls ------------------------------------------

  private def modifyIor(json: Json, f: A => A): Ior[Chain[JsonFailure], Json] =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(arr)    =>
        val (newArr, chain) = mapElementsIor(arr, updateElementDecodedIor(_, f))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  private def transformIor(
      json: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(arr)    =>
        val (newArr, chain) = mapElementsIor(arr, updateElementRawIor(_, f))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  private def placeIor(json: Json, a: A): Ior[Chain[JsonFailure], Json] =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(arr)    =>
        val (newArr, chain) = mapElementsIor(arr, placeElementIor(_, a))
        val out = rebuildRoot(newArr, prefixParents)
        if chain.isEmpty then Ior.Right(out) else Ior.Both(chain, out)

  private def getAllIor(json: Json): Ior[Chain[JsonFailure], Vector[A]] =
    val prefixParents = new Array[AnyRef](prefix.length)
    walkPrefix(json, prefixParents) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(arr)    =>
        val b = Vector.newBuilder[A]
        b.sizeHint(arr.length)
        var chain: Chain[JsonFailure] = Chain.empty
        var i = 0
        while i < arr.length do
          readElementIor(arr(i)) match
            case Ior.Right(a)   => b += a
            case Ior.Left(c)    => chain = chain ++ c
            case Ior.Both(c, a) =>
              chain = chain ++ c
              b += a
          i += 1
        val result = b.result()
        if chain.isEmpty then Ior.Right(result) else Ior.Both(chain, result)

  /** Per-element update with failure accumulation. Elements whose update fails (walk / read /
    * decode) are left unchanged in the output; their failure(s) contribute to the outer chain.
    */
  private def mapElementsIor(
      arr: Vector[Json],
      elemUpdate: Json => Ior[Chain[JsonFailure], Json],
  ): (Vector[Json], Chain[JsonFailure]) =
    val b = Vector.newBuilder[Json]
    b.sizeHint(arr.length)
    var chain: Chain[JsonFailure] = Chain.empty
    var i = 0
    while i < arr.length do
      elemUpdate(arr(i)) match
        case Ior.Right(j)   => b += j
        case Ior.Both(c, j) =>
          chain = chain ++ c
          b += j
        case Ior.Left(c) =>
          chain = chain ++ c
          b += arr(i)
      i += 1
    (b.result(), chain)

  private def updateElementDecodedIor(
      elemJson: Json,
      f: A => A,
  ): Ior[Chain[JsonFailure], Json] =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(failure) => Ior.Both(Chain.one(failure), elemJson)
      case Right(obj)    =>
        readFields(obj) match
          case Left(chain) => Ior.Both(chain, elemJson)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Left(df) =>
                val step =
                  if elemParent.length == 0 then PathStep.Field("")
                  else elemParent(elemParent.length - 1)
                Ior.Both(Chain.one(JsonFailure.DecodeFailed(step, df)), elemJson)
              case Right(a) =>
                val newA = f(a)
                val newParent = writeFields(obj, newA)
                val newElem =
                  if elemParent.length == 0 then Json.fromJsonObject(newParent)
                  else rebuildElemWithPrefix(newParent, ep)
                Ior.Right(newElem)

  private def updateElementRawIor(
      elemJson: Json,
      f: Json => Json,
  ): Ior[Chain[JsonFailure], Json] =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(failure) => Ior.Both(Chain.one(failure), elemJson)
      case Right(obj)    =>
        val sub = buildSubObject(obj)
        f(Json.fromJsonObject(sub)).asObject match
          case None =>
            val step =
              if elemParent.length == 0 then PathStep.Field("")
              else elemParent(elemParent.length - 1)
            Ior.Both(Chain.one(JsonFailure.NotAnObject(step)), elemJson)
          case Some(newSub) =>
            var newParent: JsonObject = obj
            var i = 0
            while i < fieldNames.length do
              val name = fieldNames(i)
              newSub(name) match
                case Some(v) => newParent = newParent.add(name, v)
                case None    => ()
              i += 1
            val newElem =
              if elemParent.length == 0 then Json.fromJsonObject(newParent)
              else rebuildElemWithPrefix(newParent, ep)
            Ior.Right(newElem)

  private def placeElementIor(
      elemJson: Json,
      a: A,
  ): Ior[Chain[JsonFailure], Json] =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(failure) => Ior.Both(Chain.one(failure), elemJson)
      case Right(obj)    =>
        val newParent = writeFields(obj, a)
        val newElem =
          if elemParent.length == 0 then Json.fromJsonObject(newParent)
          else rebuildElemWithPrefix(newParent, ep)
        Ior.Right(newElem)

  private def readElementIor(elemJson: Json): Ior[Chain[JsonFailure], A] =
    val ep = new Array[AnyRef](elemParent.length)
    walkElemParent(elemJson, ep) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(obj)    =>
        readFields(obj) match
          case Left(chain) => Ior.Left(chain)
          case Right(sub)  =>
            Json.fromJsonObject(sub).as[A](using decoder) match
              case Right(a) => Ior.Right(a)
              case Left(df) =>
                val step =
                  if elemParent.length == 0 then PathStep.Field("")
                  else elemParent(elemParent.length - 1)
                Ior.Left(Chain.one(JsonFailure.DecodeFailed(step, df)))
