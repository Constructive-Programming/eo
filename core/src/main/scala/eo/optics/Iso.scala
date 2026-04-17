package eo
package optics

import data.Forgetful

object Iso:

  def apply[S, T, A, B](f: S => A, g: B => T) =
    BijectionIso[S, T, A, B](f, g)

/** Concrete Optic subclass for an isomorphism — a bijection between
  * `S` / `T` and `A` / `B`. Stores `get: S => A` and `reverseGet: B => T`
  * directly as fields so the hot path never goes through the
  * `Accessor[Forgetful]` / `ReverseAccessor[Forgetful]` type-class
  * dispatches that the generic `Optic` extensions would otherwise
  * perform.
  *
  * Mirrors Monocle's `case class Iso[S, A](get: S => A, reverseGet:
  * A => S)` storage shape, which is the reason their 1.5-ns hot paths
  * are reachable.
  *
  * Returned by [[Iso.apply]] so every hand-written iso picks up the
  * fused path without a type annotation at the call site.
  */
final class BijectionIso[S, T, A, B](
    val get: S => A,
    val reverseGet: B => T,
) extends Optic[S, T, A, B, Forgetful]:
  type X = Nothing
  inline def to: S => A = get
  inline def from: B => T = reverseGet
  inline def modify(f: A => B): S => T =
    s => reverseGet(f(get(s)))
  inline def replace(b: B): S => T =
    val t = reverseGet(b)
    _ => t
