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

        otherFieldSyms.size match
          case 0 =>
            buildZeroOther[S, A](cc, fieldName, selector)
          case 1 =>
            buildOneOther[S, A](cc, fieldName, selector, sTpe, otherFieldSyms.head)
          case n =>
            report.errorAndAbort(
              s"lens[${Type.prettyPrint[S]}]: structural Lens derivation currently "
                + s"supports case classes with up to 2 fields (got $n other fields "
                + s"besides '$fieldName'). Future work: build a TupleN complement "
                + s"for wider records."
            )

  /** 1-field case class: complement `XA = EmptyTuple`. */
  private def buildZeroOther[S: Type, A: Type](
      cc: CaseClass[S],
      fieldName: String,
      selector: Expr[S => A],
  ): Expr[SimpleLens[S, A, EmptyTuple]] =
    val split: Expr[S => (EmptyTuple, A)] =
      '{ (s: S) => (EmptyTuple, $selector(s)) }

    val combine: Expr[(EmptyTuple, A) => S] =
      '{ (_: EmptyTuple, a: A) =>
        ${
          val constructed: Id[Option[Expr[S]]] = cc.construct[Id] {
            (param: Parameter) =>
              if param.name == fieldName then
                Existential[Expr, A]('{ a }): Expr_??
              else
                report.errorAndAbort(
                  s"lens[${Type.prettyPrint[S]}]: internal error -- "
                    + s"unexpected parameter '${param.name}'."
                )
          }
          constructed.getOrElse {
            report.errorAndAbort(
              s"lens[${Type.prettyPrint[S]}]: failed to call the primary"
                + s" constructor of ${Type.prettyPrint[S]}."
            )
          }
        }
      }

    '{ SimpleLens[S, A, EmptyTuple]($selector, $split, $combine) }

  /** 2-field case class: complement `XA` is the sibling field's type. */
  private def buildOneOther[S: Type, A: Type](
      cc: CaseClass[S],
      fieldName: String,
      selector: Expr[S => A],
      sTpe: TypeRepr,
      otherSym: Symbol,
  ): Expr[SimpleLens[S, A, ?]] =
    val otherName = otherSym.name
    val otherType = sTpe.memberType(otherSym)

    otherType.asType match
      case '[xa] =>
        val split: Expr[S => (xa, A)] =
          '{ (s: S) =>
            val x: xa = ${ Select.unique('{ s }.asTerm, otherName).asExprOf[xa] }
            (x, $selector(s))
          }

        val combine: Expr[(xa, A) => S] =
          '{ (x: xa, a: A) =>
            ${
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] {
                (param: Parameter) =>
                  if param.name == fieldName then
                    Existential[Expr, A]('{ a }): Expr_??
                  else if param.name == otherName then
                    Existential[Expr, xa]('{ x }): Expr_??
                  else
                    report.errorAndAbort(
                      s"lens[${Type.prettyPrint[S]}]: internal error -- "
                        + s"unexpected parameter '${param.name}'."
                    )
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
