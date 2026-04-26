package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen, Prop}

/** Shared `(name, Prop)` builders for the optic law families. Several discipline `*Tests` RuleSets
  * have a common 4-prop subset — `modify identity`, `compose modify`, `replace idempotent`,
  * `consistent replace-modify` — that they each spelled out in identical `forAll` form.
  *
  * '''2026-04-26 dedup.''' Each builder takes the ''per-family'' law method as a function value, so
  * the `forAll` wrapping happens once. The discipline RuleSet bodies that used to spell three to
  * six identical forAlls in a row now stitch a list of named props together. The user-facing name
  * strings ("modify identity", "compose modify", …) and the actual property predicates are
  * unchanged — only the surrounding boilerplate moves here.
  */
private[discipline] object OpticLawProps:

  /** `"modify identity" -> forAll((s: S) => check(s))`. Used by every modify-bearing law family. */
  def modifyIdentity[S: Arbitrary](check: S => Boolean): (String, Prop) =
    "modify identity" -> forAll((s: S) => check(s))

  /** `"compose modify" -> forAll((s: S, f: A => A, g: A => A) => check(s, f, g))`. */
  def composeModify[S: Arbitrary, A: Arbitrary: Cogen](
      check: (S, A => A, A => A) => Boolean
  ): (String, Prop) =
    "compose modify" -> forAll((s: S, f: A => A, g: A => A) => check(s, f, g))

  /** `"replace idempotent" -> forAll((s: S, a: A) => check(s, a))`. */
  def replaceIdempotent[S: Arbitrary, A: Arbitrary](
      check: (S, A) => Boolean
  ): (String, Prop) =
    "replace idempotent" -> forAll((s: S, a: A) => check(s, a))

  /** `"consistent replace-modify" -> forAll((s: S, a: A) => check(s, a))`. */
  def consistentReplaceModify[S: Arbitrary, A: Arbitrary](
      check: (S, A) => Boolean
  ): (String, Prop) =
    "consistent replace-modify" -> forAll((s: S, a: A) => check(s, a))
