package dev.constructive.eo
package generics

import scala.quoted.*

import dev.constructive.eo.data.PSVec
import dev.constructive.eo.optics.Plated

/** Compile-time derivation of a [[dev.constructive.eo.optics.Plated]] for a recursive ADT `S`.
  *
  * The derived `plate` focuses the **immediate same-typed children** of every case: each field
  * whose declared type is exactly `S` (the "exact self-type" rule — `List[S]`, `Option[S]`, tuples
  * of `S`, etc. are *not* descended in this version; their elements are leftover skeleton). For
  *
  * {{{
  * enum Expr:
  *   case Var(name: String)        // 0 children
  *   case App(f: Expr, x: Expr)    // 2 children
  *   case Lam(bind: String, body: Expr) // 1 child (bind:String is skeleton)
  * }}}
  *
  * `plate[Expr]` gathers `App`'s `f`/`x` and `Lam`'s `body`, leaving `Var.name` / `Lam.bind`
  * untouched. Works on Scala 3 enums, sealed hierarchies (via Hearth's `Enum.parse`), and plain
  * recursive case classes (via `CaseClass.parse`). Reconstruction routes through
  * `CaseClass.construct[Id]` (`new V(...)`, never `.copy`), so enum cases reassemble fine.
  *
  * `children` reads the self-typed fields in declaration order; `rebuild` swaps them back by the
  * same declaration-order index, so the two agree without relying on `construct`'s call order.
  */
object PlateMacro:

  /** @group Constructors */
  inline def derive[S]: Plated[S] = ${ deriveImpl[S] }

  def deriveImpl[S: Type](using q: Quotes): Expr[Plated[S]] =
    new HearthPlateMacro(q).derivePlate[S]

final private class HearthPlateMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  def derivePlate[S](using Type[S]): Expr[Plated[S]] =
    val sTpe = TypeRepr.of[S]
    Enum.parse[S].toEither match
      case Right(enumView) => deriveSum[S](enumView, sTpe)
      case Left(_)         =>
        CaseClass.parse[S].toEither match
          case Right(cc)    => deriveProduct[S](cc, sTpe)
          case Left(reason) =>
            report.errorAndAbort(
              s"plate[${Type.prettyPrint[S]}]: $reason — expected a case class, enum,"
                + " sealed hierarchy, or union type."
            )

  // ---- sum / enum path -------------------------------------------------------

  private def deriveSum[S: Type](
      enumView: Enum[S],
      sTpe: TypeRepr,
  ): Expr[Plated[S]] =
    val childrenExpr: Expr[S => PSVec[S]] =
      '{ (s: S) =>
        ${
          enumView
            .matchOn[Id, PSVec[S]]('{ s }) { matched =>
              import matched.{Underlying as V, value as vExpr}
              selfFieldVec[S, V](vExpr, sTpe)
            }
            .getOrElse(
              report.errorAndAbort(s"plate[${Type.prettyPrint[S]}]: matchOn produced no cases.")
            )
        }
      }

    val rebuildExpr: Expr[(S, PSVec[S]) => S] =
      '{ (s: S, vec: PSVec[S]) =>
        ${
          enumView
            .matchOn[Id, S]('{ s }) { matched =>
              import matched.{Underlying as V, value as vExpr}
              reconstruct[S, V](vExpr, '{ vec }, sTpe)
            }
            .getOrElse(
              report.errorAndAbort(s"plate[${Type.prettyPrint[S]}]: matchOn produced no cases.")
            )
        }
      }

    '{ Plated.fromChildrenVec[S]($childrenExpr, $rebuildExpr) }

  // ---- product / case-class path --------------------------------------------

  private def deriveProduct[S: Type](
      cc: CaseClass[S],
      sTpe: TypeRepr,
  ): Expr[Plated[S]] =
    val childrenExpr: Expr[S => PSVec[S]] =
      '{ (s: S) => ${ selfFieldVec[S, S]('{ s }, sTpe) } }

    val rebuildExpr: Expr[(S, PSVec[S]) => S] =
      '{ (s: S, vec: PSVec[S]) => ${ reconstructProduct[S](cc, '{ s }, '{ vec }, sTpe) } }

    '{ Plated.fromChildrenVec[S]($childrenExpr, $rebuildExpr) }

  // ---- shared codegen --------------------------------------------------------

  /** Names of the case fields of `V` whose declared type is exactly `S`, in declaration order. */
  private def selfFieldNames[V: Type](sTpe: TypeRepr): List[String] =
    val vTpe = TypeRepr.of[V]
    vTpe.typeSymbol.caseFields.filter(f => vTpe.memberType(f) =:= sTpe).map(_.name)

  /** The self-typed field reads of `src` packed into a `PSVec`, declaration order —
    * `Expr[PSVec[S]]`. Specialised at 0 / 1 children so leaves and unary nodes skip the
    * backing-array allocation.
    */
  private def selfFieldVec[S: Type, V: Type](
      src: Expr[V],
      sTpe: TypeRepr,
  ): Expr[PSVec[S]] =
    val reads: List[Expr[S]] =
      selfFieldNames[V](sTpe).map(name => Select.unique(src.asTerm, name).asExprOf[S])
    reads match
      case Nil      => '{ PSVec.empty[S] }
      case r :: Nil => '{ PSVec.singleton[S]($r) }
      case _        =>
        val anyRefs: List[Expr[AnyRef]] = reads.map(r => '{ $r.asInstanceOf[AnyRef] })
        '{ PSVec.unsafeWrap[S](scala.Array[AnyRef](${ Varargs(anyRefs) }*)) }

  /** Reconstruct enum variant `V` (`<: S`) from `src` (its current value) and `vec` (the new
    * children, declaration order), upcast to `S`. Parameterless variants pass through.
    */
  private def reconstruct[S: Type, V: Type](
      src: Expr[V],
      vec: Expr[PSVec[S]],
      sTpe: TypeRepr,
  ): Expr[S] =
    // `V` is an enum child of `S` (so `V <: S` at runtime) but the macro carries no `<: S` bound,
    // hence the explicit upcast cast rather than an ascription.
    CaseClass.parse[V].toEither match
      case Left(_)   => '{ ${ src }.asInstanceOf[S] } // parameterless case — nothing to rebuild
      case Right(cc) =>
        val built: Expr[V] = reconstructFields[S, V](cc, src, vec, sTpe)
        '{ ${ built }.asInstanceOf[S] }

  /** Product `S` reconstruction (single-case path) — already at type `S`, no upcast needed. */
  private def reconstructProduct[S: Type](
      cc: CaseClass[S],
      src: Expr[S],
      vec: Expr[PSVec[S]],
      sTpe: TypeRepr,
  ): Expr[S] =
    reconstructFields[S, S](cc, src, vec, sTpe)

  /** The constructor-threading core: route each declaration-order parameter to either the matching
    * new child (`vec(i)`, by self-field index) or the preserved field read off `src`.
    */
  private def reconstructFields[S: Type, V: Type](
      cc: CaseClass[V],
      src: Expr[V],
      vec: Expr[PSVec[S]],
      sTpe: TypeRepr,
  ): Expr[V] =
    val vTpe = TypeRepr.of[V]
    val nameToType: Map[String, TypeRepr] =
      vTpe.typeSymbol.caseFields.map(f => f.name -> vTpe.memberType(f)).toMap
    val selfNames: List[String] = selfFieldNames[V](sTpe)

    val constructed: Id[Option[Expr[V]]] = cc.construct[Id] { (param: Parameter) =>
      val selfIdx = selfNames.indexOf(param.name)
      if selfIdx >= 0 then
        // self-typed child: take it from the rebuilt children vector at its declaration index.
        val idxExpr = scala.quoted.Expr(selfIdx)
        Existential[Expr, S]('{ ${ vec }(${ idxExpr }) }): Expr_??
      else
        // skeleton field: preserve the original value read off `src`.
        val tpe = nameToType.getOrElse(
          param.name,
          report.errorAndAbort(s"plate: internal error — unexpected parameter '${param.name}'."),
        )
        tpe.asType match
          case '[t] =>
            Existential[Expr, t](Select.unique(src.asTerm, param.name).asExprOf[t]): Expr_??
    }

    constructed.getOrElse(
      report.errorAndAbort(
        s"plate[${Type.prettyPrint[V]}]: failed to call the primary constructor."
      )
    )
