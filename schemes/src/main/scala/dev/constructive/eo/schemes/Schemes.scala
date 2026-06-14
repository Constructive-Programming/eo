package dev.constructive.eo
package schemes

import cats.Traverse

import data.{Forget, ForgetK}
import optics.Optic
import zoo.{Ana, Apo, Attr, Cata, Coattr, Futu, Histo, Hylo, Meta, Para}

/** Typed recursion schemes as composable optics, over a user-supplied **pattern functor** `F[_]` (+
  * `Traverse[F]`) and the [[Basis]] (`Project`/`Embed`) correspondence to the recursive type `S`.
  *
  * ==The thesis==
  *
  * A recursion scheme is an [[Optic]] whose existential `X` (see [[Scheme]]) is the *index* of the
  * recursion — what the scheme retains — and **the (co)free (co)monads are the universal indices**:
  *
  * | scheme    | `X`                             | index                                         |
  * |:----------|:--------------------------------|:----------------------------------------------|
  * | [[cata]]  | `Nothing`                       | the forgetful (trivial) fold                  |
  * | [[para]]  | `F[(S, A)]`                     | the **store-comonad** complement (subterms)   |
  * | [[histo]] | [[zoo.Attr]] = `νX. A × F[X]`   | the **cofree comonad** (course-of-value fold) |
  * | [[ana]]   | `S`                             | the materialising unfold                      |
  * | [[apo]]   | `Either[S, A]`                  | the **Prism** residual (graft, build-side)    |
  * | [[futu]]  | [[zoo.Coattr]] = `μX. A + F[X]` | the **free monad** (multi-layer unfold)       |
  *
  * `para`/`histo` refine `cata`'s index up the comonad tower; `apo`/`futu` refine `ana`'s up the
  * monad tower. (`para`'s existential is the writable-Lens complement — get-put holds
  * definitionally, put-get only under algebra-coherence, so the lawful writable put is a scoped
  * follow-up.)
  *
  * ==hylo is the fusion, not a primitive — and meta is the honest non-fusion==
  *
  * [[ana]] is a build (`Review`-shaped) and [[cata]] a node-blind fold (`Getter`-shaped); the
  * build⇄read seam `ana.cross(cata)` (definitionally `ana.reverse.andThen(cata)`) **fuses** over
  * the [[Scheme]] carrier — which keeps the `coalg`/`alg` alive — into [[hylo]], building *no
  * intermediate `S`*. The [[FusionSpec]] pins the hylo law and witnesses the deforestation (the
  * fused refold never calls `project`/`embed`).
  *
  * The **fold→unfold** seam `cata.meta(ana)` is the direction-dual ([[meta]], the metamorphism),
  * and it **cannot fuse**: fold and unfold range over *different* functors, so the neck value is
  * genuinely materialised (the [[zoo.Meta]] existential is `X = A`, not `Nothing`). The 2×2 the two
  * seams complete — refold vs metamorphism × trivial vs universal index — is [[hylo]] / [[meta]] /
  * [[chrono]] / [[metaChrono]]; the quadrant's diagonals are [[dyna]] (`ana.cross(histo)`) and
  * [[codyna]] (`futu.cross(cata)`). [[elgot]] / [[coelgot]] are the short-circuit / seed-reading
  * refold variants (both fuse, driven by [[Machines.foldLayeredOr]] / the seed-passing combine).
  *
  * All schemes run on one stack-safe engine ([[Machines.foldLayered]]): a `< 512`-deep on-stack
  * fast path falling back per deep subtree to a heap `ArrayDeque` machine — stack-safe to 10⁶,
  * tested.
  *
  * The citizen classes live in [[zoo]] ([[zoo.Cata]] / [[zoo.Para]] / [[zoo.Histo]] / [[zoo.Ana]] /
  * [[zoo.Apo]] / [[zoo.Futu]] / [[zoo.Hylo]] / [[zoo.Meta]]); this object is the user-facing
  * constructor surface plus [[fLayer]].
  */
object Schemes:

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as the existing
    * [[dev.constructive.eo.data.Forget]] carrier. `to = project: S => F[S]`, `from = embed: F[S] =>
    * S`, so it is a genuine `Optic[S, S, S, S, Forget[F]]` with **no change to the `Optic` trait**
    * — the concrete proof that a typed `F` is an optic carrier, and an observational read (given
    * `Foldable[F]`) of a layer's immediate foci via `.foldMap`. It is a single-layer peel/glue
    * (like `Plated`'s `plate`, but one layer, not the recursion); the schemes below drive
    * `to`/`from` themselves.
    */
  def fLayer[F[_], S](using Project[F, S], Embed[F, S]): Optic[S, S, S, S, Forget[F]] =
    new FLayer[F, S]

  /** The named class behind [[fLayer]] — the single-layer peel/glue optic. */
  final private class FLayer[F[_], S](using P: Project[F, S], E: Embed[F, S])
      extends Optic[S, S, S, S, Forget[F]]:
    type X = Any
    def to(s: S): Forget[F][X, S] = ForgetK(P.project(s))
    def from(fs: Forget[F][X, S]): S = E.embed(fs.value)

  /** Catamorphism — a **node-blind** fold `alg: F[A] => A`, worn as a [[zoo.Cata]] (`X = Nothing`).
    * The algebra sees only the already-folded children as a typed `F[A]` (named constructors — no
    * positional `AnyRef` indexing), never the original node `S`; that blindness is what makes
    * `ana.cross(cata)` sound to fuse. Consumed via `.get`. Stack-safe.
    */
  def cata[F[_], S, A](alg: F[A] => A)(using Traverse[F], Project[F, S]): Cata[F, S, A] =
    new Cata[F, S, A](alg)

  /** Histomorphism — a course-of-value fold `alg: F[Attr[F, A]] => A`, worn as a [[zoo.Histo]] (`X =
    * Attr[F, A]`, the cofree comonad). Each child slot carries its full decorated history
    * ([[zoo.Attr]]: result + that child's own decorated layer), so the algebra can read arbitrarily
    * far down. Consumed via `.get`. Stack-safe; retains O(n) `Attr` cells by nature.
    */
  def histo[F[_], S, A](alg: F[Attr[F, A]] => A)(using Traverse[F], Project[F, S]): Histo[F, S, A] =
    new Histo[F, S, A](alg)

  /** Paramorphism — a fold retaining the original subterms, `alg: F[(S, A)] => A`, worn as a
    * [[zoo.Para]] (`X = F[(S, A)]`). Each child slot pairs its original subterm with its folded
    * result, so the algebra can read the subtree itself, not just its summary. Consumed via `.get`.
    * Ignoring the `S` half degenerates to [[cata]]. Stack-safe. (See [[zoo.Para]] on why the
    * existential is the store-comonad complement but the writable `Lens` put is conditional.)
    */
  def para[F[_], S, A](alg: F[(S, A)] => A)(using Traverse[F], Project[F, S]): Para[F, S, A] =
    new Para[F, S, A](alg)

  /** Anamorphism — an unfold `coalg: Seed => F[Seed]`, worn as an [[zoo.Ana]] (`X = S`). Each step
    * yields one typed layer of child seeds; [[Embed]] glues each layer into the built `S`.
    * Materialising — the built `S` is O(nodes). Consumed via `.reverseGet`. Compose onto a fold
    * with `ana.cross(cata)` (the fused build⇄read seam — that is [[hylo]]). Stack-safe.
    */
  def ana[F[_], Seed, S](coalg: Seed => F[Seed])(using Traverse[F], Embed[F, S]): Ana[F, Seed, S] =
    new Ana[F, Seed, S](coalg)

  /** Futumorphism — a multi-layer unfold `coalg: A => F[Coattr[F, A]]`, worn as a [[zoo.Futu]] (`X =
    * Coattr[F, A]`, the free monad). Each slot answers [[zoo.Coattr.Pure]] (keep unfolding) or
    * [[zoo.Coattr.Roll]] (a prebuilt layer, no coalgebra call), so one step may emit several
    * layers. Consumed via `.reverseGet`. Stack-safe.
    */
  def futu[F[_], A, S](coalg: A => F[Coattr[F, A]])(using Traverse[F], Embed[F, S]): Futu[F, A, S] =
    new Futu[F, A, S](coalg)

  /** Apomorphism — an unfold that may graft a finished subtree, `coalg: A => F[Either[S, A]]`, worn
    * as an [[zoo.Apo]] (`X = Either[S, A]`). `Left(s)` grafts a built `S` by reference (O(1), never
    * re-walked); `Right(a)` keeps unfolding. The build-side dual of [[para]]. Consumed via
    * `.reverseGet`. All-`Right` degenerates to [[ana]]. Stack-safe.
    */
  def apo[F[_], A, S](coalg: A => F[Either[S, A]])(using Traverse[F], Embed[F, S]): Apo[F, A, S] =
    new Apo[F, A, S](coalg)

  /** Hylomorphism — the **fused** refold `Seed => A`, building **no intermediate `S`** (so it needs
    * neither `Project` nor `Embed`, only `Traverse[F]`). Definitionally
    * `ana(coalg).cross(cata(alg))` — this constructor builds the same one-pass machine directly for
    * callers who never name the intermediate type. `coalg` unfolds a seed into one typed layer;
    * `alg` folds the layer's results to `A` (node-blind, like [[cata]]). Stack-safe.
    */
  def hylo[F[_], Seed, A](coalg: Seed => F[Seed], alg: F[A] => A)(using
      F: Traverse[F]
  ): Hylo[Seed, A] =
    new Hylo[Seed, A](Machines.foldLayered[F, Seed, A](coalg, (_, fr) => alg(fr)))

  /** Chronomorphism — the **fused** futu-then-histo refold `A => B`, **hylo lifted to the universal
    * indices**: it unfolds through the free monad ([[zoo.Coattr]]) and folds through the cofree
    * comonad ([[zoo.Attr]]), building **no intermediate `S`**. Definitionally `futu(coalg).cross(
    * histo(algebra))`; this constructor builds the same one-pass machine directly.
    *
    * Like [[hylo]] it needs **only `Traverse[F]`** — no `Project`, no `Embed` — which is the
    * compile-time deforestation proof: with no `Basis` in scope there is no `S` to build. The
    * `Coattr` (multi-layer input) and `Attr` (course-of-value history) cells are threaded
    * internally; only the root head is read out. Heads-only `algebra` + all-`Pure` `coalg`
    * degenerate to [[hylo]]. Stack-safe (the [[Machines.foldLayered]] machine); retains O(n) `Attr`
    * cells by nature.
    */
  def chrono[F[_], A, B](
      coalg: A => F[Coattr[F, A]],
      algebra: F[Attr[F, B]] => B,
  )(using F: Traverse[F]): Hylo[A, B] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val build: Coattr[F, A] => Attr[F, B] =
      Machines.foldLayered[F, Coattr[F, A], Attr[F, B]](
        expand,
        (_, layer) => Attr(algebra(layer), layer),
      )
    new Hylo[A, B](a => Attr.forget(build(Coattr.Pure(a))))

  /** Metamorphism — the **fold-then-unfold** read `S => T`, the direction-dual of [[hylo]]. Fold
    * the `F`-recursive `S` to a neck value `A` (node-blind `alg`), then unfold `A` into a fresh
    * `G`-recursive `T` (`coalg`). Definitionally `cata(alg).meta(ana(coalg))`; this builds the same
    * two-pass machine directly.
    *
    * **Does not fuse** — unlike [[hylo]] it keeps *both* `Basis`es (`Project[F, S]` to fold,
    * `Embed[G, T]` to build), because the fold's `F` and the unfold's `G` differ: there is no
    * shared functor whose `project ∘ embed` could cancel, so the neck `A` is genuinely materialised
    * (the [[zoo.Meta]]'s existential `X = A`). The compile-time tell is right here in the
    * signature: where `hylo` needs only `Traverse`, `meta` cannot drop either `Basis`. Stack-safe
    * (two [[Machines.foldLayered]] passes).
    */
  def meta[F[_], S, A, G[_], T](
      alg: F[A] => A,
      coalg: A => G[A],
  )(using F: Traverse[F], P: Project[F, S], G: Traverse[G], E: Embed[G, T]): Meta[S, A, T] =
    val fold: S => A = Machines.foldLayered[F, S, A](P.project, (_, fr) => alg(fr))
    val unfold: A => T = Machines.foldLayered[G, A, T](coalg, (_, gr) => E.embed(gr))
    new Meta[S, A, T](fold.andThen(unfold))

  /** Metamorphism at the universal indices — the **fold-then-unfold dual of [[chrono]]**. Fold the
    * `F`-recursive `S` course-of-value to a neck `A` (the cofree history, [[zoo.Attr]]), then
    * multi-layer-unfold `A` into a `G`-recursive `T` (the free coalgebra, [[zoo.Coattr]]).
    * Definitionally `histo(algebra).meta(futu(coalg))`.
    *
    * **Does not fuse**, for the same reason as [[meta]]: `F ≠ G`, so the neck `A` is materialised
    * (`X = A`). The cofree comonad on the fold side and the free monad on the unfold side never
    * cancel across the neck — `chrono` is exactly this combination *with `F = G`*, where they do.
    * Stack-safe.
    */
  def metaChrono[F[_], S, A, G[_], T](
      algebra: F[Attr[F, A]] => A,
      coalg: A => G[Coattr[G, A]],
  )(using F: Traverse[F], P: Project[F, S], G: Traverse[G], E: Embed[G, T]): Meta[S, A, T] =
    val fold: S => A =
      val toAttr = Machines.foldLayered[F, S, Attr[F, A]](
        P.project,
        (_, layer) => Attr(algebra(layer), layer),
      )
      s => Attr.forget(toAttr(s))
    val expand: Coattr[G, A] => G[Coattr[G, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val unfold: A => T =
      val run = Machines.foldLayered[G, Coattr[G, A], T](expand, (_, gr) => E.embed(gr))
      a => run(Coattr.Pure(a))
    new Meta[S, A, T](fold.andThen(unfold))

  /** Dynamorphism — the **fused** plain-unfold → course-of-value-fold refold `A => B`, the
    * quadrant-diagonal between [[hylo]] (plain→plain) and [[chrono]] (free→cofree). `coalg` unfolds
    * one layer; the fold sees each node's full decorated history ([[zoo.Attr]], the cofree memo).
    * Definitionally `ana(coalg).cross(histo(alg))`. Fuses — `Traverse[F]` only, the [[zoo.Attr]] is
    * threaded internally with no intermediate `S`. Heads-only `alg` degenerates to [[hylo]].
    * Stack-safe; retains O(n) `Attr` cells.
    */
  def dyna[F[_], A, B](coalg: A => F[A], alg: F[Attr[F, B]] => B)(using
      F: Traverse[F]
  ): Hylo[A, B] =
    val build: A => Attr[F, B] =
      Machines.foldLayered[F, A, Attr[F, B]](coalg, (_, layer) => Attr(alg(layer), layer))
    new Hylo[A, B](a => Attr.forget(build(a)))

  /** The **fused** multi-layer-unfold → node-blind-fold refold `A => B` — the mirror of [[dyna]],
    * opposite diagonal of the refold quadrant. `coalg` may emit several layers per step
    * ([[zoo.Coattr]], the free monad); the fold is a plain `cata`. Definitionally
    * `futu(coalg).cross(cata(alg))`. Fuses — `Traverse[F]` only, no intermediate `S`. All-`Pure`
    * `coalg` degenerates to [[hylo]]. Stack-safe. (`codyna` is a descriptive name — the
    * free-unfold/plain-fold refold has no standard one in the literature.)
    */
  def codyna[F[_], A, B](coalg: A => F[Coattr[F, A]], alg: F[B] => B)(using
      F: Traverse[F]
  ): Hylo[A, B] =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val run = Machines.foldLayered[F, Coattr[F, A], B](expand, (_, fr) => alg(fr))
    new Hylo[A, B](a => run(Coattr.Pure(a)))

  /** Elgot algorithm — a [[hylo]] whose **unfold may short-circuit**: `coalg: A => Either[B, F[A]]`
    * answers `Left(b)` (the seed resolves directly to an answer, stop) or `Right(layer)` (keep
    * unfolding); `alg: F[B] => B` folds the rest. Fused refold `A => B` (`Traverse[F]` only, no
    * intermediate `S`), driven by the short-circuit-aware [[Machines.foldLayeredOr]]. An
    * all-`Right` `coalg` degenerates to [[hylo]]. Stack-safe.
    */
  def elgot[F[_], A, B](coalg: A => Either[B, F[A]], alg: F[B] => B)(using
      Traverse[F]
  ): Hylo[A, B] =
    new Hylo[A, B](Machines.foldLayeredOr[F, A, B](coalg, fr => alg(fr)))

  /** Co-Elgot algorithm — a [[hylo]] whose **fold may read the seed**: `coalg: A => F[A]` unfolds,
    * `alg: (A, F[B]) => B` folds with the originating seed in hand (the build-side analogue of
    * [[para]]'s subterm retention, but on the fused refold). Fused `A => B` (`Traverse[F]` only).
    * Ignoring the seed argument degenerates to [[hylo]]. Stack-safe.
    */
  def coelgot[F[_], A, B](coalg: A => F[A], alg: (A, F[B]) => B)(using
      Traverse[F]
  ): Hylo[A, B] =
    new Hylo[A, B](Machines.foldLayered[F, A, B](coalg, (a, fr) => alg(a, fr)))
