package eo
package generics

import scala.quoted.*

import eo.optics.{Lens, Optic, SimpleLens}

/** Compile-time derivation of a `Lens` as a [[SimpleLens]].
  *
  * Usage:
  * {{{
  * import eo.generics.lens
  * case class Person(age: Int, name: String)
  * val ageLens = lens[Person](_.age)
  * // ageLens: SimpleLens[Person, Int, String]
  * ageLens.get(Person(30, "Alice"))        // 30
  * ageLens.replace(40)(Person(30, "Alice"))// Person(40, "Alice")
  * }}}
  *
  * Implementation notes:
  *   - The macro exposes the actual *structural complement* of `S` as
  *     the optic's `X` parameter: for a 2-field case class focused on
  *     one field, `X` is the type of the remaining field. That gives
  *     `transform`, `place`, and `transfer` the evidence they need for
  *     free -- no companion `given` required at the call site.
  *   - The selector lambda is parsed with vanilla `quotes.reflect`
  *     pattern matching (Hearth has no surface for "the AST of an
  *     anonymous function").
  *   - `combine` is built through Hearth's [[hearth.typed.Classes]]
  *     view: `CaseClass.parse[S]` validates that S is a real case
  *     class, and `construct[Id]` emits the primary constructor call
  *     with `a` at the target position and `x` at the sibling
  *     position.
  *   - Routing constructor synthesis through Hearth means recursive /
  *     parameterised types and Scala 3 enum cases (which lack a
  *     `.copy` method) are both handled uniformly -- `new T(...)`
  *     works for both.
  *   - Only 1-field and 2-field case classes are supported currently.
  *     Extending to N-field classes (N≥3) requires building a `TupleN`
  *     complement; tracked as future work.
  */
object LensMacro:

  // `transparent inline` so the specific `XA` that `deriveImpl`
  // synthesises (EmptyTuple for 1-field records, the sibling field's
  // type for 2-field records) propagates to the call site. The
  // declared `? ` wildcard is an upper bound; the call-site type is
  // always a concrete `SimpleLens[S, A, <specificXA>]`.
  transparent inline def derive[S, A](
      inline selector: S => A,
  ): SimpleLens[S, A, ?] =
    ${ deriveImpl[S, A]('selector) }

  def deriveImpl[S: Type, A: Type](
      selector: Expr[S => A],
  )(using q: Quotes): Expr[SimpleLens[S, A, ?]] =
    new HearthLensMacro(q).deriveLens[S, A](selector)

/** Hearth-backed Lens macro implementation, extends
  * `_root_.hearth.MacroCommonsScala3` so the high-level
  * `CaseClass` / `Type` views are in scope.
  */
private final class HearthLensMacro(q: Quotes)
    extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  def deriveLens[S, A](
      selector: Expr[S => A],
  )(using Type[S], Type[A]): Expr[SimpleLens[S, A, ?]] =
    val fieldName: String = extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"""lens[${Type.prettyPrint[S]}, ${Type.prettyPrint[A]}]: selector must be a
           |single-field accessor like `_.fieldName`.
           |Nested paths (e.g. `_.a.b`) are not yet supported.
           |Got: ${selector.asTerm.show}""".stripMargin
      )
    }

    CaseClass.parse[S].toEither match
      case Left(reason) =>
        report.errorAndAbort(s"lens[${Type.prettyPrint[S]}]: $reason")
      case Right(cc) =>
        val knownFields = cc.caseFields.map(_.value.name)
        if !knownFields.contains(fieldName) then
          report.errorAndAbort(
            s"lens[${Type.prettyPrint[S]}, ${Type.prettyPrint[A]}]: '$fieldName' is not a "
              + s"field of ${Type.prettyPrint[S]}. Known fields: ${knownFields.mkString(", ")}"
          )

        val sTpe = TypeRepr.of[S]
        val otherFieldSyms = sTpe.typeSymbol.caseFields.filter(_.name != fieldName)

        buildLens[S, A](cc, fieldName, selector, sTpe, otherFieldSyms)

  /** Build a `SimpleLens[S, A, XA]` for a case class with any number
    * of fields.
    *
    * The complement `XA` is synthesised as a Scala-3
    * `NamedTuple[Names, Values]` over the non-focused fields. Both
    * the field names (as singleton-String types) and the field types
    * are reconstructed at macro time from the primary constructor's
    * parameter list, preserving declaration order:
    *
    *   * 1-field record  →  `XA = NamedTuple[EmptyTuple, EmptyTuple]`
    *   * 2-field record  →  `XA = NamedTuple["sibling" *: EmptyTuple,
    *                                         Sibling *: EmptyTuple]`
    *   * N-field record  →  `XA = NamedTuple["n1" *: "n2" *: …,
    *                                         T1 *: T2 *: …]`
    *
    * Since `NamedTuple` is an opaque alias over its `Values` tuple,
    * the runtime representation is identical to a plain
    * `T1 *: T2 *: …` — no extra boxing. The upgrade over plain
    * `Tuple` buys downstream users pattern-match ergonomics
    * (`x.fieldName` at the call site) without a perf cost.
    *
    * `split` reads each non-focused field and packs them with
    * `Expr.ofTupleFromSeq`, then narrows the result to `xa` via
    * `asExprOf` (safe because the `Values` tuple IS-A `NamedTuple`
    * through opaque subtyping). `combine` reads back via
    * `.toTuple.apply(i).asInstanceOf[T_i]` and threads the focused
    * value through the primary constructor via Hearth's
    * `CaseClass.construct`. */
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

        val combine: Expr[(xa, A) => S] =
          '{ (x: xa, a: A) =>
            ${
              val xTerm = '{ x }.asTerm
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] {
                (param: Parameter) =>
                  if param.name == fieldName then
                    Existential[Expr, A]('{ a }): Expr_??
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

  /** Strips Inlined/Typed wrappers and peeks inside the lambda body
    * for a single Select. `Lambda` must be tried before `Block`
    * because a lambda IS a `Block(DefDef, Closure)` structurally --
    * stripping the outer Block would leave us with the Closure.
    */
  private def extractFieldName(t: Term): Option[String] =
    t match
      case Inlined(_, _, inner)                       => extractFieldName(inner)
      case Typed(inner, _)                            => extractFieldName(inner)
      case Lambda(_, Select(_, name))                 => Some(name)
      case Lambda(_, Inlined(_, _, Select(_, name)))  => Some(name)
      case Lambda(_, Typed(Select(_, name), _))       => Some(name)
      case _                                          => None
