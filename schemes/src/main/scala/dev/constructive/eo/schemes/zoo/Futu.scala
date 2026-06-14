package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import optics.Optic

/** Futumorphism citizen — a multi-layer unfold worn as an optic over [[Scheme]] with **`X =
  * Coattr[F, A]`**, the free monad `μX. A + F[X]`. The build-side mirror of [[Histo]]: where the
  * cofree comonad is the universal index for folds, the free monad is the universal index for
  * unfolds. `Ana` is the same optic at the resolution `X = S`; `Futu` refines it so the coalgebra
  * may emit several layers per step.
  *
  * `coalg: A => F[Coattr[F, A]]` answers each child slot with either [[Coattr.Pure]] (a seed the
  * engine keeps unfolding) or [[Coattr.Roll]] (a prebuilt layer unrolled with **no** further
  * coalgebra call) — so one step can produce more than one layer of structure. `Review`-shaped
  * (`Optic[Unit, S, Unit, A, Scheme]`), consumed via `.reverseGet`; the root seed enters as
  * `Coattr.Pure`. Stack-safe (the [[Machines.foldLayered]] machine). An all-`Pure` coalgebra
  * (`map(coalg(_))(Coattr.Pure(_))`) degenerates to [[Ana]].
  */
final class Futu[F[_], A, S](private[zoo] val coalg: A => F[Coattr[F, A]])(using
    F: Traverse[F],
    E: Embed[F, S],
) extends Optic[Unit, S, Unit, A, Scheme]:
  type X = Coattr[F, A]

  private val build: A => S =
    val expand: Coattr[F, A] => F[Coattr[F, A]] =
      case Coattr.Pure(a)     => coalg(a)
      case Coattr.Roll(layer) => layer
    val run = Machines.foldLayered[F, Coattr[F, A], S](expand, (_, fr) => E.embed(fr))
    a => run(Coattr.Pure(a))

  def to(u: Unit): Scheme[X, Unit] = Scheme(())
  def from(b: Scheme[X, A]): S = build(Scheme.value(b))

  /** The fused chrono seam: futu ∘ histo — **hylo at the universal indices**. The build threads the
    * free monad ([[Coattr]]), the fold the cofree comonad ([[Attr]]); fused, the intermediate `S` is
    * never built (mirrors [[Ana.cross]] for the trivial indices). Delegates to [[Schemes.chrono]],
    * which needs only `Traverse[F]` — the `Embed`/`Project` carried by `this`/`histo` go unused.
    *
    * A member (not the generic `Optic.cross`, which would `reverse.andThen` and materialise) so the
    * fused chrono wins overload resolution.
    */
  def cross[B](histo: Histo[F, S, B]): Hylo[A, B] =
    Schemes.chrono[F, A, B](coalg, histo.alg)(using F)
