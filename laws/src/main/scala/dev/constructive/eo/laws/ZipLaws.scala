package dev.constructive.eo
package laws

import optics.Optic
import optics.Optic.*

/** Law equations for a zipped (fanout) lens `l1.zip(l2)` — [[optics.Optic.zip]].
  *
  * `zip` is only conditionally lawful: the composite is a well-behaved `Lens[S, (A, C)]` ''iff the
  * two legs focus disjoint parts of `S`''. This set is the runnable certificate of that obligation
  * (the same posture as Haskell's `Control.Lens.Unsound.lensProduct`): instantiate it with the pair
  * you intend to zip, and `checkAll` fails loudly when the foci overlap.
  *
  * The `get-replace` / `replace-get` round-trips are the two that break on overlap — self-zipping
  * one lens (`l.zip(l)`) makes `replace((a1, a2))` write `a1` then `a2`, so `get` reads back
  * `(a2, a2)` and `replace-get` fails. The three `*-stable` / `writes-commute` equations pin the
  * independence that makes the round-trips hold, so a failure localises to the offending leg.
  */
trait ZipLaws[S, A, C]:
  def l1: Optic[S, S, A, A, Tuple2]
  def l2: Optic[S, S, C, C, Tuple2]

  final def zipped: Optic[S, S, (A, C), (A, C), Tuple2] = l1.zip(l2)

  /** Reading the zip is the pair of the leg reads. Definitional; holds unconditionally. */
  def getConsistent(s: S): Boolean =
    zipped.get(s) == ((l1.get(s), l2.get(s)))

  /** Classic lens `get`-then-`replace` round-trip on the composite. Breaks under overlap. */
  def getReplace(s: S): Boolean =
    zipped.replace(zipped.get(s))(s) == s

  /** Classic lens `replace`-then-`get` round-trip on the composite. Breaks under overlap. */
  def replaceGet(s: S, ac: (A, C)): Boolean =
    zipped.get(zipped.replace(ac)(s)) == ac

  /** Disjointness — the two leg writes commute. */
  def writesCommute(s: S, a: A, c: C): Boolean =
    l2.replace(c)(l1.replace(a)(s)) == l1.replace(a)(l2.replace(c)(s))

  /** Disjointness — leg-1's focus is stable under a leg-2 write. */
  def read1StableUnder2(s: S, c: C): Boolean =
    l1.get(l2.replace(c)(s)) == l1.get(s)

  /** Disjointness — leg-2's focus is stable under a leg-1 write. */
  def read2StableUnder1(s: S, a: A): Boolean =
    l2.get(l1.replace(a)(s)) == l2.get(s)
