package eo.circe

import cats.Eq
import cats.data.{Chain, Ior}

import io.circe.{DecodingFailure, Json, ParsingFailure}
import io.circe.parser.parse as circeParse

/** Structured failure surfaced by the default Ior-bearing surface of [[JsonPrism]] /
  * [[JsonFieldsPrism]] / [[JsonTraversal]] / [[JsonFieldsTraversal]].
  *
  * Every case carries a [[PathStep]] so the walk that produced the failure can point at the
  * specific cursor position that refused. The default enum `toString` keeps the structural
  * representation for testability; [[message]] gives a human-readable diagnostic.
  *
  * Shape choice: a Scala 3 `enum`, consistent with [[PathStep]]. An expanded `pathContext:
  * Array[PathStep]` field was discussed (OQ3) and deferred to a later pass — the adapter
  * `JsonFailure.withPath(prefix)` can decorate after the fact if user demand appears.
  */
enum JsonFailure:

  /** Named field absent from its parent JsonObject at `step`. */
  case PathMissing(step: PathStep)

  /** Parent wasn't a JsonObject at `step` (so the walker couldn't look up a field). */
  case NotAnObject(step: PathStep)

  /** Parent wasn't a JSON array at `step` (so the walker couldn't index). */
  case NotAnArray(step: PathStep)

  /** Index was outside `[0, size)` at `step`. `size` is the actual array length. */
  case IndexOutOfRange(step: PathStep, size: Int)

  /** Decoder refused at `step`. `step` is `PathStep.Field("")` at root-level decode failures on a
    * path-empty prism.
    */
  case DecodeFailed(step: PathStep, cause: DecodingFailure)

  /** Input `String` didn't parse as JSON. Surfaced only by the `Json | String` overloads; when the
    * caller passes a `Json` directly this case cannot fire. The wrapped `ParsingFailure` carries
    * circe's line/column diagnostics.
    */
  case ParseFailed(cause: ParsingFailure)

  /** Human-readable diagnostic. Kept separate from `toString` so the default enum representation
    * remains useful for structural inspection / pattern-matching-in-tests.
    */
  def message: String = this match
    case PathMissing(s)        => s"path missing at $s"
    case NotAnObject(s)        => s"expected JSON object at $s"
    case NotAnArray(s)         => s"expected JSON array at $s"
    case IndexOutOfRange(s, n) => s"index out of range at $s (size=$n)"
    case DecodeFailed(s, c)    => s"decode failed at $s: ${c.message}"
    case ParseFailed(c)        => s"input string didn't parse as JSON: ${c.message}"

object JsonFailure:

  /** Structural equality — two [[JsonFailure]] values are equal iff they are the same case with the
    * same arguments (`DecodingFailure` falls back to reference equality inside `DecodeFailed` via
    * the circe-provided `equals`).
    *
    * Required for `Eq[Chain[JsonFailure]]` to be summonable at specs2-`===` call sites.
    */
  given Eq[JsonFailure] = Eq.fromUniversalEquals

  /** Resolve a `Json | String` input to a parsed `Json`, threading parse failures through the Ior
    * channel. Used by every `Json | String`-accepting overload on `JsonPrism` / `JsonFieldsPrism` /
    * `JsonTraversal` / `JsonFieldsTraversal` so the parse step is uniform (same failure shape, same
    * message format).
    *
    * When `input` is already a `Json`, this is a pure `Ior.Right`. When it's a `String`, parse
    * failures arrive as `Ior.Left(Chain(JsonFailure.ParseFailed(cause)))`. Downstream `flatMap`s
    * short-circuit naturally on Left.
    */
  private[circe] def parseInputIor(input: Json | String): Ior[Chain[JsonFailure], Json] =
    input match
      case j: Json   => Ior.Right(j)
      case s: String =>
        circeParse(s) match
          case Right(j) => Ior.Right(j)
          case Left(pf) => Ior.Left(Chain.one(JsonFailure.ParseFailed(pf)))

  /** Resolve a `Json | String` input to a parsed `Json`, dropping failures. For the `*Unsafe`
    * escape hatches: `Json` input passes through, `String` that doesn't parse becomes `Json.Null`
    * (there's no meaningful silent fallback for unparseable text — callers who need parse
    * diagnostics must use the Ior-bearing default surface).
    */
  private[circe] def parseInputUnsafe(input: Json | String): Json =
    input match
      case j: Json   => j
      case s: String => circeParse(s).getOrElse(Json.Null)
