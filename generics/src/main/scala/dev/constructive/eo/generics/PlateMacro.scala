package dev.constructive.eo
package generics

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.quoted.*
import scala.reflect.ClassTag

import cats.Traverse
import dev.constructive.eo.optics.{Children, Plated}

/** Compile-time derivation of a [[dev.constructive.eo.optics.Plated]] for a recursive ADT or
  * mutually recursive type family `S`.
  *
  *   - '''Single-sorted ADTs''' (`enum Expr`, sealed hierarchies, recursive case classes): focuses
  *     every immediate child field of type `S`, plus children inside `Option[S]`, `List[S]`,
  *     `Vector[S]`, `ArraySeq[S]`, `Seq[S]`, `Array[S]`, or any `F[S]` with a `cats.Traverse[F]` in
  *     scope (O'Connor 2011 §5.1, Figure 5). Containers compose, so `Option[List[S]]`,
  *     `List[List[S]]`, … are reached too. A field whose type is a product record that is not
  *     itself an `S` is looked through: the `S` positions inside the record (at any depth, through
  *     further containers and records) become children of the enclosing case, and `mapChildren`
  *     rebuilds the record. The children stay typed `S`, so no union and no runtime type tests are
  *     needed.
  *   - '''Mutually recursive families / union types''' (`type Lang = Stm | Expr | Var | Typ`,
  *     `plate[Lang]`): derives a multi-sorted plate (O'Connor 2011 §5) over the union's branches,
  *     descending into any field (or container field) whose element type is a subtype of `S`.
  *     Unsealed leaf branches in a union (e.g. Spark's `DataType` after its recursive `StructType |
  *     ArrayType | MapType` subtypes) are treated as leaves (`0` children, identity rebuild).
  *
  * Reconstruction routes through `CaseClass.construct[Id]` (`new V(...)`, never `.copy`), so Scala
  * 3 enum cases reassemble uniformly.
  *
  * A recursive holder record (a record that reaches itself again before reaching `S`) is not
  * expanded further: the repeated type is treated as a leaf, so such a field's deeper `S` positions
  * stay out of the walk.
  */
object PlateMacro:

  /** Quoted-macro implementation behind the top-level `plate[S]` (spliced directly by the package
    * object) — see the object doc for supported field, container, record, and union shapes.
    */
  def deriveImpl[S: Type](using q: Quotes): Expr[Plated[S]] =
    new HearthPlateMacro(q).derivePlate[S]

  /** Shared zero-length array for a leaf's internal children array. */
  val NoChildren: Array[Any] = new Array[Any](0)

  /** One-shot child source for the generated `rebuildArray`: `apply` ignores the old child and
    * hands back the next rewritten element. One instance per rebuild, no iterator/lambda.
    */
  final class Feed[S](kids: Array[Any]) extends (S => S):
    private var i = 0

    def apply(old: S): S =
      val next = kids(i)
      i += 1
      next.asInstanceOf[S]

  /** Sentinel sort check for slots whose element type is a supertype of `S`: no per-child test. */
  val AnyChild: Any => Boolean = _ => true

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

final private class HearthPlateMacro(q: Quotes) extends HearthMacro(q):

  import quotes.reflect.*

  /** One field's path to the `S` positions it holds.
    *
    * `tpe` is the static type of the value at this level; `kind` says how to read its `S`
    * descendants and how to rebuild it. Containers compose (`Option[List[S]]`), and a product
    * record that is not itself an `S` is looked through (`Product`), so any stack of supported
    * containers over a record over `S` is a child position.
    */
  final private case class Focus(tpe: TypeRepr, kind: FocusKind)

  private enum FocusKind:
    /** The value itself is a child (a subtype of `S`). */
    case Direct

    /** `Option[elemTpe]` holding `inner`. */
    case Opt(elemTpe: TypeRepr, inner: Focus)

    /** A `Seq`-shaped container of `elemTpe` holding `inner`; `isList` preserves `List` sharing. */
    case SeqOf(elemTpe: TypeRepr, isList: Boolean, inner: Focus)
    case ArrayOf(elemTpe: TypeRepr, inner: Focus)
    case TraverseOf(tyconTpe: TypeRepr, elemTpe: TypeRepr, inner: Focus)

    /** A product record that is not an `S`, with the fields (declaration order) that reach `S`. */
    case Product(fields: List[(String, Focus)])

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

    val childrenGen = new BranchGen[Children[S]]:
      def product[W: Type](b: Branch[W])(using Quotes) = productChildren[S, W](b._2, sTpe)
      def leaf[W: Type](w: Expr[W])(using Quotes) = '{ Children.empty[S] }

    val arrayGen = new BranchGen[Array[Any]]:
      def product[W: Type](b: Branch[W])(using Quotes) =
        productChildrenArray[S, W](b._2, sTpe)
      def leaf[W: Type](w: Expr[W])(using Quotes) = '{ PlateMacro.NoChildren }

    def mapGen(g: Expr[S => S]) = new BranchGen[S]:
      def product[W: Type](b: Branch[W])(using Quotes) =
        '{ ${ mapFields[S, W](b._1, b._2, g, sTpe) }.asInstanceOf[S] }
      def leaf[W: Type](w: Expr[W])(using Quotes) = '{ $w.asInstanceOf[S] }

    val childrenExpr: Expr[S => Children[S]] =
      '{ (s: S) => ${ dispatch('{ s }, childrenGen) } }
    val childrenArrayExpr: Expr[S => Array[Any]] =
      '{ (s: S) => ${ dispatch('{ s }, arrayGen) } }
    val mapChildrenExpr: Expr[(S, S => S) => S] =
      '{ (s: S, g: S => S) => ${ dispatch('{ s }, mapGen('{ g })) } }
    val rebuildExpr: Expr[(S, Children[S]) => S] =
      '{ (s: S, kids: Children[S]) =>
        val it = kids.iterator
        ${ dispatch('{ s }, mapGen('{ (_: S) => it.next() })) }
      }
    val rebuildArrayExpr: Expr[(S, Array[Any]) => S] =
      '{ (s: S, kids: Array[Any]) =>
        val feed: S => S = new PlateMacro.Feed[S](kids)
        ${ dispatch('{ s }, mapGen('{ feed })) }
      }
    '{
      Plated.fromChildren[S](
        $childrenExpr,
        $rebuildExpr,
        $mapChildrenExpr,
        $rebuildArrayExpr,
        $childrenArrayExpr,
      )
    }

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
  ): Expr[Children[S]] =
    // The node's children are exactly its direct fields: one fresh array the engine takes over
    // (identity view, no per-field wrappers).
    productView[S, V, Children[S]](src, sTpe)(
      empty = '{ Children.empty[S] },
      direct = arr => '{ Children.fromOwnedArray[S]($arr) },
      walk = identity,
    )

  /** One product's children, in the form the caller wants: `empty` for none, `direct` over one
    * fresh array when every child field is a direct `S` position, `walk` over the per-field
    * [[partsChildren]] view otherwise.
    */
  private def productView[S: Type, V: Type, R](src: Expr[V], sTpe: TypeRepr)(
      empty: Expr[R],
      direct: Expr[Array[Any]] => Expr[R],
      walk: Expr[Children[S]] => Expr[R],
  )(using Quotes): Expr[R] =
    val fields = childFieldsOf[V](sTpe)
    if fields.forall((_, f) => isDirect(f.kind)) then
      fields.map((name, _) => Select.unique(src.asTerm, name).asExpr) match
        case Nil   => empty
        case reads => direct('{ scala.Array[Any](${ Varargs(reads) }*) })
    else walk(partsChildren[S](fields, src.asTerm))

  private def partsChildren[S: Type](
      fields: List[(String, Focus)],
      holder: Term,
  )(using Quotes): Expr[Children[S]] =
    val parts = fields.map((name, f) => childrenFocus[S](f, Select.unique(holder, name)))
    parts match
      case Nil         => '{ Children.empty[S] }
      case part :: Nil => part
      case _           => parts.reduceRight((a, b) => '{ Children.concat[S]($a, $b) })

  /** The engine's array view of one product: the direct-fields fast path builds one fresh array
    * itself (the `childrenArray` contract), everything else materialises from [[partsChildren]].
    */
  private def productChildrenArray[S: Type, V: Type](src: Expr[V], sTpe: TypeRepr)(using
      Quotes
  ): Expr[Array[Any]] =
    productView[S, V, Array[Any]](src, sTpe)(
      empty = '{ PlateMacro.NoChildren },
      direct = identity,
      walk = kids => '{ Children.toAnyArray($kids) },
    )

  /** `mapChildren` for one product: a leaf (no child fields) comes back as is; otherwise one
    * constructor call (`new V(...)`, never `.copy`) whose child arguments are `g`'s guarded results
    * and whose other arguments are read off `src`. Scala evaluates constructor arguments left to
    * right, so `g` sees the children in declaration order — the order [[productChildren]] packs
    * them, which the generated `rebuildArray` fast path relies on.
    */
  private def mapFields[S: Type, V: Type](
      cc: CaseClass[V],
      src: Expr[V],
      g: Expr[S => S],
      sTpe: TypeRepr,
  )(using Quotes): Expr[V] =
    val fields = childFieldsOf[V](sTpe)
    if fields.isEmpty then src
    else
      '{
        val gAny = $g.asInstanceOf[Any => Any]
        ${ constructHolder[S, V](cc, src.asTerm, fields, '{ gAny }) }
      }

  // ---- one field: children, map --------------------------------------------------------------

  /** One field's code generation, by the kind of position it is. [[visitFocus]] resolves each
    * kind's types and hands the field over as a typed expression; the read ([[ChildrenFocus]]) and
    * the write ([[MapFocus]]) supply only the code they generate, so the two walk the same
    * positions in the same order by construction.
    */
  private type Fields = List[(String, Focus)]
  private type QType[f[_]] = scala.quoted.Type[f]
  private type TravOf[f[_]] = Expr[Traverse[f]]

  private trait FocusVisitor[R]:
    def direct[t: Type](x: Expr[t], f: Focus): Expr[R]
    def option[e: Type](xs: Expr[Option[e]], inner: Focus): Expr[R]
    def seq[e: Type](raw: Term, isList: Boolean, inner: Focus): Expr[R]
    def array[e: Type](xs: Expr[Array[e]], inner: Focus): Expr[R]

    def traverse[f[_]: QType, e: Type](xs: Expr[f[e]], trav: TravOf[f], inner: Focus): Expr[R]

    def product[t: Type](holder: Expr[t], f: Focus, fields: Fields): Expr[R]

  private def visitFocus[R](f: Focus, raw: Term, v: FocusVisitor[R])(using Quotes): Expr[R] =
    f.kind match
      case FocusKind.Direct => f.tpe.asType match { case '[t] => v.direct(raw.asExprOf[t], f) }
      case FocusKind.Opt(elemTpe, inner) =>
        elemTpe.asType match { case '[e] => v.option(raw.asExprOf[Option[e]], inner) }
      case FocusKind.SeqOf(elemTpe, isList, inner) =>
        elemTpe.asType match { case '[e] => v.seq[e](raw, isList, inner) }
      case FocusKind.ArrayOf(elemTpe, inner) =>
        elemTpe.asType match { case '[e] => v.array(raw.asExprOf[Array[e]], inner) }
      case FocusKind.TraverseOf(tyconTpe, elemTpe, inner) =>
        (tyconTpe.asType, elemTpe.asType) match
          case ('[type f[_]; f], '[e]) =>
            v.traverse[f, e](raw.asExprOf[f[e]], traverseFor[f](tyconTpe), inner)
          case _ =>
            report.errorAndAbort(s"plate: expected unary type constructor for ${tyconTpe.show}")
      case FocusKind.Product(fields) =>
        f.tpe.asType match { case '[t] => v.product(raw.asExprOf[t], f, fields) }

  /** Read one field: its `S` positions, as a [[Children]] view. */
  private def childrenFocus[S: Type](f: Focus, raw: Term)(using Quotes): Expr[Children[S]] =
    visitFocus(f, raw, ChildrenFocus[S]())

  private class ChildrenFocus[S: Type](using Quotes) extends FocusVisitor[Children[S]]:

    /** The read of one element of a container field. */
    private def each[e: Type](inner: Focus): Expr[e => Children[S]] =
      '{ (x: e) => ${ childrenFocus[S](inner, '{ x }.asTerm) } }

    def direct[t: Type](x: Expr[t], f: Focus): Expr[Children[S]] =
      '{ Children.single[S](($x).asInstanceOf[S]) }

    def option[e: Type](xs: Expr[Option[e]], inner: Focus): Expr[Children[S]] =
      if isDirect(inner.kind) then '{ Children.fromOption[S]($xs.asInstanceOf[Option[S]]) }
      else
        '{
          if $xs.isEmpty then Children.empty[S]
          else ${ childrenFocus[S](inner, '{ $xs.get }.asTerm) }
        }

    def seq[e: Type](raw: Term, isList: Boolean, inner: Focus): Expr[Children[S]] =
      if isDirect(inner.kind) then
        if isList then '{ Children.fromList[S](${ raw.asExprOf[List[S]] }) }
        else '{ Children.fromSeq[S](${ raw.asExprOf[scala.collection.Seq[S]] }) }
      else
        // a List is a Seq: one flatten serves both (only the direct case keeps a List as-is)
        '{
          Children.flattenSeq[e, S](${ raw.asExprOf[scala.collection.Seq[e]] })(${ each[e](inner) })
        }

    def array[e: Type](xs: Expr[Array[e]], inner: Focus): Expr[Children[S]] =
      if isDirect(inner.kind) then '{ Children.fromArray[S]($xs) }
      else '{ Children.flattenArray[e, S]($xs)(${ each[e](inner) }) }

    def traverse[f[_]: QType, e: Type](
        xs: Expr[f[e]],
        trav: TravOf[f],
        inner: Focus
    ): Expr[Children[S]] =
      if isDirect(inner.kind) then
        '{ Children.fromTraverse[f, e]($xs)(using $trav).asInstanceOf[Children[S]] }
      else
        '{
          Children.flattenTraverse[f, e, S]($xs)(${ each[e](inner) })(using $trav)
        }

    def product[t: Type](
        holder: Expr[t],
        f: Focus,
        fields: Fields
    ): Expr[Children[S]] =
      '{
        val h = $holder
        ${ partsChildren[S](fields, '{ h }.asTerm) }
      }

  /** Rebuild one field: `g` over the `S` positions [[childrenFocus]] hands out, in the same order,
    * leaving every other position of the field (and of any record it passes through) untouched.
    */
  private def mapFocus[S: Type](f: Focus, raw: Term, g: Expr[Any => Any])(using Quotes): Expr[Any] =
    visitFocus(f, raw, MapFocus[S](g))

  private class MapFocus[S: Type](g: Expr[Any => Any])(using Quotes) extends FocusVisitor[Any]:

    /** The rewrite of one element of a container field. */
    private def each[e: Type](inner: Focus): Expr[e => e] =
      '{ (x: e) => ${ mapFocus[S](inner, '{ x }.asTerm, g) }.asInstanceOf[e] }

    def direct[t: Type](x: Expr[t], f: Focus): Expr[Any] =
      '{ PlateMacro.mapOne($x, $g, ${ sortCheck[S](f.tpe) }) }

    def option[e: Type](xs: Expr[Option[e]], inner: Focus): Expr[Any] =
      if isDirect(inner.kind) then
        '{
          PlateMacro
            .mapOption($xs.asInstanceOf[Option[Any]], $g, ${ sortCheck[S](inner.tpe) })
            .asInstanceOf[Option[e]]
        }
      else
        '{
          val o = $xs
          if o.isEmpty then o
          else Some(${ mapFocus[S](inner, '{ o.get }.asTerm, g) }.asInstanceOf[e])
        }

    def seq[e: Type](raw: Term, isList: Boolean, inner: Focus): Expr[Any] =
      val xs = raw.asExprOf[scala.collection.Seq[e]]
      if isDirect(inner.kind) then
        '{
          PlateMacro
            .mapSeq($xs.asInstanceOf[scala.collection.Seq[Any]], $g, ${ sortCheck[S](inner.tpe) })
            .asInstanceOf[scala.collection.Seq[e]]
        }
      else '{ $xs.map(${ each[e](inner) }) }

    def array[e: Type](xs: Expr[Array[e]], inner: Focus): Expr[Any] =
      val ct = classTagFor[e]
      if isDirect(inner.kind) then
        '{
          PlateMacro
            .mapArray[e]($xs, $g, ${ sortCheck[S](inner.tpe) })(using $ct)
            .asInstanceOf[Array[e]]
        }
      else '{ $xs.map(${ each[e](inner) })(using $ct) }

    def traverse[f[_]: QType, e: Type](xs: Expr[f[e]], trav: TravOf[f], inner: Focus): Expr[Any] =
      if isDirect(inner.kind) then
        '{
          PlateMacro
            .mapTraverse[f, e]($xs, $g, ${ sortCheck[S](inner.tpe) })(using $trav)
            .asInstanceOf[f[e]]
        }
      else '{ $trav.map($xs)(${ each[e](inner) }) }

    def product[t: Type](holder: Expr[t], f: Focus, fields: Fields): Expr[Any] =
      CaseClass.parse[t].toEither match
        case Right(cc) =>
          '{
            val h = $holder
            ${ constructHolder[S, t](cc, '{ h }.asTerm, fields, g) }
          }
        case Left(reason) =>
          report.errorAndAbort(s"plate: cannot rebuild the holder record ${f.tpe.show}: $reason")

  /** Rebuild a holder record from `src`, mapping the `S` positions its focus fields contain and
    * keeping every other constructor argument as it is.
    */
  private def constructHolder[S: Type, T: Type](
      cc: CaseClass[T],
      src: Term,
      fields: List[(String, Focus)],
      g: Expr[Any => Any],
  )(using Quotes): Expr[T] =
    val tTpe = TypeRepr.of[T]
    val fieldMap = fields.toMap
    cc.construct[Id] { (param: Parameter) =>
      val field = tTpe
        .typeSymbol
        .caseFields
        .find(_.name == param.name)
        .getOrElse(
          report.errorAndAbort(s"plate: internal error — unexpected parameter '${param.name}'.")
        )
      tTpe.memberType(field).asType match
        case '[p] =>
          val raw = Select.unique(src, param.name)
          val arg: Expr[p] = fieldMap.get(param.name) match
            case Some(focus) => '{ ${ mapFocus[S](focus, raw, g) }.asInstanceOf[p] }
            case None        => raw.asExprOf[p]
          Existential[Expr, p](arg): Expr_??
    }.getOrElse(
      report.errorAndAbort(
        s"plate[${Type.prettyPrint[T]}]: failed to call the primary constructor."
      )
    )

  // ---- field classification ------------------------------------------------------------------

  private def isDirect(kind: FocusKind): Boolean = kind match
    case FocusKind.Direct => true
    case _                => false

  private def isChildType(tpe: TypeRepr, sTpe: TypeRepr): Boolean =
    val w = tpe.widen.dealias
    !(w =:= TypeRepr.of[Nothing]) && !(w =:= TypeRepr.of[Null]) && (w <:< sTpe)

  /** The path from a field's static type to its `S` positions, or `None` when it holds none. */
  private def focusOf(fTpe: TypeRepr, sTpe: TypeRepr, visited: List[TypeRepr]): Option[Focus] =
    val w = fTpe.widen.dealias
    if isChildType(w, sTpe) then Some(Focus(w, FocusKind.Direct))
    else
      w match
        case AppliedType(tycon, List(elemRaw)) =>
          val elem = elemRaw.widen.dealias
          val sym = tycon.typeSymbol
          if sym == TypeRepr.of[Option[Any]].typeSymbol then
            focusOf(elem, sTpe, visited).map(inner => Focus(w, FocusKind.Opt(elem, inner)))
          else if seqTycons.contains(sym) then
            focusOf(elem, sTpe, visited).map(inner =>
              Focus(w, FocusKind.SeqOf(elem, isList = sym == seqTycons.head, inner))
            )
          else if sym == TypeRepr.of[Array[Any]].typeSymbol then
            focusOf(elem, sTpe, visited).map(inner => Focus(w, FocusKind.ArrayOf(elem, inner)))
          else traverseFocus(w, tycon, elem, sTpe, visited).orElse(holderFocus(w, sTpe, visited))
        case _ => holderFocus(w, sTpe, visited)

  /** `F[elem]` for a unary constructor with a `Traverse[F]` instance in scope at the call site. */
  private def traverseFocus(
      w: TypeRepr,
      tycon: TypeRepr,
      elem: TypeRepr,
      sTpe: TypeRepr,
      visited: List[TypeRepr],
  )(using Quotes): Option[Focus] =
    focusOf(elem, sTpe, visited).flatMap { inner =>
      tycon.asType match
        case '[type f[_]; f] =>
          scala.quoted.Expr.summon[Traverse[f]] match
            case Some(_) => Some(Focus(w, FocusKind.TraverseOf(tycon, elem, inner)))
            case None    => None
        case _ => None
    }

  /** A product record that is not itself an `S`: look through its case fields for `S` positions.
    * `visited` stops a record that reaches itself again (that repeated field stays a leaf).
    */
  private def holderFocus(t: TypeRepr, sTpe: TypeRepr, visited: List[TypeRepr]): Option[Focus] =
    val sym = t.typeSymbol
    if sym.isNoSymbol || visited.exists(_ =:= t) || !sym.isClassDef then None
    else
      val fields = sym.caseFields
      if fields.isEmpty then None
      else
        val nested = t :: visited
        val focused = fields.flatMap { f =>
          focusOf(t.memberType(f), sTpe, nested).map(k => f.name -> k)
        }
        if focused.isEmpty then None else Some(Focus(t, FocusKind.Product(focused)))

  /** Container type constructors handled as a `Seq` (`List` first: it gets an exact-size fill). */
  private lazy val seqTycons: List[Symbol] = List(
    TypeRepr.of[List[Any]].typeSymbol,
    TypeRepr.of[Vector[Any]].typeSymbol,
    TypeRepr.of[ArraySeq[Any]].typeSymbol,
    TypeRepr.of[scala.collection.immutable.Seq[Any]].typeSymbol,
    TypeRepr.of[scala.collection.Seq[Any]].typeSymbol,
  )

  private def childFieldsOf[V: Type](sTpe: TypeRepr): List[(String, Focus)] =
    val vTpe = TypeRepr.of[V]
    vTpe.typeSymbol.caseFields.flatMap { f =>
      focusOf(vTpe.memberType(f), sTpe, Nil).map(k => f.name -> k)
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
