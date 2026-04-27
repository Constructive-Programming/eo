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

  /** Macro for `avroPrism.each`. Reads the element type from `A`'s `Iterable` base, summons the
    * element's [[AvroCodec]], and emits a `toTraversal[B]` call that hands the current path off as
    * the new traversal's prefix. Mirror of `JsonPrismMacro.eachImpl`.
    */
  def eachImpl[A: Type](
      parent: Expr[AvroPrism[A]]
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("AvroPrism.each")

    elemTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"AvroPrism.each: no given AvroCodec[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.toTraversal[b](using $codecB) }

  /** Macro for `traversal.field(_.x)`. Counterpart to [[fieldImpl]] — extends the traversal's
    * suffix by a named field.
    */
  def fieldTraversalImpl[A: Type, B: Type](
      parent: Expr[AvroTraversal[A]],
      selector: Expr[A => B],
      codecB: Expr[AvroCodec[B]],
  )(using q: Quotes): Expr[AvroTraversal[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "AvroTraversal.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenSuffix[B](${ Expr(name) })(using $codecB)
    }

  /** Macro for `traversal.at(i)`. Counterpart to [[atImpl]] — extends the traversal's suffix by an
    * array index.
    */
  def atTraversalImpl[A: Type](
      parent: Expr[AvroTraversal[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("AvroTraversal.at")

    elemTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"AvroTraversal.at: no given AvroCodec[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenSuffixIndex[b]($iE)(using $codecB) }

  /** Macro for `codecPrism[A].fields(_.a, _.b, ...)` — multi-field focus. Parses the varargs
    * selector list, validates arity (≥ 2), duplicate-ness, known-fields-ness, non-nested-ness,
    * synthesises a Scala 3 NamedTuple type in SELECTOR order, summons `AvroCodec[NT]` from the
    * enclosing scope, and emits a `toFieldsPrism` call on the parent prism.
    *
    * Mirrors `JsonPrismMacro.fieldsImpl` row-for-row; the codec-summoning side is simpler than
    * circe's because [[AvroCodec]] is one typeclass instead of two (`Encoder` + `Decoder`).
    */
  def fieldsImpl[A: Type](
      parent: Expr[AvroPrism[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("AvroPrism.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          codecNT: Expr[AvroCodec[nt]],
      ) => '{ $parent.toFieldsPrism[nt]($namesExpr)(using $codecNT) }
    }

  /** Macro for `traversal.fields(_.a, _.b, ...)` — multi-field focus on a traversal. Mirror of
    * [[fieldsImpl]] on the traversal side: parses varargs, validates per D10, synthesises the
    * NamedTuple type in SELECTOR order, summons the codec, emits `toFieldsTraversal`.
    */
  def fieldsTraversalImpl[A: Type](
      parent: Expr[AvroTraversal[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("AvroTraversal.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          codecNT: Expr[AvroCodec[nt]],
      ) => '{ $parent.toFieldsTraversal[nt]($namesExpr)(using $codecNT) }
    }

  /** Macro for `traversal.<fieldName>`. Counterpart to [[selectFieldImpl]] — drives the Dynamic
    * sugar on [[AvroTraversal]] by extending the suffix.
    */
  def selectFieldTraversalImpl[A: Type](
      parent: Expr[AvroTraversal[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A](
      "AvroTraversal selectDynamic",
      nameE,
    ) { [b] => (name: String, codecB: Expr[AvroCodec[b]]) =>
      '{ $parent.widenSuffix[b](${ Expr(name) })(using $codecB) }
    }

  /** Shared backbone for [[fieldsImpl]] (and, in Unit 6+, the matching traversal-side
    * `fieldsTraversalImpl`). Validates the varargs selector list per the plan's D10 rules,
    * synthesises the SELECTOR-order NamedTuple type, summons its [[AvroCodec]], and hands the three
    * pieces (`namesExpr`, the codec expression, plus the `Type[nt]` evidence) to the
    * caller-supplied `emit` callback.
    *
    * Counterpart to `JsonPrismMacro.fieldsCommon`; the only differences are the codec-summon shape
    * (one [[AvroCodec]] vs circe's `Encoder` / `Decoder` pair) and the error-message hint pointing
    * at `AvroCodec.derived` instead of `KindlingsCodecAsObject.derive`.
    */
  private def fieldsCommon[A: Type](
      who: String,
      selectorsE: Expr[Seq[A => Any]],
  )(
      emit: [nt] => (Expr[Array[String]], Expr[AvroCodec[nt]]) => Type[nt] ?=> Expr[Any]
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val selectors: List[Expr[A => Any]] =
      selectorsE match
        case Varargs(es) => es.toList
        case other       =>
          report.errorAndAbort(
            s"$who[${Type.show[A]}]: could not destructure varargs selector"
              + s" list. Got: ${other.asTerm.show}"
          )

    if selectors.sizeIs < 2 then
      report.errorAndAbort(
        s"$who[${Type.show[A]}]: requires at least two field selectors"
          + " (for one selector, use .field(_.x) instead)."
      )

    val aTpe = TypeRepr.of[A]
    val aSym = aTpe.typeSymbol
    val caseFields = aSym.caseFields

    if caseFields.isEmpty then
      report.errorAndAbort(
        s"$who[${Type.show[A]}]: parent focus ${Type.show[A]} has no case fields;"
          + " .fields requires a case class."
      )

    val knownFields: List[String] = caseFields.map(_.name)

    val resolved: List[(Int, String)] =
      selectors.zipWithIndex.map { (sel, i) =>
        val name = extractSingleFieldName(sel.asTerm).getOrElse {
          report.errorAndAbort(
            s"$who[${Type.show[A]}]: selector at position $i must be a single-field"
              + " accessor like `_.fieldName`. Nested paths (e.g. `_.a.b`) are not yet supported."
              + s" Got: ${sel.asTerm.show}"
          )
        }
        if !knownFields.contains(name) then
          report.errorAndAbort(
            s"$who[${Type.show[A]}]: '$name' is not a field of ${Type.show[A]}."
              + s" Known fields: ${knownFields.mkString(", ")}."
          )
        (i, name)
      }

    // Duplicate selectors: compile error (D10). Routed through the shared selector-validation
    // helper in `eo-generics` (this module already depends on generics for the lens macro).
    dev
      .constructive
      .eo
      .generics
      .MacroSelectors
      .reportDuplicateSelectors(
        s"$who[${Type.show[A]}]",
        resolved,
      )

    val selectedNames: List[String] = resolved.map(_._2)

    // Build the NamedTuple type in SELECTOR order. `namesTpe` is the
    // singleton-String names tuple, `valuesTpe` the field-types tuple.
    val selectorSyms: List[Symbol] = selectedNames.map { name =>
      caseFields.find(_.name == name).getOrElse {
        report.errorAndAbort(
          s"$who[${Type.show[A]}]: internal error — field '$name' not found on"
            + s" ${Type.show[A]}."
        )
      }
    }
    val selectorTypes: List[TypeRepr] = selectorSyms.map(sym => aTpe.memberType(sym))

    val namesTpe: TypeRepr =
      selectedNames.foldRight(TypeRepr.of[EmptyTuple]) { (n, acc) =>
        TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(n)), acc))
      }
    val valuesTpe: TypeRepr =
      selectorTypes.foldRight(TypeRepr.of[EmptyTuple]) { (t, acc) =>
        TypeRepr.of[*:].appliedTo(List(t, acc))
      }
    val ntTpe: TypeRepr =
      TypeRepr.of[scala.NamedTuple.NamedTuple].appliedTo(List(namesTpe, valuesTpe))

    ntTpe.asType match
      case '[nt] =>
        val codecNT = summonCodec[nt](
          s"$who[${Type.show[A]}]: no given AvroCodec[${Type.show[nt]}] in scope."
            + s" Derive one via `given AvroCodec[${Type.show[nt]}] = AvroCodec.derived` (which"
            + " auto-summons kindlings' AvroEncoder / AvroDecoder / AvroSchemaFor)."
        )

        // Emit `parent.toFieldsPrism[nt](Array(n1, n2, ...))(using codecNT)`.
        // We splice the field names as a compile-time-known Array[String]
        // so the runtime class doesn't need to re-parse them.
        val namesExpr: Expr[Array[String]] =
          '{ Array[String](${ Varargs(selectedNames.map(Expr(_))) }*) }

        emit[nt](namesExpr, codecNT)

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

  /** Strict variant of [[extractFieldName]] that rejects nested Select chains — routes through the
    * shared `MacroSelectors.extractSingleFieldName` helper in eo-generics to keep the strict
    * receiver-is-Ident rule consistent with the lens macro's selector parsing (and the circe-side
    * `JsonPrismMacro.fields` selector validation).
    */
  private def extractSingleFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    dev.constructive.eo.generics.MacroSelectors.extractSingleFieldName(t)

end AvroPrismMacro
