package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Affine

import optics.Optic
import optics.Optic.*

/** Laws for a '''navigation optic''' — the `field(name)` / `at(i)` / `key(k)` / `variant(name)`
  * shape every untyped-tree kit ships (circe `Json`, kyo `Structure.Value`, zio-schema
  * `DynamicValue`, zio-json `Json`). These kits share a contract that no existing law suite states,
  * so each one used to re-assert it by hand in its own spec — and drift between them is exactly how
  * a bug survives in one kit after being fixed in another.
  *
  * Two equations on top of [[OptionalLaws]] / [[SeamLaws]]:
  *
  *   - [[putGet]] — writing then reading returns what was written. `OptionalLaws` deliberately
  *     omits it (Monocle parity), but for a keyed navigation optic it is the law that pins WHICH
  *     occurrence a write targets: an optic that reads the first duplicate key and writes all of
  *     them satisfies put-get, while one that reads the first and writes the LAST fails it.
  *   - [[missWriteIsNoOp]] — a write through a miss changes nothing. Every kit documents this
  *     ("misses pass through writes untouched"); nothing checked it. A carrier that invents the
  *     missing node — appending an absent field, growing a sequence to reach an out-of-range index
  *     — fails here, and that is a real hazard for any kit tempted to make `field` an upsert.
  *
  * Run them on a '''drilled''' optic and with a generator that actually produces misses (a record
  * without the field, a short sequence, a different variant); with hits only, `missWriteIsNoOp` is
  * vacuous. Equality is injected for the same reason as [[SeamLaws]]: some carriers' `S` has no
  * lawful universal `==` (avro `IndexedRecord`), and some have one that is not even reflexive
  * (zio-json's `Json.Obj` on duplicate keys maps the left operand before comparing).
  */
trait NavigationLaws[S, A]:

  /** The optic under test, as its bare `Optic` supertype so writes take the generic seam. */
  def navigation: Optic[S, S, A, A, Affine]

  /** Structural equality on `S`. */
  def eqv: (S, S) => Boolean

  /** Mirror of `Optional.getOption`, recovered from the `Affine` carrier. */
  def getOption(s: S): Option[A] =
    navigation.to(s).fold(_ => None, (_, a) => Some(a))

  /** put-get: on a hit, `getOption(replace(a)(s)) == Some(a)`. Vacuous on a miss. */
  def putGet(s: S, a: A): Boolean =
    getOption(s).isEmpty || getOption(navigation.replace(a)(s)) == Some(a)

  /** A write through a miss is the identity — no node is invented. Vacuous on a hit. */
  def missWriteIsNoOp(s: S, a: A): Boolean =
    getOption(s).nonEmpty || eqv(navigation.replace(a)(s), s)
