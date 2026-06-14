package dev.constructive.eo
package schemes

import cats.{~>, Monad, Traverse}

import data.{BiAffine, MultiFocus}
import optics.{GetReplaceLens, Lens, Optic}
import optics.Optic.get
import zoo.*

/** Typed recursion schemes as composable optics, over a user-supplied **pattern functor** `F[_]` (+
  * `Traverse[F]`) and the [[Basis]] (`Project`/`Embed`) correspondence to the recursive type `S`.
  *
  * ==The thesis==
  *
  * A recursion scheme is an [[Optic]] over the [[dev.constructive.eo.data.Direct]] carrier whose
  * existential `X` is the *index* of the recursion — what the scheme retains — and **the (co)free
  * (co)monads are the universal indices**:
  *
  * | scheme     | `X`                             | index                                         |
  * |:-----------|:--------------------------------|:----------------------------------------------|
  * | [[cata]]   | `Nothing`                       | the forgetful (trivial) fold                  |
  * | [[zygo]]   | `F[(B, A)]`                     | store comonad over an auxiliary carrier `B`   |
  * | [[para]]   | `F[(S, A)]`                     | the **store-comonad** complement (subterms)   |
  * | [[histo]]  | [[zoo.Attr]] = `νX. A × F[X]`   | the **cofree comonad** (course-of-value fold) |
  * | [[ana]]    | `S`                             | the materialising unfold                      |
  * | [[cozygo]] | `Either[B, A]`                  | *g-apo* residual over an auxiliary coalgebra  |
  * | [[apo]]    | `Either[S, A]`                  | the **Prism** residual (graft, build-side)    |
  * | [[futu]]   | [[zoo.Coattr]] = `μX. A + F[X]` | the **free monad** (multi-layer unfold)       |
  *
  * `para`/`histo` refine `cata`'s index up the comonad tower; `apo`/`futu` refine `ana`'s up the
  * monad tower. (`para`'s existential is the writable-Lens complement — get-put holds
  * definitionally, put-get only under algebra-coherence, so the lawful writable put is a scoped
  * follow-up.)
  *
  * The towers also have an *auxiliary* rung between the trivial and store/prism indices: [[zygo]]
  * (`X = F[(B, A)]`, the store comonad over an arbitrary carrier `B` — `para` is `zygo` at `B = S`)
  * and its mutual-recursion generalisation [[mutu]] (`X = F[(A, B)]`), with build-side duals
  * [[cozygo]] (`X = Either[B, A]`, *g-apo*) and [[comutu]] (`X = Either[A, B]`).
  *
  * Orthogonal to both towers is the **natural-transformation axis** — [[prepro]] / [[postpro]] keep
  * the trivial index (`cata`/`ana`-shaped) and instead pre/post-compose the layer optic
  * ([[fLayer]]) with an accumulating `η : F ~> F`, so a node at depth `k` is transformed `k` times
  * (`O(n · depth)`; `η = id` recovers `cata`/`ana`).
  *
  * ==hylo is the fusion, not a primitive — and meta is the honest non-fusion==
  *
  * [[ana]] is a build (`Review`-shaped) and [[cata]] a node-blind fold (`Getter`-shaped); the
  * build⇄read seam `ana.cross(cata)` (definitionally `ana.reverse.andThen(cata)`) **fuses** — the
  * citizens keep their `coalg`/`alg` alive — into [[zoo.Hylo]], building *no intermediate `S`*. The
  * [[FusionSpec]] pins the hylo law and witnesses the deforestation (the fused refold never calls
  * `project`/`embed`).
  *
  * The **fold→unfold** seam `cata.meta(ana)` is the direction-dual ([[meta]], the metamorphism),
  * and it **cannot fuse**: fold and unfold range over *different* functors, so the neck value is
  * materialised (the [[zoo.Meta]] existential is `X = A`, not `Nothing`). The 2×2 the two seams
  * complete — refold vs metamorphism × trivial vs universal index — is [[zoo.Hylo]] / [[zoo.Meta]]
  * / [[zoo.Chrono]] / [[zoo.MetaChrono]]; the quadrant's diagonals are [[zoo.Dyna]]
  * (`ana.cross(histo)`) and [[zoo.Codyna]] (`futu.cross(cata)`). [[zoo.Elgot]] / [[zoo.Coelgot]]
  * are the short-circuit / seed-reading refold variants.
  *
  * ==Shape==
  *
  * Every scheme is a `final class` in [[zoo]] carrying its run/build function (the construction and
  * machine-wiring live in each class's companion); this object is the user-facing **factory
  * listing** — one-line delegations — plus [[fLayer]]. All schemes run on one stack-safe engine
  * ([[Machines.foldLayered]]): a `< 512`-deep on-stack fast path falling back per deep subtree to a
  * heap `ArrayDeque` machine — stack-safe to 10⁶, tested.
  */
object Schemes:

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as core's
    * [[dev.constructive.eo.data.MultiFocus]] carrier — `MultiFocus[F][X, A] = (X, F[A])`, the
    * Traversal/AlgLens/Grate carrier. `to(s) = ((), project(s))` and `from((_, fs)) = embed(fs)`,
    * so it is a genuine `Optic[S, S, S, S, MultiFocus[F]]`: a **typed single-layer self-traversal**
    * whose foci are the node's immediate children `F[S]`.
    *
    * Because it now rides the same carrier as [[dev.constructive.eo.optics.Plated.plate]] and
    * [[dev.constructive.eo.optics.Traversal.each]], it composes with the rest of core: read the
    * immediate foci via `.foldMap` (`Foldable[F]`), rewrite them via `.modify` / `.replace`
    * (`Functor[F]`), or effect over them via `.modifyA` / `.all` (`Traverse[F]`) — the read+write
    * upgrade over the former read-only `Forget[F]` spelling. It is one layer, not the recursion;
    * the `Plated.fromBasis` derivation is its recursive face, and the schemes drive `to`/`from`
    * themselves. `X = Unit`: the `F`-shape (constructor + arity) rides inside the foci `F[S]`, so
    * `embed` needs no extra leftover.
    */
  def fLayer[F[_], S](using Project[F, S], Embed[F, S]): Optic[S, S, S, S, MultiFocus[F]] =
    new FLayer[F, S]

  /** The named class behind [[fLayer]] — the single-layer peel/glue self-traversal. */
  final private class FLayer[F[_], S](using P: Project[F, S], E: Embed[F, S])
      extends Optic[S, S, S, S, MultiFocus[F]]:
    type X = Unit
    def to(s: S): MultiFocus[F][X, S] = MultiFocus((), P.project(s))
    def from(pair: MultiFocus[F][X, S]): S = E.embed(pair.foci)

  // ===== Folds ===============================================================================

  /** Catamorphism — a node-blind fold `alg: F[A] => A` ([[zoo.Cata]], `X = Nothing`). `.get`. */
  def cata[F[_], S, A](alg: F[A] => A)(using Traverse[F], Project[F, S]): Cata[F, S, A] =
    new Cata[F, S, A](alg)

  /** Paramorphism — a subterm-retaining fold `alg: F[(S, A)] => A` ([[zoo.Para]], `X = F[(S, A)]`).
    * `.get`. Ignoring the `S` half degenerates to [[cata]].
    */
  def para[F[_], S, A](alg: F[(S, A)] => A)(using Traverse[F], Project[F, S]): Para[F, S, A] =
    new Para[F, S, A](alg)

  /** Histomorphism — a course-of-value fold `alg: F[Attr[F, A]] => A` ([[zoo.Histo]], `X = Attr`,
    * the cofree comonad). `.get`. Heads-only degenerates to [[cata]].
    */
  def histo[F[_], S, A](alg: F[Attr[F, A]] => A)(using Traverse[F], Project[F, S]): Histo[F, S, A] =
    new Histo[F, S, A](alg)

  /** Zygomorphism — a fold with an **auxiliary algebra** `aux: F[B] => B` feeding the main `alg:
    * F[(B, A)] => A` ([[zoo.Zygo]], `X = F[(B, A)]`). The comonad-tower rung between [[cata]] and
    * [[para]]: `para` is `zygo` at `B = S`, `aux = embed`; ignoring the `B` half degenerates to
    * [[cata]]. `.get`.
    */
  def zygo[F[_], S, A, B](aux: F[B] => B)(alg: F[(B, A)] => A)(using
      Traverse[F],
      Project[F, S],
  ): Zygo[F, S, A, B] = new Zygo[F, S, A, B](aux, alg)

  /** Mutumorphism — a fold by **mutual recursion**: two algebras `F[(A, B)] => A` /
    * `F[(A, B)] => B` compute a pair per node, returning the `A` half ([[zoo.Mutu]],
    * `X = F[(A, B)]`). Generalises [[zygo]] (whose `aux` is an `algB` blind to the `A` half).
    * `.get`.
    */
  def mutu[F[_], S, A, B](algA: F[(A, B)] => A, algB: F[(A, B)] => B)(using
      Traverse[F],
      Project[F, S],
  ): Mutu[F, S, A, B] = new Mutu[F, S, A, B](algA, algB)

  // ===== Unfolds =============================================================================

  /** Anamorphism — an unfold `coalg: Seed => F[Seed]` ([[zoo.Ana]], `X = S`). `.reverseGet`. */
  def ana[F[_], Seed, S](coalg: Seed => F[Seed])(using Traverse[F], Embed[F, S]): Ana[F, Seed, S] =
    new Ana[F, Seed, S](coalg)

  /** Apomorphism — an unfold that grafts finished subtrees, `coalg: A => F[Either[S, A]]`
    * ([[zoo.Apo]], `X = Either[S, A]`). `Left` grafts by reference (O(1)). `.reverseGet`.
    * All-`Right` degenerates to [[ana]].
    */
  def apo[F[_], A, S](coalg: A => F[Either[S, A]])(using Traverse[F], Embed[F, S]): Apo[F, A, S] =
    new Apo[F, A, S](coalg)

  /** [[apo]]'s per-slot residual worn on the [[data.BiAffine]] build seam — a composable *scatter*
    * optic (`Left(s) → Done(s)` the O(1) graft, `Right(a) → Step((), a)` keep unfolding). `X = (S,
    * Unit)`. Composes via [[data.BiAffine.assoc]] and the [[data.BiAffine.either2biaffine]] bridge;
    * it is the carried decoration [[apo]]'s engine itself drives (see [[zoo.Apo]]).
    */
  def apoScatter[S, A]: Optic[Either[S, A], Unit, A, Unit, BiAffine] { type X = (S, Unit) } =
    Apo.scatter[S, A]

  /** Futumorphism — a multi-layer unfold `coalg: A => F[Coattr[F, A]]` ([[zoo.Futu]], `X = Coattr`,
    * the free monad). `.reverseGet`. All-`Pure` degenerates to [[ana]].
    */
  def futu[F[_], A, S](coalg: A => F[Coattr[F, A]])(using Traverse[F], Embed[F, S]): Futu[F, A, S] =
    new Futu[F, A, S](coalg)

  /** Cozygomorphism (g-apomorphism) — the build-side dual of [[zygo]]: an **auxiliary coalgebra**
    * `aux: B => F[B]` alongside the main `coalg: A => F[Either[B, A]]` ([[zoo.Cozygo]], `X =
    * Either[B, A]`). `Left(b)` keeps unfolding through `aux`; all-`Right` degenerates to [[ana]].
    * `.reverseGet`.
    */
  def cozygo[F[_], A, B, S](aux: B => F[B])(coalg: A => F[Either[B, A]])(using
      Traverse[F],
      Embed[F, S],
  ): Cozygo[F, A, B, S] = new Cozygo[F, A, B, S](aux, coalg)

  /** Comutumorphism — the build-side dual of [[mutu]]: two mutually co-recursive coalgebras
    * `A => F[Either[A, B]]` / `B => F[Either[A, B]]`, entered at an `A` ([[zoo.Comutu]], `X =
    * Either[A, B]`). Generalises [[cozygo]]; degenerates to [[ana]] when one coalgebra suffices.
    * `.reverseGet`.
    */
  def comutu[F[_], A, B, S](coalgA: A => F[Either[A, B]], coalgB: B => F[Either[A, B]])(using
      Traverse[F],
      Embed[F, S],
  ): Comutu[F, A, B, S] = new Comutu[F, A, B, S](coalgA, coalgB)

  // ===== Refolds (fused — Traverse[F] only, no intermediate S) ===============================

  /** Hylomorphism — the fused unfold→fold `Seed => A` ([[zoo.Hylo]]). Definitionally
    * `ana(coalg).cross(cata(alg))`.
    */
  def hylo[F[_], Seed, A](coalg: Seed => F[Seed], alg: F[A] => A)(using
      Traverse[F]
  ): Hylo[Seed, A] =
    Hylo(coalg, alg)

  /** Dynamorphism — the fused plain-unfold → cofree-fold `A => B` ([[zoo.Dyna]]). Definitionally
    * `ana(coalg).cross(histo(alg))`.
    */
  def dyna[F[_], A, B](coalg: A => F[A], alg: F[Attr[F, B]] => B)(using Traverse[F]): Dyna[A, B] =
    Dyna(coalg, alg)

  /** Codynamorphism — the fused free-unfold → node-blind-fold `A => B` ([[zoo.Codyna]], the mirror
    * of [[dyna]]). Definitionally `futu(coalg).cross(cata(alg))`.
    */
  def codyna[F[_], A, B](coalg: A => F[Coattr[F, A]], alg: F[B] => B)(using
      Traverse[F]
  ): Codyna[A, B] = Codyna(coalg, alg)

  /** Chronomorphism — the fused free-unfold → cofree-fold `A => B` ([[zoo.Chrono]]), [[hylo]] at
    * the universal indices. Definitionally `futu(coalg).cross(histo(algebra))`.
    */
  def chrono[F[_], A, B](coalg: A => F[Coattr[F, A]], algebra: F[Attr[F, B]] => B)(using
      Traverse[F]
  ): Chrono[A, B] = Chrono(coalg, algebra)

  /** Elgot — a [[hylo]] whose unfold may short-circuit, `coalg: A => Either[B, F[A]]`
    * ([[zoo.Elgot]]). All-`Right` degenerates to [[hylo]].
    */
  def elgot[F[_], A, B](coalg: A => Either[B, F[A]], alg: F[B] => B)(using
      Traverse[F]
  ): Elgot[A, B] =
    Elgot(coalg, alg)

  /** Co-Elgot — a [[hylo]] whose fold reads the seed, `alg: (A, F[B]) => B` ([[zoo.Coelgot]]).
    * Ignoring the seed degenerates to [[hylo]].
    */
  def coelgot[F[_], A, B](coalg: A => F[A], alg: (A, F[B]) => B)(using
      Traverse[F]
  ): Coelgot[A, B] = Coelgot(coalg, alg)

  // ===== Metamorphisms (fold→unfold — do NOT fuse; keep both Bases) ==========================

  /** Metamorphism — the fold-then-unfold `S => T` ([[zoo.Meta]], `X = A`, the neck). Fold the
    * `F`-recursive `S` to `A`, then unfold a `G`-recursive `T`. Definitionally `cata(alg).meta(
    * ana(coalg))`.
    */
  def meta[F[_], S, A, G[_], T](alg: F[A] => A, coalg: A => G[A])(using
      Traverse[F],
      Project[F, S],
      Traverse[G],
      Embed[G, T],
  ): Meta[S, A, T] = Meta(alg, coalg)

  /** Metamorphism at the universal indices — the fold→unfold dual of [[chrono]]
    * ([[zoo.MetaChrono]]): course-of-value fold then multi-layer unfold. Definitionally
    * `histo(algebra).meta(futu(coalg))`.
    */
  def metaChrono[F[_], S, A, G[_], T](
      algebra: F[Attr[F, A]] => A,
      coalg: A => G[Coattr[G, A]],
  )(using Traverse[F], Project[F, S], Traverse[G], Embed[G, T]): MetaChrono[S, A, T] =
    MetaChrono(algebra, coalg)

  // ===== Layer-transforming schemes (the natural-transformation axis) ========================
  // Orthogonal to the (co)monad index towers: these keep the trivial index and instead pre/post-
  // compose the layer optic (fLayer) with an accumulating natural transformation η : F ~> F.

  /** Prepromorphism — a [[cata]]-shaped fold (`alg: F[A] => A`, `X = Nothing`) that applies a
    * natural transformation **`η : F ~> F` before recursing**, so a node at depth `k` sees `η`
    * applied `k` times ([[zoo.Prepro]]). `η = id` degenerates to [[cata]]. `.get`. `O(n · depth)`.
    */
  def prepro[F[_], S, A](eta: F ~> F)(alg: F[A] => A)(using
      Traverse[F],
      Project[F, S],
      Embed[F, S],
  ): Prepro[F, S, A] = new Prepro[F, S, A](eta, alg)

  /** Postpromorphism — the build-side mirror of [[prepro]]: an [[ana]]-shaped unfold (`coalg: A =>
    * F[A]`, `X = S`) that applies **`η : F ~> F` after each step** ([[zoo.Postpro]]). `η = id`
    * degenerates to [[ana]]. `.reverseGet`. `O(n · depth)`.
    */
  def postpro[F[_], A, S](eta: F ~> F)(coalg: A => F[A])(using
      Traverse[F],
      Project[F, S],
      Embed[F, S],
  ): Postpro[F, A, S] = new Postpro[F, A, S](eta, coalg)

  // ===== Monadic schemes (effects sequenced through the recursion) ===========================
  // The `*M` family lifts the zoo into a `Monad[M]` via the single [[Machines.foldLayeredM]]
  // engine. NOT a parallel class hierarchy: all read-side variants are [[zoo.FoldM]], all
  // build-side ones [[zoo.BuildM]] (the index rides each citizen's phantom `XI`). The layer
  // adapters mirror the pure side's — [[liftProject]] / [[liftCoalg]] / [[embedM]] lift the
  // `project` / `coalg` / `embed` wiring into the engine's `Or`-shape, and [[zoo.Coattr.expandM]] /
  // [[zoo.Attr.decorateM]] are the M-twins of `Coattr.expand` / `Attr.decorate` — so each `*M`
  // factory reads as terse as its pure twin. `M` must be single-pass / linear / sequential (`Id`,
  // `Eval`, `State`, `IO`); a branching/replaying `M` corrupts the engine's mutable walk state.

  // The fold-side expand: lift a pure `project` into the engine's `N => M[Either[R, F[N]]]`
  // (always `Right` — a fold never grafts; `R` is phantom).
  private def liftProject[M[_], F[_], S, R](project: S => F[S])(using
      M: Monad[M]
  ): S => M[Either[R, F[S]]] = s => M.pure(Right(project(s)))

  // The build-side expand: lift an effectful `coalg` the same way (always `Right`; `R` phantom).
  private def liftCoalg[M[_], F[_], A, R](coalg: A => M[F[A]])(using
      M: Monad[M]
  ): A => M[Either[R, F[A]]] = a => M.map(coalg(a))(Right(_))

  // The build-side combine: glue a rebuilt layer back with `Embed`, lifted into `M`.
  private def embedM[M[_], F[_], S, N](using M: Monad[M], E: Embed[F, S]): (N, F[S]) => M[S] =
    (_, fr) => M.pure(E.embed(fr))

  /** Monadic catamorphism — a node-blind fold `alg: F[A] => M[A]` ([[zoo.FoldM]], `X = Nothing`).
    * `.get` yields `M[A]`. At `M = Id` it is exactly [[cata]].
    */
  def cataM[M[_], F[_], S, A](alg: F[A] => M[A])(using
      M: Monad[M],
      F: Traverse[F],
      P: Project[F, S],
  ): FoldM[S, A, M, Nothing] =
    FoldM(Machines.foldLayeredM[M, F, S, A](liftProject(P.project), (_, fr) => alg(fr)))

  /** Monadic paramorphism — a subterm-retaining effectful fold `alg: F[(S, A)] => M[A]`
    * ([[zoo.FoldM]], `X = F[(S, A)]`). `.get` yields `M[A]`.
    */
  def paraM[M[_], F[_], S, A](alg: F[(S, A)] => M[A])(using
      M: Monad[M],
      F: Traverse[F],
      P: Project[F, S],
  ): FoldM[S, A, M, F[(S, A)]] =
    FoldM(
      Machines.foldLayeredM[M, F, S, A](
        liftProject(P.project),
        (s, fa) =>
          val it = F.toList(fa).iterator
          alg(F.map(P.project(s))(sub => (sub, it.next()))),
      )
    )

  /** Monadic histomorphism — a course-of-value effectful fold `alg: F[Attr[F, A]] => M[A]`
    * ([[zoo.FoldM]], `X = Attr[F, A]`, the cofree comonad). `.get` yields `M[A]`.
    */
  def histoM[M[_], F[_], S, A](alg: F[Attr[F, A]] => M[A])(using
      M: Monad[M],
      F: Traverse[F],
      P: Project[F, S],
  ): FoldM[S, A, M, Attr[F, A]] =
    val toAttr =
      Machines.foldLayeredM[M, F, S, Attr[F, A]](liftProject(P.project), Attr.decorateM(alg))
    FoldM(s => M.map(toAttr(s))(Attr.forget))

  /** Monadic anamorphism — an effectful unfold `coalg: Seed => M[F[Seed]]` ([[zoo.BuildM]], `X =
    * S`). `.reverseGet` yields `M[S]`. At `M = Id` it is exactly [[ana]].
    */
  def anaM[M[_], F[_], Seed, S](coalg: Seed => M[F[Seed]])(using
      M: Monad[M],
      F: Traverse[F],
      E: Embed[F, S],
  ): BuildM[S, Seed, M, S] =
    BuildM(Machines.foldLayeredM[M, F, Seed, S](liftCoalg(coalg), embedM))

  /** Monadic apomorphism — an effectful grafting unfold `coalg: A => M[F[Either[S, A]]]`
    * ([[zoo.BuildM]], `X = Either[S, A]`). `Left(s)` grafts a finished subtree by reference (O(1),
    * no effect). `.reverseGet` yields `M[S]`.
    */
  def apoM[M[_], F[_], A, S](coalg: A => M[F[Either[S, A]]])(using
      M: Monad[M],
      F: Traverse[F],
      E: Embed[F, S],
  ): BuildM[S, A, M, Either[S, A]] =
    val run = Machines.foldLayeredM[M, F, Either[S, A], S](
      {
        case Left(s)  => M.pure(Left(s))
        case Right(a) => M.map(coalg(a))(Right(_))
      },
      embedM,
    )
    BuildM(a => run(Right(a)))

  /** Monadic futumorphism — an effectful multi-layer unfold `coalg: A => M[F[Coattr[F, A]]]`
    * ([[zoo.BuildM]], `X = Coattr[F, A]`, the free monad). `Roll` unrolls a prebuilt layer with no
    * effect. `.reverseGet` yields `M[S]`.
    */
  def futuM[M[_], F[_], A, S](coalg: A => M[F[Coattr[F, A]]])(using
      M: Monad[M],
      F: Traverse[F],
      E: Embed[F, S],
  ): BuildM[S, A, M, Coattr[F, A]] =
    val run = Machines.foldLayeredM[M, F, Coattr[F, A], S](Coattr.expandM(coalg), embedM)
    BuildM(a => run(Coattr.Pure(a)))

  /** Monadic hylomorphism — the fused effectful refold `Seed => M[A]` ([[zoo.FoldM]], `X =
    * Nothing`), building **no intermediate `S`**. `Traverse[F]` only. At `M = Id` it is [[hylo]].
    */
  def hyloM[M[_], F[_], Seed, A](coalg: Seed => M[F[Seed]], alg: F[A] => M[A])(using
      M: Monad[M],
      F: Traverse[F],
  ): FoldM[Seed, A, M, Nothing] =
    FoldM(Machines.foldLayeredM[M, F, Seed, A](liftCoalg(coalg), (_, fr) => alg(fr)))

  /** Monadic chronomorphism — the fused effectful free-unfold → cofree-fold `A => M[B]`
    * ([[zoo.FoldM]], `X = Nothing`), [[hyloM]] at the universal indices. `Traverse[F]` only.
    */
  def chronoM[M[_], F[_], A, B](
      coalg: A => M[F[Coattr[F, A]]],
      alg: F[Attr[F, B]] => M[B],
  )(using M: Monad[M], F: Traverse[F]): FoldM[A, B, M, Nothing] =
    val build =
      Machines.foldLayeredM[M, F, Coattr[F, A], Attr[F, B]](
        Coattr.expandM(coalg),
        Attr.decorateM(alg),
      )
    FoldM(a => M.map(build(Coattr.Pure(a)))(Attr.forget))

  // ===== Writable scheme — the paramorphism as a Lens ========================================

  /** The paramorphism promoted from a Getter to a **Lens** — [[para]] is the read (`get`), this
    * adds the write (`enplace`), yielding a core [[dev.constructive.eo.optics.GetReplaceLens]] that
    * composes with hand-written / derived Lenses on the fused `Tuple2` path.
    *
    * '''Why `enplace` is a parameter, not derived.''' `para` is *unconditionally* a Getter, but a
    * fold result is not in general a recoverable component of `S`. The automatic put the
    * store-comonad story suggests — re-embed the retained subterms (`X = F[(S, A)]`) — makes
    * **get-put** hold definitionally yet leaves **put-get** conditional on the algebra having a
    * coherent inverse. Rather than ship a `Lens` that is only conditionally lawful, the coherent
    * put-direction is supplied by the caller; `get` / `enplace` then obey the ordinary Lens laws.
    * (The fully-automatic, read-only decorated optic — each child paired with its fold result,
    * `X = F[(S, A)]`, the store comonad over subterms — is the natural enrichment of [[fLayer]] and
    * a follow-up; it stays read-only because the decoration cannot be lawfully written.)
    *
    * @param alg
    *   the subterm-retaining fold `F[(S, A)] => A` — the `get`, a genuine paramorphism.
    * @param enplace
    *   the coherent put: rebuild an `S` whose `get` is the new focus.
    */
  def paraLens[F[_], S, A](alg: F[(S, A)] => A)(enplace: (S, A) => S)(using
      Traverse[F],
      Project[F, S],
  ): GetReplaceLens[S, S, A, A] =
    val fold = para(alg)
    Lens(fold.get(_), enplace)
