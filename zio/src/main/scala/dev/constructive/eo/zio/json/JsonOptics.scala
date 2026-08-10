package dev.constructive.eo
package zio
package json

import scala.annotation.tailrec

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
    * misses (not an object, or no such field) pass through writes untouched.
    *
    * `Json.Obj` is `Chunk`-backed, so duplicate keys are representable (legal JSON, and zio-json's
    * parser preserves them). Reads AND writes both target the '''first''' matching field, which is
    * what keeps the optic lawful on such documents — `replace(getOption(s).get)(s) == s` — and
    * matches zio-json's own first-match `transformOrDelete`. A write-all variant would be a
    * Traversal, not an Optional.
    *
    * Caveat when ASSERTING on such documents (ours or anyone's): zio-json's `Json.Obj.equals` maps
    * the left operand before comparing each right entry, so a duplicate-key object compares unequal
    * to itself. Compare the `fields` chunk (via [[obj]]) instead of the `Json` values.
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
          case Json.Obj(fields) =>
            val i = fields.indexWhere(_._1 == name)
            if i < 0 then s else Json.Obj(fields.updated(i, (name, b)))
          case other => other,
    )

  /** [[optics.At]]-style access to an `Obj` field: the focus is the `Option[Json]` at `name`, so a
    * write can '''create or delete''' the field — `Some(v)` updates the first occurrence (appending
    * when absent), `None` removes it. [[field]] can do neither: its focus is the value, so an
    * absent field is a miss and a miss passes writes through.
    *
    * Partial only in "is this an object"; within one it is total, presence living in the focus (so
    * `getOption` yields `Some(None)` for an object lacking the field). `None` removes '''every'''
    * occurrence of a duplicated key rather than just the first — that is what keeps put-get honest
    * on such documents: after a delete, a read must not find a leftover twin.
    */
  def atField(name: String): Optional[Json, Json, Option[Json], Option[Json]] =
    Optional[Json, Json, Option[Json], Option[Json]](
      {
        case Json.Obj(fields) => Right(fields.collectFirst { case (n, v) if n == name => v })
        case other            => Left(other)
      },
      (s, ov) =>
        s match
          case Json.Obj(fields) =>
            val i = fields.indexWhere(_._1 == name)
            ov match
              case Some(v) =>
                if i < 0 then Json.Obj(fields :+ (name -> v))
                else Json.Obj(fields.updated(i, (name, v)))
              case None => Json.Obj(fields.filterNot(_._1 == name))
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
          case Json.Arr(_) => Json.Arr(Chunk.fromIterator(children.toList.iterator))
          case other       => other,
    )

  /** [[optics.Plated]] over the `Json` tree — children are an object's field values or an array's
    * elements (scalars have none); rebuilding keeps field names and order. Positional by
    * construction, so duplicate object keys are each rewritten independently (unlike [[field]],
    * which targets the first).
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
      Json.Obj(Chunk.fromIterator(fields.indices.iterator.map(i => (fields(i)._1, cs(i)))))
    case Json.Arr(_) => Json.Arr(Chunk.fromIterator(cs.toList.iterator))
    case other       => other

end JsonValues

extension [To <: Json](self: JsonCursor[?, To])

  /** The cursor as a sibling-preserving eo Optional — reads via zio-json's own `Json.get`, writes
    * by rebuilding exactly the spine the cursor describes. A cursor that misses (wrong shape,
    * absent field, out-of-range index, failed type filter) passes writes through untouched, exactly
    * like [[JsonValues.field]] / [[JsonValues.at]].
    *
    * The write is a single root-to-leaf descent: each level is read once and rebuilt on the unwind,
    * and `DownField` targets the first matching field (see [[JsonValues.field]] on duplicate keys).
    */
  def optional: Optional[Json, Json, To, To] =
    Optional[Json, Json, To, To](
      j => j.get(self).fold(_ => Left(j), Right(_)),
      (j, b) => writeSteps(cursorSteps(self, Nil), j, b).getOrElse(j),
    )

/** The cursor's steps root-first — `JsonCursor` is parent-linked (leaf outermost), so the chain is
  * reversed with a `@tailrec` accumulator before the descent.
  */
@tailrec
private def cursorSteps(
    c: JsonCursor[?, ?],
    acc: List[JsonCursor[?, ?]],
): List[JsonCursor[?, ?]] =
  c match
    case d: JsonCursor.DownField     => cursorSteps(d.parent, d :: acc)
    case e: JsonCursor.DownElement   => cursorSteps(e.parent, e :: acc)
    case t: JsonCursor.FilterType[?] => cursorSteps(t.parent, t :: acc)
    case _                           => acc // Identity — the root

/** Rebuild `node` with `b` at the end of `steps`, reading each level exactly once. `None` is a miss
  * at some level (absent field, out-of-range index, wrong shape, failed filter), which the caller
  * turns into the write pass-through.
  */
private def writeSteps(steps: List[JsonCursor[?, ?]], node: Json, b: Json): Option[Json] =
  steps match
    case Nil                               => Some(b)
    case (d: JsonCursor.DownField) :: rest =>
      node match
        case Json.Obj(fields) =>
          val i = fields.indexWhere(_._1 == d.name)
          if i < 0 then None
          else writeSteps(rest, fields(i)._2, b).map(v => Json.Obj(fields.updated(i, (d.name, v))))
        case _ => None
    case (e: JsonCursor.DownElement) :: rest =>
      node match
        case Json.Arr(es) if es.isDefinedAt(e.index) =>
          writeSteps(rest, es(e.index), b).map(v => Json.Arr(es.updated(e.index, v)))
        case _ => None
    case (t: JsonCursor.FilterType[a]) :: rest =>
      // Re-validate the filter with zio-json's own predicate rather than re-deriving it here.
      if node.get(JsonCursor.identity.filterType(t.jsonType)).isRight then writeSteps(rest, node, b)
      else None
    case _ :: rest => writeSteps(rest, node, b) // Identity mid-chain (unreachable: stripped above)

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
