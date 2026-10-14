package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec

import scalus.uplc.{DefaultFun, DefaultUni, Term}

/** S ⇒ A: reads a decoded UPLC program back as a typed, Scala-like [[Expr]], walking it by node id.
  *
  * Every rewrite here is an exact equivalence under the CEK machine's evaluation order — they only
  * remove steps that cannot be observed (building a closure that is immediately applied, delaying a
  * branch that is immediately forced, partially applying a builtin that is immediately saturated).
  * Effects keep their order; anything the typer does not recognise stays a dynamic `Apply` /
  * `Force` and is executed exactly as the machine would.
  */
object Typer:

  /** Longest lambda chain compiled into one JVM method (the JVM caps a method at 255 parameter
    * slots, one of which holds the captured environment).
    */
  val MaxChain = 200

  def typeProgram(source: SourceProgram): TypedProgram =
    TypedProgram(source, new Typing(source).expr(0, Vector.empty))

/** What a binder is statically known to hold. */
private enum Known:
  case Unknown

  /** A lambda chain (UPLC node `origin`): saturated calls go straight to its body method. */
  case Lambda(origin: Int, arity: Int)

  /** A builtin, already forced `forces` times: calls through the variable are builtin calls. */
  case Builtin(fun: DefaultFun, forces: Int)

/** What the typer knows about a binder: its static type and what value it holds. */
final private case class Binder(ty: Ty, known: Known)

/** One link of an application spine, innermost first: an argument (its node id) or a force. */
private enum Link:
  case Arg(node: Int)
  case Frc

final private class Typing(source: SourceProgram):

  import source.{children, nodes}

  private val Param = Binder(None, Known.Unknown)

  /** Builtins that return one of their arguments after the first unchanged: over delayed branches,
    * only the branch they pick runs.
    */
  private val selectors =
    Set(DefaultFun.IfThenElse, DefaultFun.ChooseList, DefaultFun.ChooseUnit, DefaultFun.ChooseData)

  private def lookup(scope: Vector[Binder], index: Int): Option[Binder] =
    if index >= 1 && index <= scope.size then Some(scope(scope.size - index)) else None

  def expr(n: Int, scope: Vector[Binder]): Expr = nodes(n) match
    case Term.Var(name, _) =>
      lookup(scope, name.index).fold(Expr.Unbound(name.index))(b => Expr.Ref(name.index, b.ty))
    case _: Term.LamAbs         => lambda(n, scope, recursive = false)
    case _: Term.Delay          => Expr.Delayed(n, expr(n + 1, scope))
    case Term.Const(value, _)   => Expr.Lit(n, value)
    case Term.Builtin(fun, _)   => Expr.BuiltinRef(fun)
    case Term.Error(_)          => Expr.Fail
    case Term.Constr(tag, _, _) => Expr.Construct(tag.value, children(n).map(expr(_, scope)))
    case Term.Case(scrut, _, _) =>
      val s :: branches = children(n): @unchecked
      scrut match
        // case (constr k a1 … an) [… f …]: the multi-argument call compilers emit. The fields are
        // evaluated first, then the branch, then the branch is applied to them — an ordinary
        // application when nothing observable can run in between (see `applies`).
        case Term.Constr(tag, fields, _)
            if tag.value >= 0 && tag.value < branches.size &&
              applies(branches(tag.value.toInt), fields.size, scope) =>
          application(branches(tag.value.toInt), children(s).map(Link.Arg(_)), scope)
        case _ =>
          val e = expr(s, scope)
          val bs = branches.map(expr(_, scope))
          // Only a case on a known scalar passes no arguments to its branch, so only then is the
          // branch's own type the result type.
          val scalar = e.ty.exists(t => t == DefaultUni.Bool || t == DefaultUni.Integer)
          Expr.Match(e, bs, if Semantics.caseOnBuiltins && scalar then Ty.join(bs) else None)
    case _ =>
      val (head, links) = spine(n, Nil)
      application(head, links, scope)

  /** Peels `Apply`/`Force` off a spine. Returns the head and the links in evaluation order. */
  @tailrec
  private def spine(n: Int, acc: List[Link]): (Int, List[Link]) = nodes(n) match
    case _: Term.Apply => spine(n + 1, Link.Arg(children(n)(1)) :: acc)
    case _: Term.Force => spine(n + 1, Link.Frc :: acc)
    case _             => (n, acc)

  /** The parameters of the lambda chain at `n` (capped at `MaxChain`) and its body. */
  private def chain(n: Int): (List[String], Int) =
    @tailrec
    def go(m: Int, names: List[String]): (List[String], Int) = nodes(m) match
      case Term.LamAbs(name, _, _) if names.size < Typer.MaxChain => go(m + 1, name :: names)
      case _                                                      => (names.reverse, m)
    go(n, Nil)

  /** A lambda chain. When `recursive`, its first parameter is known to hold the chain itself. */
  private def lambda(n: Int, scope: Vector[Binder], recursive: Boolean): Expr =
    val (params, body) = chain(n)
    val first = if recursive then Binder(None, Known.Lambda(n, params.size)) else Param
    Expr.Lambda(n, params, expr(body, scope ++ (first +: Vector.fill(params.size - 1)(Param))))

  /** Every occurrence of variable `i` in `t` applies it to itself — `[i i …]`, or
    * `case (constr 0 [i …]) [i]` — so the closure it names never escapes anywhere else.
    */
  private def selfApplied(t: Term, i: Int): Boolean = t match
    case Term.Apply(Term.Var(f, _), Term.Var(a, _), _) if f.index == i && a.index == i => true
    case Term.Case(Term.Constr(tag, Term.Var(a, _) :: args, _), List(Term.Var(f, _)), _)
        if tag.value == 0 && a.index == i && f.index == i =>
      args.forall(selfApplied(_, i))
    case Term.Var(v, _)          => v.index != i
    case Term.LamAbs(_, b, _)    => selfApplied(b, i + 1)
    case Term.Apply(f, a, _)     => selfApplied(f, i) && selfApplied(a, i)
    case Term.Force(b, _)        => selfApplied(b, i)
    case Term.Delay(b, _)        => selfApplied(b, i)
    case Term.Constr(_, args, _) => args.forall(selfApplied(_, i))
    case Term.Case(s, cases, _)  => selfApplied(s, i) && cases.forall(selfApplied(_, i))
    case _                       => true

  private def application(head: Int, links: List[Link], scope: Vector[Binder]): Expr =
    nodes(head) match
      case Term.Builtin(fun, _) => builtinSpine(fun, links, scope)

      case _: Term.LamAbs =>
        val args = leading(links)
        val (params, _) = chain(head)
        val bound = math.min(args.size, params.size)
        if bound == 0 then rest(expr(head, scope), links, scope)
        else
          // (λx1…λxk. body) a1 … am  ⇒  let x1 = a1 … xj = aj in <the rest of the chain>
          val rhss = args.take(bound).zipWithIndex.map { (a, j) =>
            // let x = λself … in … [x x] …: a recursive function, tied by self-application. When
            // both x and self are only ever applied to themselves, self always holds the chain.
            val recursive = nodes(a).isInstanceOf[Term.LamAbs] &&
              selfApplied(nodes(head + bound), bound - j) && selfApplied(nodes(a + 1), 1)
            if recursive then lambda(a, scope, recursive) else expr(a, scope)
          }
          val inner = expr(head + bound, scope ++ rhss.map(r => Binder(r.ty, known(r))))
          val bindings = params.take(bound).zip(rhss).map(Binding.apply)
          rest(Expr.Let(head, bindings, inner, inner.ty), links.drop(bound), scope)

      // force (delay t)  ⇒  t
      case _: Term.Delay if links.headOption.contains(Link.Frc) =>
        val (h, ls) = spine(head + 1, Nil)
        application(h, ls ++ links.tail, scope)

      case Term.Var(name, _) =>
        lookup(scope, name.index).map(_.known) match
          case Some(Known.Lambda(origin, arity)) if leading(links).size >= arity =>
            val args = links.take(arity).collect { case Link.Arg(a) => expr(a, scope) }
            val call = Expr.CallKnown(Expr.Ref(name.index, None), origin, args)
            rest(call, links.drop(arity), scope)
          // Builtin values are pure: re-deriving the forced builtin at the call is unobservable.
          case Some(Known.Builtin(fun, forces)) if links.nonEmpty =>
            builtinSpine(fun, List.fill(forces)(Link.Frc) ++ links, scope)
          case _ => rest(expr(head, scope), links, scope)

      case _ => rest(expr(head, scope), links, scope)

  /** The arguments at the front of a spine, before any force. */
  private def leading(links: List[Link]): List[Int] =
    links.takeWhile(_.isInstanceOf[Link.Arg]).collect { case Link.Arg(a) => a }

  /** Whether `branch` applied to `fields` arguments evaluated before it is an ordinary application:
    * it is a lambda chain, or a known function, taking at least that many.
    */
  private def applies(branch: Int, fields: Int, scope: Vector[Binder]): Boolean =
    fields == 0 || (nodes(branch) match
      case _: Term.LamAbs    => chain(branch)._1.size >= fields
      case Term.Var(name, _) =>
        lookup(scope, name.index).exists(_.known match
          case Known.Lambda(_, arity) => arity >= fields
          case _                      => false)
      case _ => false)

  private def known(rhs: Expr): Known =
    @tailrec
    def forced(e: Expr, n: Int): Known = e match
      case Expr.Force(inner)                                              => forced(inner, n + 1)
      case Expr.BuiltinRef(fun) if Semantics.scheme(fun).numTypeVars >= n => Known.Builtin(fun, n)
      case _                                                              => Known.Unknown
    rhs match
      case Expr.Lambda(origin, params, _) => Known.Lambda(origin, params.size)
      case other                          => forced(other, 0)

  /** Applies the remaining links dynamically, in order. */
  private def rest(acc: Expr, links: List[Link], scope: Vector[Binder]): Expr =
    links.foldLeft(acc) {
      case (f, Link.Arg(a)) => Expr.Apply(f, expr(a, scope))
      case (f, Link.Frc)    => Expr.Force(f)
    }

  private def builtinSpine(fun: DefaultFun, links: List[Link], scope: Vector[Binder]): Expr =
    val scheme = Semantics.scheme(fun)
    val forces = scheme.numTypeVars
    val steps = forces + scheme.arity
    val prefix = links.take(steps)
    // A builtin runs when its last force or argument arrives; with none at all it never would.
    val saturated = steps > 0 && prefix.size == steps && prefix.zipWithIndex.forall {
      case (Link.Frc, i)    => i < forces
      case (Link.Arg(_), i) => i >= forces
    }
    if !saturated then rest(Expr.BuiltinRef(fun), links, scope)
    else
      val args = prefix.collect { case Link.Arg(a) => a }
      val after = links.drop(steps)
      val delayed = args.drop(1).filter(nodes(_).isInstanceOf[Term.Delay])
      // force (ifThenElse c (delay a) (delay b))  ⇒  if c then a else b
      if selectors(fun) && after.headOption.contains(Link.Frc) && delayed.size == args.size - 1 then
        val branches = delayed.map(d => expr(d + 1, scope))
        val choose = Expr.Choose(fun, expr(args.head, scope), branches, Ty.join(branches))
        rest(choose, after.tail, scope)
      else
        val es = args.map(expr(_, scope))
        val ty =
          if fun == DefaultFun.IfThenElse then Ty.join(es.drop(1))
          else Semantics.result(fun, es.map(_.ty))
        rest(Expr.Call(fun, es, ty), after, scope)
