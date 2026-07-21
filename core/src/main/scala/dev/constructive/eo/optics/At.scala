package dev.constructive.eo
package optics

/** Monocle's `At` as a plain constructor object — NOT a typeclass. `At(k)` builds an ordinary total
  * [[Lens]] from a `Map[K, V]` to the `Option[V]` at key `k`: replacing with `Some(v)` inserts or
  * updates, replacing with `None` deletes the key. The Lens laws hold for every key (get-replace,
  * replace-get, replace-replace). When you only want to touch an EXISTING value — never insert —
  * use [[Index]], whose focus is the bare `V`.
  *
  * @group Optics
  */
object At:

  /** Total Lens to the `Option[V]` at key `k` — `Some` inserts/updates, `None` deletes.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val port = At[String, Int]("port")
    * port.get(Map.empty)                        // None
    * port.replace(Some(80))(Map.empty)          // Map("port" -> 80)
    * port.replace(None)(Map("port" -> 80))      // Map()
    *   }}}
    */
  def apply[K, V](k: K): GetReplaceLens[Map[K, V], Map[K, V], Option[V], Option[V]] =
    Lens[Map[K, V], Option[V]](
      _.get(k),
      (m, ov) => ov.fold(m - k)(v => m.updated(k, v)),
    )
