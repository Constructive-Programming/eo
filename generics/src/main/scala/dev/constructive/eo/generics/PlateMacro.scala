package dev.constructive.eo
package generics

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.quoted.*
import scala.reflect.ClassTag

import cats.Traverse
import dev.constructive.eo.data.{ObjArrBuilder, PSVec}
import dev.constructive.eo.optics.Plated

/** Compile-time derivation of a [[dev.constructive.eo.optics.Plated]] for a recursive ADT or
  * mutually recursive type family `S`.
  *
  *   - '''Single-sorted ADTs''' (`enum Expr`, sealed hierarchies, recursive case classes): focuses
  *     every immediate child field of type `S`, plus children inside `Option[S]`, `List[S]`,
  *     `Vector[S]`, `ArraySeq[S]`, `Seq[S]`, `Array[S]`, or any `F[S]` with a `cats.Traverse[F]` in
  *     scope (O'Connor 2011 §5.1, Figure 5).
  *   - '''Mutually recursive families / union types''' (`type Lang = Stm | Expr | Var | Typ`,
  *     `plate[Lang]`): derives a multi-sorted plate (O'Connor 2011 §5) over the union's branches,
  *     descending into any field (or container field) whose element type is a subtype of `S`.
  *     Unsealed leaf branches in a union (e.g. Spark's `DataType` after its recursive `StructType |
  *     ArrayType | MapType` subtypes) are treated as leaves (`0` children, identity rebuild).
  *
  * Reconstruction routes through `CaseClass.construct[Id]` (`new V(...)`, never `.copy`), so Scala
  * 3 enum cases reassemble uniformly.
  */
object PlateMacro:

  /** Quoted-macro implementation behind the top-level `plate[S]` (spliced directly by the package
    * object) — see the object doc for supported field, container, and union shapes.
    */
  def deriveImpl[S: Type](using q: Quotes): Expr[Plated[S]] =
    new HearthPlateMacro(q).derivePlate[S]

  /** Concatenate multiple per-field `PSVec[S]` slices into one freshly-allocated `PSVec[S]`. */
  def concat[S](parts: Array[PSVec[S]]): PSVec[S] =
    val buf = new ObjArrBuilder(8)
    val n = parts.length
    @tailrec def loop(i: Int): Unit =
      if i < n then
        buf.appendAllFromPSVec(parts(i))
        loop(i + 1)
    loop(0)
    buf.freezeAsPSVec[S]

  /** Pack a `Traverse[F]` container of child elements into a freshly-allocated `PSVec[S]`. */
  def fromTraverse[F[_], S, E](fa: F[E])(using F: Traverse[F]): PSVec[S] =
    val buf = new ObjArrBuilder(8)
    F.foldLeft(fa, ())((_, e) => { buf.append(e); () })
    buf.freezeAsPSVec[S]

  /** Positional cursor over `PSVec[S]` used when reconstructing a product that carries container
    * child fields (`Option`, `List`, `Vector`, `ArraySeq`, `Seq`, `Array`, or `F: Traverse`).
    */
  final class RebuildCursor[S](val vec: PSVec[S]):
    private var pos: Int = 0

    def takeOne[E](): E =
      val e = vec(pos).asInstanceOf[E]
      pos += 1
      e

    def takeOption[E](orig: Option[E]): Option[E] =
      if orig.isDefined then
        val e = vec(pos).asInstanceOf[E]
        pos += 1
        Some(e)
      else None

    def takeList[E](orig: List[E]): List[E] =
      val n = orig.length
      if n == 0 then orig
      else
        val start = pos
        pos += n
        @tailrec def build(i: Int, acc: List[E]): List[E] =
          if i >= start then build(i - 1, vec(i).asInstanceOf[E] :: acc)
          else acc
        build(start + n - 1, Nil)

    def takeVector[E](orig: Vector[E]): Vector[E] =
      val n = orig.length
      if n == 0 then orig
      else
        val start = pos
        pos += n
        Vector.tabulate(n)(i => vec(start + i).asInstanceOf[E])

    def takeArraySeq[E](orig: ArraySeq[E]): ArraySeq[E] =
      val n = orig.length
      if n == 0 then orig
      else
        val start = pos
        pos += n
        val arr = new Array[Any](n)
        @tailrec def fill(i: Int): Unit =
          if i < n then
            arr(i) = vec(start + i)
            fill(i + 1)
        fill(0)
        ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[E]]

    def takeSeq[E](orig: scala.collection.Seq[E]): IndexedSeq[E] =
      val n = orig.length
      if n == 0 then Vector.empty
      else
        val start = pos
        pos += n
        Vector.tabulate(n)(i => vec(start + i).asInstanceOf[E])

    def takeArray[E: ClassTag](orig: Array[E]): Array[E] =
      val n = orig.length
      if n == 0 then orig
      else
        val out = new Array[E](n)
        val start = pos
        pos += n
        @tailrec def fill(i: Int): Unit =
          if i < n then
            out(i) = vec(start + i).asInstanceOf[E]
            fill(i + 1)
        fill(0)
        out

    def takeTraverse[F[_], E](orig: F[E])(using F: Traverse[F]): F[E] =
      F.map(orig) { _ =>
        val e = vec(pos).asInstanceOf[E]
        pos += 1
        e
      }

final private class HearthPlateMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  private enum ChildFieldKind:
    case Direct(elemTpe: TypeRepr)
    case OptionField(elemTpe: TypeRepr)
    case ListField(elemTpe: TypeRepr)
    case VectorField(elemTpe: TypeRepr)
    case ArraySeqField(elemTpe: TypeRepr)
    case SeqField(elemTpe: TypeRepr)
    case ArrayField(elemTpe: TypeRepr)
    case TraverseField(tyconTpe: TypeRepr, elemTpe: TypeRepr)

  def derivePlate[S](using Type[S]): Expr[Plated[S]] =
    val sTpe = TypeRepr.of[S]
    val unionBranches = flattenOrTypes(sTpe)
    if unionBranches.sizeIs > 1 then deriveUnion[S](orderBranches(unionBranches), sTpe)
    else
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

  private def flattenOrTypes(tpe: TypeRepr): List[TypeRepr] = tpe.dealias match
    case OrType(l, r) => flattenOrTypes(l) ++ flattenOrTypes(r)
    case other        => List(other)

  /** Order union branches so strict subtypes precede their supertypes (e.g. `StructType` before
    * `DataType`), preserving declaration order among unrelated branches.
    */
  private def orderBranches(branches: List[TypeRepr]): List[TypeRepr] =
    val deduped = branches.foldLeft(List.empty[TypeRepr]) { (acc, t) =>
      if acc.exists(_ =:= t) then acc else acc :+ t
    }
    @tailrec def topo(remaining: List[TypeRepr], acc: List[TypeRepr]): List[TypeRepr] =
      remaining match
        case Nil => acc
        case _   =>
          val idx = remaining.indexWhere { candidate =>
            !remaining.exists(other => !(other =:= candidate) && (other <:< candidate))
          }
          val pickIdx = if idx >= 0 then idx else 0
          val picked = remaining(pickIdx)
          val rest = remaining.take(pickIdx) ++ remaining.drop(pickIdx + 1)
          topo(rest, acc :+ picked)
    topo(deduped, Nil)

  // ---- union path (multi-sorted Multiplate) ----------------------------------

  private def deriveUnion[S: Type](
      branches: List[TypeRepr],
      sTpe: TypeRepr,
  ): Expr[Plated[S]] =
    val hasAdtBranch = branches.exists { bTpe =>
      bTpe.asType match
        case '[b] => CaseClass.parse[b].toEither.isRight || Enum.parse[b].toEither.isRight
    }
    if !hasAdtBranch then
      report.errorAndAbort(
        s"plate[${Type.prettyPrint[S]}]: union has no case-class or enum branches."
      )

    val childrenExpr: Expr[S => PSVec[S]] =
      '{ (s: S) => ${ unionChildrenChain[S](branches, '{ s }, sTpe) } }

    val rebuildExpr: Expr[(S, PSVec[S]) => S] =
      '{ (s: S, vec: PSVec[S]) => ${ unionRebuildChain[S](branches, '{ s }, '{ vec }, sTpe) } }

    '{ Plated.fromChildrenVec[S]($childrenExpr, $rebuildExpr) }

  private def unionChildrenChain[S: Type](
      branches: List[TypeRepr],
      sExpr: Expr[S],
      sTpe: TypeRepr,
  )(using Quotes): Expr[PSVec[S]] =
    branches match
      case Nil         => '{ PSVec.empty[S] }
      case last :: Nil =>
        last.asType match
          case '[b] =>
            '{
              ($sExpr: Any) match
                case v: (b @unchecked) => ${ branchChildren[S, b]('{ v }, sTpe) }
                case _                 => PSVec.empty[S]
            }
      case head :: tail =>
        head.asType match
          case '[b] =>
            val elseBr = unionChildrenChain[S](tail, sExpr, sTpe)
            '{
              ($sExpr: Any) match
                case v: (b @unchecked) => ${ branchChildren[S, b]('{ v }, sTpe) }
                case _                 => $elseBr
            }

  private def unionRebuildChain[S: Type](
      branches: List[TypeRepr],
      sExpr: Expr[S],
      vecExpr: Expr[PSVec[S]],
      sTpe: TypeRepr,
  )(using Quotes): Expr[S] =
    branches match
      case Nil         => sExpr
      case last :: Nil =>
        last.asType match
          case '[b] =>
            '{
              ($sExpr: Any) match
                case v: (b @unchecked) => ${ branchReconstruct[S, b]('{ v }, vecExpr, sTpe) }
                case _                 => $sExpr
            }
      case head :: tail =>
        head.asType match
          case '[b] =>
            val elseBr = unionRebuildChain[S](tail, sExpr, vecExpr, sTpe)
            '{
              ($sExpr: Any) match
                case v: (b @unchecked) => ${ branchReconstruct[S, b]('{ v }, vecExpr, sTpe) }
                case _                 => $elseBr
            }

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
              branchChildren[S, V](vExpr, sTpe)
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
              branchReconstruct[S, V](vExpr, '{ vec }, sTpe)
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
      '{ (s: S) => ${ productChildrenVec[S, S]('{ s }, sTpe) } }

    val rebuildExpr: Expr[(S, PSVec[S]) => S] =
      '{ (s: S, vec: PSVec[S]) => ${ reconstructFields[S, S](cc, '{ s }, '{ vec }, sTpe) } }

    '{ Plated.fromChildrenVec[S]($childrenExpr, $rebuildExpr) }

  // ---- branch dispatch (product, nested sum, or leaf) ------------------------

  private def branchChildren[S: Type, V: Type](
      src: Expr[V],
      sTpe: TypeRepr,
  )(using Quotes): Expr[PSVec[S]] =
    CaseClass.parse[V].toEither match
      case Right(_) =>
        productChildrenVec[S, V](src, sTpe)
      case Left(_) =>
        Enum.parse[V].toEither match
          case Right(subEnum) =>
            subEnum
              .matchOn[Id, PSVec[S]](src) { matched =>
                import matched.{Underlying as W, value as wExpr}
                if TypeRepr.of[W] =:= TypeRepr.of[V] then '{ PSVec.empty[S] }
                else branchChildren[S, W](wExpr, sTpe)
              }
              .getOrElse(
                report.errorAndAbort(s"plate[${Type.prettyPrint[V]}]: matchOn produced no cases.")
              )
          case Left(_) =>
            '{ PSVec.empty[S] }

  private def branchReconstruct[S: Type, V: Type](
      src: Expr[V],
      vec: Expr[PSVec[S]],
      sTpe: TypeRepr,
  )(using Quotes): Expr[S] =
    CaseClass.parse[V].toEither match
      case Right(cc) =>
        val built: Expr[V] = reconstructFields[S, V](cc, src, vec, sTpe)
        '{ $built.asInstanceOf[S] }
      case Left(_) =>
        Enum.parse[V].toEither match
          case Right(subEnum) =>
            subEnum
              .matchOn[Id, S](src) { matched =>
                import matched.{Underlying as W, value as wExpr}
                if TypeRepr.of[W] =:= TypeRepr.of[V] then '{ $wExpr.asInstanceOf[S] }
                else branchReconstruct[S, W](wExpr, vec, sTpe)
              }
              .getOrElse(
                report.errorAndAbort(s"plate[${Type.prettyPrint[V]}]: matchOn produced no cases.")
              )
          case Left(_) =>
            '{ $src.asInstanceOf[S] }

  // ---- field classification & codegen ---------------------------------------

  private def isChildType(tpe: TypeRepr, sTpe: TypeRepr): Boolean =
    val w = tpe.widen.dealias
    !(w =:= TypeRepr.of[Nothing]) && !(w =:= TypeRepr.of[Null]) && (w <:< sTpe)

  private def classifyField(fTpe: TypeRepr, sTpe: TypeRepr): Option[ChildFieldKind] =
    if isChildType(fTpe, sTpe) then Some(ChildFieldKind.Direct(fTpe.widen))
    else
      fTpe.widen.dealias match
        case AppliedType(tycon, List(elemRaw)) if isChildType(elemRaw, sTpe) =>
          val elemTpe = elemRaw.widen
          val sym = tycon.typeSymbol
          if sym == TypeRepr.of[Option[Any]].typeSymbol then
            Some(ChildFieldKind.OptionField(elemTpe))
          else if sym == TypeRepr.of[List[Any]].typeSymbol then
            Some(ChildFieldKind.ListField(elemTpe))
          else if sym == TypeRepr.of[Vector[Any]].typeSymbol then
            Some(ChildFieldKind.VectorField(elemTpe))
          else if sym == TypeRepr.of[ArraySeq[Any]].typeSymbol then
            Some(ChildFieldKind.ArraySeqField(elemTpe))
          else if sym == TypeRepr.of[scala.collection.immutable.Seq[Any]].typeSymbol ||
            sym == TypeRepr.of[scala.collection.Seq[Any]].typeSymbol
          then Some(ChildFieldKind.SeqField(elemTpe))
          else if sym == TypeRepr.of[Array[Any]].typeSymbol then
            Some(ChildFieldKind.ArrayField(elemTpe))
          else
            tycon.asType match
              case '[type f[_]; f] =>
                scala.quoted.Expr.summon[Traverse[f]] match
                  case Some(_) => Some(ChildFieldKind.TraverseField(tycon, elemTpe))
                  case None    => None
              case _ => None
        case _ => None

  private def childFieldsOf[V: Type](sTpe: TypeRepr): List[(String, ChildFieldKind)] =
    val vTpe = TypeRepr.of[V]
    vTpe.typeSymbol.caseFields.flatMap { f =>
      classifyField(vTpe.memberType(f), sTpe).map(k => f.name -> k)
    }

  private def fieldChildrenExpr[S: Type, V: Type](
      src: Expr[V],
      name: String,
      kind: ChildFieldKind,
  )(using Quotes): Expr[PSVec[S]] =
    val rawTerm = Select.unique(src.asTerm, name)
    kind match
      case ChildFieldKind.Direct(_) =>
        val e = rawTerm.asExprOf[S]
        '{ PSVec.singleton[S]($e) }
      case ChildFieldKind.OptionField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val opt = rawTerm.asExprOf[Option[e]]
            '{
              $opt match
                case Some(x) => PSVec.singleton[S](x.asInstanceOf[S])
                case None    => PSVec.empty[S]
            }
      case ChildFieldKind.ListField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val xs = rawTerm.asExprOf[List[e]]
            '{ PSVec.from[e]($xs).asInstanceOf[PSVec[S]] }
      case ChildFieldKind.VectorField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val xs = rawTerm.asExprOf[Vector[e]]
            '{ PSVec.from[e]($xs.iterator).asInstanceOf[PSVec[S]] }
      case ChildFieldKind.ArraySeqField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val xs = rawTerm.asExprOf[ArraySeq[e]]
            '{ PSVec.from[e]($xs.iterator).asInstanceOf[PSVec[S]] }
      case ChildFieldKind.SeqField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val xs = rawTerm.asExprOf[scala.collection.Seq[e]]
            '{ PSVec.from[e]($xs.iterator).asInstanceOf[PSVec[S]] }
      case ChildFieldKind.ArrayField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val arr = rawTerm.asExprOf[Array[e]]
            '{ PSVec.from[e]($arr).asInstanceOf[PSVec[S]] }
      case ChildFieldKind.TraverseField(tyconTpe, elemTpe) =>
        (tyconTpe.asType, elemTpe.asType) match
          case ('[type f[_]; f], '[e]) =>
            val trav = scala
              .quoted
              .Expr
              .summon[Traverse[f]]
              .getOrElse(
                report.errorAndAbort(s"plate: missing Traverse for ${tyconTpe.show}")
              )
            val fa = rawTerm.asExprOf[f[e]]
            '{ PlateMacro.fromTraverse[f, S, e]($fa)(using $trav) }
          case _ =>
            report.errorAndAbort(s"plate: expected unary type constructor for ${tyconTpe.show}")

  private def productChildrenVec[S: Type, V: Type](
      src: Expr[V],
      sTpe: TypeRepr,
  )(using Quotes): Expr[PSVec[S]] =
    val fields = childFieldsOf[V](sTpe)
    val allDirect = fields.forall(_._2.isInstanceOf[ChildFieldKind.Direct])
    if allDirect then
      val reads: List[Expr[S]] =
        fields.map((name, _) => Select.unique(src.asTerm, name).asExprOf[S])
      reads match
        case Nil             => '{ PSVec.empty[S] }
        case r :: Nil        => '{ PSVec.singleton[S]($r) }
        case r0 :: r1 :: Nil => '{ PSVec.of[S]($r0, $r1) }
        case _               =>
          val anys: List[Expr[Any]] = reads
          '{ PSVec.unsafeWrap[S](scala.Array[Any](${ Varargs(anys) }*)) }
    else
      fields match
        case Nil                 => '{ PSVec.empty[S] }
        case (name, kind) :: Nil => fieldChildrenExpr[S, V](src, name, kind)
        case _                   =>
          val parts: List[Expr[PSVec[S]]] =
            fields.map((name, kind) => fieldChildrenExpr[S, V](src, name, kind))
          '{ PlateMacro.concat[S](scala.Array[PSVec[S]](${ Varargs(parts) }*)) }

  /** The constructor-threading core: route each declaration-order parameter to either the matching
    * new child (by index on the fixed-arity path, or through [[PlateMacro.RebuildCursor]] when
    * container fields are present) or the preserved skeleton field read off `src`.
    */
  private def reconstructFields[S: Type, V: Type](
      cc: CaseClass[V],
      src: Expr[V],
      vec: Expr[PSVec[S]],
      sTpe: TypeRepr,
  )(using Quotes): Expr[V] =
    val vTpe = TypeRepr.of[V]
    val nameToType: Map[String, TypeRepr] =
      vTpe.typeSymbol.caseFields.map(f => f.name -> vTpe.memberType(f)).toMap
    val fields: List[(String, ChildFieldKind)] = childFieldsOf[V](sTpe)
    val allDirect = fields.forall(_._2.isInstanceOf[ChildFieldKind.Direct])

    if allDirect then
      val directNames: List[String] = fields.map(_._1)
      val directTypes: List[TypeRepr] = fields.collect {
        case (_, ChildFieldKind.Direct(t)) => t
      }
      val constructed: Id[Option[Expr[V]]] = cc.construct[Id] { (param: Parameter) =>
        val selfIdx = directNames.indexOf(param.name)
        if selfIdx >= 0 then
          val idxExpr = scala.quoted.Expr(selfIdx)
          val elemTpe = directTypes(selfIdx)
          elemTpe.asType match
            case '[e] =>
              val rebuilt: Expr[e] =
                if sTpe <:< elemTpe then '{ ${ vec }(${ idxExpr }).asInstanceOf[e] }
                else
                  val orig = Select.unique(src.asTerm, param.name).asExprOf[e]
                  '{
                    (${ vec }(${ idxExpr }): Any) match
                      case typed: (e @unchecked) => typed
                      case _                     => $orig
                  }
              Existential[Expr, e](rebuilt): Expr_??
        else skeletonField[V](src, nameToType, param.name)
      }
      constructed.getOrElse(
        report.errorAndAbort(
          s"plate[${Type.prettyPrint[V]}]: failed to call the primary constructor."
        )
      )
    else
      val fieldMap: Map[String, ChildFieldKind] = fields.toMap
      '{
        val cur = new PlateMacro.RebuildCursor[S]($vec)
        ${
          val constructed: Id[Option[Expr[V]]] = cc.construct[Id] { (param: Parameter) =>
            fieldMap.get(param.name) match
              case Some(kind) =>
                rebuildCursorParam[S, V](src, '{ cur }, param.name, kind, nameToType(param.name))
              case None =>
                skeletonField[V](src, nameToType, param.name)
          }
          constructed.getOrElse(
            report.errorAndAbort(
              s"plate[${Type.prettyPrint[V]}]: failed to call the primary constructor."
            )
          )
        }
      }

  private def skeletonField[V: Type](
      src: Expr[V],
      nameToType: Map[String, TypeRepr],
      paramName: String,
  )(using Quotes): Expr_?? =
    val tpe = nameToType.getOrElse(
      paramName,
      report.errorAndAbort(s"plate: internal error — unexpected parameter '$paramName'."),
    )
    tpe.asType match
      case '[t] =>
        Existential[Expr, t](Select.unique(src.asTerm, paramName).asExprOf[t]): Expr_??

  private def rebuildCursorParam[S: Type, V: Type](
      src: Expr[V],
      cur: Expr[PlateMacro.RebuildCursor[S]],
      name: String,
      kind: ChildFieldKind,
      paramTpe: TypeRepr,
  )(using Quotes): Expr_?? =
    val rawOrig = Select.unique(src.asTerm, name)
    kind match
      case ChildFieldKind.Direct(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            Existential[Expr, e]('{ $cur.takeOne[e]() }): Expr_??
      case ChildFieldKind.OptionField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val orig = rawOrig.asExprOf[Option[e]]
            Existential[Expr, Option[e]]('{ $cur.takeOption[e]($orig) }): Expr_??
      case ChildFieldKind.ListField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val orig = rawOrig.asExprOf[List[e]]
            Existential[Expr, List[e]]('{ $cur.takeList[e]($orig) }): Expr_??
      case ChildFieldKind.VectorField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val orig = rawOrig.asExprOf[Vector[e]]
            Existential[Expr, Vector[e]]('{ $cur.takeVector[e]($orig) }): Expr_??
      case ChildFieldKind.ArraySeqField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val orig = rawOrig.asExprOf[ArraySeq[e]]
            Existential[Expr, ArraySeq[e]]('{ $cur.takeArraySeq[e]($orig) }): Expr_??
      case ChildFieldKind.SeqField(elemTpe) =>
        paramTpe.asType match
          case '[p] =>
            elemTpe.asType match
              case '[e] =>
                val orig = rawOrig.asExprOf[scala.collection.Seq[e]]
                Existential[Expr, p]('{ $cur.takeSeq[e]($orig).asInstanceOf[p] }): Expr_??
      case ChildFieldKind.ArrayField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val ct = scala
              .quoted
              .Expr
              .summon[ClassTag[e]]
              .getOrElse(
                report.errorAndAbort(s"plate: missing ClassTag[${Type.prettyPrint[e]}]")
              )
            val orig = rawOrig.asExprOf[Array[e]]
            Existential[Expr, Array[e]]('{ $cur.takeArray[e]($orig)(using $ct) }): Expr_??
      case ChildFieldKind.TraverseField(tyconTpe, elemTpe) =>
        (tyconTpe.asType, elemTpe.asType) match
          case ('[type f[_]; f], '[e]) =>
            val trav = scala
              .quoted
              .Expr
              .summon[Traverse[f]]
              .getOrElse(
                report.errorAndAbort(s"plate: missing Traverse for ${tyconTpe.show}")
              )
            val orig = rawOrig.asExprOf[f[e]]
            Existential[Expr, f[e]]('{ $cur.takeTraverse[f, e]($orig)(using $trav) }): Expr_??
          case _ =>
            report.errorAndAbort(s"plate: expected unary type constructor for ${tyconTpe.show}")
