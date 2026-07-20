package dev.constructive.eo.jsoniter

import scala.quoted.*

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import dev.constructive.eo.generics.MacroSelectors

/** Macros backing `JsoniterPrism.field(_.fieldName)`, `selectDynamic`, `at(i)`, `each` and their
  * `JsoniterTraversal` counterparts. Extracts the field name from the selector AST (same pattern as
  * eo-circe's `JsonPrismMacro`) and emits a `widenField` / `widenIndex` / `toTraversal` call. The
  * single divergence from the circe macro: one `JsonValueCodec[B]` summon instead of the
  * `Encoder[B]` / `Decoder[B]` pair.
  *
  * {{{
  *   val streetP: JsoniterPrism[String] =
  *     JsoniterPrism[Person].field(_.address).field(_.street)
  * }}}
  */
object JsoniterPrismMacro:

  /** Child `JsoniterPrism[B]` from `parent: JsoniterPrism[A]` via a `(_.field)` selector. */
  def fieldImpl[A: Type, B: Type](
      parent: Expr[JsoniterPrism[A]],
      selector: Expr[A => B],
      codecB: Expr[JsonValueCodec[B]],
  )(using q: Quotes): Expr[JsoniterPrism[B]] =
    '{ $parent.widenField[B](${ Expr(fieldName("JsoniterPrism.field", selector)) })(using $codecB) }

  /** Traversal counterpart to [[fieldImpl]]. */
  def fieldTraversalImpl[A: Type, B: Type](
      parent: Expr[JsoniterTraversal[A]],
      selector: Expr[A => B],
      codecB: Expr[JsonValueCodec[B]],
  )(using q: Quotes): Expr[JsoniterTraversal[B]] =
    '{
      $parent.widenField[B](${ Expr(fieldName("JsoniterTraversal.field", selector)) })(using
        $codecB
      )
    }

  /** Drives `JsoniterPrism[Person].address`. Looks the field up on `A`'s schema, summons its codec,
    * emits `widenField`. Returns `Expr[Any]` so `transparent inline` refines per call site.
    */
  def selectFieldImpl[A: Type](
      parent: Expr[JsoniterPrism[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsoniterPrism selectDynamic", nameE) {
      [b] => (
          name: String,
          codec: Expr[JsonValueCodec[b]],
      ) => '{ $parent.widenField[b](${ Expr(name) })(using $codec) }
    }

  /** Traversal counterpart to [[selectFieldImpl]]. */
  def selectFieldTraversalImpl[A: Type](
      parent: Expr[JsoniterTraversal[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsoniterTraversal selectDynamic", nameE) {
      [b] => (
          name: String,
          codec: Expr[JsonValueCodec[b]],
      ) => '{ $parent.widenField[b](${ Expr(name) })(using $codec) }
    }

  /** `.at(i)` — verifies `A <: Iterable`, summons the element codec, emits `widenIndex`. */
  def atImpl[A: Type](
      parent: Expr[JsoniterPrism[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    elementWise[A]("JsoniterPrism.at") { [b] => (codec: Expr[JsonValueCodec[b]]) =>
      '{ $parent.widenIndex[b]($iE)(using $codec) }
    }

  /** Traversal counterpart to [[atImpl]]. */
  def atTraversalImpl[A: Type](
      parent: Expr[JsoniterTraversal[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    elementWise[A]("JsoniterTraversal.at") { [b] => (codec: Expr[JsonValueCodec[b]]) =>
      '{ $parent.widenIndex[b]($iE)(using $codec) }
    }

  /** `.each` — emits `toTraversal[B]` over `A`'s element type. */
  def eachImpl[A: Type](
      parent: Expr[JsoniterPrism[A]]
  )(using q: Quotes): Expr[Any] =
    elementWise[A]("JsoniterPrism.each") { [b] => (codec: Expr[JsonValueCodec[b]]) =>
      '{ $parent.toTraversal[b](using $codec) }
    }

  // ---- shared helpers -------------------------------------------------

  /** Extract the single-field selector name or abort with a `who`-tagged error. */
  private def fieldName[A: Type, B: Type](
      who: String,
      selector: Expr[A => B],
  )(using q: Quotes): String =
    import quotes.reflect.*
    MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"$who: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

  /** Shared backbone for the Dynamic sugar: validates the literal-string name, looks the field up
    * on `A`'s case-class schema, summons the field type's `JsonValueCodec`, and routes through the
    * caller-supplied `emit` callback.
    */
  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Expr[JsonValueCodec[b]]) => Type[b] ?=> Expr[Any])(using
      q: Quotes
  ): Expr[Any] =
    import quotes.reflect.*

    val name: String = nameE.value.getOrElse {
      report.errorAndAbort(s"$who: field name must be a compile-time string literal.")
    }

    val aTpe = TypeRepr.of[A]
    val cases = aTpe.typeSymbol.caseFields

    val fieldSym = cases.find(_.name == name).getOrElse {
      val available =
        if cases.isEmpty then s"${Type.show[A]} has no case fields (is it a case class?)"
        else s"Available: ${cases.map(_.name).mkString(", ")}"
      report.errorAndAbort(
        s"$who: type ${Type.show[A]} has no case field named '$name'. $available"
      )
    }

    aTpe.memberType(fieldSym).widen.asType match
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
          + " Derive one via `given JsonValueCodec[" + Type.show[B] + "] = JsonCodecMaker.make`"
          + " (jsoniter-scala-macros), or provide one manually."
      )
    }
