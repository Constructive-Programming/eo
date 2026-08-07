package dev.constructive.eo
package kyo

import scala.NamedTuple.{withNames, AnyNamedTuple, NamedTuple}
import scala.quoted.*

import _root_.kyo.*

import optics.{BijectionIso, Iso}

/** Macro backing `Record.iso` — stages the NamedTuple/case-class ↔ `Record` bijection for a
  * concrete shape into straight-line code (see the scaladoc on `iso` for the expansion). All typing
  * flows through kyo's public seams: `~` / `&` build the record with the literal-singleton field
  * names, `getField` reads it back under summoned `Fields.Have` evidence, `withNames` renames the
  * result tuple (named tuples) and `new T(…)` rebuilds the product (case classes — the primary
  * constructor, not `copy`, mirroring eo-generics). `asExprOf` coercions are compile-time-checked;
  * up to 22 fields the expansion contains no runtime casts, and beyond that named-tuple elements go
  * through `scala.runtime.Tuples.apply` / `Tuples.fromIArray` plus the same match-type-justified
  * casts the stdlib's `Tuple#apply` / `Tuple.fromIArray` perform (case classes select fields
  * directly, so they never need the fallback).
  */
private[kyo] object RecordIsoMacro:

  def isoImpl[T: Type](using q: Quotes): Expr[BijectionIso[T, T, ?, ?]] =
    import q.reflect.*

    def fail(msg: String): Nothing = report.errorAndAbort(s"Record.iso: $msg")

    val tpe = TypeRepr.of[T]

    val AppliedType(tildeCons, _) = TypeRepr.of[("x" ~ Int)]: @unchecked

    def fieldTpe(n: String, v: TypeRepr): TypeRepr =
      tildeCons.appliedTo(List(ConstantType(StringConstant(n)), v))

    // ("n0" ~ v0) & ("n1" ~ v1) & …, threading the precise intersection type through the
    // left-nested `&` chain (=:= to any other nesting by intersection associativity).
    // NB `iso` is transparent inline, so this intersection spelling is refined into CLIENT
    // bytecode at every call site — changing how the field type is nested or spelled is a
    // binary-incompatible change for downstream code, not an internal refactor.
    def combine(parts: List[(TypeRepr, Expr[?])]): (TypeRepr, Expr[?]) =
      parts.reduceLeft { (acc, part) =>
        val resTpe = AndType(acc._1, part._1)
        (acc._1.asType, part._1.asType, resTpe.asType) match
          case ('[a], '[b], '[r]) =>
            (
              resTpe,
              '{ ${ acc._2.asExprOf[Record[a]] } & ${ part._2.asExprOf[Record[b]] } }
                .asExprOf[Record[r]]
            )
          case _ => fail(s"unreachable: ${resTpe.show} failed to re-type")
      }

    def namedField(n: String, vt: TypeRepr, value: Expr[?]): (TypeRepr, Expr[?]) =
      (ConstantType(StringConstant(n)).asType, vt.asType) match
        case ('[type nS <: String; nS], '[v]) =>
          (fieldTpe(n, vt), '{ ${ Expr(n).asExprOf[nS] } ~ ${ value.asExprOf[v] } }: Expr[?])
        case _ => fail(s"unreachable: field '$n' failed to re-type")

    // rec.getField("n") under summoned Fields.Have evidence. Term-level application: the quoted
    // type-binder pattern loses the `Singleton` half of getField's `Name <: String & Singleton`
    // bound, but the concrete ConstantType satisfies it, so the staged tree is sound.
    def readField(fTpe: TypeRepr, rec: Expr[?], n: String, vt: TypeRepr): Expr[?] =
      val nTpe = ConstantType(StringConstant(n))
      val haveTpe = TypeRepr.of[Fields.Have].appliedTo(List(fTpe, nTpe))
      val have = (haveTpe.asType match
        case '[h] => Expr.summon[h]
      ).getOrElse {
        fail(s"no Fields.Have[${fTpe.show}, \"$n\"] — field lookup evidence did not derive")
      }
      vt.asType match
        case '[v] =>
          Select
            .unique(rec.asTerm, "getField")
            .appliedToTypes(List(nTpe, vt))
            .appliedTo(Literal(StringConstant(n)))
            .appliedTo(have.asTerm)
            .asExprOf[v]

    def assemble(fTpe: TypeRepr)(
        to: Expr[T] => Expr[?],
        from: Expr[?] => Expr[T],
    ): Expr[BijectionIso[T, T, ?, ?]] =
      fTpe.asType match
        case '[fT] =>
          '{
            Iso[T, T, Record[fT], Record[fT]](
              (s: T) => ${ to('s).asExprOf[Record[fT]] },
              (rec: Record[fT]) => ${ from('rec) },
            )
          }.asExprOf[BijectionIso[T, T, ?, ?]]
        case _ => fail(s"unreachable: ${fTpe.show} failed to re-type")

    // ---- named tuples ---------------------------------------------------

    def namedTupleIso: Expr[BijectionIso[T, T, ?, ?]] =
      def tupleTypes(t: TypeRepr): List[TypeRepr] =
        t.asType match
          case '[h *: rest]  => TypeRepr.of[h] :: tupleTypes(TypeRepr.of[rest])
          case '[EmptyTuple] => Nil
          case _             => fail(s"not a concrete tuple type: ${t.show}")

      val (namesTpe, valuesTpe) = tpe.dealias match
        case AppliedType(_, List(ns, vs)) => (ns, vs)
        case other => fail(s"expected a concrete NamedTuple type, got ${other.show}")

      val names = tupleTypes(namesTpe).map {
        case ConstantType(StringConstant(s)) => s
        case other => fail(s"field name is not a string literal: ${other.show}")
      }
      val valueTpes = tupleTypes(valuesTpe)
      if names.isEmpty then fail("empty named tuples are not supported")
      val fTpe = names.lazyZip(valueTpes).map(fieldTpe).reduceLeft(AndType(_, _))

      (namesTpe.asType, valuesTpe.asType) match
        case ('[type nsT <: Tuple; nsT], '[type vsT <: Tuple; vsT]) =>

          // Typed element read: real `_N` accessors inside the TupleN range, and beyond it the
          // same `scala.runtime.Tuples.apply` + match-type-justified cast that the stdlib's
          // `Tuple#apply` inlines to (TupleXXL has no per-element accessors).
          def element(tup: Expr[vsT], i: Int, vt: TypeRepr): Expr[?] =
            vt.asType match
              case '[v] =>
                if names.sizeIs <= 22 then Select.unique(tup.asTerm, "_" + (i + 1)).asExprOf[v]
                else '{ scala.runtime.Tuples.apply($tup, ${ Expr(i) }).asInstanceOf[v] }

          assemble(fTpe)(
            to = nt =>
              '{
                val tup: vsT = ${ nt.asExprOf[NamedTuple[nsT, vsT]] }.toTuple
                ${
                  combine(
                    names
                      .lazyZip(valueTpes)
                      .zipWithIndex
                      .map {
                        case ((n, vt), i) =>
                          namedField(n, vt, element('tup, i, vt))
                      }
                      .toList
                  )._2
                }
              },
            from = rec =>
              val fieldExprs = names.lazyZip(valueTpes).map((n, vt) => readField(fTpe, rec, n, vt))
              // TupleN literal inside the range; beyond it the same `Tuples.fromIArray` +
              // match-type-justified cast that the stdlib's `Tuple.fromIArray` performs.
              val tuple =
                if fieldExprs.sizeIs <= 22 then Expr.ofTupleFromSeq(fieldExprs).asExprOf[vsT]
                else
                  '{
                    scala
                      .runtime
                      .Tuples
                      .fromIArray(IArray[Any](${
                        Varargs(fieldExprs)
                      }*).asInstanceOf[IArray[Object]])
                      .asInstanceOf[vsT]
                  }
              '{ $tuple.withNames[nsT] }.asExprOf[T],
          )

        case _ => fail(s"unreachable: ${tpe.show} failed to re-type")

    // ---- case classes ---------------------------------------------------

    def caseClassIso(sym: Symbol): Expr[BijectionIso[T, T, ?, ?]] =
      val fields = sym.caseFields
      if fields.isEmpty then fail(s"case class ${sym.name} has no fields")
      val names = fields.map(_.name)
      val valueTpes = fields.map(f => tpe.memberType(f).widenByName)
      val fTpe = names.lazyZip(valueTpes).map(fieldTpe).reduceLeft(AndType(_, _))

      val ctor = sym.primaryConstructor
      if ctor.paramSymss.count(_.exists(!_.isType)) > 1 then
        fail(s"case class ${sym.name} has multiple value parameter lists")
      val targs = tpe.dealias match
        case AppliedType(_, as) => as
        case _                  => Nil

      assemble(fTpe)(
        to = cc =>
          combine(
            fields
              .lazyZip(valueTpes)
              .map { (f, vt) =>
                vt.asType match
                  case '[v] => namedField(f.name, vt, cc.asTerm.select(f).asExprOf[v])
              }
              .toList
          )._2,
        from = rec =>
          val args = names.lazyZip(valueTpes).map((n, vt) => readField(fTpe, rec, n, vt).asTerm)
          val sel = Select(New(Inferred(tpe.dealias)), ctor)
          val typed = if targs.isEmpty then sel else TypeApply(sel, targs.map(Inferred(_)))
          Apply(typed, args.toList).asExprOf[T],
      )

    // ---- dispatch -------------------------------------------------------

    if tpe.dealias <:< TypeRepr.of[AnyNamedTuple] then namedTupleIso
    else
      tpe.classSymbol.filter(_.flags.is(Flags.Case)) match
        case Some(sym) => caseClassIso(sym)
        case None => fail(s"expected a concrete NamedTuple or case class type, got ${tpe.show}")
