package dev.constructive.eo
package schemes
package zoo

import cats.Monad

import data.{Forget, ForgetK}
import optics.Optic

/** Generic effectful-fold citizen: `run: S => M[A]`. What `cataM` / `anaM` / `hyloM` return.
  *
  * Effectful scheme citizens — the M-generic drivers' return types. Computational steps evolve in a
  * `Monad[M]` (the arbo `Calculator` shape: fetching a node's children is effectful), and the
  * results are **`Forget[M]`-carried** optics: `S => M[A]` worn as `Optic[S, Unit, A, Unit,
  * Forget[M]]`, composing same-carrier through `assocForgetMonad`.
  *
  * Consumption: effect Ms (IO, State, …) have no `Foldable`, so the Foldable-gated Fold operations
  * (`.foldMap`/`.headOption`) and ReadCompose cells do NOT apply — the public consumption surface
  * is the stored [[FoldM.run]] (not raw `.to`). An Accessor-into-M capability is follow-up
  * material.
  *
  * Supported Ms are **single-pass and linear** — the lifted machine threads mutable state (the
  * frame deque, in-place child arrays), so a branching/replaying `M` (`List`, retrying or streaming
  * effects) would share that state across branches and corrupt the fold. See the linear-M boundary
  * test in `SchemesMSpec`. A persistent-state variant is deferred until a real consumer needs one.
  *
  * Re-forcing the same `M[A]` value returned by [[FoldM.run]] is safe — each force allocates its
  * own fresh mutable state (the frame deque is allocated inside the `M`, not before it). Concurrent
  * forcing of a single `M[A]` value remains unsupported; each `run(s)` call is independent.
  *
  * Composition: `anaM.andThen(cataM)` Kleisli-chains two `FoldM`s (`M.flatMap`) into the
  * materialising effectful hylo (`M[S]` built, then folded); `Schemes.hyloM` is the fused one-pass
  * spelling (no `M[S]`). NB the M rung composes via `andThen` (both `cataM` and `anaM` are Kleisli
  * arrows `_ => M[_]` — see [[Schemes.anaM]] on why the effect collapses the read/build duality),
  * whereas the pure rung composes a `Review` (`ana`) into a `Getter` (`cata`) via `cross`.
  *
  * `FoldM` is a concrete, public citizen: `cataM` / `anaM` / `hyloM` all return it, and users may
  * wrap their own `S => M[A]` as one.
  */
class FoldM[M[_], S, A](val run: S => M[A]) extends Optic[S, Unit, A, Unit, Forget[M]]:
  type X = Nothing

  def to(s: S): Forget[M][X, A] = ForgetK(run(s))
  def from(d: Forget[M][X, Unit]): Unit = ()

  /** The focus-seam composition of two effectful reads — the **materialising** effectful hylo:
    * `run` this `FoldM` fully to `M[A]`, then Kleisli-chain into `inner` (`M.flatMap`). A concrete
    * same-type member (mirroring the pure `Getter.andThen`), so it is strictly more specific than
    * the generic `Optic.andThen` overloads — `anaM.andThen(cataM)` resolves here and stays a
    * `FoldM`, no ascription needed. Requires `Monad[M]` (the effect's bind); `Schemes.hyloM` is the
    * fused one-pass spelling that builds no intermediate `M[A]`.
    */
  def andThen[C](inner: FoldM[M, A, C])(using M: Monad[M]): FoldM[M, S, C] =
    new FoldM[M, S, C](s => M.flatMap(run(s))(inner.run))
