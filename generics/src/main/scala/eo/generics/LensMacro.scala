package eo
package generics

import scala.quoted.*

import eo.optics.{BijectionIso, Optic, SimpleLens}

/** Compile-time derivation of a `Lens` (or, on full coverage, an `Iso`) from one or more
  * single-field case-class accessors.
  *
  * Usage:
  * {{{
  * import eo.generics.lens
  * case class Person(age: Int, name: String)
  *
  * // Single-selector, partial cover — `SimpleLens[Person, Int, <NamedTuple complement>]`.
  * val ageL = lens[Person](_.age)
  *
  * // Multi-selector, partial cover — `SimpleLens[Person, <NamedTuple focus>, <NamedTuple complement>]`.
  * // Focus NamedTuple is in SELECTOR order, complement in DECLARATION order among non-focused fields.
  * // (Not applicable on `Person` alone — `Person` has only 2 fields, so 2 selectors = full cover.)
  *
  * // Full cover — `BijectionIso[Person, Person, <NamedTuple focus>, <NamedTuple focus>]`.
  * val asTuple = lens[Person](_.name, _.age)          // 2-of-2 in declaration order
  * val asReversed = lens[Person](_.age, _.name)       // 2-of-2 in selector order (reversed)
  * }}}
  *
  * Implementation notes:
  *   - The macro exposes the actual *structural complement* of `S` as the optic's `X` parameter on
  *     the single-selector path: for a 2-field case class focused on one field, `X` is the type of
  *     the remaining field. That gives `transform`, `place`, and `transfer` the evidence they need
  *     for free -- no companion `given` required at the call site.
  *   - Each selector lambda is parsed with vanilla `quotes.reflect` pattern matching (Hearth has no
  *     surface for "the AST of an anonymous function").
  *   - `combine` is built through Hearth's [[hearth.typed.Classes]] view: `CaseClass.parse[S]`
  *     validates that S is a real case class, and `construct[Id]` emits the primary constructor
  *     call with `a` at the target position and `x` at the sibling position.
  *   - Routing constructor synthesis through Hearth means recursive / parameterised types and Scala
  *     3 enum cases (which lack a `.copy` method) are both handled uniformly -- `new T(...)` works
  *     for both.
  *   - N-field case classes are supported through a `NamedTuple` complement — the per-class sibling
  *     fields are encoded as a compile-time-known named tuple, so the derived lens carries
  *     structural evidence for `transform` / `place` / `transfer` regardless of the record's arity.
  *   - The varargs entry (`lens[S](_.a, _.b, ...)`) dispatches on arity and cover status at macro
  *     time. Single-selector + partial cover runs the legacy codegen path unchanged;
  *     single-selector + full cover (one-field case classes) and multi-selector arms emit a
  *     NamedTuple focus.
  */
object LensMacro:

  // `transparent inline` so the specific concrete return subclass the macro synthesises
  // (`SimpleLens[S, A, XA]` on partial cover, `BijectionIso[S, S, T, T]` on full cover)
  // propagates to the call site. The declared `Optic[S, S, ?, ?, ?]` is the narrowest
  // cats-eo supertype spanning both arms; the call-site type is always a concrete
  // subclass, so `.andThen` picks up the fused concrete-subclass overloads.
  transparent inline def deriveMulti[S](
      inline selectors: (S => Any)*
  ): Optic[S, S, ?, ?, ?] =
    ${ deriveMultiImpl[S]('selectors) }

  def deriveMultiImpl[S: Type](
      selectorsExpr: Expr[Seq[S => Any]]
  )(using q: Quotes): Expr[Optic[S, S, ?, ?, ?]] =
    new HearthLensMacro(q).deriveMulti[S](selectorsExpr)

/** Hearth-backed Lens macro implementation, extends `_root_.hearth.MacroCommonsScala3` so the
  * high-level `CaseClass` / `Type` views are in scope.
  */
final private class HearthLensMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  /** Entry point for the varargs macro. Recovers the individual selector expressions, validates
    * arity + cover + duplicates per the D6 diagnostic catalogue, and dispatches to the appropriate
    * codegen arm:
    *
    *   - partial cover, 1 selector → legacy `SimpleLens` path ([[deriveSingle]]);
    *   - partial cover, N ≥ 2 selectors → multi-field Lens with NamedTuple focus + complement
    *     ([[buildMultiLens]]);
    *   - full cover at any arity (including N = 1 on a 1-field case class per D2) →
    *     `BijectionIso[S, S, Focus, Focus]` with NamedTuple focus ([[buildMultiIso]]).
    */
  def deriveMulti[S](
      selectorsExpr: Expr[Seq[S => Any]]
  )(using Type[S]): Expr[Optic[S, S, ?, ?, ?]] =
    val selectors: List[Expr[S => Any]] =
      selectorsExpr match
        case Varargs(es) => es.toList
        case other       =>
          // Fallback for OQ3: if `Varargs.unapply` ever returns None on an `inline`
          // varargs param, walk the AST manually. Under Scala 3.8.3 we haven't seen
          // this, but the fallback path preserves correctness and points the user
          // at the offending expression.
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

        // Resolve every selector to a field name, surfacing the position so error
        // messages can point at the offending selector.
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

        // Duplicate selectors are a compile error (D3) — silent dedupe hides
        // copy-paste bugs; fail-fast gives a sharp signal.
        val byName: Map[String, List[Int]] =
          resolved.groupMap(_._2)(_._1)
        byName.find(_._2.sizeIs > 1).foreach {
          case (name, positions) =>
            val sorted = positions.sorted
            report.errorAndAbort(
              s"lens[${Type.prettyPrint[S]}]: duplicate field selector '$name' at"
                + s" positions ${sorted.mkString(", ")}. Each field may appear at most once."
            )
        }

        val selectedNames: List[String] = resolved.map(_._2)
        val fullCover: Boolean = selectedNames.toSet == knownFields.toSet

        // Arity + cover dispatch per D7. Full cover at ANY arity — including N = 1 on
        // a 1-field case class — emits a `BijectionIso` per D2. The behaviour change
        // for `lens[Wrapper](_.value)` on single-field wrappers is accepted pre-release
        // at 0.1.0-SNAPSHOT.
        if fullCover then buildMultiIso[S](cc, selectedNames)
        else if selectors.sizeIs == 1 then deriveSingle[S](cc, selectors.head, selectedNames.head)
        else buildMultiLens[S](cc, selectedNames)

  /** Single-selector, partial-cover codegen — emits `SimpleLens[S, A, XA]` exactly as the legacy
    * `lens[Person](_.age)` form did. `XA` is the `NamedTuple` complement over the non-focused
    * fields in declaration order.
    *
    * The selector arrives from the varargs entry as `S => Any` (the erased varargs element type),
    * so we do NOT cast it with `asExprOf[S => A]` (that would fail at `ExprCastException`).
    * Instead, we rebuild the selector from a fresh `Select.unique(s, fieldName)` lambda whose
    * return type IS the member type `A`.
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
        // Reconstitute the selector at the proper static return type so the
        // builder can embed it into the emitted quote. `selector` is typed as
        // `S => Any` due to varargs erasure of the return type.
        val typedSelector: Expr[S => a] =
          '{ (s: S) => ${ Select.unique('{ s }.asTerm, fieldName).asExprOf[a] } }
        buildLens[S, a](cc, fieldName, typedSelector, sTpe, otherFieldSyms)

  /** Build a `SimpleLens[S, A, XA]` for a case class with any number of fields.
    *
    * The complement `XA` is synthesised as a Scala-3 `NamedTuple[Names, Values]` over the
    * non-focused fields. Both the field names (as singleton-String types) and the field types are
    * reconstructed at macro time from the primary constructor's parameter list, preserving
    * declaration order:
    *
    * * 1-field record → `XA = NamedTuple[EmptyTuple, EmptyTuple]` * 2-field record → `XA =
    * NamedTuple["sibling" *: EmptyTuple, Sibling *: EmptyTuple]` * N-field record → `XA =
    * NamedTuple["n1" *: "n2" *: …, T1 *: T2 *: …]`
    *
    * Since `NamedTuple` is an opaque alias over its `Values` tuple, the runtime representation is
    * identical to a plain `T1 *: T2 *: …` — no extra boxing. The upgrade over plain `Tuple` buys
    * downstream users pattern-match ergonomics (`x.fieldName` at the call site) without a perf
    * cost.
    *
    * `split` reads each non-focused field and packs them with `Expr.ofTupleFromSeq`, then narrows
    * the result to `xa` via `asExprOf` (safe because the `Values` tuple IS-A `NamedTuple` through
    * opaque subtyping). `combine` reads back via `.toTuple.apply(i).asInstanceOf[T_i]` and threads
    * the focused value through the primary constructor via Hearth's `CaseClass.construct`.
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

    // Names tuple: the singleton-String types of the non-focused
    // fields, in declaration order.
    val namesTpe: TypeRepr =
      otherSyms.foldRight(TypeRepr.of[EmptyTuple]) { (sym, acc) =>
        val nameLit = ConstantType(StringConstant(sym.name))
        TypeRepr.of[*:].appliedTo(List(nameLit, acc))
      }

    // Values tuple: the field types of the non-focused fields, in
    // declaration order.
    val valuesTpe: TypeRepr =
      otherTypes.foldRight(TypeRepr.of[EmptyTuple]) { (t, acc) =>
        TypeRepr.of[*:].appliedTo(List(t, acc))
      }

    // xa = NamedTuple[namesTpe, valuesTpe] — the full named-tuple
    // carrier for the complement.
    val xaTpe: TypeRepr =
      TypeRepr
        .of[scala.NamedTuple.NamedTuple]
        .appliedTo(List(namesTpe, valuesTpe))

    xaTpe.asType match
      case '[xa] =>
        val split: Expr[S => (xa, A)] =
          '{ (s: S) =>
            val x: xa = ${
              // `Expr.ofTupleFromSeq` packs a Seq[Expr[Any]] into an
              // `Expr[Tuple]`. Fully qualified to avoid shadowing by
              // Hearth's own `Expr` companion. The narrowing to `xa`
              // is sound because the NamedTuple's runtime storage
              // IS-A Tuple — the opaque-subtype relation
              // `Values <: NamedTuple[Names, Values]` carries the
              // value through the asExprOf check.
              val otherReads: List[scala.quoted.Expr[Any]] =
                otherSyms.map { sym =>
                  Select.unique('{ s }.asTerm, sym.name).asExpr
                }
              scala.quoted.Expr.ofTupleFromSeq(otherReads).asExprOf[xa]
            }
            (x, $selector(s))
          }

        // `a` is threaded through the nested splice via `'{ a }` on the
        // `Existential[Expr, A]` line below — `-Wunused:explicits` can't
        // see across the quote/splice boundary, so it flags `a` as
        // unused. Silenced module-level via `-Wconf` in build.sbt.
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
                      // NamedTuple's runtime representation IS the
                      // underlying values tuple (opaque subtyping:
                      // `Values <: NamedTuple[Names, Values]`), so
                      // `asInstanceOf[Tuple]` is an erased cast the
                      // JVM absorbs at zero cost. `.apply(i)` then
                      // reads the position; the final
                      // `asInstanceOf[t]` re-types the boxed slot
                      // back to the field's static type.
                      val xExpr = xTerm.asExprOf[xa]
                      val idxExpr = scala.quoted.Expr(idx)
                      val accessor: Expr[t] =
                        '{
                          $xExpr
                            .asInstanceOf[Tuple]
                            .apply($idxExpr)
                            .asInstanceOf[t]
                        }
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

  /** Multi-selector, partial-cover codegen — emits a `SimpleLens[S, Focus, Complement]` with:
    *
    *   - `Focus` = `NamedTuple[selectorNames, selectorTypes]` in SELECTOR ORDER (D1).
    *   - `Complement` = `NamedTuple[otherNames, otherTypes]` in DECLARATION ORDER among the
    *     non-focused fields (D1).
    *
    * `split: S => (Complement, Focus)` reads each field via `Select.unique`, packs the focus /
    * complement tuples via `Expr.ofTupleFromSeq`, and narrows through `asExprOf` (sound because the
    * underlying values tuple IS-A NamedTuple through the opaque-subtype relation
    * `Values <: NamedTuple[Names, Values]`).
    *
    * `combine: (Complement, Focus) => S` threads the primary constructor through Hearth's
    * `cc.construct[Id]`: for each declaration-order parameter it decides whether the value is in
    * the focus (selector-order index lookup) or the complement (declaration-order-among-
    * non-focused index lookup) and emits the matching
    * `<named-tuple>.asInstanceOf[Tuple].apply(i).asInstanceOf[t]` read.
    */
  private def buildMultiLens[S: Type](
      cc: CaseClass[S],
      selectedNames: List[String],
  ): Expr[Optic[S, S, ?, ?, ?]] =
    val sTpe = TypeRepr.of[S]
    val allFieldSyms: List[Symbol] = sTpe.typeSymbol.caseFields

    // Selector-order focus metadata. `selectorSyms(i)` is the symbol of the
    // i-th selector's target field; `selectorTypes(i)` its field type.
    val selectorSyms: List[Symbol] = selectedNames.map { name =>
      allFieldSyms.find(_.name == name).getOrElse {
        report.errorAndAbort(
          s"lens[${Type.prettyPrint[S]}]: internal error -- field '$name' not"
            + s" found on ${Type.prettyPrint[S]}."
        )
      }
    }
    val selectorTypes: List[TypeRepr] = selectorSyms.map(sym => sTpe.memberType(sym))

    // Declaration-order complement metadata, restricted to non-selected fields.
    val selectedSet: Set[String] = selectedNames.toSet
    val otherSyms: List[Symbol] = allFieldSyms.filterNot(sym => selectedSet.contains(sym.name))
    val otherTypes: List[TypeRepr] = otherSyms.map(sym => sTpe.memberType(sym))

    // Build a NamedTuple[Names, Values] type from a list of symbols + types.
    def namedTupleTypeOf(names: List[String], tpes: List[TypeRepr]): TypeRepr =
      val namesTpe = names.foldRight(TypeRepr.of[EmptyTuple]) { (n, acc) =>
        TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(n)), acc))
      }
      val valuesTpe = tpes.foldRight(TypeRepr.of[EmptyTuple]) { (t, acc) =>
        TypeRepr.of[*:].appliedTo(List(t, acc))
      }
      TypeRepr
        .of[scala.NamedTuple.NamedTuple]
        .appliedTo(List(namesTpe, valuesTpe))

    val focusTpe = namedTupleTypeOf(selectedNames, selectorTypes)
    val complementTpe = namedTupleTypeOf(otherSyms.map(_.name), otherTypes)

    (focusTpe.asType, complementTpe.asType) match
      case ('[focus], '[complement]) =>
        // `get: S => focus` — reads the selected fields in SELECTOR order and packs
        // them into a NamedTuple via `Expr.ofTupleFromSeq` + `asExprOf`.
        val get: Expr[S => focus] =
          '{ (s: S) =>
            ${
              val focusReads: List[scala.quoted.Expr[Any]] =
                selectorSyms.map { sym =>
                  Select.unique('{ s }.asTerm, sym.name).asExpr
                }
              scala.quoted.Expr.ofTupleFromSeq(focusReads).asExprOf[focus]
            }
          }

        // `split: S => (complement, focus)` — reads complement (declaration order)
        // and focus (selector order), returns the pair. Used by `SimpleLens.to`.
        val split: Expr[S => (complement, focus)] =
          '{ (s: S) =>
            val x: complement = ${
              val otherReads: List[scala.quoted.Expr[Any]] =
                otherSyms.map { sym =>
                  Select.unique('{ s }.asTerm, sym.name).asExpr
                }
              scala.quoted.Expr.ofTupleFromSeq(otherReads).asExprOf[complement]
            }
            val a: focus = ${
              val focusReads: List[scala.quoted.Expr[Any]] =
                selectorSyms.map { sym =>
                  Select.unique('{ s }.asTerm, sym.name).asExpr
                }
              scala.quoted.Expr.ofTupleFromSeq(focusReads).asExprOf[focus]
            }
            (x, a)
          }

        // `combine: (complement, focus) => S` — threads the primary constructor
        // through `cc.construct[Id]`, routing each declaration-order parameter to
        // its home (focus / complement) and emitting the matching indexed read.
        //
        // `x` and `a` are threaded into the nested splice via `'{ x }` / `'{ a }`;
        // `-Wunused:explicits` can't see across quote/splice boundaries and the
        // file-level `-Wconf` silences the false positives.
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
                      val aExpr = aTerm.asExprOf[focus]
                      val idxExpr = scala.quoted.Expr(focusIdx)
                      val accessor: Expr[t] =
                        '{
                          $aExpr
                            .asInstanceOf[Tuple]
                            .apply($idxExpr)
                            .asInstanceOf[t]
                        }
                      Existential[Expr, t](accessor): Expr_??
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
                      val xExpr = xTerm.asExprOf[complement]
                      val idxExpr = scala.quoted.Expr(complementIdx)
                      val accessor: Expr[t] =
                        '{
                          $xExpr
                            .asInstanceOf[Tuple]
                            .apply($idxExpr)
                            .asInstanceOf[t]
                        }
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

        '{ SimpleLens[S, focus, complement]($get, $split, $combine) }

  /** Full-cover codegen — emits `BijectionIso[S, S, Focus, Focus]` when the user-supplied selector
    * set covers every case field of `S`. Applies at ANY arity, including N = 1 (D2 — this is the
    * behaviour change flagged in the plan for `lens[Wrapper](_.value)`).
    *
    *   - `get: S => Focus` reads each selected field in SELECTOR order via `Select.unique` and
    *     packs them into the NamedTuple via `Expr.ofTupleFromSeq` + `asExprOf`.
    *   - `reverseGet: Focus => S` threads Hearth's `cc.construct[Id]` through the primary
    *     constructor: for each declaration-order parameter it computes the selector-order index,
    *     reads `focus.asInstanceOf[Tuple].apply(i).asInstanceOf[t]`, and plugs it in at the
    *     matching parameter position.
    */
  private def buildMultiIso[S: Type](
      cc: CaseClass[S],
      selectedNames: List[String],
  ): Expr[Optic[S, S, ?, ?, ?]] =
    val sTpe = TypeRepr.of[S]
    val allFieldSyms: List[Symbol] = sTpe.typeSymbol.caseFields

    val selectorSyms: List[Symbol] = selectedNames.map { name =>
      allFieldSyms.find(_.name == name).getOrElse {
        report.errorAndAbort(
          s"lens[${Type.prettyPrint[S]}]: internal error -- field '$name' not"
            + s" found on ${Type.prettyPrint[S]}."
        )
      }
    }
    val selectorTypes: List[TypeRepr] = selectorSyms.map(sym => sTpe.memberType(sym))

    val namesTpe: TypeRepr =
      selectedNames.foldRight(TypeRepr.of[EmptyTuple]) { (n, acc) =>
        TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(n)), acc))
      }
    val valuesTpe: TypeRepr =
      selectorTypes.foldRight(TypeRepr.of[EmptyTuple]) { (t, acc) =>
        TypeRepr.of[*:].appliedTo(List(t, acc))
      }
    val focusTpe: TypeRepr =
      TypeRepr
        .of[scala.NamedTuple.NamedTuple]
        .appliedTo(List(namesTpe, valuesTpe))

    focusTpe.asType match
      case '[focus] =>
        // `get: S => focus` — pack selected fields in SELECTOR order into a
        // NamedTuple via `Expr.ofTupleFromSeq` + `asExprOf[focus]`.
        val get: Expr[S => focus] =
          '{ (s: S) =>
            ${
              val focusReads: List[scala.quoted.Expr[Any]] =
                selectorSyms.map { sym =>
                  Select.unique('{ s }.asTerm, sym.name).asExpr
                }
              scala.quoted.Expr.ofTupleFromSeq(focusReads).asExprOf[focus]
            }
          }

        // `reverseGet: focus => S` — thread the primary constructor through
        // `cc.construct[Id]`, looking up each declaration-order parameter in
        // the selector-order focus tuple.
        //
        // `a` is used inside the nested splice via `'{ a }`; `-Wunused` can't
        // see across quote/splice boundaries. The module-level `-Wconf`
        // silences the false positive.
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
                    val aExpr = aTerm.asExprOf[focus]
                    val idxExpr = scala.quoted.Expr(focusIdx)
                    val accessor: Expr[t] =
                      '{
                        $aExpr
                          .asInstanceOf[Tuple]
                          .apply($idxExpr)
                          .asInstanceOf[t]
                      }
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

        '{ BijectionIso[S, S, focus, focus]($get, $reverseGet) }

  /** Strips Inlined/Typed wrappers and peeks inside the lambda body for a single Select whose
    * receiver is exactly the lambda parameter (an `Ident`). `Lambda` must be tried before `Block`
    * because a lambda IS a `Block(DefDef, Closure)` structurally -- stripping the outer Block would
    * leave us with the Closure.
    *
    * The receiver-is-Ident check matters for nested paths: `_.inner.count` parses as
    * `Select(Select(Ident(param), "inner"), "count")`. The old pattern `Select(_, name)` would
    * happily return `Some("count")`, which then routed into the "unknown field" diagnostic with a
    * confusing error. We now require the Select's receiver to be the parameter itself, which lets
    * the macro produce the cleaner "nested paths not supported" message via the Option-unpack site
    * above.
    */
  private def extractFieldName(t: Term): Option[String] =
    def oneHop(body: Term): Option[String] =
      body match
        case Inlined(_, _, inner)   => oneHop(inner)
        case Typed(inner, _)        => oneHop(inner)
        case Select(Ident(_), name) =>
          // Receiver is the lambda parameter itself — single-field accessor.
          Some(name)
        case _ =>
          // Any other shape (method call, nested Select, etc.) falls out
          // and produces the non-field-accessor diagnostic at the call
          // site.
          None
    t match
      case Inlined(_, _, inner) => extractFieldName(inner)
      case Typed(inner, _)      => extractFieldName(inner)
      case Lambda(_, body)      => oneHop(body)
      case _                    => None
