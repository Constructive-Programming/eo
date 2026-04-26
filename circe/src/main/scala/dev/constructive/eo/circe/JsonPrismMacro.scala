package dev.constructive.eo.circe

import scala.quoted.*

import io.circe.{Decoder, Encoder}

/** `.field(_.fieldName)` on a `JsonPrism[S]` — macro sugar over [[JsonPrism.widenPath]]. Extracts
  * the field name from the selector AST (same pattern as eo-generics' `lens[S](_.field)`) and emits
  * a cursor-step via `ACursor.downField`.
  *
  * Usage:
  *
  * {{{
  *   val streetPrism: JsonPrism[String] =
  *     codecPrism[Person].field(_.address).field(_.street)
  * }}}
  *
  * Nested paths chain naturally — each `.field` call appends one `downField` to the stored
  * navigation function.
  */
object JsonPrismMacro:

  /** Build a child `JsonPrism[B]` from a parent `JsonPrism[A]` via a compile-time selector
    * `(_.field)` plus in-scope circe `Encoder[B]` and `Decoder[B]`.
    */
  def fieldImpl[A: Type, B: Type](
      parent: Expr[JsonPrism[A]],
      selector: Expr[A => B],
      encB: Expr[Encoder[B]],
      decB: Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonPrism[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "JsonPrism.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `_.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenPath[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Drives the Selectable sugar: `codecPrism[Person].address` compiles to `selectFieldImpl` with
    * `name = "address"`. We look the field up on `A`'s case-class schema, verify it exists, grab
    * its declared type (widened to strip precise singletons), summon `Encoder[B]` / `Decoder[B]`
    * from the enclosing scope, and emit the same `widenPath` call that the explicit `.field(_.x)`
    * macro does.
    *
    * Returns `Expr[Any]` so the `transparent inline def selectDynamic` wrapper can refine the type
    * at each call site.
    */
  def selectFieldImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsonPrism selectDynamic", nameE) {
      [b] => (
          name: String,
          enc: Expr[Encoder[b]],
          dec: Expr[Decoder[b]],
      ) => '{ $parent.widenPath[b](${ Expr(name) })(using $enc, $dec) }
    }

  /** Macro for `jsonPrism.at(i)`. Verifies the parent focus `A` looks like a Scala collection (i.e.
    * derives from `Iterable`), extracts the element type via `A`'s `Iterable` base type, summons
    * the element's circe `Encoder` / `Decoder`, and emits a `widenPathIndex` call.
    */
  def atImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("JsonPrism.at")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonPrism.at: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenPathIndex[b]($iE)(using $enc, $dec) }

  /** Macro for `jsonPrism.each`. Reads the element type from `A`'s `Iterable` base, summons the
    * element's `Encoder` / `Decoder`, and emits a `toTraversal[B]` call that hands the current path
    * off as the new traversal's prefix.
    */
  def eachImpl[A: Type](
      parent: Expr[JsonPrism[A]]
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("JsonPrism.each")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonPrism.each: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.toTraversal[b](using $enc, $dec) }

  /** Macro for `traversal.field(_.x)`. Counterpart to [[fieldImpl]] — extends the traversal's
    * suffix by a named field.
    */
  def fieldTraversalImpl[A: Type, B: Type](
      parent: Expr[JsonTraversal[A]],
      selector: Expr[A => B],
      encB: Expr[Encoder[B]],
      decB: Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonTraversal[B]] =
    import quotes.reflect.*

    val name: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "JsonTraversal.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenSuffix[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Macro for `traversal.at(i)`. Counterpart to [[atImpl]] — extends the traversal's suffix by an
    * array index.
    */
  def atTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = iterableElementType[A]("JsonTraversal.at")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonTraversal.at: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenSuffixIndex[b]($iE)(using $enc, $dec) }

  /** Macro for `codecPrism[A].fields(_.a, _.b, ...)` — multi-field focus. Parses the varargs
    * selector list, validates arity (≥ 2), duplicate-ness, known-fields-ness, non-nested-ness,
    * synthesises a Scala 3 NamedTuple type in SELECTOR order, summons `Encoder[NT]` / `Decoder[NT]`
    * from the enclosing scope, and emits a `toFieldsPrism` call on the parent prism.
    *
    * See the plan's D10 for the full error-message catalogue — every row has an exact-message test
    * in `FieldsMacroErrorSpec` (Unit 5).
    */
  def fieldsImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("JsonPrism.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          enc: Expr[Encoder[nt]],
          dec: Expr[Decoder[nt]],
      ) => '{ $parent.toFieldsPrism[nt]($namesExpr)(using $enc, $dec) }
    }

  /** Macro for `traversal.fields(_.a, _.b, ...)` — multi-field focus on a traversal. Mirror of
    * [[fieldsImpl]] on the traversal side: parses varargs, validates per D10, synthesises the
    * NamedTuple type in SELECTOR order, summons codecs, emits `toFieldsTraversal`.
    */
  def fieldsTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("JsonTraversal.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          enc: Expr[Encoder[nt]],
          dec: Expr[Decoder[nt]],
      ) => '{ $parent.toFieldsTraversal[nt]($namesExpr)(using $enc, $dec) }
    }

  /** Shared backbone for `fieldsImpl` (JsonPrism side) and `fieldsTraversalImpl` (JsonTraversal
    * side). Validates the varargs selector list per D10, synthesises the SELECTOR-order NamedTuple
    * type, summons its `Encoder` / `Decoder`, and hands the four pieces (`namesExpr`, `enc`, `dec`,
    * plus the `Type[nt]` evidence) to the caller-supplied `emit` callback. The two former entry
    * points differed only in the error tag and the final `toFieldsPrism` / `toFieldsTraversal`
    * emit; everything else (~115 lines of validation + NamedTuple construction) was copy-pasted
    * between them.
    */
  private def fieldsCommon[A: Type](
      who: String,
      selectorsE: Expr[Seq[A => Any]],
  )(
      emit: [nt] => (Expr[Array[String]], Expr[Encoder[nt]], Expr[Decoder[nt]]) => Type[
        nt
      ] ?=> Expr[
        Any
      ]
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
    dev.constructive.eo.generics.MacroSelectors.reportDuplicateSelectors(
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
        val (enc, dec) = summonCodecs[nt](role =>
          s"$who[${Type.show[A]}]: no given $role[${Type.show[nt]}] in scope."
            + s" Derive one via `given Codec.AsObject[${Type.show[nt]}] ="
            + " KindlingsCodecAsObject.derive`, or provide one manually."
        )

        // Emit `parent.toFields*[nt](Array(n1, n2, ...))(using enc, dec)`.
        // We splice the field names as a compile-time-known Array[String]
        // so the runtime class doesn't need to re-parse them.
        val namesExpr: Expr[Array[String]] =
          '{ Array[String](${ Varargs(selectedNames.map(Expr(_))) }*) }

        emit[nt](namesExpr, enc, dec)

  /** Macro for `traversal.<fieldName>`. Counterpart to [[selectFieldImpl]] — drives the Dynamic
    * sugar on [[JsonTraversal]] by extending the suffix.
    */
  def selectFieldTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsonTraversal selectDynamic", nameE) {
      [b] => (
          name: String,
          enc: Expr[Encoder[b]],
          dec: Expr[Decoder[b]],
      ) => '{ $parent.widenSuffix[b](${ Expr(name) })(using $enc, $dec) }
    }

  /** Shared backbone for `selectFieldImpl` (JsonPrism side) and `selectFieldTraversalImpl`
    * (JsonTraversal side). Validates the literal-string name, looks the field up on `A`'s
    * case-class schema, widens the field type, summons `Encoder` / `Decoder`, and routes through
    * the caller-supplied `emit` callback for the side-specific final expression (`widenPath` vs
    * `widenSuffix`).
    */
  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Expr[Encoder[b]], Expr[Decoder[b]]) => Type[b] ?=> Expr[Any])(using
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
        val (enc, dec) = summonCodecs[b](role =>
          s"$who: no given $role[${Type
              .show[b]}] in scope for field '$name' of ${Type.show[A]}."
        )
        emit[b](name, enc, dec)

  /** Shared helper: pull the element type out of a Scala collection's `Iterable` base type, or
    * abort with a clear error mentioning the call site (`who`).
    */
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

  /** Shared helper: summon both `Encoder[B]` and `Decoder[B]` from the enclosing scope, with a
    * caller-supplied error message on either failure. Each former call site spelled the same "no
    * given Encoder[…] in scope. …" twice, once for `Encoder` and once for `Decoder`; the caller now
    * passes a single `errorMsg(role)` and the helper plugs in `"Encoder"` / `"Decoder"` for the
    * missing one.
    */
  private def summonCodecs[B: Type](
      errorMsg: String => String
  )(using q: Quotes): (Expr[Encoder[B]], Expr[Decoder[B]]) =
    import quotes.reflect.*
    val enc = Expr.summon[Encoder[B]].getOrElse(report.errorAndAbort(errorMsg("Encoder")))
    val dec = Expr.summon[Decoder[B]].getOrElse(report.errorAndAbort(errorMsg("Decoder")))
    (enc, dec)

  /** Strip `Inlined` / `Typed` wrappers around a lambda and pull the field name out of its body.
    * Mirrors the eo-generics `extractFieldName` helper so both macros agree on what a "single-field
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

  /** Strict variant of [[extractFieldName]] that rejects nested Select chains — routes through
    * the shared `MacroSelectors.extractSingleFieldName` helper in eo-generics to keep the strict
    * receiver-is-Ident rule consistent with the lens macro's selector parsing.
    */
  private def extractSingleFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    dev.constructive.eo.generics.MacroSelectors.extractSingleFieldName(t)
