package dev.constructive.eo
package schemes

import cats.{Monad, Traverse}

import data.{Forget, ForgetK}
import optics.{Getter, Optic}
import zoo.*

/** Typed recursion schemes as composable optics, over a user-supplied **pattern functor** `F[_]` (+
  * `Traverse[F]`) and the [[Basis]] (`Project`/`Embed`) correspondence to the recursive type `S` —
  * algebras pattern-match `F`'s *named constructors*, no positional indexing.
  *
  *   - [[cata]] folds (`Getter[S, A]`); [[ana]] unfolds (`Getter[Seed, S]` — forward, mirroring
  *     [[anaM]]'s `FoldM[Seed, S]`); both are plain `Getter`s, so `ana.andThen(cata) : Getter[Seed,
  *     A]` is the materialising hylo via the core fused `Getter.andThen`. [[hylo]] is the **fused**
  *     zero-`S` refold (builds no intermediate `S`); `ana.andThen(cata) == hylo` is the hylo law.
  *   - The zoo: [[para]] (subterms paired from the walked nodes), [[apo]] (O(1) graft), [[histo]] /
  *     [[futu]] (course-of-value / multi-layer, via [[Attr]] / [[Coattr]]). Decorated generically
  *     through the [[Gather]]/[[Scatter]] decoration optics (over the `BiAffine` carrier) —
  *     zygo/dyna/chrono are user-written [[Gather]] / [[Scatter]] values fed to the generic
  *     [[cata]] / [[ana]] overloads.
  *   - The M-generic drivers [[cataM]] / [[anaM]] / [[hyloM]] run the same machine lifted through
  *     `Monad[M].tailRecM` (effectful layers, single-pass linear Ms).
  *
  * All drivers run on one stack-safe engine family: a `< 512`-deep on-stack fast path falling back
  * per deep subtree to a heap `ArrayDeque` machine ([[Machines.foldLayered]] and siblings) —
  * stack-safe to 10⁶, tested.
  */
object Schemes:

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as the existing
    * [[dev.constructive.eo.data.Forget]] carrier. `to = project: S => F[S]`, `from = embed: F[S] =>
    * S`, so it is a genuine `Optic[S, S, S, S, Forget[F]]` with **no change to the `Optic` trait**.
    *
    * It is a single-layer *peel/glue* (like `Plated`'s `plate`, but one layer, not the recursion).
    * The recursive schemes below drive `to`/`from` themselves and return `Direct`-carried optics,
    * so `fLayer` is mainly the concrete proof that a typed `F` is an optic carrier, plus an
    * observational read: given `Foldable[F]` it reads its layer's foci via `.foldMap`. Note it does
    * NOT compose as freely as `plate` (a `Traversal`): same-carrier `andThen` over `Forget[F]`
    * needs `Monad[F]`, which most pattern functors are not — so `fLayer` is a one-layer lens on the
    * structure, not a composable traversal.
    */
  def fLayer[F[_], S](using Project[F, S], Embed[F, S]): Optic[S, S, S, S, Forget[F]] =
    new FLayer[F, S]

  /** The named class behind [[fLayer]] — the single-layer peel/glue optic. */
  final private class FLayer[F[_], S](using P: Project[F, S], E: Embed[F, S])
      extends Optic[S, S, S, S, Forget[F]]:
    type X = Any
    def to(s: S): Forget[F][X, S] = ForgetK(P.project(s))
    def from(fs: Forget[F][X, S]): S = E.embed(fs.value)

  /** Catamorphism over a typed pattern functor `F`, as a composable `Getter`. `alg` sees the
    * original node `S` (paramorphism-flavored) plus its already-folded children as a typed `F[A]`.
    * Pure `F[A] => A` folds ignore the `S`. Stack-safe to arbitrary depth (the
    * [[Machines.foldLayered]] machine, not a trampoline). Requires `Project[F, S]` (to peel each
    * layer) and `Traverse[F]` (any lawful instance — the machine, not the user's `foldRight`,
    * provides stack-safety).
    */
  def cata[F[_], S, A](
      alg: (S, F[A]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    Getter[S, A](
      Machines.foldLayered[F, S, A](
        P.project,
        (s, fs, out) => alg(s, Machines.rebuildLayer[F, S, A](fs, out)),
      )
    )

  /** Generalized (decorated) catamorphism — the gcata of the typed path, with the decoration
    * supplied as a [[Gather]] optic value. Interior nodes apply `gather ∘ galg`; the **root applies
    * `galg` alone** (droste's `gcata` shape). The driver calls [[Gather.gather]] directly — fully
    * typed, no per-node carrier wrappers and no dispatch: the undecorated fold has its own overload
    * above (the fast path), and `cata(Gather.cata)(galg)` is law-pinned equal to it. `histo` is the
    * [[Gather.histo]] instance; user-written decorations (zygo, dyna, …) plug in the same way.
    *
    * (type-param order: `[F, S, W, A]` — compare [[ana]] `[F, A, W, S]`, which mirrors these in
    * input-before-output order: `A` is the input seed there, `S` the built output.)
    */
  def cata[F[_], S, W, A](
      gather: Gather[F, W, A]
  )(galg: (S, F[W]) => A)(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    val toW: S => W = Machines.foldLayered[F, S, W](
      P.project,
      (s, fs, out) =>
        val fw = Machines.rebuildLayer[F, S, W](fs, out)
        gather.gather(fw, galg(s, fw)),
    )
    Getter[S, A] { s =>
      val layer = P.project(s)
      galg(s, F.map(layer)(toW))
    }

  /** Paramorphism over a typed pattern functor `F` — each child slot pairs the **original subterm**
    * with its folded result. Native route: the machine already walks real `S` nodes and keeps each
    * frame's projected layer, so subterms are paired positionally — no per-node re-`embed`
    * (droste's `Gather.para` must reconstruct the subterm it threw away). Stack-safe (the
    * [[Machines.foldLayered]] machine).
    */
  def para[F[_], S, A](
      alg: (S, F[(S, A)]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    Getter[S, A](
      Machines.foldLayered[F, S, A](
        P.project,
        (s, fs, out) => alg(s, Machines.rebuildLayerPaired[F, S, A](fs, out)),
      )
    )

  /** Histomorphism over a typed pattern functor `F` — the algebra sees each child's **full
    * decorated history** ([[Attr]]: result + that child's own decorated layer).
    *
    * Native route: the combine builds the `Attr` directly, the root projects its head — one less
    * dispatch than the generic [[Gather.histo]] route (whose `Step` is EA-elided: B/op identical;
    * law-pinned equal in `DecorLawsSpec`). The remaining gap to droste's histo (558k vs 361k B/op
    * on the 8k-node fixture) is the stack-safe machine's per-node child array — droste's zoo
    * recursion is stack-UNSAFE plain recursion; the ~24 B/node is the price of the guarantee.
    *
    * Space honesty: course-of-value recursion retains O(n) `Attr` cells by nature.
    */
  def histo[F[_], S, A](
      alg: (S, F[Attr[F, A]]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    val toAttr: S => Attr[F, A] = Machines.foldLayered[F, S, Attr[F, A]](
      P.project,
      (s, fs, out) =>
        val layer = Machines.rebuildLayer[F, S, Attr[F, A]](fs, out)
        Attr(alg(s, layer), layer),
    )
    Getter[S, A](s => toAttr(s).head)

  /** Anamorphism over a typed pattern functor `F`, as a `Review`. The single fused coalgebra `Seed
    * => F[Seed]` yields one typed layer of child seeds; [[Embed]] assembles each layer into the
    * built `S`. Materializing — the built `S` is O(nodes). Stack-safe (the [[Machines.foldLayered]]
    * machine). Requires `Embed[F, S]` and `Traverse[F]`. Type params are `[F, Seed, S]` (input
    * before output) to match [[hylo]] and the `PSVec` [[ana]].
    */
  def ana[F[_], Seed, S](
      coalg: Seed => F[Seed]
  )(using F: Traverse[F], E: Embed[F, S]): Getter[Seed, S] =
    Getter[Seed, S](
      Machines.foldLayered[F, Seed, S](
        coalg,
        (_, fSeed, out) => E.embed(Machines.rebuildLayer[F, Seed, S](fSeed, out)),
      )
    )

  /** Apomorphism over a typed pattern functor `F` — per child slot the coalgebra answers
    * `Right(seed)` (keep unfolding) or `Left(s)` (an **already-finished subtree**). Native O(1)
    * graft: `Left` subtrees are prefilled into their result slots **by reference** — never
    * recursed, never projected ([[Machines.foldLayeredOr]]). Contrast droste's scatter-apo, which
    * re-walks grafts through `project` (O(graft) per graft — the distApo route, kept only as a law
    * fixture in the test suite). Stack-safe.
    */
  def apo[F[_], A, S](
      coalg: A => F[Either[S, A]]
  )(using F: Traverse[F], E: Embed[F, S]): Getter[A, S] =
    val run = Machines.foldLayeredOr[F, Either[S, A], S](
      {
        case Left(s)  => Left(s)
        case Right(a) => Right(coalg(a))
      },
      (fw, out) => E.embed(Machines.rebuildLayer[F, Either[S, A], S](fw, out)),
    )
    Getter[A, S](a => run(Right(a)))

  /** Futumorphism over a typed pattern functor `F` — the coalgebra may emit **multiple layers per
    * step** ([[Coattr]]: `Pure` keeps unfolding, `Roll` is a prebuilt layer unrolled with no
    * coalgebra call).
    *
    * Native route: the expand matches `Coattr` directly — one less dispatch than the generic
    * [[Scatter.futu]] route (whose per-slot `Step` is EA-elided: B/op identical; law-pinned equal
    * in `DecorLawsSpec`). The gap to droste's futu (655k vs 459k B/op) is the stack-safe machine's
    * per-node child array — droste's zoo recursion is stack-unsafe.
    */
  def futu[F[_], A, S](
      coalg: A => F[Coattr[F, A]]
  )(using F: Traverse[F], E: Embed[F, S]): Getter[A, S] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val build = Machines.foldLayered[F, Coattr[F, A], S](
      expand,
      (_, fw, out) => E.embed(Machines.rebuildLayer[F, Coattr[F, A], S](fw, out)),
    )
    Getter[A, S](a => build(Coattr.Pure(a)))

  /** Generalized (decorated) anamorphism — the gana of the typed path, with the decoration supplied
    * as a [[Scatter]] optic value. Each `W` slot is scattered ([[Scatter.scatter]], called directly
    * — fully typed, no per-node carrier wrappers and no dispatch: the undecorated unfold has its
    * own overload above, and `ana(Scatter.ana)(gcoalg)` is law-pinned equal to it): `Right(seed)`
    * calls `gcoalg`, `Left(layer)` unrolls the prebuilt layer with **no coalgebra call**. The root
    * seed enters through the decoration's pointed unit ([[Scatter.unit]] — gana's `pure`). `futu`
    * is the [[Scatter.futu]] instance. (apo has no shipped Scatter value — distApo is inferior by
    * construction; the O(1) graft belongs to the native `apo` engine.)
    *
    * (type-param order: compare [[cata]] `[F, S, W, A]` — the fold mirror swaps `Seed`/`A`.)
    */
  def ana[F[_], A, W, S](
      scatter: Scatter[F, W, A]
  )(gcoalg: A => F[W])(using F: Traverse[F], E: Embed[F, S]): Getter[A, S] =
    val expand: W => F[W] = w =>
      scatter.scatter(w) match
        case Right(a)    => gcoalg(a)
        case Left(layer) => layer
    val build: W => S = Machines.foldLayered[F, W, S](
      expand,
      (_, fw, out) => E.embed(Machines.rebuildLayer[F, W, S](fw, out)),
    )
    Getter[A, S](a => build(scatter.unit(a)))

  /** Hylomorphism over a typed pattern functor `F` — the **fused** refold `Seed => A`, building
    * **no intermediate `S`** (so it needs neither `Project` nor `Embed`, only `Traverse[F]`).
    * `coalg` unfolds a seed into one typed layer; `alg` folds the layer's results to `A` (the seed
    * is supplied, paramorphism-flavored). Stack-safe (the [[Machines.foldLayered]] machine). Equal
    * to the materialising `ana(coalg).andThen(cata(alg))` for a *pure* algebra (the hylo law) —
    * `hylo` fuses it into one pass with no intermediate `S`; for a node-reading para algebra the
    * two agree only under the seed↔`embed(coalg(seed))` correspondence.
    */
  def hylo[F[_], Seed, A](
      coalg: Seed => F[Seed],
      alg: (Seed, F[A]) => A,
  )(using F: Traverse[F]): Getter[Seed, A] =
    Getter[Seed, A](
      Machines.foldLayered[F, Seed, A](
        coalg,
        (seed, fSeed, out) => alg(seed, Machines.rebuildLayer[F, Seed, A](fSeed, out)),
      )
    )

  /** Effectful catamorphism — the algebra runs in `M` (`(S, F[A]) => M[A]`); the layer peel stays
    * the pure `Project`. Returns the `Forget[M]`-carried [[zoo.FoldM]] citizen; consume via `.run`.
    */
  def cataM[M[_], F[_], S, A](
      algM: (S, F[A]) => M[A]
  )(using M: Monad[M], F: Traverse[F], P: Project[F, S]): FoldM[M, S, A] =
    new FoldM[M, S, A](
      Machines.foldLayeredM[M, F, S, A](
        s => M.pure(Right(P.project(s))),
        (s, fs, out) => algM(s, Machines.rebuildLayer[F, S, A](fs, out)),
      )
    )

  /** Effectful anamorphism — the coalgebra (the layer producer) runs in `M` (`Seed => M[F[Seed]]`,
    * the arbo `GetSellOptions` shape: fetching children is effectful). Returns the [[zoo.FoldM]]
    * citizen; consume via `.run`; `anaM.andThen(cataM)` is the materialising effectful hylo,
    * `hyloM` the fused one.
    */
  def anaM[M[_], F[_], Seed, S](
      coalgM: Seed => M[F[Seed]]
  )(using M: Monad[M], F: Traverse[F], E: Embed[F, S]): FoldM[M, Seed, S] =
    new FoldM[M, Seed, S](
      Machines.foldLayeredM[M, F, Seed, S](
        seed => M.map(coalgM(seed))(Right(_)),
        (_, fSeed, out) => M.pure(E.embed(Machines.rebuildLayer[F, Seed, S](fSeed, out))),
      )
    )

  /** Effectful hylomorphism — the always-fused M spelling (what the D6 `eoHyloM` bench row runs):
    * `Seed => M[A]` with **no intermediate `S`**, seed-typed algebra.
    */
  def hyloM[M[_], F[_], Seed, A](
      coalgM: Seed => M[F[Seed]],
      algM: (Seed, F[A]) => M[A],
  )(using M: Monad[M], F: Traverse[F]): FoldM[M, Seed, A] =
    new FoldM[M, Seed, A](
      Machines.foldLayeredM[M, F, Seed, A](
        seed => M.map(coalgM(seed))(Right(_)),
        (seed, fSeed, out) => algM(seed, Machines.rebuildLayer[F, Seed, A](fSeed, out)),
      )
    )
