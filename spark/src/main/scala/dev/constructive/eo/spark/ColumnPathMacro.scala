package dev.constructive.eo
package spark

import scala.annotation.tailrec
import scala.deriving.Mirror
import scala.quoted.*

/** The macro behind [[ColumnOptics.path]]: parses `_.a.b.get.c` into [[ColumnPath.Step]]s, checking
  * every hop is a case field of its receiver (or `.get` on an `Option`), and picks
  * [[ColumnPath.Total]] / [[ColumnPath.Partial]] by whether any `.get` occurs.
  */
private[spark] object ColumnPathMacro:

  def impl[S: Type](path: Expr[S => Any])(using Quotes): Expr[ColumnPath[S, ?]] =
    import quotes.reflect.*

    @tailrec def unwrap(t: Term): Term = t match
      case Inlined(_, _, inner) => unwrap(inner)
      case Typed(inner, _)      => unwrap(inner)
      case Block(Nil, inner)    => unwrap(inner)
      case other                => other

    val (param, body) = unwrap(path.asTerm) match
      case Lambda(List(p), b) => (p.symbol, unwrap(b))
      case other              =>
        report.errorAndAbort(
          s"ColumnOptics.path: expected a selector like _.a.b, got ${other.show}"
        )

    // Innermost hop first: walk the Select chain back to the lambda parameter.
    @tailrec def hops(t: Term, acc: List[(Term, String)]): List[(Term, String)] = unwrap(t) match
      case Select(receiver, name)          => hops(receiver, (unwrap(receiver), name) :: acc)
      case id: Ident if id.symbol == param => acc
      case other                           =>
        report.errorAndAbort(
          s"ColumnOptics.path: only field selections on the parameter are allowed, got ${other.show}"
        )

    val chain = hops(body, Nil)
    if chain.isEmpty then
      report.errorAndAbort("ColumnOptics.path: the selector must select at least one field")

    val optionSym = TypeRepr.of[Option[Any]].typeSymbol
    val steps: List[Expr[ColumnPath.Step]] = chain.map { (receiver, name) =>
      val rt = receiver.tpe.widen.dealias
      if rt.typeSymbol == optionSym then
        if name != "get" then
          report.errorAndAbort(s"ColumnOptics.path: on an Option only .get is allowed, got .$name")
        '{ ColumnPath.Step.Some }
      else
        val fields = rt.typeSymbol.caseFields.map(_.name)
        val index = fields.indexOf(name)
        if index < 0 then
          report.errorAndAbort(
            s"ColumnOptics.path: '$name' is not a case field of ${rt.show}. Known fields: ${fields.mkString(", ")}."
          )
        rt.asType match
          case '[r] =>
            Expr.summon[Mirror.ProductOf[r]] match
              case Some(m) => '{ ColumnPath.Step.Field(${ Expr(name) }, ${ Expr(index) }, $m) }
              case None    =>
                report.errorAndAbort(s"ColumnOptics.path: no Mirror.ProductOf[${rt.show}]")
    }
    val names = Expr(chain.collect {
      case (r, n) if r.tpe.widen.dealias.typeSymbol != optionSym => n
    })
    val partial = chain.exists((r, _) => r.tpe.widen.dealias.typeSymbol == optionSym)
    val stepList = Expr.ofList(steps)

    body.tpe.widen.asType match
      case '[a] =>
        if partial then '{ new ColumnPath.Partial[S, a]($names, $stepList) }
        else '{ new ColumnPath.Total[S, a]($names, $stepList) }
