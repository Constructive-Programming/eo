package dev.constructive.eo
package schemes

import cats.Traverse

import data.{Forget, ForgetK}
import optics.Optic
import zoo.{Ana, Attr, Cata, Coattr, Futu, Histo, Hylo}

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
  * | [[histo]] | [[zoo.Attr]] = `νX. A × F[X]`   | the **cofree comonad** (course-of-value fold) |
  * | [[ana]]   | `S`                             | the materialising unfold                      |
  * | [[futu]]  | [[zoo.Coattr]] = `μX. A + F[X]` | the **free monad** (multi-layer unfold)       |
  *
  * `histo` refines `cata`'s index up the comonad tower; `futu` refines `ana`'s up the monad tower.
  *
  * ==hylo is the fusion, not a primitive==
  *
  * [[ana]] is a build (`Review`-shaped) and [[cata]] a node-blind fold (`Getter`-shaped); the
  * build⇄read seam `ana.cross(cata)` (definitionally `ana.reverse.andThen(cata)`) **fuses** over
  * the [[Scheme]] carrier — which keeps the `coalg`/`alg` alive — into [[hylo]], building *no
  * intermediate `S`*. The [[FusionSpec]] pins the hylo law and witnesses the deforestation (the
  * fused refold never calls `project`/`embed`).
  *
  * All schemes run on one stack-safe engine ([[Machines.foldLayered]]): a `< 512`-deep on-stack
  * fast path falling back per deep subtree to a heap `ArrayDeque` machine — stack-safe to 10⁶,
  * tested.
  *
  * The citizen classes live in [[zoo]] ([[zoo.Cata]] / [[zoo.Ana]] / [[zoo.Hylo]] / [[zoo.Histo]] /
  * [[zoo.Futu]]); this object is the user-facing constructor surface plus [[fLayer]].
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
