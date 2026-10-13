package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec

import scalus.uplc.DefaultFun

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
    TypedProgram(source, expr(source.root, Vector.empty))

  /** What a `let` binder is statically known to hold. */
  private enum Known:
    case Unknown

    /** A lambda chain (UPLC node `origin`): saturated calls go straight to its body method. */
    case Lambda(origin: Int, arity: Int)

    /** A builtin, already forced `forces` times: calls through the variable are builtin calls. */
    case Builtin(fun: DefaultFun, forces: Int)

  /** What the typer knows about a binder: its static type and what value it holds. */
  final private case class Binder(ty: Ty, known: Known)

  private val Param = Binder(Ty.Dyn, Known.Unknown)

  /** One link of an application spine, innermost first. */
  private enum Link:
    case Arg(node: Node)
    case Frc

  private def lookup(scope: Vector[Binder], index: Int): Option[Binder] =
    if index >= 1 && index <= scope.size then Some(scope(scope.size - index)) else None

  private def expr(node: Node, scope: Vector[Binder]): Expr = node match
    case Node.Var(_, i) =>
      lookup(scope, i).fold(Expr.Unbound(i))(b => Expr.Ref(i, b.ty))
    case Node.Lam(id, _, _)        => lambda(id, node, scope)
    case Node.Delay(id, body)      => Expr.Delayed(id, expr(body, scope))
    case Node.Const(_, ord, value) => Expr.Lit(ord, value)
    case Node.Builtin(_, fun)      => Expr.BuiltinRef(fun)
    case Node.Error(_)             => Expr.Fail
    case Node.Constr(_, tag, args) => Expr.Construct(tag, args.map(expr(_, scope)))

    // case (constr k a1 … an) [… f …]: the multi-argument call compilers emit (one CEK step
    // applies every field). The fields are evaluated first, then the selected branch, then the
    // branch is applied to them.
    case Node.Case(_, Node.Constr(_, tag, args), branches) if tag >= 0 && tag < branches.size =>
      caseApply(branches(tag.toInt), args, scope)

    case Node.Case(_, scrut, branches) =>
      val s = expr(scrut, scope)
      val bs = branches.map(expr(_, scope))
      // Only a case on a known scalar passes no arguments to its branch, so only then is the
      // branch's own type the result type.
      val ty =
        if Semantics.caseOnBuiltins && (s.ty == Ty.Bool || s.ty == Ty.Integer) then
          bs.map(_.ty).foldLeft(Ty.Bottom)(_.join(_))
        else Ty.Dyn
      Expr.Match(s, bs, ty)
    case Node.Apply(_, _, _) | Node.Force(_, _) =>
      val (head, links) = spine(node, Nil)
      application(head, links, scope)

  /** Peels `Apply`/`Force` off a spine. Returns the head and the links in evaluation order. */
  @tailrec
  private def spine(node: Node, acc: List[Link]): (Node, List[Link]) = node match
    case Node.Apply(_, fn, arg) => spine(fn, Link.Arg(arg) :: acc)
    case Node.Force(_, body)    => spine(body, Link.Frc :: acc)
    case head                   => (head, acc)

  /** The parameters of the lambda chain rooted at `node` (capped at [[MaxChain]]) and its body. */
  private def chain(node: Node): (List[String], Node) =
    @tailrec
    def go(n: Node, names: List[String]): (List[String], Node) = n match
      case Node.Lam(_, name, body) if names.size < MaxChain => go(body, name :: names)
      case body                                             => (names.reverse, body)
    go(node, Nil)

  private def lambda(origin: Int, node: Node, scope: Vector[Binder]): Expr =
    val (params, body) = chain(node)
    Expr.Lambda(origin, params, expr(body, scope ++ Vector.fill(params.size)(Param)))

  /** What remains of a lambda chain after `skip` of its parameters were let-bound: the inner chain
    * (a closure rooted at its own node, `origin + skip`) or, if all were bound, the body.
    */
  @tailrec
  private def residual(node: Node, skip: Int, scope: Vector[Binder]): Expr = node match
    case Node.Lam(_, _, body) if skip > 0 => residual(body, skip - 1, scope)
    case other                            => expr(other, scope)

  private def application(head: Node, links: List[Link], scope: Vector[Binder]): Expr =
    head match
      case Node.Builtin(_, fun) => builtinSpine(fun, links, scope)

      case lam: Node.Lam =>
        val args = links.takeWhile(_.isInstanceOf[Link.Arg]).collect { case Link.Arg(a) => a }
        val (params, _) = chain(lam)
        val bound = math.min(args.size, params.size)
        if bound == 0 then rest(expr(lam, scope), links, scope)
        else
          // (λx1…λxk. body) a1 … am  ⇒  let x1 = a1 … xj = aj in <the rest of the chain>
          val rhss = args.take(bound).map(expr(_, scope))
          val binders = rhss.map(r => Binder(r.ty, known(r)))
          val inner = residual(lam, bound, scope ++ binders)
          val let = Expr.Let(
            lam.id,
            params.take(bound).zip(rhss).map(Binding.apply),
            inner,
            inner.ty,
          )
          rest(let, links.drop(bound), scope)

      // force (delay t)  ⇒  t
      case Node.Delay(_, body) if links.headOption.contains(Link.Frc) =>
        val (h, ls) = spine(body, Nil)
        application(h, ls ++ links.tail, scope)

      case Node.Var(_, i) =>
        lookup(scope, i).map(_.known) match
          case Some(Known.Lambda(origin, arity)) if leadingArgs(links) >= arity =>
            val args = links.take(arity).collect { case Link.Arg(a) => expr(a, scope) }
            val call = Expr.CallKnown(Expr.Ref(i, Ty.Dyn), origin, args)
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
  private def caseApply(branch: Node, fields: List[Node], scope: Vector[Binder]): Expr =
    val links = fields.map(Link.Arg(_))
    branch match
      case _ if fields.isEmpty                                => expr(branch, scope)
      case lam: Node.Lam if chain(lam)._1.size >= fields.size => application(lam, links, scope)
      case Node.Var(_, i) if lookup(scope, i).map(_.known).exists {
            case Known.Lambda(_, arity) => arity >= fields.size
            case _                      => false
          } =>
        application(branch, links, scope)
      case _ => Expr.Spread(fields.map(expr(_, scope)), expr(branch, scope))

  private def known(rhs: Expr): Known =
    @tailrec
    def forced(e: Expr, n: Int): Known = e match
      case Expr.Force(inner) => forced(inner, n + 1)
      case Expr.BuiltinRef(fun)
          if Semantics.signature(fun).steps.takeWhile(_ == Step.Force).size >= n =>
        Known.Builtin(fun, n)
      case _ => Known.Unknown
    rhs match
      case Expr.Lambda(origin, params, _) => Known.Lambda(origin, params.size)
      case other                          => forced(other, 0)

  /** Applies the remaining links dynamically, in order. */
  private def rest(acc: Expr, links: List[Link], scope: Vector[Binder]): Expr =
    links.foldLeft(acc) {
      case (f, Link.Arg(a)) => Expr.Apply(f, expr(a, scope))
      case (f, Link.Frc)    => Expr.Force(f)
    }

  /** Builtins whose arguments may be arbitrary values and which return one of them unchanged. The
    * runtime implements these natively instead of delegating to Scalus.
    */
  private val selectors: Map[DefaultFun, Int] = Map(
    DefaultFun.IfThenElse -> 2,
    DefaultFun.ChooseList -> 2,
    DefaultFun.ChooseUnit -> 1,
    DefaultFun.ChooseData -> 5,
  )

  private def builtinSpine(fun: DefaultFun, links: List[Link], scope: Vector[Binder]): Expr =
    val sig = Semantics.signature(fun)
    val prefix = links.take(sig.steps.size)
    // A builtin runs when its last force or argument arrives; with none at all it never would.
    val matches =
      sig.steps.nonEmpty && prefix.size == sig.steps.size && prefix.zip(sig.steps).forall {
        case (Link.Frc, Step.Force)  => true
        case (Link.Arg(_), Step.Arg) => true
        case _                       => false
      }
    if !matches then rest(Expr.BuiltinRef(fun), links, scope)
    else
      val argNodes = prefix.collect { case Link.Arg(a) => a }
      val after = links.drop(sig.steps.size)
      val delayedBranches = argNodes.tail.collect { case Node.Delay(_, b) => b }
      selectors.get(fun) match
        // force (ifThenElse c (delay a) (delay b))  ⇒  if c then a else b
        case Some(n) if after.headOption.contains(Link.Frc) && delayedBranches.size == n =>
          val scrut = expr(argNodes.head, scope)
          val branches = delayedBranches.map(expr(_, scope))
          val ty = branches.map(_.ty).foldLeft(Ty.Bottom)(_.join(_))
          rest(Expr.Choose(fun, scrut, branches, ty), after.tail, scope)
        case _ =>
          val args = argNodes.map(expr(_, scope))
          val ty =
            if fun == DefaultFun.IfThenElse then args.tail.map(_.ty).foldLeft(Ty.Bottom)(_.join(_))
            else sig.result
          rest(Expr.Call(fun, args, ty), after, scope)
