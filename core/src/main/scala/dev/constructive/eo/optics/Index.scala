package dev.constructive.eo
package optics

import scala.collection.immutable.SeqOps

/** Monocle's `Index` as a plain constructor object — NOT a typeclass. `Index(i)` / `Index(k)`
  * builds an ordinary [[Optional]] focusing one positional or keyed slot; there is no
  * `Index[S, I, A]` typeclass to instance. Monocle `index` semantics hold: the focus is the slot's
  * CURRENT value, so a write on an absent slot (index out of bounds, key missing) is a silent
  * pass-through — `Index` never inserts. For insert-or-update-or-delete on a `Map`, use [[At]],
  * whose focus is the `Option[V]`.
  *
  * Bare `Index(0)` cannot pick between the sequence and map overloads on its own — supply the type
  * arguments (`Index[Vector, Int](0)`, `Index[String, Long]("k")`) or let `.andThen` composition
  * pin the source type.
  *
  * @group Optics
  */
object Index:

  /** Optional focusing the `i`-th element of an immutable sequence (`List`, `Vector`, …). Out of
    * bounds (or negative `i`) is a miss: reads see nothing, writes pass through.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val second = Index[Vector, Int](1)
    * second.getOption(Vector(1, 2, 3))    // Some(2)
    * second.replace(9)(Vector(1, 2, 3))   // Vector(1, 9, 3)
    * second.replace(9)(Vector.empty[Int]) // Vector() — no insert
    *   }}}
    */
  def apply[CC[X] <: SeqOps[X, CC, CC[X]], A](i: Int): Optional[CC[A], CC[A], A, A] =
    Optional[CC[A], CC[A], A, A](
      s => Either.cond(s.isDefinedAt(i), s(i), s),
      { case (s, a) => if s.isDefinedAt(i) then s.updated(i, a) else s },
    )

  /** Optional focusing the value at key `k` of a `Map`. A missing key is a miss: reads see nothing,
    * writes pass through (no insert — that is [[At]]'s job).
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val port = Index[String, Int]("port")
    * port.getOption(Map("port" -> 80))  // Some(80)
    * port.replace(8080)(Map.empty)      // Map() — no insert
    *   }}}
    */
  def apply[K, V](k: K): Optional[Map[K, V], Map[K, V], V, V] =
    Optional[Map[K, V], Map[K, V], V, V](
      m => m.get(k).toRight(m),
      { case (m, v) => if m.contains(k) then m.updated(k, v) else m },
    )
