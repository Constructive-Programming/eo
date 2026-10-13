package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec

import scalus.uplc.{DefaultFun, DefaultUni, Term}

/** S ⇒ A: reads a decoded UPLC program back as a typed, Scala-like [[Expr]].
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
    TypedProgram(source, new Typing(source).expr(source.root, Vector.empty))

/** What a `let` binder is statically known to hold. */
private enum Known:
  case Unknown

  /** A lambda chain (UPLC node `origin`): saturated calls go straight to its body method. */
  case Lambda(origin: Int, arity: Int)

  /** A builtin, already forced `forces` times: calls through the variable are builtin calls. */
  case Builtin(fun: DefaultFun, forces: Int)

/** What the typer knows about a binder: its static type and what value it holds. */
final private case class Binder(ty: Ty, known: Known)

/** One link of an application spine, innermost first. */
private enum Link:
  case Arg(term: Term)
  case Frc

final private class Typing(source: SourceProgram):

  private val Param = Binder(None, Known.Unknown)

  /** Builtins whose arguments may be arbitrary values and which return one of them unchanged. The
    * runtime implements these natively instead of delegating to Scalus.
    */
  private val selectors: Map[DefaultFun, Int] = Map(
    DefaultFun.IfThenElse -> 2,
    DefaultFun.ChooseList -> 2,
    DefaultFun.ChooseUnit -> 1,
    DefaultFun.ChooseData -> 5,
  )

  private def lookup(scope: Vector[Binder], index: Int): Option[Binder] =
    if index >= 1 && index <= scope.size then Some(scope(scope.size - index)) else None

  def expr(term: Term, scope: Vector[Binder]): Expr = term match
    case Term.Var(name, _) =>
      lookup(scope, name.index).fold(Expr.Unbound(name.index))(b => Expr.Ref(name.index, b.ty))
    case lam: Term.LamAbs          => lambda(lam, scope)
    case Term.Delay(body, _)       => Expr.Delayed(source.id(term), expr(body, scope))
    case Term.Const(value, _)      => Expr.Lit(source.id(term), value)
    case Term.Builtin(fun, _)      => Expr.BuiltinRef(fun)
    case Term.Error(_)             => Expr.Fail
    case Term.Constr(tag, args, _) => Expr.Construct(tag.value, args.map(expr(_, scope)))

    // case (constr k a1 … an) [… f …]: the multi-argument call compilers emit (one CEK step
    // applies every field). The fields are evaluated first, then the selected branch, then the
    // branch is applied to them.
    case Term.Case(Term.Constr(tag, args, _), branches, _)
        if tag.value >= 0 && tag.value < branches.size =>
      caseApply(branches(tag.value.toInt), args, scope)

    case Term.Case(scrut, branches, _) =>
      val s = expr(scrut, scope)
      val bs = branches.map(expr(_, scope))
      // Only a case on a known scalar passes no arguments to its branch, so only then is the
      // branch's own type the result type.
      val scalar = s.ty.exists(t => t == DefaultUni.Bool || t == DefaultUni.Integer)
      Expr.Match(s, bs, if Semantics.caseOnBuiltins && scalar then Ty.join(bs) else None)
    case _: Term.Apply | _: Term.Force =>
      val (head, links) = spine(term, Nil)
      application(head, links, scope)

  /** Peels `Apply`/`Force` off a spine. Returns the head and the links in evaluation order. */
  @tailrec
  private def spine(term: Term, acc: List[Link]): (Term, List[Link]) = term match
    case Term.Apply(fn, arg, _) => spine(fn, Link.Arg(arg) :: acc)
    case Term.Force(body, _)    => spine(body, Link.Frc :: acc)
    case head                   => (head, acc)

  /** The parameters of the lambda chain rooted at `term` (capped at `MaxChain`) and its body. */
  private def chain(term: Term): (List[String], Term) =
    @tailrec
    def go(t: Term, names: List[String]): (List[String], Term) = t match
      case Term.LamAbs(name, body, _) if names.size < Typer.MaxChain => go(body, name :: names)
      case body                                                      => (names.reverse, body)
    go(term, Nil)

  private def lambda(lam: Term.LamAbs, scope: Vector[Binder]): Expr =
    val (params, body) = chain(lam)
    Expr.Lambda(source.id(lam), params, expr(body, scope ++ Vector.fill(params.size)(Param)))

  /** What remains of a lambda chain after `skip` of its parameters were let-bound: the inner chain
    * (a closure rooted at its own node) or, if all were bound, the body.
    */
  @tailrec
  private def residual(term: Term, skip: Int, scope: Vector[Binder]): Expr = term match
    case Term.LamAbs(_, body, _) if skip > 0 => residual(body, skip - 1, scope)
    case other                               => expr(other, scope)

  private def application(head: Term, links: List[Link], scope: Vector[Binder]): Expr =
    head match
      case Term.Builtin(fun, _) => builtinSpine(fun, links, scope)

      case lam: Term.LamAbs =>
        val args = links.takeWhile(_.isInstanceOf[Link.Arg]).collect { case Link.Arg(a) => a }
        val (params, _) = chain(lam)
        val bound = math.min(args.size, params.size)
        if bound == 0 then rest(expr(lam, scope), links, scope)
        else
          // (λx1…λxk. body) a1 … am  ⇒  let x1 = a1 … xj = aj in <the rest of the chain>
          val rhss = args.take(bound).map(expr(_, scope))
          val binders = rhss.map(r => Binder(r.ty, known(r)))
          val inner = residual(lam, bound, scope ++ binders)
          val bindings = params.take(bound).zip(rhss).map(Binding.apply)
          rest(Expr.Let(source.id(lam), bindings, inner, inner.ty), links.drop(bound), scope)

      // force (delay t)  ⇒  t
      case Term.Delay(body, _) if links.headOption.contains(Link.Frc) =>
        val (h, ls) = spine(body, Nil)
        application(h, ls ++ links.tail, scope)

      case Term.Var(name, _) =>
        lookup(scope, name.index).map(_.known) match
          case Some(Known.Lambda(origin, arity)) if leadingArgs(links) >= arity =>
            val args = links.take(arity).collect { case Link.Arg(a) => expr(a, scope) }
            val call = Expr.CallKnown(Expr.Ref(name.index, None), origin, args)
            rest(call, links.drop(arity), scope)
          // Builtin values are pure: re-deriving the forced builtin at the call is unobservable.
          case Some(Known.Builtin(fun, forces)) if links.nonEmpty =>
            builtinSpine(fun, List.fill(forces)(Link.Frc) ++ links, scope)
          case _ => rest(expr(head, scope), links, scope)

      case other => rest(expr(other, scope), links, scope)

  private def leadingArgs(links: List[Link]): Int = links.takeWhile(_.isInstanceOf[Link.Arg]).size

  /** A branch applied to fields that were evaluated before it. When nothing observable can run
    * between the fields and the application — the branch is a lambda chain or a known function
    * taking at least that many arguments — this IS an ordinary application (a `let` or a direct
    * call). Otherwise the fields-first order is kept explicitly.
    */
  private def caseApply(branch: Term, fields: List[Term], scope: Vector[Binder]): Expr =
    val links = fields.map(Link.Arg(_))
    branch match
      case _ if fields.isEmpty                                   => expr(branch, scope)
      case lam: Term.LamAbs if chain(lam)._1.size >= fields.size => application(lam, links, scope)
      case Term.Var(name, _) if lookup(scope, name.index).map(_.known).exists {
            case Known.Lambda(_, arity) => arity >= fields.size
            case _                      => false
          } =>
        application(branch, links, scope)
      case _ => Expr.Spread(fields.map(expr(_, scope)), expr(branch, scope))

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
      val argTerms = prefix.collect { case Link.Arg(a) => a }
      val after = links.drop(steps)
      val delayedBranches = argTerms.drop(1).collect { case Term.Delay(b, _) => b }
      selectors.get(fun) match
        // force (ifThenElse c (delay a) (delay b))  ⇒  if c then a else b
        case Some(n) if after.headOption.contains(Link.Frc) && delayedBranches.size == n =>
          val scrut = expr(argTerms.head, scope)
          val branches = delayedBranches.map(expr(_, scope))
          rest(Expr.Choose(fun, scrut, branches, Ty.join(branches)), after.tail, scope)
        case _ =>
          val args = argTerms.map(expr(_, scope))
          val ty =
            if fun == DefaultFun.IfThenElse then Ty.join(args.drop(1))
            else Semantics.signature(fun)._2
          rest(Expr.Call(fun, args, ty), after, scope)
