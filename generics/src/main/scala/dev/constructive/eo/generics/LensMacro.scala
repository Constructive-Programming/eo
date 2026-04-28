package dev.constructive.eo
package generics

import scala.quoted.*

import dev.constructive.eo.optics.{BijectionIso, Optic, SimpleLens}

/** Compile-time derivation of a `Lens` (or `Iso` on full coverage) from one or more case-class
  * accessors.
  *
  * {{{
  * case class Person(age: Int, name: String)
  *
  * val ageL = lens[Person](_.age)                // SimpleLens, complement = remaining fields NT
  * val asTuple = lens[Person](_.name, _.age)     // BijectionIso (full cover)
  * }}}
  *
  *   - Single-selector, partial cover → `SimpleLens[S, A, XA]` with `X` the structural complement
  *     (gives `transform` / `place` / `transfer` for free).
  *   - Multi-selector, partial cover → `SimpleLens` with NamedTuple focus (SELECTOR order) and
  *     NamedTuple complement (DECLARATION order among non-focused fields).
  *   - Full cover at any arity → `BijectionIso[S, S, NT, NT]`.
  *
  * Constructor synthesis routes through Hearth's `CaseClass.parse[S]` + `construct[Id]`, so
  * parameterised / recursive types and Scala 3 enum cases (which lack `.copy`) work uniformly via
  * `new S(...)`.
  */
object LensMacro:

  /** Varargs entry — `transparent inline` so the macro-synthesised concrete subclass (`SimpleLens`
    * partial / `BijectionIso` full) propagates to the call site.
    *
    * @group Constructors
    * @tparam S
    *   source case-class type
    */
  transparent inline def deriveMulti[S](
      inline selectors: (S => Any)*
  ): Optic[S, S, ?, ?, ?] =
    ${ deriveMultiImpl[S]('selectors) }

  def deriveMultiImpl[S: Type](
      selectorsExpr: Expr[Seq[S => Any]]
  )(using q: Quotes): Expr[Optic[S, S, ?, ?, ?]] =
    new HearthLensMacro(q).deriveMulti[S](selectorsExpr)

/** Hearth-backed Lens macro implementation. Brings the high-level `CaseClass` / `Type` views into
  * scope.
  */
final private class HearthLensMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  /** Recover selector exprs, validate arity / cover / duplicates, dispatch:
    *   - 1 selector + partial cover → [[deriveSingle]] (`SimpleLens`).
    *   - N ≥ 2 selectors + partial cover → [[buildMultiLens]] (NT focus + complement).
    *   - full cover at any arity → [[buildMultiIso]] (`BijectionIso`).
    */
  def deriveMulti[S](
      selectorsExpr: Expr[Seq[S => Any]]
  )(using Type[S]): Expr[Optic[S, S, ?, ?, ?]] =
    val selectors: List[Expr[S => Any]] =
      selectorsExpr match
        case Varargs(es) => es.toList
        case other       =>
          // Fallback path if `Varargs.unapply` ever returns None on an inline varargs param.
          report.errorAndAbort(
            s"lens[${Type.prettyPrint[S]}]: could not destructure varargs selector"
              + s" list. Got: ${other.asTerm.show}"
          )

    if selectors.isEmpty then
      report.errorAndAbort(
        s"lens[${Type.prettyPrint[S]}]: requires at least one field selector."
      )

    CaseClass.parse[S].toEither match
      case Left(reason) =>
        report.errorAndAbort(s"lens[${Type.prettyPrint[S]}]: $reason")
      case Right(cc) =>
        val knownFields: List[String] = cc.caseFields.map(_.value.name)

        // Resolve every selector to a field name (position threaded through for error messages).
        val resolved: List[(Int, String)] =
          selectors.zipWithIndex.map { (sel, i) =>
            val name = extractFieldName(sel.asTerm).getOrElse {
              report.errorAndAbort(
                s"lens[${Type.prettyPrint[S]}]: selector at position $i must be a"
                  + " single-field accessor like `_.fieldName`. Nested paths (e.g."
                  + s" `_.a.b`) are not yet supported. Got: ${sel.asTerm.show}"
              )
            }
            if !knownFields.contains(name) then
              report.errorAndAbort(
                s"lens[${Type.prettyPrint[S]}]: '$name' is not a field of "
                  + s"${Type.prettyPrint[S]}. Known fields: ${knownFields.mkString(", ")}"
              )
            (i, name)
          }

        // Duplicate selectors → compile error.
        MacroSelectors.reportDuplicateSelectors(s"lens[${Type.prettyPrint[S]}]", resolved)

        val selectedNames: List[String] = resolved.map(_._2)
        val fullCover: Boolean = selectedNames.toSet == knownFields.toSet

        // Full cover at any arity (including N = 1 on a 1-field case class) → BijectionIso.
        if fullCover then buildMultiIso[S](cc, selectedNames)
        else if selectors.sizeIs == 1 then deriveSingle[S](cc, selectors.head, selectedNames.head)
        else buildMultiLens[S](cc, selectedNames)

  /** Single-selector, partial-cover — emits `SimpleLens[S, A, XA]` with `XA` the NT complement.
    * Rebuilds the selector via `Select.unique` rather than `asExprOf[S => A]` (varargs erases the
    * return type to `Any`).
    */
  private def deriveSingle[S: Type](
      cc: CaseClass[S],
      selector: Expr[S => Any],
      fieldName: String,
  ): Expr[Optic[S, S, ?, ?, ?]] =
    val sTpe = TypeRepr.of[S]
    val focusSym = sTpe.typeSymbol.caseFields.find(_.name == fieldName).getOrElse {
      report.errorAndAbort(
        s"lens[${Type.prettyPrint[S]}]: internal error -- field '$fieldName' not"
          + s" found on ${Type.prettyPrint[S]}."
      )
    }
    val otherFieldSyms = sTpe.typeSymbol.caseFields.filter(_.name != fieldName)
    val aTpe = sTpe.memberType(focusSym)

    aTpe.asType match
      case '[a] =>
        // Reconstitute the selector at static return type `a`.
        val typedSelector: Expr[S => a] =
          '{ (s: S) => ${ Select.unique('{ s }.asTerm, fieldName).asExprOf[a] } }
        buildLens[S, a](cc, fieldName, typedSelector, sTpe, otherFieldSyms)

  /** Build `SimpleLens[S, A, XA]` with `XA = NamedTuple[Names, Values]` over the non-focused fields
    * in declaration order (singleton-String names, original field types). NamedTuple is an opaque
    * alias over its `Values` tuple, so runtime layout is identical to a plain tuple but downstream
    * users get `x.fieldName` ergonomics. `combine` threads through Hearth's `CaseClass.construct`.
    */
  private def buildLens[S: Type, A: Type](
      cc: CaseClass[S],
      fieldName: String,
      selector: Expr[S => A],
      sTpe: TypeRepr,
      otherSyms: List[Symbol],
  ): Expr[SimpleLens[S, A, ?]] =
    val otherTypes: List[TypeRepr] =
      otherSyms.map(sym => sTpe.memberType(sym))

    // xa = NamedTuple[otherNames, otherTypes] (the complement carrier).
    val xaTpe: TypeRepr = namedTupleTypeOf(otherSyms.map(_.name), otherTypes)

    xaTpe.asType match
      case '[xa] =>
        val split: Expr[S => (xa, A)] =
          '{ (s: S) =>
            val x: xa = ${
              // Fully qualified to avoid shadowing by Hearth's own `Expr`. The narrowing to `xa`
              // is sound — the opaque relation `Values <: NamedTuple[Names, Values]` carries it.
              scala
                .quoted
                .Expr
                .ofTupleFromSeq(tupleFieldReads('{ s }.asTerm, otherSyms))
                .asExprOf[xa]
            }
            (x, $selector(s))
          }

        // `a` flows through the splice; -Wunused false-positive silenced via `-Wconf` in build.sbt.
        val combine: Expr[(xa, A) => S] =
          '{ (x: xa, a: A) =>
            ${
              val xTerm = '{ x }.asTerm
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] { (param: Parameter) =>
                if param.name == fieldName then Existential[Expr, A]('{ a }): Expr_??
                else
                  val idx = otherSyms.indexWhere(_.name == param.name)
                  if idx < 0 then
                    report.errorAndAbort(
                      s"lens[${Type.prettyPrint[S]}]: internal error -- "
                        + s"unexpected parameter '${param.name}'."
                    )
                  val tpe = otherTypes(idx)
                  tpe.asType match
                    case '[t] =>
                      val accessor = tupleIndexAccessor[xa, t](xTerm.asExprOf[xa], idx)
                      Existential[Expr, t](accessor): Expr_??
              }
              constructed.getOrElse {
                report.errorAndAbort(
                  s"lens[${Type.prettyPrint[S]}]: failed to call the primary"
                    + s" constructor of ${Type.prettyPrint[S]}."
                )
              }
            }
          }

        '{ SimpleLens[S, A, xa]($selector, $split, $combine) }

  /** Multi-selector, partial-cover — emits `SimpleLens[S, Focus, Complement]` where
    * `Focus = NamedTuple[selectorNames, selectorTypes]` (SELECTOR order) and
    * `Complement = NamedTuple[otherNames, otherTypes]` (DECLARATION order among non-focused).
    * `combine` threads through Hearth's `cc.construct[Id]`, routing each declaration-order
    * parameter to focus or complement.
    */
  private def buildMultiLens[S: Type](
      cc: CaseClass[S],
      selectedNames: List[String],
  ): Expr[Optic[S, S, ?, ?, ?]] =
    val ctx = MultiBuildContext.from[S](selectedNames)
    import ctx.{sTpe, allFieldSyms, selectorSyms, selectorTypes}

    // Declaration-order complement metadata, restricted to non-selected fields.
    val selectedSet: Set[String] = selectedNames.toSet
    val otherSyms: List[Symbol] = allFieldSyms.filterNot(sym => selectedSet.contains(sym.name))
    val otherTypes: List[TypeRepr] = otherSyms.map(sym => sTpe.memberType(sym))

    val focusTpe = namedTupleTypeOf(selectedNames, selectorTypes)
    val complementTpe = namedTupleTypeOf(otherSyms.map(_.name), otherTypes)

    (focusTpe.asType, complementTpe.asType) match
      case ('[focus], '[complement]) =>
        // get: S => focus — selected fields in SELECTOR order packed into a NamedTuple.
        val get: Expr[S => focus] =
          '{ (s: S) =>
            ${
              scala
                .quoted
                .Expr
                .ofTupleFromSeq(tupleFieldReads('{ s }.asTerm, selectorSyms))
                .asExprOf[focus]
            }
          }

        // split: S => (complement, focus) — complement in declaration order, focus in selector order.
        val split: Expr[S => (complement, focus)] =
          '{ (s: S) =>
            val x: complement = ${
              scala
                .quoted
                .Expr
                .ofTupleFromSeq(tupleFieldReads('{ s }.asTerm, otherSyms))
                .asExprOf[complement]
            }
            val a: focus = ${
              scala
                .quoted
                .Expr
                .ofTupleFromSeq(tupleFieldReads('{ s }.asTerm, selectorSyms))
                .asExprOf[focus]
            }
            (x, a)
          }

        // `combine: (complement, focus) => S` — threads the primary constructor
        // through `cc.construct[Id]`, routing each declaration-order parameter to
        // its home (focus / complement) and emitting the matching indexed read.
        //
        // -Wunused false-positives across splices silenced via file-level -Wconf.
        val combine: Expr[(complement, focus) => S] =
          '{ (x: complement, a: focus) =>
            ${
              val xTerm = '{ x }.asTerm
              val aTerm = '{ a }.asTerm
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] { (param: Parameter) =>
                val focusIdx = selectedNames.indexOf(param.name)
                if focusIdx >= 0 then
                  val tpe = selectorTypes(focusIdx)
                  tpe.asType match
                    case '[t] =>
                      Existential[Expr, t](
                        tupleIndexAccessor[focus, t](aTerm.asExprOf[focus], focusIdx)
                      ): Expr_??
                else
                  val complementIdx = otherSyms.indexWhere(_.name == param.name)
                  if complementIdx < 0 then
                    report.errorAndAbort(
                      s"lens[${Type.prettyPrint[S]}]: internal error -- "
                        + s"unexpected parameter '${param.name}'."
                    )
                  val tpe = otherTypes(complementIdx)
                  tpe.asType match
                    case '[t] =>
                      Existential[Expr, t](
                        tupleIndexAccessor[complement, t](
                          xTerm.asExprOf[complement],
                          complementIdx,
                        )
                      ): Expr_??
              }
              constructed.getOrElse {
                report.errorAndAbort(
                  s"lens[${Type.prettyPrint[S]}]: failed to call the primary"
                    + s" constructor of ${Type.prettyPrint[S]}."
                )
              }
            }
          }

        '{ SimpleLens[S, focus, complement]($get, $split, $combine) }

  /** Full-cover codegen — emits `BijectionIso[S, S, Focus, Focus]` when the selector set covers
    * every case field. `get` packs selected fields in SELECTOR order; `reverseGet` threads Hearth's
    * `cc.construct[Id]` through the primary constructor.
    */
  private def buildMultiIso[S: Type](
      cc: CaseClass[S],
      selectedNames: List[String],
  ): Expr[Optic[S, S, ?, ?, ?]] =
    val ctx = MultiBuildContext.from[S](selectedNames)
    import ctx.{selectorSyms, selectorTypes}

    val focusTpe: TypeRepr = namedTupleTypeOf(selectedNames, selectorTypes)

    focusTpe.asType match
      case '[focus] =>
        // get: S => focus — selected fields packed in SELECTOR order.
        val get: Expr[S => focus] =
          '{ (s: S) =>
            ${
              scala
                .quoted
                .Expr
                .ofTupleFromSeq(tupleFieldReads('{ s }.asTerm, selectorSyms))
                .asExprOf[focus]
            }
          }

        // reverseGet: focus => S — `cc.construct[Id]` looks up each declaration-order parameter in
        // the selector-order focus tuple.
        val reverseGet: Expr[focus => S] =
          '{ (a: focus) =>
            ${
              val aTerm = '{ a }.asTerm
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] { (param: Parameter) =>
                val focusIdx = selectedNames.indexOf(param.name)
                if focusIdx < 0 then
                  report.errorAndAbort(
                    s"lens[${Type.prettyPrint[S]}]: internal error -- "
                      + s"unexpected parameter '${param.name}' absent from full-cover focus."
                  )
                val tpe = selectorTypes(focusIdx)
                tpe.asType match
                  case '[t] =>
                    Existential[Expr, t](
                      tupleIndexAccessor[focus, t](aTerm.asExprOf[focus], focusIdx)
                    ): Expr_??
              }
              constructed.getOrElse {
                report.errorAndAbort(
                  s"lens[${Type.prettyPrint[S]}]: failed to call the primary"
                    + s" constructor of ${Type.prettyPrint[S]}."
                )
              }
            }
          }

        '{ BijectionIso[S, S, focus, focus]($get, $reverseGet) }

  /** Build `NamedTuple[Names, Values]` `TypeRepr` from a list of (name, type) pairs. */
  private def namedTupleTypeOf(names: List[String], tpes: List[TypeRepr]): TypeRepr =
    val namesTpe =
      names.foldRight(TypeRepr.of[EmptyTuple]) { (n, acc) =>
        TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(n)), acc))
      }
    val valuesTpe =
      tpes.foldRight(TypeRepr.of[EmptyTuple]) { (t, acc) =>
        TypeRepr.of[*:].appliedTo(List(t, acc))
      }
    TypeRepr.of[scala.NamedTuple.NamedTuple].appliedTo(List(namesTpe, valuesTpe))

  /** Shared prelude for [[buildMultiLens]] / [[buildMultiIso]]. */
  private case class MultiBuildContext(
      sTpe: TypeRepr,
      allFieldSyms: List[Symbol],
      selectorSyms: List[Symbol],
      selectorTypes: List[TypeRepr],
  )

  private object MultiBuildContext:

    def from[S: Type](selectedNames: List[String]): MultiBuildContext =
      val sTpe = TypeRepr.of[S]
      val allFieldSyms: List[Symbol] = sTpe.typeSymbol.caseFields
      val syms: List[Symbol] = selectedNames.map { name =>
        allFieldSyms.find(_.name == name).getOrElse {
          report.errorAndAbort(
            s"lens[${Type.prettyPrint[S]}]: internal error -- field '$name' not"
              + s" found on ${Type.prettyPrint[S]}."
          )
        }
      }
      val tpes: List[TypeRepr] = syms.map(sym => sTpe.memberType(sym))
      MultiBuildContext(sTpe, allFieldSyms, syms, tpes)

  /** Read each `Select.unique(sourceTerm, sym.name)`. */
  private def tupleFieldReads(sourceTerm: Term, fieldSyms: List[Symbol]): List[Expr[Any]] =
    fieldSyms.map(sym => Select.unique(sourceTerm, sym.name).asExpr)

  /** `NamedTuple → Tuple → apply(i) → t` indexed read. NamedTuple's runtime IS the values tuple via
    * opaque subtyping; the cast is erased. `(using Quotes)` keeps the splice's `Quotes` in scope
    * rather than the class-level one.
    */
  private def tupleIndexAccessor[Carrier: Type, T: Type](
      carrier: Expr[Carrier],
      idx: Int,
  )(using Quotes): Expr[T] =
    val idxExpr = scala.quoted.Expr(idx)
    '{ $carrier.asInstanceOf[Tuple].apply($idxExpr).asInstanceOf[T] }

  /** Selector-AST extraction lives on `MacroSelectors`. */
  private def extractFieldName(t: Term): Option[String] =
    MacroSelectors.extractSingleFieldName(t)
