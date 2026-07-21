package dev.constructive.eo.avro

import scala.quoted.*

import dev.constructive.eo.generics.MacroSelectors

/** Macros backing the drilling sugar on [[AvroPrism]] and [[AvroTraversal]] — `.field(_.x)`,
  * `selectDynamic`, `.at(i)`, `.union[Branch]`, `.each`, `.fields(...)`. Mirror of
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

    val name: String = MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "AvroPrism.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `_.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenPath[B](${ Expr(name) }, ${ Expr(declIndexOf[A](name)) })(using $codecB)
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
    ) { [b] => (name: String, declIdx: Int, codecB: Expr[AvroCodec[b]]) =>
      '{ $parent.widenPath[b](${ Expr(name) }, ${ Expr(declIdx) })(using $codecB) }
    }

  /** Macro for `.at(i)`. Verifies `A <: Iterable`, extracts the element type, summons the codec,
    * emits `widenPathIndex`.
    */
  def atImpl[A: Type](
      parent: Expr[AvroPrism[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("AvroPrism.at")

    elemTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"AvroPrism.at: no given AvroCodec[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenPathIndex[b]($iE)(using $codecB) }

  /** `.union[Branch]` — drill into one alternative of a union-shaped focus. Supported parent
    * shapes: `Option[T]` (user writes `.union[T]`), sealed traits, Scala 3 `enum`, and Scala 3
    * untagged unions `A | B | C` (`Branch` must be one of the members). Branch identifier resolves
    * at runtime off `AvroCodec[Branch].schema.getFullName` (robust against kindlings naming drift).
    */
  def unionImpl[A: Type, B: Type](
      parent: Expr[AvroPrism[A]]
  )(using q: Quotes): Expr[Any] =
    val codecB = validateUnionBranch[A, B]("AvroPrism")
    // Branch name resolved at runtime off the codec schema (robust against kindlings naming).
    '{
      val branchName: String = $codecB.schema.getFullName
      $parent.widenPathUnion[B](branchName)(using $codecB)
    }

  /** Traversal counterpart to [[unionImpl]] — narrows the per-element focus to one union branch by
    * appending a [[PathStep.UnionBranch]] to the suffix. Same validation as the prism path.
    */
  def unionTraversalImpl[A: Type, B: Type](
      parent: Expr[AvroTraversal[A]]
  )(using q: Quotes): Expr[Any] =
    val codecB = validateUnionBranch[A, B]("AvroTraversal")
    '{
      val branchName: String = $codecB.schema.getFullName
      $parent.widenSuffixUnion[B](branchName)(using $codecB)
    }

  /** Shared `.union[Branch]` validation for the prism and traversal paths. Confirms the parent
    * focus `A` is a union-shaped type (`Option[T]`, sealed trait, Scala 3 `enum`, or an untagged
    * union `A | B | C`) with `B` as a valid alternative, and summons `AvroCodec[B]`. `label` names
    * the caller in diagnostics (`"AvroPrism"` / `"AvroTraversal"`).
    */
  private def validateUnionBranch[A: Type, B: Type](
      label: String
  )(using q: Quotes): Expr[AvroCodec[B]] =
    import quotes.reflect.*

    val aTpe = TypeRepr.of[A].dealias
    val bTpe = TypeRepr.of[B].dealias

    // Flatten a (possibly nested) Scala 3 untagged union `A | B | C` into its leaf members.
    def unionMembers(t: TypeRepr): List[TypeRepr] =
      t.dealias match
        case OrType(l, r) => unionMembers(l) ++ unionMembers(r)
        case other        => List(other)

    // Option[T]: kindlings emits union<null, T>; the user writes .union[T].
    val optionElemTpe: Option[TypeRepr] =
      aTpe match
        case AppliedType(tycon, elem :: Nil) if tycon =:= TypeRepr.of[Option] =>
          Some(elem.dealias.widen)
        case _ => None

    aTpe match
      // Scala 3 untagged union `A | B | C`: verify Branch is one of the members. Kindlings derives
      // the same UNION<members...> schema as a sealed trait, so runtime resolution off the branch
      // codec's getFullName (below) is identical — only the compile-time shape check differs.
      case OrType(_, _) =>
        val members = unionMembers(aTpe)
        val matched = members.exists(t => bTpe =:= t || bTpe =:= t.widen)
        if !matched then
          val knownNames = members.map(_.show).mkString(" | ")
          report.errorAndAbort(
            s"$label.union[${Type.show[B]}]: ${Type.show[B]} is not a member of the union"
              + s" ${Type.show[A]}. Members: $knownNames."
          )

      case _ if optionElemTpe.isDefined =>
        val elem = optionElemTpe.get
        if !(bTpe =:= elem) then
          report.errorAndAbort(
            s"$label.union[${Type.show[B]}]: parent focus is Option[${elem.show}];"
              + s" the only valid branch is ${elem.show} (got ${Type.show[B]})."
          )

      case _ =>
        // Sealed trait / Scala 3 enum: verify Branch is among the direct children.
        val aSym = aTpe.typeSymbol
        val isSealed = aSym.flags.is(Flags.Sealed)
        val isEnum = aSym.flags.is(Flags.Enum)
        if !isSealed && !isEnum then
          report.errorAndAbort(
            s"$label.union[${Type.show[B]}]: parent focus ${Type.show[A]} is not a union-shaped"
              + " type. Expected: a sealed trait, a Scala 3 `enum`, an untagged union `A | B`, or"
              + " `Option[T]`."
          )

        val children: List[Symbol] = aSym.children
        if children.isEmpty then
          report.errorAndAbort(
            s"$label.union[${Type.show[B]}]: ${Type.show[A]} has no direct children;"
              + " cannot resolve a union alternative."
          )

        val childTypes: List[TypeRepr] = children.map(c => c.typeRef.dealias)
        val matched = childTypes.exists(t => bTpe =:= t || bTpe =:= t.widen)
        if !matched then
          val knownNames = children.map(_.name).mkString(", ")
          report.errorAndAbort(
            s"$label.union[${Type.show[B]}]: ${Type.show[B]} is not a known alternative"
              + s" of ${Type.show[A]}. Known alternatives: $knownNames."
          )

    summonCodec[B](
      s"$label.union[${Type.show[B]}]: no given AvroCodec[${Type.show[B]}] in scope."
        + s" Derive one via `given AvroCodec[${Type.show[B]}] = AvroCodec.derived` (which"
        + " auto-summons kindlings' AvroEncoder / AvroDecoder / AvroSchemaFor)."
    )

  /** `.each` — emits `toTraversal[B]` over `A`'s element type. */
  def eachImpl[A: Type](
      parent: Expr[AvroPrism[A]]
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("AvroPrism.each")

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

    val name: String = MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "AvroTraversal.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenSuffix[B](${ Expr(name) }, ${ Expr(declIndexOf[A](name)) })(using $codecB)
    }

  /** Traversal counterpart to [[atImpl]] — extends the suffix by an array index. */
  def atTraversalImpl[A: Type](
      parent: Expr[AvroTraversal[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("AvroTraversal.at")

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
          declIdxsExpr: Expr[Array[Int]],
          codecNT: Expr[AvroCodec[nt]],
      ) => '{ $parent.toFieldsPrism[nt]($namesExpr, $declIdxsExpr)(using $codecNT) }
    }

  /** Traversal counterpart to [[fieldsImpl]]. */
  def fieldsTraversalImpl[A: Type](
      parent: Expr[AvroTraversal[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("AvroTraversal.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          declIdxsExpr: Expr[Array[Int]],
          codecNT: Expr[AvroCodec[nt]],
      ) => '{ $parent.toFieldsTraversal[nt]($namesExpr, $declIdxsExpr)(using $codecNT) }
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
    ) { [b] => (name: String, declIdx: Int, codecB: Expr[AvroCodec[b]]) =>
      '{ $parent.widenSuffix[b](${ Expr(name) }, ${ Expr(declIdx) })(using $codecB) }
    }

  /** Shared backbone for [[fieldsImpl]] / [[fieldsTraversalImpl]] — validation + SELECTOR-order
    * NamedTuple synthesis live in the shared `MacroSelectors.fieldsSelectorNT` (eo-generics); this
    * stub owns the module-specific parts: the [[AvroCodec]] summon (vs circe's `Encoder` /
    * `Decoder` pair, with the hint pointing at `AvroCodec.derived`) and the decl-index array
    * (schema field names resolve by position at construction time — issue #35).
    */
  private def fieldsCommon[A: Type](
      who: String,
      selectorsE: Expr[Seq[A => Any]],
  )(
      emit: [nt] => (
          Expr[Array[String]],
          Expr[Array[Int]],
          Expr[AvroCodec[nt]],
      ) => Type[nt] ?=> Expr[Any]
  )(using q: Quotes): Expr[Any] =
    val (selectedNames, declIdxs, ntTpe) = MacroSelectors.fieldsSelectorNT[A](who, selectorsE)
    ntTpe.asType match
      case '[nt] =>
        val codecNT = summonCodec[nt](
          s"$who[${Type.show[A]}]: no given AvroCodec[${Type.show[nt]}] in scope."
            + s" Derive one via `given AvroCodec[${Type.show[nt]}] = AvroCodec.derived` (which"
            + " auto-summons kindlings' AvroEncoder / AvroDecoder / AvroSchemaFor)."
        )
        val namesExpr: Expr[Array[String]] =
          '{ Array[String](${ Varargs(selectedNames.map(Expr(_))) }*) }
        val declIdxsExpr: Expr[Array[Int]] =
          '{ Array[Int](${ Varargs(declIdxs.map(Expr(_))) }*) }
        emit[nt](namesExpr, declIdxsExpr, codecNT)

  // ---- Shared helpers ----------------------------------------------------------

  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Int, Expr[AvroCodec[b]]) => Type[b] ?=> Expr[Any])(using
      q: Quotes
  ): Expr[Any] =
    val (name, declIdx, fieldTpe) = MacroSelectors.caseFieldType[A](who, nameE)
    fieldTpe.asType match
      case '[b] =>
        val codecB = summonCodec[b](
          s"$who: no given AvroCodec[${Type.show[b]}] in scope for field '$name' of ${Type.show[A]}."
        )
        emit[b](name, declIdx, codecB)

  /** Declaration index of case field `name` in `A` (`-1` when `A` isn't a case class / has no such
    * case field — a NamedTuple parent, say). Fed to the widen helpers so schema field names resolve
    * by position; `-1` makes them fall back to the literal name (issue #35).
    */
  private def declIndexOf[A: Type](name: String)(using q: Quotes): Int =
    import quotes.reflect.*
    TypeRepr.of[A].typeSymbol.caseFields.indexWhere(_.name == name)

  /** Summon `AvroCodec[B]` with a caller-supplied error message. */
  private def summonCodec[B: Type](
      errorMsg: String
  )(using q: Quotes): Expr[AvroCodec[B]] =
    import quotes.reflect.*
    Expr.summon[AvroCodec[B]].getOrElse(report.errorAndAbort(errorMsg))

end AvroPrismMacro
