package dev.constructive.eo
package schemes

import cats.Traverse

import data.{Forget, ForgetK}
import optics.Optic
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

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as the existing
    * [[dev.constructive.eo.data.Forget]] carrier. `to = project: S => F[S]`, `from = embed: F[S] =>
    * S`, so it is a genuine `Optic[S, S, S, S, Forget[F]]` with **no change to the `Optic` trait**
    * — the concrete proof that a typed `F` is an optic carrier, and an observational read (given
    * `Foldable[F]`) of a layer's immediate foci via `.foldMap`. It is a single-layer peel/glue
    * (like `Plated`'s `plate`, but one layer, not the recursion); the schemes drive `to`/`from`
    * themselves.
    */
  def fLayer[F[_], S](using Project[F, S], Embed[F, S]): Optic[S, S, S, S, Forget[F]] =
    new FLayer[F, S]

  /** The named class behind [[fLayer]] — the single-layer peel/glue optic. */
  final private class FLayer[F[_], S](using P: Project[F, S], E: Embed[F, S])
      extends Optic[S, S, S, S, Forget[F]]:
    type X = Any
    def to(s: S): Forget[F][X, S] = ForgetK(P.project(s))
    def from(fs: Forget[F][X, S]): S = E.embed(fs.value)

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

  /** Futumorphism — a multi-layer unfold `coalg: A => F[Coattr[F, A]]` ([[zoo.Futu]], `X = Coattr`,
    * the free monad). `.reverseGet`. All-`Pure` degenerates to [[ana]].
    */
  def futu[F[_], A, S](coalg: A => F[Coattr[F, A]])(using Traverse[F], Embed[F, S]): Futu[F, A, S] =
    new Futu[F, A, S](coalg)

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
