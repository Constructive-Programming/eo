package eo
package laws

import optics.Optic
import optics.Optic.*
import _root_.eo.data.Forgetful
import _root_.eo.data.Forgetful.given

/** Law equations for a `Getter[S, A]` — `Optic[S, Unit, A, A,
  * Forgetful]`.
  *
  * A `Getter` is a read-only optic: its `from` side is fixed to
  * `Unit` so no write path exists. The only observable behaviour is
  * `get`, which makes the meaningful law a *constructor-correctness*
  * check: `getter.get(s)` must equal whatever reference function the
  * caller claims the getter represents. This rules out plumbing
  * mistakes like swapping `_._1` / `_._2` or accidentally threading
  * the input through `identity`.
  *
  * Compared to Monocle's `GetterLaws`, we drop the fold-consistency
  * law (`getter.fold.getAll(s).headOption == Some(getter.get(s))`)
  * because the `Composer[Forgetful, Forget[F]]` bridge does not exist
  * in core — the morph path from Getter to Fold is not available at
  * this version. Revisit when a new carrier adds that Composer.
  */
trait GetterLaws[S, A]:
  def getter: Optic[S, Unit, A, A, Forgetful]

  /** The function this getter is declared to represent. Concrete
    * subclass supplies this — typically the same `S => A` that was
    * passed to `Getter.apply`. */
  def reference: S => A

  /** `getter.get(s)` equals the reference function applied at `s`. */
  def getConsistent(s: S): Boolean =
    getter.get(s) == reference(s)
