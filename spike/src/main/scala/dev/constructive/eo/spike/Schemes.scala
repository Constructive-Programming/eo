package dev.constructive.eo.spike

import cats.{Monad, Traverse}
import cats.syntax.all.*
import dev.constructive.eo.data.PSVec

/** Stage 1 (U1a/U1b): stack-safe encoding-A schemes via an explicit `ArrayDeque` post-order machine
  * — the plan's "one synchronous engine", mirroring `Plated.transformMachine` (core
  * `Plated.scala:128-147`): every node is *pushed as a frame*, never recursed on the JVM call
  * stack, so descent is heap-bounded for any coalgebra. No fixpoint axis ⇒ no `Eval` needed (the
  * effectful variant — already shown in `EffectfulDemo` — threads its monad via a traverse;
  * combining that with this heap machine for arbitrary `M` is the genuinely harder follow-on).
  *
  * This is the thing that justifies a scheme over a hand-written one-off: the naive recursion
  * ([[hyloNaive]]) `StackOverflow`s on a deep spine; [[hylo]] does not.
  */
object Schemes:

  /** Encoding-A coalgebra: a seed yields its child seeds plus a combiner from the folded child
    * results. A leaf is `(PSVec.empty, _ => value)`. Node payload is available to the combiner via
    * closure capture.
    */
  type CoalgA[Seed, A] = Seed => (PSVec[Seed], PSVec[A] => A)

  /** Stack-safe fused hylo: `Seed => A`, building no persistent intermediate tree. */
  def hylo[Seed, A](coalg: CoalgA[Seed, A])(seed: Seed): A =
    final class Frame(
        val kids: PSVec[Seed],
        val out: Array[AnyRef],
        val combine: PSVec[A] => A,
        var i: Int,
    )
    val stack = new java.util.ArrayDeque[Frame]()
    var ret: AnyRef = null.asInstanceOf[AnyRef]
    def enter(s: Seed): Unit =
      val (kids, combine) = coalg(s)
      if kids.isEmpty then ret = combine(PSVec.empty[A]).asInstanceOf[AnyRef]
      else stack.push(new Frame(kids, new Array[AnyRef](kids.length), combine, 0))
    enter(seed)
    while !stack.isEmpty do
      val fr = stack.peek()
      if fr.i > 0 then fr.out(fr.i - 1) = ret
      if fr.i < fr.kids.length then
        val child = fr.kids(fr.i)
        fr.i += 1
        enter(child)
      else
        ret = fr.combine(PSVec.unsafeWrap[A](fr.out)).asInstanceOf[AnyRef]
        val _ = stack.pop()
    ret.asInstanceOf[A]

  /** Naive recursive hylo — the hand-written baseline; overflows on a deep spine. */
  def hyloNaive[Seed, A](coalg: CoalgA[Seed, A])(seed: Seed): A =
    val (kids, combine) = coalg(seed)
    if kids.isEmpty then combine(PSVec.empty[A])
    else
      val out = new Array[AnyRef](kids.length)
      var i = 0
      while i < kids.length do
        out(i) = hyloNaive(coalg)(kids(i)).asInstanceOf[AnyRef]
        i += 1
      combine(PSVec.unsafeWrap[A](out))

  // ---- stack-safe AND effectful (the full arbo shape) ----
  //
  // The effect is a fail-able `Either[E, *]` — arbo's `M` shape (an I/O lookup that can
  // fail). The heap machine gives depth-safety; threading `Either` is a `Left`-abort of the
  // loop. Stack-safe + effectful together. (For a general stack-safe monad the same machine
  // threads via `Eval` exactly as `Plated.rewrite` does — this concretises it for `Either`.)

  /** Effectful, fail-able coalgebra: a seed yields `Left(e)` (effect failure) or
    * `Right((childSeeds, combine))`.
    */
  type CoalgEither[E, Seed, A] = Seed => Either[E, (PSVec[Seed], PSVec[A] => A)]

  /** Stack-safe effectful fused hylo: `Seed => Either[E, A]`. Heap stack; short-circuits on the
    * first `Left`. Completes deep unfolds that [[hyloEitherNaive]] overflows on.
    */
  def hyloEither[E, Seed, A](coalg: CoalgEither[E, Seed, A])(seed: Seed): Either[E, A] =
    final class Frame(
        val kids: PSVec[Seed],
        val out: Array[AnyRef],
        val combine: PSVec[A] => A,
        var i: Int,
    )
    val stack = new java.util.ArrayDeque[Frame]()
    var ret: AnyRef = null.asInstanceOf[AnyRef]
    // Some(e) aborts the whole machine with that failure.
    def enter(s: Seed): Option[E] =
      coalg(s) match
        case Left(e)                => Some(e)
        case Right((kids, combine)) =>
          if kids.isEmpty then
            ret = combine(PSVec.empty[A]).asInstanceOf[AnyRef]
            None
          else
            stack.push(new Frame(kids, new Array[AnyRef](kids.length), combine, 0))
            None
    enter(seed) match
      case Some(e) => Left(e)
      case None    =>
        var aborted: Option[E] = None
        while aborted.isEmpty && !stack.isEmpty do
          val fr = stack.peek()
          if fr.i > 0 then fr.out(fr.i - 1) = ret
          if fr.i < fr.kids.length then
            val child = fr.kids(fr.i)
            fr.i += 1
            aborted = enter(child)
          else
            ret = fr.combine(PSVec.unsafeWrap[A](fr.out)).asInstanceOf[AnyRef]
            val _ = stack.pop()
        aborted match
          case Some(e) => Left(e)
          case None    => Right(ret.asInstanceOf[A])

  // ---- stack-safe + effectful for ANY lawful monad (general case) ----
  //
  // `cats.Monad` *requires* a stack-safe `tailRecM` (monad law), so driving the heap
  // machine's descent through `tailRecM` makes the recursion stack-safe for ANY lawful
  // `M` — the JVM call stack is never used for descent; the work-stack lives on the heap
  // and the effect threads through `tailRecM`. This is a *stronger* guarantee than droste's
  // `hyloM`, which is stack-safe only when `M` itself is. Each `tailRecM` step performs one
  // effectful `coalg` expansion; pure post-order bubbling between steps is an iterative loop.

  /** Effectful coalgebra for an arbitrary monad: a seed yields `M[(childSeeds, combine)]`. */
  type CoalgM[M[_], Seed, A] = Seed => M[(PSVec[Seed], PSVec[A] => A)]

  /** Stack-safe fused hylo for any lawful `Monad[M]`: `Seed => M[A]`. */
  def hyloM[M[_], Seed, A](coalg: CoalgM[M, Seed, A])(seed: Seed)(using M: Monad[M]): M[A] =
    final class Frame(
        val kids: PSVec[Seed],
        val out: Array[AnyRef],
        val combine: PSVec[A] => A,
        var i: Int,
    )
    val stack = new java.util.ArrayDeque[Frame]()
    var ret: AnyRef = null.asInstanceOf[AnyRef]
    // Pure post-order bubble: store the just-computed `ret` into the parent, then either
    // pick the next child to enter (`Left`) or, when the work-stack drains, finish (`Right`).
    def advance(): Either[Seed, A] =
      var result: Either[Seed, A] | Null = null
      while result == null do
        if stack.isEmpty then result = Right(ret.asInstanceOf[A])
        else
          val fr = stack.peek()
          if fr.i > 0 then fr.out(fr.i - 1) = ret
          if fr.i < fr.kids.length then
            val child = fr.kids(fr.i)
            fr.i += 1
            result = Left(child)
          else
            ret = fr.combine(PSVec.unsafeWrap[A](fr.out)).asInstanceOf[AnyRef]
            val _ = stack.pop()
      result.asInstanceOf[Either[Seed, A]]
    M.tailRecM(seed) { s =>
      M.map(coalg(s)) {
        case (kids, combine) =>
          if kids.isEmpty then ret = combine(PSVec.empty[A]).asInstanceOf[AnyRef]
          else stack.push(new Frame(kids, new Array[AnyRef](kids.length), combine, 0))
          advance()
      }
    }

  // ---- Encoding B: typed descriptor (pattern functor) ----
  //
  // B reaches the engine by *desugaring to encoding A* through a `Traverse[F]`: children are
  // `F.toList`, and the typed algebra's input `F[A]` is rebuilt by positionally re-filling
  // the original `F[Seed]` shape with the folded results (lawful `Traverse` ⇒ map order =
  // toList order). So B **inherits A's stack-safety and monad-generality for free** and adds
  // exactly one thing: a *typed, named* algebra `F[A] => A` — at the cost of defining the
  // pattern functor `F` and its `Traverse`. This makes the A-vs-B delta concrete: B = A + a
  // pattern functor + a `Traverse` instance, buying a typed fold.

  /** Stack-safe, effectful hylo over a typed descriptor `F` (encoding B). */
  def hyloM_B[M[_]: Monad, F[_]: Traverse, Seed, A](
      coalg: Seed => M[F[Seed]],
      alg: F[A] => A,
  )(seed: Seed): M[A] =
    val F = Traverse[F]
    hyloM[M, Seed, A] { s =>
      coalg(s).map { fSeed =>
        val kids = PSVec.fromIterable(F.toList(fSeed))
        val combine: PSVec[A] => A = results =>
          var i = 0
          val fa = F.map(fSeed) { _ =>
            val r = results(i)
            i += 1
            r
          }
          alg(fa)
        (kids, combine)
      }
    }(seed)

  /** Naive effectful recursion — overflows on a deep spine (the hand-written baseline). */
  def hyloEitherNaive[E, Seed, A](coalg: CoalgEither[E, Seed, A])(seed: Seed): Either[E, A] =
    coalg(seed) match
      case Left(e)                => Left(e)
      case Right((kids, combine)) =>
        if kids.isEmpty then Right(combine(PSVec.empty[A]))
        else
          val out = new Array[AnyRef](kids.length)
          def loop(i: Int): Either[E, Unit] =
            if i >= kids.length then Right(())
            else
              hyloEitherNaive(coalg)(kids(i)) match
                case Left(e)  => Left(e)
                case Right(a) =>
                  out(i) = a.asInstanceOf[AnyRef]
                  loop(i + 1)
          loop(0) match
            case Left(e)  => Left(e)
            case Right(_) => Right(combine(PSVec.unsafeWrap[A](out)))
