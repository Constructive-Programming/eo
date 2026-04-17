package eo
package optics

import data.Forgetful

object Iso:

  def apply[S, T, A, B](f: S => A, g: B => T) =
    BijectionIso[S, T, A, B](f, g)

/** Concrete Optic subclass for an isomorphism — a bijection between
  * `S` / `T` and `A` / `B`. Stores the forward and reverse functions
  * as private fields and exposes `get(s)` / `reverseGet(b)` as
  * single-method combined "field load + `Function1.apply`" operations,
  * matching the bytecode shape of Monocle's `PIso$$anon$3`:
  *
  * {{{
  *   public Object get(Object s):
  *     getfield  _fn
  *     aload_1
  *     invokeinterface Function1.apply
  * }}}
  *
  * Using plain `def` (not `val`) for `get` / `reverseGet` avoids the
  * extra `invokevirtual accessor()Function1` hop that a `val get: S
  * => A` would introduce at every call site. Returned by [[Iso.apply]]
  * so every hand-written iso picks up the fused path without a type
  * annotation.
  */
final class BijectionIso[S, T, A, B](
    _get: S => A,
    _reverseGet: B => T,
) extends Optic[S, T, A, B, Forgetful]:
  type X = Nothing
  def to: S => A     = _get
  def from: B => T   = _reverseGet
  def get(s: S): A   = _get(s)
  def reverseGet(b: B): T = _reverseGet(b)
  inline def modify(f: A => B): S => T =
    s => _reverseGet(f(_get(s)))
  inline def replace(b: B): S => T =
    val t = _reverseGet(b)
    _ => t
