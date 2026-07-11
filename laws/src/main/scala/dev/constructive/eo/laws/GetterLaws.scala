package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Direct

import optics.Optic
import optics.Optic.*

/** Law equations for a `Getter[S, A]` — `Optic[S, Unit, A, Unit, Direct]`.
  *
  * A `Getter` is a read-only optic: both its leftover `T` and its back-focus `B` are `Unit` so no
  * write path exists (and so Getters compose through the ordinary `andThen`). The only observable
  * behaviour is `get`, which makes the meaningful law a *constructor-correctness* check:
  * `getter.get(s)` must equal whatever reference function the caller claims the getter represents.
  * This rules out plumbing mistakes like swapping `_._1` / `_._2` or accidentally threading the
  * input through `identity`.
  *
  * Compared to Monocle's `GetterLaws`, we omit the fold-consistency law
  * (`getter.fold.getAll(s).headOption == Some(getter.get(s))`). The `Composer[Direct, Forget[F]]`
  * bridge it needs exists ([[dev.constructive.eo.optics.Unfold]] made `Forget[F]`'s `from` a sound
  * singleton-pick), so a Getter→Fold morph is available; the law is simply not yet restated here.
  */
trait GetterLaws[S, A]:
  /** The optic under test. */
  def getter: Optic[S, Unit, A, Unit, Direct]

  /** The function this getter is declared to represent. Concrete subclass supplies this — typically
    * the same `S => A` that was passed to `Getter.apply`.
    */
  def reference: S => A

  /** `getter.get(s)` equals the reference function applied at `s`. */
  def getConsistent(s: S): Boolean =
    getter.get(s) == reference(s)
