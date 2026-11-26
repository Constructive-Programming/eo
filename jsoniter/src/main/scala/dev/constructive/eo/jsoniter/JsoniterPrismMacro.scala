package dev.constructive.eo.jsoniter

import scala.quoted.*

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import dev.constructive.eo.generics.MacroSelectors

/** Macros backing the [[JsoniterPath]] builder — `.field(_.fieldName)`, `selectDynamic`, `at(i)`
  * and `each` — for every eo-jsoniter optic: the whole-array `JsoniterPrism` / `JsoniterTraversal`
  * and their in-place faces. Extracts the field name from the selector AST (same pattern as
  * eo-circe's `JsonPrismMacro`) and emits a `widenField` / `widenIndex` / `toTraversal` call on the
  * builder, which builds the next optic of the builder's own kind (`P`, or `T` for `.each`). The
  * single divergence from the circe macro: one `JsonValueCodec[B]` summon instead of the
  * `Encoder[B]` / `Decoder[B]` pair.
  *
  * {{{
  *   val streetP: JsoniterPrism[String] =
  *     JsoniterPrism[Person].field(_.address).field(_.street)
  *   val streetIn: JsoniterInPlacePrism[String] =
  *     JsoniterPrism.inPlace[Person].field(_.address).field(_.street)
  * }}}
  */
object JsoniterPrismMacro:

  // ---- `JsoniterPrism.path` / `JsoniterTraversal.path` ----------------
  //
  // The path is a literal, so it is parsed here, at compile time, by the companion's own `steps`:
  // a malformed one is a compile error carrying the message `fromPath` would return as a `Left`,
  // and the parsed steps are emitted as a literal list, so nothing is parsed (or thrown) at
  // runtime.

  private given ToExpr[PathStep] with

    def apply(step: PathStep)(using Quotes): Expr[PathStep] = step match
      case PathStep.Field(name) => '{ PathStep.Field(${ Expr(name) }) }
      case PathStep.Index(i)    => '{ PathStep.Index(${ Expr(i) }) }
      case PathStep.Wildcard    => '{ PathStep.Wildcard }

  def path[A: Type, O[_]: Type](
      companion: Expr[JsoniterPathConstructors[O]],
      pathE: Expr[String],
      codec: Expr[JsonValueCodec[A]],
  )(using q: Quotes): Expr[O[A]] =
    import q.reflect.*
    // The companion is only an expression here; its `steps` runs now, so pick the object itself.
    val (who, parse) =
      if TypeRepr.of[O].typeSymbol == TypeRepr.of[JsoniterPrism].typeSymbol then
        ("JsoniterPrism.path", JsoniterPrism.steps)
      else ("JsoniterTraversal.path", JsoniterTraversal.steps)
    val steps = pathE.value match
      case Some(path) => parse(path).fold(report.errorAndAbort(_, pathE), identity)
      case None       =>
        report.errorAndAbort(
          s"$who takes a literal path. For a path known only at runtime, use fromPath, which " +
            "returns a malformed path as a Left.",
          pathE,
        )
    '{ $companion.fromSteps[A](${ Expr(steps) })(using $codec) }

  // ---- cursor selectors ------------------------------------------------

  /** Child `P[B]` from `parent` via a `(_.field)` selector. */
  def fieldImpl[A: Type, B: Type, P[_]: Type](
      parent: Expr[JsoniterPath[A, P]],
      selector: Expr[A => B],
      codecB: Expr[JsonValueCodec[B]],
  )(using q: Quotes): Expr[P[B]] =
    '{ $parent.widenField[B](${ Expr(fieldName(who[P]("field"), selector)) })(using $codecB) }

  /** Drives `JsoniterPrism[Person].address`. Looks the field up on `A`'s schema, summons its codec,
    * emits `widenField`. Returns `Expr[Any]` so `transparent inline` refines per call site.
    */
  def selectFieldImpl[A: Type, P[_]: Type](
      parent: Expr[JsoniterPath[A, P]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A](who[P]("selectDynamic", " "), nameE) {
      [b] => (
          name: String,
          codec: Expr[JsonValueCodec[b]],
      ) => '{ $parent.widenField[b](${ Expr(name) })(using $codec) }
    }

  /** `.at(i)` — verifies `A <: Iterable`, summons the element codec, emits `widenIndex`. */
  def atImpl[A: Type, P[_]: Type](
      parent: Expr[JsoniterPath[A, P]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    elementWise[A](who[P]("at")) { [b] => (codec: Expr[JsonValueCodec[b]]) =>
      '{ $parent.widenIndex[b]($iE)(using $codec) }
    }

  /** `.each` — emits `toTraversal[B]` over `A`'s element type. */
  def eachImpl[A: Type, T[_]: Type](
      parent: Expr[JsoniterPrismPath[A, ?, T]]
  )(using q: Quotes): Expr[Any] =
    elementWise[A]("JsoniterPrism.each") { [b] => (codec: Expr[JsonValueCodec[b]]) =>
      '{ $parent.toTraversal[b](using $codec) }
    }

  // ---- shared helpers -------------------------------------------------

  /** The name diagnostics give the optic being drilled: `JsoniterPrism.field`, or
    * `JsoniterTraversal selectDynamic` — the whole-array optic's name, also for its in-place face,
    * which drills exactly as it does.
    */
  private def who[P[_]: Type](step: String, sep: String = ".")(using q: Quotes): String =
    import quotes.reflect.*
    val traversal = TypeRepr.of[P].typeSymbol.name.endsWith("Traversal")
    s"${if traversal then "JsoniterTraversal" else "JsoniterPrism"}$sep$step"

  /** Extract the single-field selector name or abort with a `who`-tagged error. */
  private def fieldName[A: Type, B: Type](
      who: String,
      selector: Expr[A => B],
  )(using q: Quotes): String =
    import quotes.reflect.*
    val name = MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"$who: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }
    MacroSelectors.requireCaseField[A](who, name)
    name

  /** Shared backbone for the Dynamic sugar — name validation + field lookup live in the shared
    * `MacroSelectors.caseFieldType` (eo-generics); this stub owns the `JsonValueCodec` summon.
    */
  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Expr[JsonValueCodec[b]]) => Type[b] ?=> Expr[Any])(using
      q: Quotes
  ): Expr[Any] =
    val (name, _, fieldTpe) = MacroSelectors.caseFieldType[A](who, nameE)
    fieldTpe.asType match
      case '[b] =>
        emit[b](name, summonCodec[b](s"$who: for field '$name' of ${Type.show[A]}"))

  /** Shared backbone for `.at` / `.each`: resolves `A`'s Iterable element type, summons its codec,
    * hands it to `emit`.
    */
  private def elementWise[A: Type](
      who: String
  )(emit: [b] => Expr[JsonValueCodec[b]] => Type[b] ?=> Expr[Any])(using q: Quotes): Expr[Any] =
    MacroSelectors.iterableElementType[A](who).asType match
      case '[b] =>
        emit[b](summonCodec[b](s"$who: for the element type of ${Type.show[A]}"))

  /** Summon `JsonValueCodec[B]` from the enclosing scope, aborting with a derivation hint. */
  private def summonCodec[B: Type](context: String)(using q: Quotes): Expr[JsonValueCodec[B]] =
    import quotes.reflect.*
    Expr.summon[JsonValueCodec[B]].getOrElse {
      report.errorAndAbort(
        s"$context: no given JsonValueCodec[${Type.show[B]}] in scope."
          + " Derive one via `given JsoniterCodec[" + Type.show[B] + "] = JsoniterCodec.derived`"
          + " (jsoniter-scala-macros), or provide one manually."
      )
    }
