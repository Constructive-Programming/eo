package dev.constructive.eo
package zio
package json

import _root_.zio.Chunk
import _root_.zio.json.JsonCodec
import _root_.zio.json.ast.{Json, JsonCursor}

import data.PSVec
import optics.{MendTearPrism, Optional, PickMendPrism, Plated, Prism, Traversal}

/** zio-json integration (optional dependency — add `zio-json` yourself to use this sub-package).
  * The circe playbook on `zio.json.ast.Json`, plus the seam circe doesn't have: [[optional]] turns
  * any existing [[JsonCursor]] — zio-json's own typed path — into an eo Optional, so cursor-based
  * codebases get eo composition without rewriting a path.
  *
  *   - '''Constructor prisms''' ([[JsonValues.str]], [[JsonValues.obj]], …): one per `Json` case.
  *   - '''Navigation''' ([[JsonValues.field]], [[JsonValues.at]], [[JsonValues.each]]) —
  *     sibling-preserving `Optional`s / `Traversal` into objects and arrays.
  *   - '''Whole-tree rewrites''' — [[JsonValues.platedJson]] for `Plated.transform` / `rewrite` /
  *     `universe`.
  *   - '''Wire face''' — [[stringPrism(JsonCodec)]] on any `JsonCodec[A]`; [[JsonValues.text]] is
  *     its `Json`-AST instance, the `String ↔ Json` on-ramp.
  */
object JsonValues:

  /** Prism onto the `Str` case. */
  val str: PickMendPrism[Json, String, String] =
    Prism.optional({ case Json.Str(s) => Some(s); case _ => None }, Json.Str(_))

  /** Prism onto the `Bool` case. */
  val bool: PickMendPrism[Json, Boolean, Boolean] =
    Prism.optional({ case Json.Bool(b) => Some(b); case _ => None }, Json.Bool(_))

  /** Prism onto the `Num` case (stored as `java.math.BigDecimal`). */
  val num: PickMendPrism[Json, java.math.BigDecimal, java.math.BigDecimal] =
    Prism.optional({ case Json.Num(n) => Some(n); case _ => None }, Json.Num(_))

  /** Prism onto the `Null` case object (focus `Unit`). */
  val nul: PickMendPrism[Json, Unit, Unit] =
    Prism.optional({ case Json.Null => Some(()); case _ => None }, _ => Json.Null)

  /** Prism onto an `Obj`'s ordered `(name, value)` fields. */
  val obj: PickMendPrism[Json, Chunk[(String, Json)], Chunk[(String, Json)]] =
    Prism.optional({ case Json.Obj(fs) => Some(fs); case _ => None }, Json.Obj(_))

  /** Prism onto an `Arr`'s elements. */
  val arr: PickMendPrism[Json, Chunk[Json], Chunk[Json]] =
    Prism.optional({ case Json.Arr(es) => Some(es); case _ => None }, Json.Arr(_))

  /** Optional into an `Obj` field by name — the other fields (and their order) survive writes;
    * misses (not an object, or no such field) pass through writes untouched. Reads take the first
    * matching field, writes update all.
    */
  def field(name: String): Optional[Json, Json, Json, Json] =
    Optional[Json, Json, Json, Json](
      {
        case o @ Json.Obj(fields) =>
          fields.collectFirst { case (n, v) if n == name => v }.toRight(o)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case Json.Obj(fields) if fields.exists(_._1 == name) =>
            Json.Obj(fields.map((n, v) => if n == name then (n, b) else (n, v)))
          case other => other,
    )

  /** Optional into an `Arr` element by index — siblings survive writes, out-of-range or non-array
    * misses pass through.
    */
  def at(i: Int): Optional[Json, Json, Json, Json] =
    Optional[Json, Json, Json, Json](
      {
        case a @ Json.Arr(es) => if es.isDefinedAt(i) then Right(es(i)) else Left(a)
        case other            => Left(other)
      },
      (s, b) =>
        s match
          case Json.Arr(es) if es.isDefinedAt(i) => Json.Arr(es.updated(i, b))
          case other                             => other,
    )

  /** Traversal over every element of an `Arr` — zero foci (write pass-through) on anything else. */
  val each: Traversal[Json, Json, Json, Json] =
    Traversal.selfChildren[Json](
      { case Json.Arr(es) => PSVec.from(es); case _ => emptyVec },
      (v, children) =>
        v match
          case Json.Arr(_) => Json.Arr(Chunk.from(children.toList))
          case other       => other,
    )

  /** [[optics.Plated]] over the `Json` tree — children are an object's field values or an array's
    * elements (scalars have none); rebuilding keeps field names and order.
    */
  given platedJson: Plated[Json] =
    Plated.fromChildrenVec(children, rebuild)

  /** `String ↔ Json` wire face — [[stringPrism(JsonCodec)]] at the AST itself, the on-ramp from raw
    * text into every optic above.
    */
  val text: MendTearPrism[String, String, Json, Json] =
    summon[JsonCodec[Json]].stringPrism

  private val emptyVec: PSVec[Json] = PSVec.empty[Json]

  private def children(v: Json): PSVec[Json] = v match
    case Json.Obj(fields) => PSVec.from(fields.map(_._2))
    case Json.Arr(es)     => PSVec.from(es)
    case _                => emptyVec

  private def rebuild(v: Json, cs: PSVec[Json]): Json = v match
    case Json.Obj(fields) =>
      Json.Obj(Chunk.from(fields.indices.map(i => (fields(i)._1, cs(i)))))
    case Json.Arr(_) => Json.Arr(Chunk.from(cs.toList))
    case other       => other

end JsonValues

extension [To <: Json](self: JsonCursor[?, To])

  /** The cursor as a sibling-preserving eo Optional — reads via zio-json's own `Json.get`, writes
    * by rebuilding the spine the cursor describes. A cursor that misses (wrong shape, absent field,
    * out-of-range index, failed type filter) passes writes through untouched, exactly like
    * [[JsonValues.field]] / [[JsonValues.at]].
    */
  def optional: Optional[Json, Json, To, To] =
    Optional[Json, Json, To, To](
      j => j.get(self).fold(_ => Left(j), Right(_)),
      (j, b) => if j.get(self).isRight then write(self, j, b) else j,
    )

/** Rebuild `root` with `b` at the focus of `c` — callers have already checked the full cursor reads
  * successfully, so the guards only defend the recursion's intermediate reads.
  */
private def write(c: JsonCursor[?, ?], root: Json, b: Json): Json = c match
  case JsonCursor.Identity     => b
  case d: JsonCursor.DownField =>
    root.get(d.parent) match
      case Right(Json.Obj(fields)) if fields.exists(_._1 == d.name) =>
        val updated = Json.Obj(fields.map((n, v) => if n == d.name then (n, b) else (n, v)))
        write(d.parent, root, updated)
      case _ => root
  case e: JsonCursor.DownElement =>
    root.get(e.parent) match
      case Right(Json.Arr(es)) if es.isDefinedAt(e.index) =>
        write(e.parent, root, Json.Arr(es.updated(e.index, b)))
      case _ => root
  case t: JsonCursor.FilterType[?] => write(t.parent, root, b)

extension [A](self: JsonCodec[A])

  /** Prism between JSON text and `A` — encode/decode as the two halves, the same laws and caveats
    * as every byte face (roundtrip identity one way, re-encode normalisation the other, misses pass
    * through writes).
    */
  def stringPrism: MendTearPrism[String, String, A, A] =
    Prism[String, A](
      s => self.decodeJson(s).fold(_ => Left(s), Right(_)),
      a => self.encodeJson(a, None).toString,
    )
