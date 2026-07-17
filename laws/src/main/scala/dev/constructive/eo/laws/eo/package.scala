package dev.constructive.eo.laws

/** Laws specific to eo's composition machinery rather than any single optic (`ComposeLaws`):
  *
  *   - capability-keyed compose-coherence — `get` / `getOption` / `foldMap` / `reverseGet` each
  *     distribute through `andThen` (`Composed*Laws`), stated once per capability and reused across
  *     every family and cross-family cell that admits it;
  *   - `andThen` associativity for `modify` (and `get` on total-read carriers)
  *     (`ComposeAssociativityLaws`) — the identity unit law is intentionally absent because core
  *     ships no identity-optic constructor to compose against;
  *   - the per-family pair laws (`LensComposeLaws` / `IsoComposeLaws` / `PrismComposeLaws` /
  *     `OptionalComposeLaws`), which now delegate their shared members to the capability layer;
  *
  * plus fold/traverse consistency, `modifyA` coherence, reverse/transform round-trips, and `Morph`
  * faithfulness.
  */
package object eo
