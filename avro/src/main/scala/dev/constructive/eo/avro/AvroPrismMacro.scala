package dev.constructive.eo.avro

import scala.quoted.*

/** Macros backing `AvroPrism.field(_.x)`, `selectDynamic`, `at(i)`, and `union[Branch]`. Mirrors
  * `dev.constructive.eo.circe.JsonPrismMacro` with codec summoning collapsed from two givens
  * (`AvroEncoder` + `AvroDecoder`) to one project-internal [[AvroCodec]] wrapper.
  *
  * '''Macro-quote shorthand.''' Following the `JsonPrismMacro` convention, the macros use `'{ this
  * }` rather than `'this` for the parent reference — `scalafix-parser` v0.x has a known issue
  * parsing the bare `'this` form. (See `JsonPrismMacro.selectFieldImpl`'s shape.)
  */
object AvroPrismMacro:

  /** Build a child `AvroPrism[B]` from a parent `AvroPrism[A]` via a compile-time selector
    * `(_.field)` plus an in-scope [[AvroCodec]]`[B]`.
    */
  def fieldImpl[A: Type, B: Type](
      parent: Expr[AvroPrism[A]],
      selector: Expr[A => B],
      codecB: Expr[AvroCodec[B]],
  )(using q: Quotes): Expr[AvroPrism[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "AvroPrism.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `_.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenPath[B](${ Expr(name) })(using $codecB)
    }

  /** Drives the Selectable sugar: `codecPrism[Person].name` compiles to `selectFieldImpl` with
    * `name = "name"`. We look the field up on `A`'s case-class schema, verify it exists, grab its
    * declared type (widened to strip precise singletons), summon `AvroCodec[B]` from the enclosing
    * scope, and emit the same `widenPath` call that the explicit `.field(_.x)` macro does.
    *
    * Returns `Expr[Any]` so the `transparent inline def selectDynamic` wrapper can refine the type
    * at each call site.
    */
  def selectFieldImpl[A: Type](
      parent: Expr[AvroPrism[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A](
      "AvroPrism selectDynamic",
      nameE,
    ) { [b] => (name: String, codecB: Expr[AvroCodec[b]]) =>
      '{ $parent.widenPath[b](${ Expr(name) })(using $codecB) }
    }

  /** Macro for `avroPrism.at(i)`. Verifies the parent focus `A` looks like a Scala collection (i.e.
    * derives from `Iterable`), extracts the element type via `A`'s `Iterable` base type, summons
    * the element's [[AvroCodec]], and emits a `widenPathIndex` call.
    */
  def atImpl[A: Type](
      parent: Expr[AvroPrism[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("AvroPrism.at")

    elemTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"AvroPrism.at: no given AvroCodec[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenPathIndex[b]($iE)(using $codecB) }

  /** Skeleton macro for `avroPrism.union[Branch]`. Unit 8 will fill in the schema-driven branch-
    * name resolution; for v0.1.0 we emit a `widenPathUnion` call using the simple-name of `Branch`
    * as the branch name. Kindlings' macro-derived schemas use the case class' simple name for each
    * branch by default, so this matches what the reader schema declares for the common case.
    */
  def unionImpl[A: Type, B: Type](
      parent: Expr[AvroPrism[A]]
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*
    val branchTpe = TypeRepr.of[B]
    val branchName = branchTpe.typeSymbol.name match
      case "Long"    => "long"
      case "Int"     => "int"
      case "String"  => "string"
      case "Boolean" => "boolean"
      case "Float"   => "float"
      case "Double"  => "double"
      case other     => other

    val codecB = summonCodec[B](
      s"AvroPrism.union[${Type.show[B]}]: no given AvroCodec[${Type.show[B]}] in scope."
    )
    '{ $parent.widenPathUnion[B](${ Expr(branchName) })(using $codecB) }

  // ---- Shared helpers (selectDynamic backbone, iterable elem, codec summon) -----

  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Expr[AvroCodec[b]]) => Type[b] ?=> Expr[Any])(using
      q: Quotes
  ): Expr[Any] =
    import quotes.reflect.*

    val name: String = nameE.value.getOrElse {
      report.errorAndAbort(s"$who: field name must be a compile-time string literal.")
    }

    val aTpe = TypeRepr.of[A]
    val aSym = aTpe.typeSymbol
    val cases = aSym.caseFields

    val fieldSym = cases.find(_.name == name).getOrElse {
      val available =
        if cases.isEmpty then s"${Type.show[A]} has no case fields (is it a case class?)"
        else s"Available: ${cases.map(_.name).mkString(", ")}"
      report.errorAndAbort(
        s"$who: type ${Type.show[A]} has no case field named '$name'. $available"
      )
    }

    val fieldTpe = aTpe.memberType(fieldSym).widen

    fieldTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"$who: no given AvroCodec[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
        )
        emit[b](name, codecB)

  private def iterableElementType[A: Type](
      who: String
  )(using q: Quotes): q.reflect.TypeRepr =
    import quotes.reflect.*
    val aTpe = TypeRepr.of[A]
    val iterSym = TypeRepr.of[Iterable[Any]].typeSymbol
    aTpe.baseType(iterSym) match
      case AppliedType(_, elem :: Nil) => elem.widen
      case _                           =>
        report.errorAndAbort(
          s"$who: expected a Scala collection (Vector / List / Seq / …) with a single element type; got ${Type
              .show[A]}."
        )

  /** Summon a single [[AvroCodec]]`[B]` from the enclosing scope, with a caller-supplied error
    * message on failure. Counterpart to `JsonPrismMacro.summonCodecs` (which summoned both an
    * Encoder and a Decoder); cats-eo-avro's [[AvroCodec]] wrapper collapses kindlings'
    * `AvroEncoder` / `AvroDecoder` / `AvroSchemaFor` triplet into one.
    */
  private def summonCodec[B: Type](
      errorMsg: String
  )(using q: Quotes): Expr[AvroCodec[B]] =
    import quotes.reflect.*
    Expr.summon[AvroCodec[B]].getOrElse(report.errorAndAbort(errorMsg))

  /** Strip `Inlined` / `Typed` wrappers around a lambda and pull the field name out of its body.
    * Mirrors the eo-circe `extractFieldName` helper so both macros agree on what a "single-field
    * selector" looks like.
    */
  private def extractFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    t match
      case Inlined(_, _, inner)                      => extractFieldName(inner)
      case Typed(inner, _)                           => extractFieldName(inner)
      case Lambda(_, Select(_, name))                => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name))) => Some(name)
      case Lambda(_, Typed(Select(_, name), _))      => Some(name)
      case _                                         => None

end AvroPrismMacro
