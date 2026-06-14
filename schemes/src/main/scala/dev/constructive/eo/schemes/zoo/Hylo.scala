package dev.constructive.eo
package schemes
package zoo

import optics.Optic

/** Hylomorphism citizen — the fused refold worn as an optic over [[Scheme]] with `X = Nothing`.
  * `Getter`-shaped (`Optic[Seed, Unit, A, Unit, Scheme]`), consumed via `.get`. Built by
  * [[Ana.cross]] or [[Schemes.hylo]]; carries only the fused `refold`, no tree. Not a primitive —
  * it *is* `ana.cross(cata)`.
  */
final class Hylo[Seed, A](private[zoo] val refold: Seed => A)
    extends Optic[Seed, Unit, A, Unit, Scheme]:
  type X = Nothing
  def to(s: Seed): Scheme[X, A] = Scheme(refold(s))
  def from(b: Scheme[X, Unit]): Unit = ()
