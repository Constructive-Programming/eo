package dev.constructive.eo.generics

import scala.quoted.*

/** Quote-context selector-AST helpers shared between [[LensMacro]] and the `JsonPrismMacro` /
  * `AvroPrismMacro` cursor macros. All parse single-field selector lambdas (`_.fieldName`) and
  * validate non-duplicates; the per-macro divergence is just the error-message tag.
  */
object MacroSelectors:

  /** Loose variant of [[extractSingleFieldName]] — strips `Inlined` / `Typed` wrappers around the
    * lambda AND around its `Select` body, and does NOT require the `Select` receiver to be the bare
    * lambda parameter. Used by the cursor macros' `.field(_.x)` sugar, whose selectors are always
    * single-hop but may arrive wrapped. Kept distinct from [[extractSingleFieldName]] (which
    * rejects nested chains by construction) so callers that need the strict form still have it.
    */
  def extractFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    t match
      case Inlined(_, _, inner)                      => extractFieldName(inner)
      case Typed(inner, _)                           => extractFieldName(inner)
      case Lambda(_, Select(_, name))                => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name))) => Some(name)
      case Lambda(_, Typed(Select(_, name), _))      => Some(name)
      case _                                         => None

  /** The single element type of a Scala collection type `A` (via its `Iterable` base type), or
    * abort with a `who`-tagged error. Shared by the cursor macros' `.at` / `.each` sugar.
    */
  def iterableElementType[A: Type](
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

  /** Strips `Inlined` / `Typed` wrappers and peeks inside the lambda body for a single `Select`
    * whose receiver is exactly the lambda parameter (an `Ident`). Receiver-is-Ident is load-bearing
    * — nested paths like `_.a.b` parse as `Select(Select(Ident(_), "a"), "b")` and fall through to
    * `None` so each macro can produce its own "nested paths" message.
    */
  def extractSingleFieldName(using Quotes)(t: quotes.reflect.Term): Option[String] =
    import quotes.reflect.*
    def oneHop(body: Term): Option[String] =
      body match
        case Inlined(_, _, inner)   => oneHop(inner)
        case Typed(inner, _)        => oneHop(inner)
        case Select(Ident(_), name) => Some(name)
        case _                      => None
    t match
      case Inlined(_, _, inner) => extractSingleFieldName(inner)
      case Typed(inner, _)      => extractSingleFieldName(inner)
      case Lambda(_, body)      => oneHop(body)
      case _                    => None

  /** Reject duplicate field names; aborts on the first duplicate. The `who` tag prefixes the
    * message.
    */
  def reportDuplicateSelectors(using
      Quotes
  )(
      who: String,
      resolved: List[(Int, String)],
  ): Unit =
    import quotes.reflect.*
    val byName: Map[String, List[Int]] = resolved.groupMap(_._2)(_._1)
    byName.find(_._2.sizeIs > 1).foreach {
      case (name, positions) =>
        val sorted = positions.sorted
        report.errorAndAbort(
          s"$who: duplicate field selector '$name' at positions"
            + s" ${sorted.mkString(", ")}. Each field may appear at most once."
        )
    }

  /** Validate a Dynamic-sugar literal field name against `A`'s case-class schema. Returns the field
    * name, its declaration index in `A`, and its widened TypeRepr; aborts with a `who`-tagged error
    * when the name isn't a compile-time literal or `A` has no such case field. Shared backbone of
    * the cursor macros' `selectDynamic` sugar — the caller owns the module-specific codec summon +
    * emit.
    */
  def caseFieldType[A: Type](using
      q: Quotes
  )(
      who: String,
      nameE: Expr[String],
  ): (String, Int, q.reflect.TypeRepr) =
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
    (name, cases.indexWhere(_.name == name), aTpe.memberType(fieldSym).widen)

  /** Validate a `.fields(_.a, _.b, …)` varargs selector list against `A`'s case-class schema and
    * synthesise the SELECTOR-order NamedTuple type: arity ≥ 2, single-hop selectors only, known
    * fields, no duplicates. Returns the selected names (selector order), their declaration indices
    * in `A` (declaration order lookup — the field-schema-naming resolution of issue #35), and the
    * `NamedTuple[names, values]` TypeRepr. Shared backbone of `AvroPrismMacro.fieldsCommon` /
    * `JsonPrismMacro.fieldsCommon` — the caller owns the module-specific codec summon + emit.
    */
  def fieldsSelectorNT[A: Type](using
      q: Quotes
  )(
      who: String,
      selectorsE: Expr[Seq[A => Any]],
  ): (List[String], List[Int], q.reflect.TypeRepr) =
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
    val caseFields = aTpe.typeSymbol.caseFields

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

    reportDuplicateSelectors(s"$who[${Type.show[A]}]", resolved)

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

    (selectedNames, selectedNames.map(knownFields.indexOf), ntTpe)
