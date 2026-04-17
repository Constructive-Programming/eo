package eo.circe

import scala.quoted.*
import io.circe.{Decoder, Encoder}

/** `.field(_.fieldName)` on a `JsonPrism[S]` — macro sugar over
  * [[JsonPrism.widenPath]]. Extracts the field name from the
  * selector AST (same pattern as eo-generics' `lens[S](_.field)`)
  * and emits a cursor-step via `ACursor.downField`.
  *
  * Usage:
  *
  * {{{
  *   val streetPrism: JsonPrism[String] =
  *     codecPrism[Person].field(_.address).field(_.street)
  * }}}
  *
  * Nested paths chain naturally — each `.field` call appends one
  * `downField` to the stored navigation function.
  */
object JsonPrismMacro:

  /** Build a child `JsonPrism[B]` from a parent `JsonPrism[A]`
    * via a compile-time selector `(_.field)` plus in-scope circe
    * `Encoder[B]` and `Decoder[B]`. */
  def fieldImpl[A: Type, B: Type](
      parent:   Expr[JsonPrism[A]],
      selector: Expr[A => B],
      encB:     Expr[Encoder[B]],
      decB:     Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonPrism[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"JsonPrism.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Nested paths are not yet supported inside a single call;\n"
          + s"chain them: `_.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenPath[B](_.downField(${ Expr(name) }))(using $encB, $decB)
    }

  /** Strip `Inlined` / `Typed` wrappers around a lambda and pull the
    * field name out of its body. Mirrors the eo-generics
    * `extractFieldName` helper so both macros agree on what a
    * "single-field selector" looks like. */
  private def extractFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    t match
      case Inlined(_, _, inner)                      => extractFieldName(inner)
      case Typed(inner, _)                           => extractFieldName(inner)
      case Lambda(_, Select(_, name))                => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name))) => Some(name)
      case Lambda(_, Typed(Select(_, name), _))      => Some(name)
      case _                                         => None
