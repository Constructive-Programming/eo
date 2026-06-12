package dev.constructive.eo
package schemes
package zoo

import data.Direct
import optics.{Getter, Optic}

/** Fold-scheme citizen: Getter-shaped, carrying the node-supplied algebra for fusion.
  *
  * Concrete scheme citizens — `cata`/`ana` return these instead of bare `Getter`/`Review` so
  * composition can **fuse**: the classes carry their (co)algebra and instances as data, and the
  * fused `cross` overload on [[Ana]] resolves on the concrete types.
  *
  * They extend the open `Optic` trait directly (`Getter`/`Review` are `final` in core — the
  * perf-pinned encoding stays untouched): full generic composition via the trait members, plus
  * `.get` / `.reverseGet` as stored fields, the use-site-friendly shape.
  *
  * Widening hazard, documented: binding a `Cata` (or `Ana`) to a wider type loses the fused `cross`
  * overload — the generic trait `cross` still typechecks and is extensionally equal, but
  * materializes the full intermediate structure. `Schemes.hylo(coalg, alg)` stays the always-fused
  * spelling.
  */
final class Cata[F[_], S, A] private[schemes] (
    val get: S => A,
    private[schemes] val alg: (S, F[A]) => A,
) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Nothing

  def to(s: S): Direct[X, A] = Direct(get(s))
  def from(d: Direct[X, Unit]): Unit = ()

  /** View as a plain [[Getter]] — re-enters Getter's fused composition fast paths (and resolves the
    * read-compose overload tie an unascribed `getter.andThen(cata(...))` can hit).
    */
  def asGetter: Getter[S, A] = Getter(get)
