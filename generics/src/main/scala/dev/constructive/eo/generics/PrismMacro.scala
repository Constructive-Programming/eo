package dev.constructive.eo
package generics

import scala.quoted.*

import dev.constructive.eo.optics.{Optic, Prism}

/** Compile-time derivation of a Prism from a sum / enum / union type `S` to a direct child
  * `A <: S`. Routes through Hearth's `Enum.parse[S]` (recognises sealed traits, Scala 3 enums, AND
  * union types). The reconstruct is the plain upcast `(a: A) => a: S`. Macro-time validation checks
  * `A` is a direct child of `S`.
  *
  * {{{
  * enum Shape:
  *   case Circle(radius: Double)
  *   case Square(side: Double)
  * val circleP = prism[Shape, Shape.Circle]
  * val intP    = prism[Int | String, Int]
  * }}}
  */
object PrismMacro:

  /** @group Constructors */
  inline def derive[S, A <: S]: Optic[S, S, A, A, Either] =
    ${ deriveImpl[S, A] }

  def deriveImpl[S: Type, A <: S: Type](using
      q: Quotes
  ): Expr[Optic[S, S, A, A, Either]] =
    new HearthPrismMacro(q).derivePrism[S, A]

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
        // Verify A is a direct child of S (catches typos / non-variant subtypes).
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
                  if Type[Variant] =:= Type[A] then '{ Right[S, A](${ variantExpr.asExprOf[A] }) }
                  else '{ Left[S, A](s) }
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
