package dev.constructive.eo
package generics

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.quoted.*
import scala.reflect.ClassTag

import cats.Traverse
import dev.constructive.eo.data.ObjArrBuilder
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

  /** The children of a leaf. Shared: a zero-length array has nothing for a caller to mutate, so it
    * satisfies `Plated.fromChildrenMap`'s fresh-array contract without allocating.
    */
  val NoChildren: Array[Any] = new Array[Any](0)

  /** Sentinel sort check for slots whose element type is a supertype of `S`: no per-child test. */
  val AnyChild: Any => Boolean = _ => true

  // ---- children: each child field's elements as a fresh array -----------------------------

  /** Concatenate per-field children arrays into one freshly-allocated, exactly-sized array. */
  def concat(parts: Array[Array[Any]]): Array[Any] =
    val n = parts.length
    @tailrec def total(i: Int, acc: Int): Int =
      if i < n then total(i + 1, acc + parts(i).length) else acc
    val size = total(0, 0)
    if size == 0 then NoChildren
    else
      val out = new Array[Any](size)
      @tailrec def copy(i: Int, at: Int): Unit =
        if i < n then
          val part = parts(i)
          System.arraycopy(part, 0, out, at, part.length)
          copy(i + 1, at + part.length)
      copy(0, 0)
      out

  def fromOption(xs: Option[Any]): Array[Any] =
    if xs.isEmpty then NoChildren else scala.Array[Any](xs.get)

  /** A `List` field's children as a fresh, exactly-sized array (one length walk, one fill). */
  def fromList(xs: List[Any]): Array[Any] =
    if xs.isEmpty then NoChildren
    else
      val out = new Array[Any](xs.length)
      @tailrec def fill(i: Int, rest: List[Any]): Unit =
        if rest.nonEmpty then
          out(i) = rest.head
          fill(i + 1, rest.tail)
      fill(0, xs)
      out

  /** Any other collection field's children as a fresh array (never the collection's own storage).
    */
  def fromIterable(xs: Iterable[Any]): Array[Any] =
    if xs.isEmpty then NoChildren else xs.toArray[Any]

  /** An `Array` field's children: a fresh copy, so callers may overwrite it in place. */
  def fromArray(xs: Array[?]): Array[Any] =
    if xs.length == 0 then NoChildren else Array.copyAs[Any](xs, xs.length)

  /** A `Traverse[F]` container's children as a fresh array, in traversal order. */
  def fromTraverse[F[_], E](fa: F[E])(using F: Traverse[F]): Array[Any] =
    val buf = new ObjArrBuilder(8)
    F.foldLeft(fa, ())((_, e) => { buf.append(e); () })
    if buf.size == 0 then NoChildren else buf.freezeArr

  // ---- mapChildren: `g` over each child, guarded by the slot's sort check --------------------

  /** `g` applied to one child of a slot whose element type `ok` tests: a result of the wrong sort
    * keeps `orig`, so a sort-swapping rewrite can't smuggle an ill-typed value into the slot.
    */
  def mapOne(orig: Any, g: Any => Any, ok: Any => Boolean): Any =
    val x = g(orig)
    if (ok eq AnyChild) || ok(x) then x else orig

  def mapOption(xs: Option[Any], g: Any => Any, ok: Any => Boolean): Option[Any] =
    if xs.isEmpty then xs else Some(mapOne(xs.get, g, ok))

  /** Any `Seq`-shaped field (`List`, `Vector`, `ArraySeq`, `Seq`): `map` rebuilds in `xs`'s own
    * collection type, so a `List` stays a `List` and a `List`-backed `Seq` too.
    */
  def mapSeq(xs: scala.collection.Seq[Any], g: Any => Any, ok: Any => Boolean): Any =
    if xs.isEmpty then xs else xs.map(mapOne(_, g, ok))

  def mapArray[E: ClassTag](xs: Array[E], g: Any => Any, ok: Any => Boolean): Array[E] =
    if xs.length == 0 then xs else xs.map(e => mapOne(e, g, ok).asInstanceOf[E])

  def mapTraverse[F[_], E](xs: F[E], g: Any => Any, ok: Any => Boolean)(using
      F: Traverse[F]
  ): F[E] =
    F.map(xs)(e => mapOne(e, g, ok).asInstanceOf[E])

final private class HearthPlateMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  private enum ChildFieldKind(val elemTpe: TypeRepr):
    case Direct(e: TypeRepr) extends ChildFieldKind(e)
    case OptionField(e: TypeRepr) extends ChildFieldKind(e)
    case SeqField(e: TypeRepr, isList: Boolean) extends ChildFieldKind(e)
    case ArrayField(e: TypeRepr) extends ChildFieldKind(e)
    case TraverseField(tyconTpe: TypeRepr, e: TypeRepr) extends ChildFieldKind(e)

  /** The two things a child field contributes: its elements as a fresh children array, and itself
    * rebuilt with `g` applied to each element (given the slot's sort check). One table, so the
    * children order and the `mapChildren` order can't drift apart.
    */
  final private case class FieldOps(
      children: Expr[Array[Any]],
      mapped: (Expr[Any => Any], Expr[Any => Boolean]) => Expr[Any],
  )

  /** What a branch of `S` generates: something for a product (case class / enum case with fields),
    * something for a leaf (an object case, or a non-ADT union branch).
    */
  private trait BranchGen[R]:
    def product[W: Type](b: Branch[W])(using Quotes): Expr[R]
    def leaf[W: Type](w: Expr[W])(using Quotes): Expr[R]

  /** A product branch: its case-class view and the value matched to it. */
  private type Branch[W] = (CaseClass[W], Expr[W])

  def derivePlate[S](using Type[S]): Expr[Plated[S]] =
    val sTpe = TypeRepr.of[S]
    val unionBranches = flattenOrTypes(sTpe)
    val isUnion = unionBranches.sizeIs > 1
    val branches = orderBranches(unionBranches)
    if isUnion then
      if !branches.exists(b => b.asType match { case '[b] => isAdt[b] }) then
        report.errorAndAbort(
          s"plate[${Type.prettyPrint[S]}]: union has no case-class or enum branches."
        )
    else if !isAdt[S] then
      report.errorAndAbort(
        s"plate[${Type.prettyPrint[S]}]: expected a case class, enum, sealed hierarchy, or union type."
      )

    def dispatch[R: Type](s: Expr[S], gen: BranchGen[R])(using Quotes): Expr[R] =
      if isUnion then unionChain[S, R](branches, s, gen) else branch[S, R](s, gen)

    val childrenGen = new BranchGen[Array[Any]]:
      def product[W: Type](b: Branch[W])(using Quotes) = productChildren[S, W](b._2, sTpe)
      def leaf[W: Type](w: Expr[W])(using Quotes) = '{ PlateMacro.NoChildren }

    def mapGen(g: Expr[S => S]) = new BranchGen[S]:
      def product[W: Type](b: Branch[W])(using Quotes) =
        '{ ${ mapFields[S, W](b._1, b._2, g, sTpe) }.asInstanceOf[S] }
      def leaf[W: Type](w: Expr[W])(using Quotes) = '{ $w.asInstanceOf[S] }

    val childrenExpr: Expr[S => Array[Any]] = '{ (s: S) => ${ dispatch('{ s }, childrenGen) } }
    val mapChildrenExpr: Expr[(S, S => S) => S] =
      '{ (s: S, g: S => S) => ${ dispatch('{ s }, mapGen('{ g })) } }
    '{ Plated.fromChildrenMap[S]($childrenExpr, $mapChildrenExpr) }

  private def isAdt[T: Type]: Boolean =
    CaseClass.parse[T].toEither.isRight || Enum.parse[T].toEither.isRight

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

  // ---- branch dispatch -----------------------------------------------------------------------

  /** A multi-sorted union (O'Connor 2011 §5): type-test each branch in subtype-first order; a value
    * matching none is a leaf.
    */
  private def unionChain[S: Type, R: Type](
      branches: List[TypeRepr],
      s: Expr[S],
      gen: BranchGen[R],
  )(using Quotes): Expr[R] =
    branches match
      case Nil          => gen.leaf(s)
      case head :: tail =>
        head.asType match
          case '[b] =>
            val elseBr = unionChain[S, R](tail, s, gen)
            '{
              ($s: Any) match
                case v: (b @unchecked) => ${ branch[b, R]('{ v }, gen) }
                case _                 => $elseBr
            }

  /** A product, a (nested) enum / sealed hierarchy matched case by case, or a leaf. */
  private def branch[V: Type, R: Type](src: Expr[V], gen: BranchGen[R])(using Quotes): Expr[R] =
    CaseClass.parse[V].toEither match
      case Right(cc) => gen.product((cc, src))
      case Left(_)   =>
        Enum.parse[V].toEither match
          case Right(subEnum) =>
            subEnum
              .matchOn[Id, R](src) { matched =>
                import matched.{Underlying as W, value as wExpr}
                if TypeRepr.of[W] =:= TypeRepr.of[V] then gen.leaf(wExpr)
                else branch[W, R](wExpr, gen)
              }
              .getOrElse(
                report.errorAndAbort(s"plate[${Type.prettyPrint[V]}]: matchOn produced no cases.")
              )
          case Left(_) => gen.leaf(src)

  // ---- one product ---------------------------------------------------------------------------

  private def productChildren[S: Type, V: Type](src: Expr[V], sTpe: TypeRepr)(using
      Quotes
  ): Expr[Array[Any]] =
    val fields = childFieldsOf[V](sTpe)
    if fields.forall(_._2.isInstanceOf[ChildFieldKind.Direct]) then
      val reads: List[Expr[Any]] = fields.map((name, _) => Select.unique(src.asTerm, name).asExpr)
      reads match
        case Nil => '{ PlateMacro.NoChildren }
        case _   => '{ scala.Array[Any](${ Varargs(reads) }*) }
    else
      fields.map((name, kind) => fieldOps[S](Select.unique(src.asTerm, name), kind).children) match
        case Nil         => '{ PlateMacro.NoChildren }
        case part :: Nil => part
        case parts       => '{ PlateMacro.concat(scala.Array[Array[Any]](${ Varargs(parts) }*)) }

  /** `mapChildren` for one product: a leaf (no child fields) comes back as is; otherwise one
    * constructor call (`new V(...)`, never `.copy`) whose child arguments are `g`'s guarded results
    * and whose other arguments are read off `src`. Scala evaluates constructor arguments left to
    * right, so `g` sees the children in declaration order — the order [[productChildren]] packs
    * them, which `Plated.fromChildrenMap`'s array rebuild relies on.
    */
  private def mapFields[S: Type, V: Type](
      cc: CaseClass[V],
      src: Expr[V],
      g: Expr[S => S],
      sTpe: TypeRepr,
  )(using Quotes): Expr[V] =
    val vTpe = TypeRepr.of[V]
    val fieldMap: Map[String, ChildFieldKind] = childFieldsOf[V](sTpe).toMap
    if fieldMap.isEmpty then src
    else
      '{
        val gAny = $g.asInstanceOf[Any => Any]
        ${
          cc.construct[Id] { (param: Parameter) =>
            val field = vTpe
              .typeSymbol
              .caseFields
              .find(_.name == param.name)
              .getOrElse(
                report
                  .errorAndAbort(s"plate: internal error — unexpected parameter '${param.name}'.")
              )
            vTpe.memberType(field).asType match
              case '[p] =>
                val raw = Select.unique(src.asTerm, param.name)
                val arg: Expr[p] = fieldMap.get(param.name) match
                  case Some(kind) =>
                    val mapped =
                      fieldOps[S](raw, kind).mapped('{ gAny }, sortCheck[S](kind.elemTpe))
                    '{ $mapped.asInstanceOf[p] }
                  case None => raw.asExprOf[p]
                Existential[Expr, p](arg): Expr_??
          }.getOrElse(
            report.errorAndAbort(
              s"plate[${Type.prettyPrint[V]}]: failed to call the primary constructor."
            )
          )
        }
      }

  private def fieldOps[S: Type](raw: Term, kind: ChildFieldKind)(using Quotes): FieldOps =
    kind match
      case ChildFieldKind.Direct(_) =>
        val x = raw.asExpr
        FieldOps('{ scala.Array[Any]($x) }, (g, ok) => '{ PlateMacro.mapOne($x, $g, $ok) })
      case ChildFieldKind.OptionField(_) =>
        val xs = raw.asExprOf[Option[Any]]
        FieldOps(
          '{ PlateMacro.fromOption($xs) },
          (g, ok) => '{ PlateMacro.mapOption($xs, $g, $ok) }
        )
      case ChildFieldKind.SeqField(_, isList) =>
        val xs = raw.asExprOf[scala.collection.Seq[Any]]
        val children =
          if isList then '{ PlateMacro.fromList(${ raw.asExprOf[List[Any]] }) }
          else '{ PlateMacro.fromIterable($xs) }
        FieldOps(children, (g, ok) => '{ PlateMacro.mapSeq($xs, $g, $ok) })
      case ChildFieldKind.ArrayField(elemTpe) =>
        elemTpe.asType match
          case '[e] =>
            val xs = raw.asExprOf[Array[e]]
            val ct = classTagFor[e]
            FieldOps(
              '{ PlateMacro.fromArray($xs) },
              (g, ok) => '{ PlateMacro.mapArray[e]($xs, $g, $ok)(using $ct) },
            )
      case ChildFieldKind.TraverseField(tyconTpe, elemTpe) =>
        (tyconTpe.asType, elemTpe.asType) match
          case ('[type f[_]; f], '[e]) =>
            val xs = raw.asExprOf[f[e]]
            val trav = traverseFor[f](tyconTpe)
            FieldOps(
              '{ PlateMacro.fromTraverse[f, e]($xs)(using $trav) },
              (g, ok) => '{ PlateMacro.mapTraverse[f, e]($xs, $g, $ok)(using $trav) },
            )
          case _ =>
            report.errorAndAbort(s"plate: expected unary type constructor for ${tyconTpe.show}")

  // ---- field classification ------------------------------------------------------------------

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
          else if seqTycons.contains(sym) then
            Some(ChildFieldKind.SeqField(elemTpe, isList = sym == seqTycons.head))
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

  /** Container type constructors handled as a `Seq` (`List` first: it gets an exact-size fill). */
  private lazy val seqTycons: List[Symbol] = List(
    TypeRepr.of[List[Any]].typeSymbol,
    TypeRepr.of[Vector[Any]].typeSymbol,
    TypeRepr.of[ArraySeq[Any]].typeSymbol,
    TypeRepr.of[scala.collection.immutable.Seq[Any]].typeSymbol,
    TypeRepr.of[scala.collection.Seq[Any]].typeSymbol,
  )

  private def childFieldsOf[V: Type](sTpe: TypeRepr): List[(String, ChildFieldKind)] =
    val vTpe = TypeRepr.of[V]
    vTpe.typeSymbol.caseFields.flatMap { f =>
      classifyField(vTpe.memberType(f), sTpe).map(k => f.name -> k)
    }

  /** The `ClassTag` an `Array[e]` child field needs to be rebuilt, or a compile error. */
  private def classTagFor[e: Type](using Quotes): Expr[ClassTag[e]] =
    scala
      .quoted
      .Expr
      .summon[ClassTag[e]]
      .getOrElse(report.errorAndAbort(s"plate: missing ClassTag[${Type.prettyPrint[e]}]"))

  /** The `Traverse` a container child field `F[e]` is walked with, or a compile error. */
  private def traverseFor[f[_]](
      tyconTpe: TypeRepr
  )(using Quotes, scala.quoted.Type[f]): Expr[Traverse[f]] =
    scala
      .quoted
      .Expr
      .summon[Traverse[f]]
      .getOrElse(report.errorAndAbort(s"plate: missing Traverse for ${tyconTpe.show}"))

  /** The runtime sort test for a child slot of element type `elemTpe`: [[PlateMacro.AnyChild]] when
    * every `S` fits (`S <: elemTpe`), else an instance test against the slot's element type.
    */
  private def sortCheck[S: Type](elemTpe: TypeRepr)(using Quotes): Expr[Any => Boolean] =
    if TypeRepr.of[S] <:< elemTpe then '{ PlateMacro.AnyChild }
    else
      elemTpe.asType match
        case '[e] =>
          '{ (x: Any) =>
            x match
              case _: (e @unchecked) => true
              case _                 => false
          }
