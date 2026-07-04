package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Affine

import optics.Optic
import optics.Optic.*

/** Laws that exercise an Optional's WRITE SEAM — the generic `Optic.modify` / `Optic.replace`
  * (`from(map(to(s), _))`), the path an UPCAST or COMPOSED write actually takes, NOT any
  * concrete-class convenience surface that might sidestep `from`.
  *
  * '''Run these on a DRILLED / partial-cover optic''' — a focus with surrounding context (a field
  * of a larger record, an element beside siblings). A carrier whose `from` reconstructs the focus
  * STANDALONE (dropping the context) fails [[seamModifyIdentity]] here, even though a full-cover
  * fixture passes it trivially. That is precisely the class of bug the full-cover-only Prism /
  * Optional discipline suites could never catch — the 2026-07 avro/circe record-face sibling-drop:
  * `Either`-carried drilled prisms whose `from(Right(b)) = reverseGet(b)` threw the siblings away
  * on every composed / upcast write. This law makes that failure loud for any carrier.
  *
  * Equality is INJECTED because some carriers' `S` has no lawful universal `==`: avro
  * `IndexedRecord` uses schema-instance-sensitive `equals`, so pass a structural comparison; circe
  * `Json` and most value types can pass `_ == _`.
  */
trait SeamLaws[S, A]:

  /** The optic under test, as its BARE `Optic` supertype — so `.modify` / `.replace` resolve to the
    * generic seam extension, not a shadowing member.
    */
  def optic: Optic[S, S, A, A, Affine]

  /** Structural equality on `S`. */
  def eqv: (S, S) => Boolean

  /** get-put through the seam: a no-op modify must round-trip the WHOLE structure, context
    * included. The sibling-drop bug fails exactly here on a drilled optic.
    */
  def seamModifyIdentity(s: S): Boolean = eqv(optic.modify(identity[A])(s), s)

  /** compose-modify through the seam. */
  def seamComposeModify(s: S, f: A => A, g: A => A): Boolean =
    eqv(optic.modify(g)(optic.modify(f)(s)), optic.modify(f.andThen(g))(s))

  /** put-put through the seam: the last replace wins, with the surrounding context preserved. */
  def seamReplaceOverwrite(s: S, a1: A, a2: A): Boolean =
    eqv(optic.replace(a2)(optic.replace(a1)(s)), optic.replace(a2)(s))
