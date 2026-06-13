package dev.constructive.eo
package schemes
package proto

import cats.Traverse

import accessor.{Accessor, ReverseAccessor}
import optics.Optic

// ===========================================================================================
// PROTOTYPE — "the honest hylo optic": schemes as X-indexed optics that fuse at `cross`.
//
// The motivating question (PR #24 follow-up): can `ana.cross(cata)` be as cheap as `hylo`
// (no intermediate `S`) instead of materialising the whole tree? The plain `Direct`
// `Getter`/`Review` CANNOT — they have collapsed to opaque `Seed => S` / `S => A` closures,
// throwing away the `coalg`/`alg`, so the `project ∘ embed = id` cancellation that fusion
// rides on is invisible. Fusion needs the (co)algebra carried.
//
// This prototype carries it on a dedicated carrier `Scheme` (value-level identical to
// `Direct`, but a DISTINCT opaque type so the scheme optics are honest citizens — not
// `Getter`/`Review` clones — and so the fused `cross` overload is reachable). The existential
// `X` records the fold's STRUCTURAL DEPENDENCY, which is exactly the soundness condition for
// deforestation:
//
//   cata : X = Nothing   node-BLIND fold (F[A] => A)        → cross FUSES (no S, hylo machine)
//   para : X = S         node-READING fold ((S, F[A]) => A) → cross MATERIALISES (needs the S)
//
// "ana.cross(cata) fused vs materialising are the same optic at two X-resolutions"
// (docs/brainstorms/2026-06-12-existential-x-is-the-decoration.md) made operational and SOUND:
// the resolution is forced by whether the algebra reads its node. The fused runtime is just
// `Machines.foldLayered(coalg, alg)` — i.e. `hylo` — so the win is already proven; the novelty
// here is purely the type encoding that lets `cross` pick it.
// ===========================================================================================

/** The scheme carrier: `Scheme[X, A] = A` (the focus; `X` is a phantom at the value level, a
  * type-level tag at the optic level). Distinct from `Direct` on purpose — see the banner.
  */
opaque type Scheme[X, A] = A

object Scheme:
  inline def apply[X, A](a: A): Scheme[X, A] = a
  extension [X, A](s: Scheme[X, A]) inline def value: A = s

  given Accessor[Scheme] with
    def get[X, A](fa: Scheme[X, A]): A = fa

  given ReverseAccessor[Scheme] with
    def reverseGet[X, A](a: A): Scheme[X, A] = a

/** Pure catamorphism — a node-BLIND fold `alg: F[A] => A`. `X = Nothing`: it retains nothing of
  * the structure, so `ana.cross(this)` is sound to fuse. Read-only (`.get` via `Accessor[Scheme]`,
  * no `asGetter`).
  */
final class SchemeCata[F[_], S, A](private[proto] val alg: F[A] => A)(using
    private[proto] val F: Traverse[F],
    private[proto] val P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Scheme]:
  type X = Nothing

  private val run: S => A =
    Machines.foldLayered[F, S, A](
      P.project,
      (_, fs, out) => alg(Machines.rebuildLayer[F, S, A](fs, out)),
    )

  def to(s: S): Scheme[X, A] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()

/** Paramorphism — a node-READING fold `alg: (S, F[A]) => A`. `X = S`: it can read the original
  * subterm, so it genuinely needs the materialised tree — `ana.cross(this)` MUST build the `S`.
  */
final class SchemePara[F[_], S, A](private[proto] val alg: (S, F[A]) => A)(using
    private[proto] val F: Traverse[F],
    private[proto] val P: Project[F, S],
) extends Optic[S, Unit, A, Unit, Scheme]:
  type X = S

  private[proto] val run: S => A =
    Machines.foldLayered[F, S, A](
      P.project,
      (s, fs, out) => alg(s, Machines.rebuildLayer[F, S, A](fs, out)),
    )

  def to(s: S): Scheme[X, A] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()

/** The composite `ana.cross(_)` result — a forward read `Seed => A`. Whether `run` is the fused
  * one-pass machine or a materialise-then-fold depends on which `cross` overload built it.
  */
final class SchemeGetter[Seed, A](private[proto] val run: Seed => A)
    extends Optic[Seed, Unit, A, Unit, Scheme]:
  type X = Nothing
  def to(s: Seed): Scheme[X, A] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()

/** Anamorphism — build `reverseGet: Seed => S`. `X = S` (the structure it threads). Build-only
  * (`.reverseGet` via `ReverseAccessor[Scheme]`). Carries `coalg` so `cross` can fuse.
  */
final class SchemeAna[F[_], Seed, S](private[proto] val coalg: Seed => F[Seed])(using
    private[proto] val F: Traverse[F],
    private[proto] val E: Embed[F, S],
) extends Optic[Unit, S, Unit, Seed, Scheme]:
  type X = S

  private[proto] val build: Seed => S =
    Machines.foldLayered[F, Seed, S](
      coalg,
      (_, fSeed, out) => E.embed(Machines.rebuildLayer[F, Seed, S](fSeed, out)),
    )

  def to(u: Unit): Scheme[X, Unit] = Scheme(())
  def from(b: Scheme[X, Seed]): S = build(Scheme.value(b))

  /** FUSED seam — `cata` is node-blind (`X = Nothing`), so deforestation is sound: rebuild the
    * single-pass hylo machine from this `coalg` + `cata.alg`. No intermediate `S`.
    */
  def cross[A](cata: SchemeCata[F, S, A]): SchemeGetter[Seed, A] =
    new SchemeGetter[Seed, A](
      Machines.foldLayered[F, Seed, A](
        coalg,
        (_, fSeed, out) => cata.alg(Machines.rebuildLayer[F, Seed, A](fSeed, out)),
      )(using F)
    )

  /** MATERIALISING seam — `para` reads its node (`X = S`), so the tree is genuinely needed: build
    * the `S`, then fold it. Same `cross` spelling; the overload (driven by the argument's `X`)
    * picks this branch.
    */
  def cross[A](para: SchemePara[F, S, A]): SchemeGetter[Seed, A] =
    new SchemeGetter[Seed, A](seed => para.run(build(seed)))

/** Prototype constructors mirroring `Schemes.{cata, para, ana}` but on the `Scheme` carrier. */
object Proto:
  def cata[F[_], S, A](alg: F[A] => A)(using Traverse[F], Project[F, S]): SchemeCata[F, S, A] =
    new SchemeCata[F, S, A](alg)

  def para[F[_], S, A](alg: (S, F[A]) => A)(using Traverse[F], Project[F, S]): SchemePara[F, S, A] =
    new SchemePara[F, S, A](alg)

  def ana[F[_], Seed, S](coalg: Seed => F[Seed])(using
      Traverse[F],
      Embed[F, S],
  ): SchemeAna[F, Seed, S] =
    new SchemeAna[F, Seed, S](coalg)
