package dev.constructive.eo.avro

import scala.quoted.*

/** Macros backing `AvroPrism.field(_.x)`, `selectDynamic`, `at(i)`, `union[Branch]`. Mirror of
  * `circe.JsonPrismMacro`, with two-given codec summoning (`AvroEncoder + AvroDecoder`) collapsed
  * to one [[AvroCodec]] wrapper. Uses `'{ this }` rather than `'this` (scalafix-parser limitation).
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

  /** Drives `codecPrism[Person].name`. Looks `name` up on `A`'s schema, summons `AvroCodec[B]`,
    * emits `widenPath`. Returns `Expr[Any]` so `transparent inline` refines per call site.
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

  /** Macro for `.at(i)`. Verifies `A <: Iterable`, extracts the element type, summons the codec,
    * emits `widenPathIndex`.
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

  /** `.union[Branch]` — drill into one alternative of a union-shaped focus. Supported parent
    * shapes: `Option[T]` (user writes `.union[T]`), sealed traits, Scala 3 `enum`. Scala 3 untagged
    * unions (`A | B`) abort. Branch identifier resolves at runtime off
    * `AvroCodec[Branch].schema.getFullName` (robust against kindlings naming drift).
    */
  def unionImpl[A: Type, B: Type](
      parent: Expr[AvroPrism[A]]
  )(using q: Quotes): Expr[Any] =
    import quotes.reflect.*

    val aTpe = TypeRepr.of[A].dealias
    val bTpe = TypeRepr.of[B].dealias

    // Reject Scala 3 untagged unions — kindlings doesn't derive for these.
    aTpe match
      case OrType(_, _) =>
        report.errorAndAbort(
          s"AvroPrism.union[${Type.show[B]}]: parent focus ${Type
              .show[A]} is a Scala 3 untagged union type; kindlings-avro-derivation does not"
            + " support these. Use a sealed trait, Scala 3 `enum`, or `Option[T]` instead."
        )
      case _ => ()

    // Option[T]: kindlings emits union<null, T>; the user writes .union[T].
    val optionElemTpe: Option[TypeRepr] =
      aTpe match
        case AppliedType(tycon, elem :: Nil) if tycon =:= TypeRepr.of[Option] =>
          Some(elem.dealias.widen)
        case _ => None

    optionElemTpe match
      case Some(elem) =>
        if !(bTpe =:= elem) then
          report.errorAndAbort(
            s"AvroPrism.union[${Type.show[B]}]: parent focus is Option[${elem.show}];"
              + s" the only valid branch is ${elem.show} (got ${Type.show[B]})."
          )
      case None =>
        // Sealed trait / Scala 3 enum: verify Branch is among the direct children.
        val aSym = aTpe.typeSymbol
        val isSealed = aSym.flags.is(Flags.Sealed)
        val isEnum = aSym.flags.is(Flags.Enum)
        if !isSealed && !isEnum then
          report.errorAndAbort(
            s"AvroPrism.union[${Type.show[B]}]: parent focus ${Type.show[A]} is not a union-shaped"
              + " type. Expected: a sealed trait, a Scala 3 `enum`, or `Option[T]`. Scala 3 untagged"
              + " union types (`A | B`) are not supported by kindlings."
          )

        val children: List[Symbol] = aSym.children
        if children.isEmpty then
          report.errorAndAbort(
            s"AvroPrism.union[${Type.show[B]}]: ${Type.show[A]} has no direct children;"
              + " cannot resolve a union alternative."
          )

        val childTypes: List[TypeRepr] = children.map(c => c.typeRef.dealias)
        val matched = childTypes.exists(t => bTpe =:= t || bTpe =:= t.widen)
        if !matched then
          val knownNames = children.map(_.name).mkString(", ")
          report.errorAndAbort(
            s"AvroPrism.union[${Type.show[B]}]: ${Type.show[B]} is not a known alternative"
              + s" of ${Type.show[A]}. Known alternatives: $knownNames."
          )

    val codecB = summonCodec[B](
      s"AvroPrism.union[${Type.show[B]}]: no given AvroCodec[${Type.show[B]}] in scope."
        + s" Derive one via `given AvroCodec[${Type.show[B]}] = AvroCodec.derived` (which"
        + " auto-summons kindlings' AvroEncoder / AvroDecoder / AvroSchemaFor)."
    )
    // Branch name resolved at runtime off the codec schema (robust against kindlings naming).
    '{
      val branchName: String = $codecB.schema.getFullName
      $parent.widenPathUnion[B](branchName)(using $codecB)
    }

  /** `.each` — emits `toTraversal[B]` over `A`'s element type. */
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

  /** Traversal counterpart to [[fieldImpl]] — extends the suffix by a named field. */
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

  /** Traversal counterpart to [[atImpl]] — extends the suffix by an array index. */
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

  /** `.fields(_.a, _.b, ...)` — multi-field focus. Validates arity ≥ 2, duplicate-ness, known
    * fields, non-nested. Synthesises an NT in SELECTOR order, summons `AvroCodec[NT]`, emits
    * `toFieldsPrism`. Mirrors `JsonPrismMacro.fieldsImpl`.
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

  /** Traversal counterpart to [[fieldsImpl]]. */
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

  /** Traversal counterpart to [[selectFieldImpl]] — drives Dynamic sugar by extending the suffix.
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

    // Duplicate selectors → compile error (via the shared MacroSelectors helper in eo-generics).
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

    // NamedTuple type in SELECTOR order (singleton-String names + field-types tuple).
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

        // Compile-time-known Array[String] of field names.
        val namesExpr: Expr[Array[String]] =
          '{ Array[String](${ Varargs(selectedNames.map(Expr(_))) }*) }

        emit[nt](namesExpr, codecNT)

  // ---- Shared helpers ----------------------------------------------------------

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

  /** Summon `AvroCodec[B]` with a caller-supplied error message. */
  private def summonCodec[B: Type](
      errorMsg: String
  )(using q: Quotes): Expr[AvroCodec[B]] =
    import quotes.reflect.*
    Expr.summon[AvroCodec[B]].getOrElse(report.errorAndAbort(errorMsg))

  /** Pull the field name out of a `_.field` selector lambda. */
  private def extractFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    t match
      case Inlined(_, _, inner)                      => extractFieldName(inner)
      case Typed(inner, _)                           => extractFieldName(inner)
      case Lambda(_, Select(_, name))                => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name))) => Some(name)
      case Lambda(_, Typed(Select(_, name), _))      => Some(name)
      case _                                         => None

  /** Strict variant — rejects nested Select chains; routes to the shared
    * `MacroSelectors.extractSingleFieldName`.
    */
  private def extractSingleFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    dev.constructive.eo.generics.MacroSelectors.extractSingleFieldName(t)

end AvroPrismMacro
