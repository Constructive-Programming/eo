package eo
package generics

import scala.quoted.*

import eo.optics.{Optic, Prism}

/** Compile-time derivation of a `Prism` from a parent sum / enum / union type `S` to one of its
  * direct child types `A <: S`.
  *
  * Usage:
  * {{{
  * import eo.generics.prism
  *
  * // Sealed trait / enum case:
  * enum Shape:
  *   case Circle(radius: Double)
  *   case Square(side: Double)
  * val circleP = prism[Shape, Shape.Circle]
  *
  * // Scala 3 union type:
  * val intP = prism[Int | String, Int]
  * }}}
  *
  * Implementation notes:
  *   - The deconstruct (`S => Either[S, A]`) is built through Hearth's
  *     [[hearth.typed.Classes.Enum]] view: `Enum.parse[S]` recognises sealed traits, Scala 3 enums,
  *     AND union types; `matchOn` generates an exhaustive pattern match over `directChildren`,
  *     using `MatchCase.eqValue` for parameterless enum cases / Java enum vals (which would
  *     otherwise lose their type at erasure) and `MatchCase.typeMatch` for the rest.
  *   - The reconstruct is a plain upcast `(a: A) => a: S`, valid for every well-formed `prism[S, A
  *     <: S]`.
  *   - Validation: we check at macro time that `A` actually appears among `directChildren` of `S`,
  *     and produce a useful error if not (e.g. when the user writes `prism[Shape, OtherEnum.Foo]`).
  */
object PrismMacro:

  inline def derive[S, A <: S]: Optic[S, S, A, A, Either] =
    ${ deriveImpl[S, A] }

  def deriveImpl[S: Type, A <: S: Type](using
      q: Quotes
  ): Expr[Optic[S, S, A, A, Either]] =
    new HearthPrismMacro(q).derivePrism[S, A]

/** Hearth-backed Prism macro implementation.
  */
final private class HearthPrismMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  def derivePrism[S, A <: S](using
      Type[S],
      Type[A],
  ): Expr[Optic[S, S, A, A, Either]] =
    Enum.parse[S].toEither match
      case Left(reason) =>
        report.errorAndAbort(
          s"prism[${Type.prettyPrint[S]}, ${Type.prettyPrint[A]}]: $reason"
        )
      case Right(enumView) =>
        // Verify A is among S's direct children: catches typos and
        // cases where the user passes a non-variant subtype.
        val aIsChild = enumView.directChildren.values.exists { c =>
          import c.Underlying
          Type[c.Underlying] =:= Type[A]
        }
        if !aIsChild then
          val knownChildren = enumView.directChildren.keys.mkString(", ")
          report.errorAndAbort(
            s"prism[${Type.prettyPrint[S]}, ${Type.prettyPrint[A]}]:"
              + s" ${Type.prettyPrint[A]} is not a direct child of"
              + s" ${Type.prettyPrint[S]}. Known children: $knownChildren"
          )

        val deconstruct: Expr[S => Either[S, A]] =
          '{ (s: S) =>
            ${
              val matchExpr: Option[Expr[Either[S, A]]] =
                enumView.matchOn[Id, Either[S, A]]('{ s }) { matched =>
                  import matched.{Underlying as Variant, value as variantExpr}
                  if Type[Variant] =:= Type[A] then
                    // Target variant: lift to Right(_).
                    '{ Right[S, A](${ variantExpr.asExprOf[A] }) }
                  else
                    // Non-target variant: surface the original `s` as Left.
                    '{ Left[S, A](s) }
                }
              matchExpr.getOrElse {
                report.errorAndAbort(
                  s"prism[${Type.prettyPrint[S]}]: matchOn produced no cases."
                )
              }
            }
          }

        val reconstruct: Expr[A => S] = '{ (a: A) => a: S }

        '{ Prism[S, A]($deconstruct, $reconstruct) }
