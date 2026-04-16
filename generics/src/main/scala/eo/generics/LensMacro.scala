package eo
package generics

import scala.quoted.*

import eo.optics.{Lens, Optic}

/** Compile-time derivation of a `Lens` from a field-accessor lambda.
  *
  * Usage:
  * {{{
  * import eo.generics.lens
  * case class Person(name: String, age: Int)
  * val ageLens = lens[Person](_.age)
  * // ageLens: Optic[Person, Person, Int, Int, Tuple2]
  * ageLens.get(Person("a", 30))            // 30
  * ageLens.replace(40)(Person("a", 30))    // Person("a", 40)
  * }}}
  *
  * Implementation notes:
  *   - The selector lambda is parsed with vanilla `quotes.reflect`
  *     pattern matching (Hearth has no surface for "the AST of an
  *     anonymous function").
  *   - The setter is built through Hearth's [[hearth.typed.Classes]]
  *     view: `CaseClass.parse[S]` validates that S is a real case
  *     class, `caseFieldValuesAt(s)` reads every field off the source
  *     instance, and `construct[Id]` calls the primary constructor
  *     with the substitution `field == fieldName ? a : s.field` for
  *     each parameter.
  *   - Routing setter construction through Hearth means recursive /
  *     parameterised types (e.g. `Branch[N]`) and Scala 3 enum cases
  *     (which lack a `.copy` method) are both handled uniformly --
  *     `new T(...)` works for both.
  */
object LensMacro:

  inline def derive[S, A](inline selector: S => A): Optic[S, S, A, A, Tuple2] =
    ${ deriveImpl[S, A]('selector) }

  def deriveImpl[S: Type, A: Type](
      selector: Expr[S => A]
  )(using q: Quotes): Expr[Optic[S, S, A, A, Tuple2]] =
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
      selector: Expr[S => A]
  )(using Type[S], Type[A]): Expr[Optic[S, S, A, A, Tuple2]] =
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

        // (s: S, a: A) => <Hearth-built constructor call substituting
        //                  `a` for the target field, `s.fi` elsewhere>
        val setter: Expr[(S, A) => S] =
          '{ (s: S, a: A) =>
            ${
              val fieldValues = cc.caseFieldValuesAt('{ s })
              val constructed: Id[Option[Expr[S]]] = cc.construct[Id] {
                (param: Parameter) =>
                  if param.name == fieldName then
                    // Substitute the new value `a` for the target field.
                    // The Applicative-derived ConstructField will
                    // upcast Expr[A] to Expr[field.tpe.Underlying].
                    Existential[Expr, A]('{ a }): Expr_??
                  else
                    // Pass through `s.<otherField>` from the source.
                    fieldValues(param.name)
              }
              constructed.getOrElse {
                report.errorAndAbort(
                  s"lens[${Type.prettyPrint[S]}]: failed to call the primary"
                    + s" constructor of ${Type.prettyPrint[S]}."
                )
              }
            }
          }

        '{ Lens[S, A]($selector, $setter) }

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
