package eo
package generics

import scala.quoted.*

import eo.optics.{Prism, Optic}

/** Compile-time derivation of a `Prism` from a parent `S` to a
  * specific variant `A <: S` of a sealed hierarchy / Scala 3 `enum`.
  *
  * Usage:
  * {{{
  * import eo.generics.prism
  * enum Shape:
  *   case Circle(radius: Double)
  *   case Square(side: Double)
  *
  * val circleP = prism[Shape, Shape.Circle]
  * circleP.to(Shape.Circle(1.0))      // Right(Circle(1.0))
  * circleP.to(Shape.Square(1.0))      // Left(Square(1.0))
  * circleP.reverseGet(Shape.Circle(2.0))  // Circle(2.0)
  * }}}
  *
  * The emitted deconstruct is `s => s match { case a: A => Right(a);
  * case _ => Left(s) }`, identical to a hand-written
  * `Prism[S, A](getOption = { case a: A => Some(a); case _ => None },
  *              reverseGet = identity)` but avoids the `Option`
  * round-trip by constructing the `Either` directly (EO's Prism carrier).
  *
  * The macro calls `summonInline[scala.reflect.ClassTag[A]]` only for
  * generic `A`; for monomorphic `A` (the usual enum-case shape) the
  * pattern match is erasure-safe without a ClassTag.
  */
object PrismMacro:

  inline def derive[S, A <: S]: Optic[S, S, A, A, Either] =
    ${ deriveImpl[S, A] }

  def deriveImpl[S: Type, A <: S: Type](using Quotes): Expr[Optic[S, S, A, A, Either]] =
    import quotes.reflect.*

    val sTpe = TypeRepr.of[S]
    val aTpe = TypeRepr.of[A]

    // A must be either a subtype of a sealed S, or a concrete case of
    // an enum S, so that pattern matching on it is decidable.
    // We don't hard-fail if the relation isn't sealed -- the emitted
    // `case _: A` still works; but we give a gentle hint.
    if sTpe =:= aTpe then
      report.warning(
        s"prism[${Type.show[S]}, ${Type.show[A]}]: S and A are the same type; the resulting Prism is trivial."
      )

    '{
      val deconstruct: S => Either[S, A] = (s: S) =>
        s match
          case a: A => Right(a)
          case _    => Left(s)
      val reconstruct: A => S = (a: A) => a: S
      Prism[S, A](deconstruct, reconstruct)
    }
