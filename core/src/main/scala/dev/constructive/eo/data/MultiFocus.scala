package dev.constructive.eo
package data

import cats.{Alternative, Applicative, Foldable, Functor, Monoid, MonoidK, Traverse}

import optics.Optic

/** Unified pair carrier for the `AlgLens` and `Kaleidoscope` optic families —
  * `MultiFocus[F][X, A] = (X, F[A])`.
  *
  * The two families are structurally identical: both pair a structural leftover `X` with a focus
  * collection / aggregate `F[A]`. Pre-unification:
  *   - `AlgLens[F]` exposed the F as a type parameter, used `Functor` / `Foldable` / `Traverse[F]`
  *     + `MonoidK[F]` from cats.
  *   - `Kaleidoscope` hid F as a path-dependent type member (`type FCarrier[_]`) and required the
  *     project-local `Reflector[F]` typeclass for its `.collect` universal.
  *
  * `MultiFocus[F]` keeps the F-as-parameter encoding (matching AlgLens) and absorbs Kaleidoscope by
  * recognising that the only piece of `Reflector[F]` that wasn't already in cats was the
  * `reflect: (fa: F[A]) => (f: F[A] => B) => F[B]` operation. That operation has two natural
  * derivations and the choice is structural, not derived:
  *
  *   - "broadcast via `Functor.map`": `reflect(fa)(f) = fa.map(_ => f(fa))`. Length-preserving,
  *     matches the v1 ZipList Reflector and Const Reflector exactly. Requires only `Functor[F]`.
  *   - "broadcast via `Applicative.pure`": `reflect(fa)(f) = F.pure(f(fa))`. Singleton/cartesian,
  *     matches the v1 List Reflector. Requires `Applicative[F]`.
  *
  * Pick the first as the default. Users wanting the cartesian collapse compose with a downstream
  * `_.headOption` fold, OR construct `MultiFocus[List]` explicitly with the singleton choice via
  * the `collectVia` constructor.
  *
  * @tparam F
  *   classifier shape — constraint requirements depend on the operation:
  *   - `.modify` / `.replace` need `Functor[F]`.
  *   - `.foldMap` needs `Foldable[F]`.
  *   - `.modifyA` / `.all` need `Traverse[F]`.
  *   - `.collect` (the Kaleidoscope universal) needs `Functor[F]` for the default broadcast, or
  *     `Applicative[F]` for the cartesian-singleton variant.
  *   - Same-carrier `.andThen` needs `Traverse[F] + MultiFocusFromList[F]`.
  *   - Constructing a bridge via `fromPrismF` / `fromOptionalF` needs `MonoidK[F]` only.
  */
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])

/** Singleton-classifier fast-path capability — preserved from AlgLensSingleton. Lets
  * `multiFocusAssoc` skip the `F.pure` wrap on push and the `pickSingletonOrThrow` on pull when the
  * inner optic is known to produce singletons. Sole shipped user: the `tuple2multifocus` Lens →
  * MultiFocus bridge.
  */
private[eo] trait MultiFocusSingleton[S, T, A, B, X0]:
  def singletonTo(s: S): (X0, A)
  def singletonFrom(x: X0, b: B): T

/** Per-F O(n) builder, identical role to the prior `AlgLensFromList`. `MonoidK[F].combineK` has
  * inconsistent asymptotics across F (O(n²) on Vector, lossy on Option), so we keep this typeclass
  * to gate the same-carrier `.andThen` push/pull paths.
  *
  * Q2 finding: `Traverse[F] + MonoidK[F]` is ENOUGH to derive `fromList` in principle —
  * `xs.foldLeft(empty)((acc, a) => combineK(acc, pure(a)))` — but the asymptotics regress on Vector
  * (O(n²)) and the cardinality is silently dropped on Option. The typeclass is therefore a known
  * per-F cost we carry forward unchanged.
  */
private[eo] trait MultiFocusFromList[F[_]]:
  def fromList[A](xs: List[A]): F[A]

  def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): F[A] =
    fromList(List.tabulate(size)(i => arr(from + i).asInstanceOf[A]))

private[eo] object MultiFocusFromList:

  given forList: MultiFocusFromList[List] with
    def fromList[A](xs: List[A]): List[A] = xs

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): List[A] =
      List.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forOption: MultiFocusFromList[Option] with

    def fromList[A](xs: List[A]): Option[A] = xs match
      case Nil      => None
      case h :: Nil => Some(h)
      case _        =>
        throw new IllegalStateException(
          s"MultiFocusFromList[Option]: cannot represent ${xs.size} elements; cardinality is 0 or 1."
        )

  given forVector: MultiFocusFromList[Vector] with
    def fromList[A](xs: List[A]): Vector[A] = xs.toVector

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): Vector[A] =
      Vector.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forChain: MultiFocusFromList[cats.data.Chain] with
    def fromList[A](xs: List[A]): cats.data.Chain[A] = cats.data.Chain.fromSeq(xs)

object MultiFocus:

  // ------------------------------------------------------------------
  // Capability instances — Functor / Foldable / Traverse over the F[A] half.
  // (Ported verbatim from AlgLens; the Kaleidoscope-side `kalFunctor`'s
  // broadcast-rebuild trick is no longer needed because MultiFocus carries
  // the leftover X as a plain value, not as a `FCarrier[A] => X` closure.)
  // ------------------------------------------------------------------

  given mfFunctor[F[_]: Functor]: ForgetfulFunctor[MultiFocus[F]] with

    def map[X, A, B](xa: (X, F[A]), f: A => B): (X, F[B]) =
      (xa._1, Functor[F].map(xa._2)(f))

  given mfFold[F[_]: Foldable]: ForgetfulFold[MultiFocus[F]] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, F[A])) => M =
      f => xa => Foldable[F].foldMap(xa._2)(f)

  given mfTraverse[F[_]: Traverse]: ForgetfulTraverse[MultiFocus[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: ((X, F[A])) => (A => G[B]) => G[(X, F[B])] =
      xa => f => Applicative[G].map(Traverse[F].traverse(xa._2)(f))(fb => (xa._1, fb))

  // ------------------------------------------------------------------
  // Same-carrier composition — the AlgLens.assocAlgMonad logic, F-parametric.
  // (Singleton fast-path preserved; structurally identical.)
  // ------------------------------------------------------------------

  given mfAssoc[F[_]: Traverse: MultiFocusFromList, Xo, Xi]
      : AssociativeFunctor[MultiFocus[F], Xo, Xi] with
    type Z = (Xo, F[(Xi, Int)])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[F]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[F]] { type X = Xi },
    ): ((Xo, F[(Xi, Int)]), F[C]) =
      val Tr = Traverse[F]
      val FL = summon[MultiFocusFromList[F]]
      val (xo, fa) = outer.to(s)
      inner match
        case is: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          val (cList, xiList) =
            Tr.mapAccumulate((List.empty[C], List.empty[(Xi, Int)]), fa) {
              case ((accC, accXi), a) =>
                val (xi, c) = is.singletonTo(a)
                ((c :: accC, (xi, 1) :: accXi), ())
            }._1
          val fc: F[C] = FL.fromList(cList.reverse)
          val fxiSize: F[(Xi, Int)] = FL.fromList(xiList.reverse)
          ((xo, fxiSize), fc)
        case _ =>
          val (flatList, fxiSize) =
            Tr.mapAccumulate(List.empty[C], fa) { (acc, a) =>
              val (xi, fc) = inner.to(a)
              val (acc2, count) = Tr.foldLeft(fc, (acc, 0)) {
                case ((l, n), c) =>
                  (c :: l, n + 1)
              }
              (acc2, (xi, count))
            }
          val fc: F[C] = FL.fromList(flatList.reverse)
          ((xo, fxiSize), fc)

    def composeFrom[S, T, A, B, C, D](
        xd: ((Xo, F[(Xi, Int)]), F[D]),
        inner: Optic[A, B, C, D, MultiFocus[F]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[F]] { type X = Xo },
    ): T =
      val Tr = Traverse[F]
      val ((xo, fxiSize), fd) = xd
      val dArr: Array[AnyRef] = foldableToArray[F, D](fd)
      inner match
        case is: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, _)) =>
              val d = dArr(cursor).asInstanceOf[D]
              (cursor + 1, is.singletonFrom(xi, d))
          }
          outer.from((xo, fb))
        case _ =>
          val FL = summon[MultiFocusFromList[F]]
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, size)) =>
              (cursor + size, inner.from((xi, FL.fromArraySlice[D](dArr, cursor, size))))
          }
          outer.from((xo, fb))

  private def foldableToArray[F[_]: Foldable, D](fd: F[D]): Array[AnyRef] =
    val n = Foldable[F].size(fd).toInt
    val arr = new Array[AnyRef](n)
    var i = 0
    Foldable[F].foldLeft(fd, ()) { (_, d) =>
      arr(i) = d.asInstanceOf[AnyRef]
      i += 1
    }
    arr

  private def pickSingletonOrThrow[F[_]: Foldable, B](fb: F[B], carrier: String): B =
    val sz = Foldable[F].size(fb)
    if sz == 1 then Foldable[F].reduceLeftToOption(fb)(identity[B])((_, b) => b).get
    else
      throw new IllegalStateException(
        s"Composer[$carrier, MultiFocus[F]]: expected F[B] of cardinality 1, got $sz."
      )

  // ------------------------------------------------------------------
  // Kaleidoscope universal — `.collect`. Q1 finding: NOT derivable from
  // Apply alone in a way that preserves all three v1 Reflector instances.
  // We expose two variants and let the user pick the aggregation shape.
  // ------------------------------------------------------------------

  /** Length-preserving broadcast — `reflect(fa)(f) = fa.map(_ => f(fa))`. Matches the v1
    * `forZipList` and `forConst` Reflector instances exactly; CHANGES the v1 `forList` semantics
    * (was singleton, becomes length-preserving). Requires only `Functor[F]`.
    *
    * For the generic `MultiFocus.apply[F, A]` factory (X = F[A], rebuild = identity), `.collectMap`
    * is `s => s.map(_ => agg(s))` — semantically identical to `Functor.map(_ => agg)` over the
    * source.
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, MultiFocus[List]])

    def collectList(agg: List[A] => B)(using ev: S =:= List[A], ev2: T =:= List[B]): S => T =
      val _ = (ev, ev2)
      // List default: cartesian / singleton — matches v1 Reflector[List]. The mapping `T = List[B]`
      // is preserved by post-wrapping `List(b)`.
      (s: S) =>
        val (_, fa) = o.to(s)
        val b: B = agg(fa.asInstanceOf[List[A]])
        o.from((null.asInstanceOf[o.X], List(b)).asInstanceOf[(o.X, List[B])])

  /** Functor-broadcast variant — preserves F-shape via `map(_ => agg(fa))`. Works for any
    * `Functor[F]`. Matches v1 ZipList / Const semantics; collapses List into ZipList-style
    * length-preserving for List.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Functor[F])

    def collectMap[C](agg: F[A] => C)(using ev: C =:= B): S => T =
      val _ = ev
      (s: S) =>
        val (x, fa) = o.to(s)
        val b: C = agg(fa)
        val fb: F[B] = F.map(fa)(_ => b.asInstanceOf[B])
        o.from((x, fb))

  // ------------------------------------------------------------------
  // Constructors — preserved from both AlgLens (Forget/Tuple2/Either/Affine
  // bridges, F[A]-focus factories) and Kaleidoscope (apply[F]).
  // ------------------------------------------------------------------

  /** Generic factory mirroring `Kaleidoscope.apply[F, A]`. Encoding: `X = F[A]`, focus = fa,
    * rebuild = identity. Replaces `Kaleidoscope.apply` cleanly because under MultiFocus the F is in
    * the optic's type, not on a path-dependent member.
    */
  def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]] =
    new Optic[F[A], F[A], A, A, MultiFocus[F]]:
      type X = F[A]
      val to: F[A] => (F[A], F[A]) = (fa: F[A]) => (fa, fa)
      val from: ((F[A], F[A])) => F[A] = { case (_, fb) => fb }

  /** Iso → MultiFocus bridge. Replaces both `forgetful2kaleidoscope` and the Iso → AlgLens path
    * (which previously fell out of `Morph.viaTuple2`'s low-priority chain).
    *
    * Requires `Applicative[F]` because the Iso's focus is a plain `A` and we need to broadcast it
    * into a singleton `F[A]`. Mirrors `Composer[Forgetful, AlgLens[F]]`'s old constraint set.
    */
  given forgetful2multifocus[F[_]: Applicative: Foldable]: Composer[Forgetful, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), Applicative[F].pure(o.to(s)))
        val from: ((Unit, F[B])) => T = {
          case (_, fb) => o.from(pickSingletonOrThrow(fb, "Forgetful"))
        }

  /** Forget[F] ↪ MultiFocus[F]. Same shape as the prior `forget2alg`. */
  given forget2multifocus[F[_]]: Composer[Forget[F], MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), o.to(s))
        val from: ((Unit, F[B])) => T = { case (_, fb) => o.from(fb) }

  /** Lens → MultiFocus[F]. Same shape as the prior `tuple2alg`; mixes in `MultiFocusSingleton` so
    * the mfAssoc fast-path fires.
    */
  given tuple2multifocus[F[_]: Applicative: Foldable]: Composer[Tuple2, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]] with MultiFocusSingleton[S, T, A, B, o.X]:
        type X = o.X
        def singletonTo(s: S): (o.X, A) = o.to(s)
        def singletonFrom(x: o.X, b: B): T = o.from((x, b))
        val to: S => (X, F[A]) = s =>
          val (x, a) = o.to(s)
          (x, Applicative[F].pure(a))
        val from: ((X, F[B])) => T = {
          case (x, fb) =>
            o.from((x, pickSingletonOrThrow(fb, "Tuple2")))
        }

  /** Prism → MultiFocus[F]. Same shape as the prior `either2alg`. */
  given either2multifocus[F[_]: Alternative: Foldable]: Composer[Either, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[o.X, Unit]
        val to: S => (X, F[A]) = s =>
          o.to(s) match
            case Right(a) => (Right(()), Applicative[F].pure(a))
            case Left(x)  => (Left(x), Alternative[F].empty[A])
        val from: ((X, F[B])) => T = {
          case (Left(xMiss), _) => o.from(Left(xMiss))
          case (Right(_), fb)   => o.from(Right(pickSingletonOrThrow(fb, "Either")))
        }

  /** Optional → MultiFocus[F]. Same shape as the prior `affine2alg`. */
  given affine2multifocus[F[_]: Alternative: Foldable]: Composer[Affine, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[Fst[o.X], Snd[o.X]]
        val to: S => (X, F[A]) = s =>
          o.to(s) match
            case h: Affine.Hit[o.X, A] =>
              (Right(h.snd), Applicative[F].pure(h.b))
            case m: Affine.Miss[o.X, A] =>
              (Left(m.fst), Alternative[F].empty[A])
        val from: ((X, F[B])) => T = {
          case (Left(fstX), _) =>
            o.from(new Affine.Miss[o.X, B](fstX))
          case (Right(sndX), fb) =>
            o.from(new Affine.Hit[o.X, B](sndX, pickSingletonOrThrow(fb, "Affine")))
        }

  /** MultiFocus[F] → SetterF. Replaces both `kaleidoscope2setter` and the (latent, never-shipped)
    * `alg2setter`. Closes the U → N gap from the composition gap analysis: the Kaleidoscope row of
    * the Composer matrix now has a uniform Setter widening for ALL F.
    */
  given multifocus2setter[F[_]: Functor]: Composer[MultiFocus[F], SetterF] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, SetterF] =
      new Optic[S, T, A, B, SetterF]:
        type X = (S, A)
        val to: S => SetterF[X, A] = s => SetterF((s, identity[A]))
        val from: SetterF[X, B] => T = sfxb =>
          val (s, f) = sfxb.setter
          val (x, fa) = o.to(s)
          o.from((x, Functor[F].map(fa)(f)))

  // ------------------------------------------------------------------
  // F[A]-focus factories — preserved from AlgLens.
  // ------------------------------------------------------------------

  def fromLensF[F[_], S, T, A, B](
      lens: Optic[S, T, F[A], F[B], Tuple2]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = lens.X
      val to: S => (X, F[A]) = lens.to
      val from: ((X, F[B])) => T = lens.from

  def fromPrismF[F[_]: MonoidK, S, T, A, B](
      prism: Optic[S, T, F[A], F[B], Either]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Either[prism.X, Unit]
      val to: S => (X, F[A]) = s =>
        prism.to(s) match
          case Right(fa) => (Right(()), fa)
          case Left(x)   => (Left(x), MonoidK[F].empty[A])
      val from: ((X, F[B])) => T = {
        case (Left(xMiss), _) => prism.from(Left(xMiss))
        case (Right(_), fb)   => prism.from(Right(fb))
      }

  def fromOptionalF[F[_]: MonoidK, S, T, A, B](
      opt: Optic[S, T, F[A], F[B], Affine]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Affine[opt.X, Unit]
      val to: S => (X, F[A]) = s =>
        opt.to(s) match
          case m: Affine.Miss[opt.X, F[A]] @unchecked =>
            (m.widenB[Unit], MonoidK[F].empty[A])
          case h: Affine.Hit[opt.X, F[A]] @unchecked =>
            (new Affine.Hit[opt.X, Unit](h.snd, ()), h.b)
      val from: ((X, F[B])) => T = {
        case (m: Affine.Miss[opt.X, Unit] @unchecked, _) =>
          opt.from(m.widenB[F[B]])
        case (h: Affine.Hit[opt.X, Unit] @unchecked, fb) =>
          opt.from(new Affine.Hit[opt.X, F[B]](h.snd, fb))
      }
