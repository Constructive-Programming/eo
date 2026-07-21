package dev.constructive.eo.laws

/** Discipline `RuleSet` bundles for the optic-family laws in [[dev.constructive.eo.laws]] — one
  * `*Tests` class per optic shape. Each is an ''abstract class'' wired by overriding its `laws`
  * member (no companion `apply`), then asking for the family's rule set:
  *
  * {{{
  * checkAll(
  *   "Lens[(Int,String), Int] — first projection",
  *   new LensTests[(Int, String), Int]:
  *     val laws = new LensLaws[(Int, String), Int]:
  *       val lens = firstLens
  *   .lens,
  * )
  * }}}
  *
  * Pick the rule set by the optic's CARRIER, not its constructor: `Tuple2` → [[LensTests]],
  * full-cover `Either` → [[PrismTests]], `Affine` → [[OptionalTests]] (or [[AffineFoldTests]] for
  * the read-only face). And ALWAYS run [[SeamTests]] on a DRILLED / partial-cover optic of the
  * carrier — full-cover fixtures pass its write-seam laws trivially and miss the sibling-drop bug
  * class.
  *
  * `checkAll` comes from `org.typelevel:discipline-specs2` (the `Discipline` trait — a test-side
  * dependency your suite adds), and every rule-set method takes YOUR `Arbitrary` / `Cogen` givens:
  * this module ships no generators.
  */
package object discipline
