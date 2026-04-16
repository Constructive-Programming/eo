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
  * The macro parses the selector lambda, extracts the field name, and
  * emits a call to `Lens.apply[S, A]` whose setter is `s.copy(name = a)`
  * -- identical to what you'd write by hand.
  *
  * Scaffolded through [[eo.generics.hearth.LensDerivation]] so a future
  * richer derivation (recursive lenses on nested paths, record-of-lenses
  * companions, etc.) can land here without changing the call-site API.
  */
object LensMacro:

  inline def derive[S, A](inline selector: S => A): Optic[S, S, A, A, Tuple2] =
    ${ deriveImpl[S, A]('selector) }

  def deriveImpl[S: Type, A: Type](
      selector: Expr[S => A]
  )(using Quotes): Expr[Optic[S, S, A, A, Tuple2]] =
    import quotes.reflect.*

    // Strip Inlined/Typed wrappers and peek inside the lambda body
    // for a single Select. Lambda must be tried BEFORE any Block
    // pattern because a lambda IS a Block(DefDef, Closure)
    // structurally -- stripping the outer Block would leave us with
    // the Closure, not the accessor body.
    def fieldOf(t: Term): Option[String] =
      t match
        case Inlined(_, _, inner)                       => fieldOf(inner)
        case Typed(inner, _)                            => fieldOf(inner)
        case Lambda(_, Select(_, name))                 => Some(name)
        case Lambda(_, Inlined(_, _, Select(_, name)))  => Some(name)
        case Lambda(_, Typed(Select(_, name), _))       => Some(name)
        case _                                          => None

    val fieldName: String = fieldOf(selector.asTerm).getOrElse {
      report.errorAndAbort(
        s"""lens[${Type.show[S]}, ${Type.show[A]}] selector must be a
           |single-field accessor like `_.fieldName`.
           |Got: ${selector.asTerm.show}""".stripMargin
      )
    }

    // Validate that S actually has a `copy` method with `fieldName`
    // as a by-name param. TypeRepr.of[S].typeSymbol.caseFields would
    // give us the list, but checking via memberType is simpler and
    // produces a useful error if the user passed a non-product S.
    val sTpe = TypeRepr.of[S]
    val caseFields = sTpe.typeSymbol.caseFields
    if caseFields.isEmpty then
      report.errorAndAbort(
        s"lens[${Type.show[S]}, ${Type.show[A]}]: ${Type.show[S]} is not a case class."
      )
    if !caseFields.exists(_.name == fieldName) then
      report.errorAndAbort(
        s"""lens[${Type.show[S]}, ${Type.show[A]}]: field `$fieldName` is not a
           |case-class field of ${Type.show[S]}. Known fields: ${caseFields.map(_.name).mkString(", ")}""".stripMargin
      )

    // Emit (s: S, a: A) => s.copy(f1 = s.f1, ..., fieldName = a, ..., fn = s.fn)
    //
    // We MUST supply every case field explicitly: `Select.appliedToArgs`
    // doesn't synthesise `copy`'s default-argument stubs the way the
    // parser does, so a single NamedArg is reported as "expected: N,
    // found: 1". Passing all fields -- `a` for the target, `s.fi` for
    // the rest -- sidesteps the default-args machinery entirely.
    val setter: Expr[(S, A) => S] =
      '{ (s: S, a: A) =>
        ${
          val args = caseFields.map { f =>
            if f.name == fieldName then NamedArg(f.name, 'a.asTerm)
            else NamedArg(f.name, Select('{ s }.asTerm, f))
          }
          Select
            .unique('{ s }.asTerm, "copy")
            .appliedToArgs(args)
            .asExprOf[S]
        }
      }

    '{ Lens[S, A]($selector, $setter) }
