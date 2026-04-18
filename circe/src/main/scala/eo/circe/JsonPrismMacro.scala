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
      $parent.widenPath[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Drives the Selectable sugar: `codecPrism[Person].address`
    * compiles to `selectFieldImpl` with `name = "address"`. We
    * look the field up on `A`'s case-class schema, verify it
    * exists, grab its declared type (widened to strip precise
    * singletons), summon `Encoder[B]` / `Decoder[B]` from the
    * enclosing scope, and emit the same `widenPath` call that the
    * explicit `.field(_.x)` macro does.
    *
    * Returns `Expr[Any]` so the `transparent inline def selectDynamic`
    * wrapper can refine the type at each call site. */
  def selectFieldImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      nameE:  Expr[String],
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val name: String = nameE.value.getOrElse {
      report.errorAndAbort(
        "JsonPrism selectDynamic: field name must be a compile-time string literal."
      )
    }

    val aTpe  = TypeRepr.of[A]
    val aSym  = aTpe.typeSymbol
    val cases = aSym.caseFields

    val fieldSym = cases.find(_.name == name).getOrElse {
      val available =
        if cases.isEmpty
        then s"${Type.show[A]} has no case fields (is it a case class?)"
        else s"Available: ${cases.map(_.name).mkString(", ")}"
      report.errorAndAbort(
        s"JsonPrism selectDynamic: type ${Type.show[A]} has no case field named '$name'. $available"
      )
    }

    val fieldTpe = aTpe.memberType(fieldSym).widen

    fieldTpe.asType match
      case '[b] =>
        val enc = Expr.summon[Encoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism selectDynamic: no given Encoder[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
          )
        }
        val dec = Expr.summon[Decoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism selectDynamic: no given Decoder[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
          )
        }
        '{ $parent.widenPath[b](${ Expr(name) })(using $enc, $dec) }

  /** Macro for `jsonPrism.at(i)`. Verifies the parent focus `A`
    * looks like a Scala collection (i.e. derives from `Iterable`),
    * extracts the element type via `A`'s `Iterable` base type,
    * summons the element's circe `Encoder` / `Decoder`, and emits
    * a `widenPathIndex` call. */
  def atImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      iE:     Expr[Int],
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val elemTpe = iterableElementType[A]("JsonPrism.at")

    elemTpe.asType match
      case '[b] =>
        val enc = Expr.summon[Encoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism.at: no given Encoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        val dec = Expr.summon[Decoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism.at: no given Decoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        '{ $parent.widenPathIndex[b]($iE)(using $enc, $dec) }

  /** Macro for `jsonPrism.each`. Reads the element type from `A`'s
    * `Iterable` base, summons the element's `Encoder` / `Decoder`,
    * and emits a `toTraversal[B]` call that hands the current
    * path off as the new traversal's prefix. */
  def eachImpl[A: Type](
      parent: Expr[JsonPrism[A]],
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val elemTpe = iterableElementType[A]("JsonPrism.each")

    elemTpe.asType match
      case '[b] =>
        val enc = Expr.summon[Encoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism.each: no given Encoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        val dec = Expr.summon[Decoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonPrism.each: no given Decoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        '{ $parent.toTraversal[b](using $enc, $dec) }

  /** Macro for `traversal.field(_.x)`. Counterpart to
    * [[fieldImpl]] — extends the traversal's suffix by a named
    * field. */
  def fieldTraversalImpl[A: Type, B: Type](
      parent:   Expr[JsonTraversal[A]],
      selector: Expr[A => B],
      encB:     Expr[Encoder[B]],
      decB:     Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonTraversal[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"JsonTraversal.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenSuffix[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Macro for `traversal.at(i)`. Counterpart to [[atImpl]] —
    * extends the traversal's suffix by an array index. */
  def atTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      iE:     Expr[Int],
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val elemTpe = iterableElementType[A]("JsonTraversal.at")

    elemTpe.asType match
      case '[b] =>
        val enc = Expr.summon[Encoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonTraversal.at: no given Encoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        val dec = Expr.summon[Decoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonTraversal.at: no given Decoder[${Type.show[b]}] in scope for the element type of ${Type.show[A]}."
          )
        }
        '{ $parent.widenSuffixIndex[b]($iE)(using $enc, $dec) }

  /** Macro for `traversal.<fieldName>`. Counterpart to
    * [[selectFieldImpl]] — drives the Dynamic sugar on
    * [[JsonTraversal]] by extending the suffix. */
  def selectFieldTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      nameE:  Expr[String],
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val name: String = nameE.value.getOrElse {
      report.errorAndAbort(
        "JsonTraversal selectDynamic: field name must be a compile-time string literal."
      )
    }

    val aTpe  = TypeRepr.of[A]
    val aSym  = aTpe.typeSymbol
    val cases = aSym.caseFields

    val fieldSym = cases.find(_.name == name).getOrElse {
      val available =
        if cases.isEmpty
        then s"${Type.show[A]} has no case fields (is it a case class?)"
        else s"Available: ${cases.map(_.name).mkString(", ")}"
      report.errorAndAbort(
        s"JsonTraversal selectDynamic: type ${Type.show[A]} has no case field named '$name'. $available"
      )
    }

    val fieldTpe = aTpe.memberType(fieldSym).widen

    fieldTpe.asType match
      case '[b] =>
        val enc = Expr.summon[Encoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonTraversal selectDynamic: no given Encoder[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
          )
        }
        val dec = Expr.summon[Decoder[b]].getOrElse {
          report.errorAndAbort(
            s"JsonTraversal selectDynamic: no given Decoder[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
          )
        }
        '{ $parent.widenSuffix[b](${ Expr(name) })(using $enc, $dec) }

  /** Shared helper: pull the element type out of a Scala
    * collection's `Iterable` base type, or abort with a clear
    * error mentioning the call site (`who`). */
  private def iterableElementType[A: Type](
      who: String,
  )(using q: Quotes): q.reflect.TypeRepr =
    import quotes.reflect.*
    val aTpe    = TypeRepr.of[A]
    val iterSym = TypeRepr.of[Iterable[Any]].typeSymbol
    aTpe.baseType(iterSym) match
      case AppliedType(_, elem :: Nil) => elem.widen
      case _ =>
        report.errorAndAbort(
          s"$who: expected a Scala collection (Vector / List / Seq / …) with a single element type; got ${Type.show[A]}."
        )

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
