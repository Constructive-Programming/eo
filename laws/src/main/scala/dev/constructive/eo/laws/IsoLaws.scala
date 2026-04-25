package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Forgetful
import _root_.dev.constructive.eo.data.Forgetful.given

import optics.Optic
import optics.Optic.*

/** Law equations for an `Iso[S, A]` — `Optic[S, S, A, A, Forgetful]`.
  *
  * Ported from Monocle's `monocle.law.IsoLaws`. Where Monocle spells `iso.get(s)` /
  * `iso.reverseGet(a)` directly, EO's iso is an `Optic` whose `Forgetful` carrier yields the same
  * operations through the `Accessor` / `ReverseAccessor` extensions. The content of the laws is
  * identical; only the carrier spelling changes.
  *
  * `modifyIdentity` / `composeModify` for Iso are direct corollaries of the two round-trip laws
  * (`modify = from ∘ f ∘ to`), so this trait does not enumerate them separately. They are also
  * awkward to express with EO's existential encoding because the `Forgetful` type alias has no
  * `Bifunctor`, so it does not reach the generic `ForgetfulFunctor` derivation that the `modify`
  * extension drives off.
  */
trait IsoLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  def roundTripOneWay(s: S): Boolean =
    iso.reverseGet(iso.get(s)) == s

  def roundTripOtherWay(a: A): Boolean =
    iso.get(iso.reverseGet(a)) == a
