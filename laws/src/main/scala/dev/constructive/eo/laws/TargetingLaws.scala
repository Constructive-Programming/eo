package dev.constructive.eo
package laws

import dev.constructive.eo.forgetful.ForgetfulFunctor

import optics.Optic

/** The extrinsic read agreement, independent of the optic's shape — shared by [[ReadTargetingLaws]]
  * (a read-only optic, `Optic[S, Unit, A, Unit, F]`) and [[TargetingLaws]] (a read-write one,
  * `Optic[S, S, A, A, F]`), whose `T` / `B` slots differ and so cannot share an `optic` member.
  *
  * '''Why extrinsic.''' Every other law in this module relates the optic to itself: get-put,
  * put-get, put-put, modify fusion, the seam round-trips. An optic whose resolution mechanism
  * (`.field(_.b)` by name, `.at(i)`, a schema-slot lookup) lands on the WRONG slot is a perfectly
  * lawful optic onto that wrong slot — it reads what it writes, writes what it reads, and preserves
  * every sibling. No intrinsic law can see the mistake, because the mistake is not a violation of
  * the optic's own contract; it is a disagreement with the data structure about WHICH slot the
  * contract is about. The defect is visible only against a reference the optic did not produce:
  * "what does this carrier's own accessor say field `b` is?".
  */
trait ReadTargeting[S, A]:

  /** The optic's OWN read, normalised to `Option` — `optic.getOption(s)` for a maybe-hitting
    * carrier (`Affine`, `Either`), `Some(optic.get(s))` for a total one (`Tuple2`, `Direct`).
    * Deliberately the optic's read, not [[refGet]]: comparing `refGet` with itself would make
    * [[readTargets]] vacuous.
    */
  def read(s: S): Option[A]

  /** EXTRINSIC reference read — what the carrier's own accessor says the focus is at `s`. Written
    * WITHOUT the optic under test: a decoder's field read, a schema-slot lookup, a hand-written
    * projection.
    */
  def refGet: S => Option[A]

  /** Read-targeting: the optic reads exactly the slot the reference names — same presence, same
    * value. Catches a resolution mechanism aimed at the wrong field, a silent miss where the
    * reference hits, and a bogus hit where the reference misses.
    */
  def readTargets(s: S): Boolean = read(s) == refGet(s)

/** Read-targeting for a READ-ONLY optic (`Getter`, `PickFold`, `Fold`, a composed read-collapse) —
  * [[ReadTargeting]] with the optic it is about. [[GetterLaws.getConsistent]] states the same
  * agreement for a total read; this trait states it for a read-only optic that can miss.
  *
  * Register [[discipline.ReadTargetingTests]] beside the intrinsic suite for the same optic, on a
  * generator that produces real data: values that all miss the reference make the law vacuous.
  */
trait ReadTargetingLaws[S, A, F[_, _]] extends ReadTargeting[S, A]:

  /** The optic under test — read-only, so its result `T` and back-focus `B` are both `Unit`. */
  def optic: Optic[S, Unit, A, Unit, F]

/** Read-targeting for a READ-WRITE optic (`Lens`, `Iso`, `Prism`, `Optional`, …) — the read
  * agreement plus the two write equations.
  *
  *   - [[readTargets]] — the read lands on the reference's slot.
  *   - [[writeTargets]] — `replace` lands on the reference's slot, leaves every sibling exactly as
  *     `refReplace` does, and is a no-op where the reference misses.
  *   - [[modifyTargets]] — `modify` equals the reference's read-modify-write. This is NOT implied
  *     by the other two on a carrier whose write path re-reads a different slot than the one it
  *     writes (a write that reads field `a` and writes field `b` can satisfy both pointwise laws
  *     for some values); it is the law that pins the composite.
  *
  * '''Usage.''' Register [[discipline.TargetingTests]] beside the intrinsic suite for the same
  * optic (`LensTests`, `OptionalTests`, `NavigationTests`, `SeamTests`), on a generator that
  * produces real data. Structural equality is INJECTED for the same reason as [[SeamLaws]] and
  * [[NavigationLaws]] — some carriers' `S` has no lawful universal `==` (avro `IndexedRecord`,
  * `Array[Byte]`).
  */
trait TargetingLaws[S, A, F[_, _]](using forgetful: ForgetfulFunctor[F])
    extends ReadTargeting[S, A]:

  /** The optic under test, as a monomorphic read-write optic. */
  def optic: Optic[S, S, A, A, F]

  /** Structural equality on `S` (injected: some carriers' `S` has no lawful universal `==`). */
  def eqv: (S, S) => Boolean

  /** EXTRINSIC reference write — put `a` in the slot [[refGet]] reads, leaving every other slot as
    * it was; on a reference miss, return the source unchanged. Written WITHOUT the optic under test
    * (typically the carrier's own `copy` / codec / record builder).
    */
  def refReplace: (S, A) => S

  /** Write-targeting: the optic's `replace` writes exactly where the reference writes, and every
    * sibling ends up as `refReplace` leaves it. On a reference miss the write must leave `s`
    * unchanged — a resolution mechanism that invents the missing slot fails here.
    */
  def writeTargets(s: S, a: A): Boolean =
    eqv(optic.replace(a)(s), refGet(s).fold(s)(_ => refReplace(s, a)))

  /** Modify-targeting: the optic's `modify` equals the reference read-modify-write — `f` applied to
    * the reference's focus, then written back by the reference. On a reference miss the source is
    * returned unchanged, so `f` is never observed.
    */
  def modifyTargets(s: S, f: A => A): Boolean =
    eqv(optic.modify(f)(s), refGet(s).fold(s)(a => refReplace(s, f(a))))
