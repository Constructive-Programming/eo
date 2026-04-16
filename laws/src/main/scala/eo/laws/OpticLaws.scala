package eo
package laws

import optics.Optic
import optics.Optic.*
import data.{Affine, Forgetful, SetterF, Forget}
import data.Forgetful.given
import data.Affine.given
import data.SetterF.given

import cats.Functor
import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline-style law sets for EO optics, ported from Monocle's
  * `monocle.law.*Laws`. Each `*Laws` trait pins the law equations and each
  * `*Tests` class bundles them into a `RuleSet` that can be `checkAll`-ed
  * from a discipline-specs2 spec.
  *
  * Where Monocle uses `Iso[S, A]` we use `Optic[S, S, A, A, F]` with the
  * appropriate carrier `F` — the law content is the same.
  */
object OpticLaws:

  // -------- Iso --------

  trait IsoLaws[S, A]:
    def iso: Optic[S, S, A, A, Forgetful]

    def roundTripOneWay(s: S): Boolean =
      iso.reverseGet(iso.get(s)) == s

    def roundTripOtherWay(a: A): Boolean =
      iso.get(iso.reverseGet(a)) == a

    // modifyIdentity / composeModify for Iso are direct corollaries of the
    // two round-trip laws (modify = from . f . to), so we don't enumerate
    // them separately. They're also harder to express with EO's existential
    // encoding because the `Forgetful` type alias has no `Bifunctor` and so
    // doesn't reach the generic `ForgetfulFunctor` derivation that the
    // `modify` extension drives off.

  abstract class IsoTests[S, A] extends Laws:
    def laws: IsoLaws[S, A]

    def iso(using
        Arbitrary[S], Arbitrary[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Iso",
        "round trip one way" -> forAll((s: S) => laws.roundTripOneWay(s)),
        "round trip other way" -> forAll((a: A) => laws.roundTripOtherWay(a)),
      )

  // -------- Lens --------

  trait LensLaws[S, A]:
    def lens: Optic[S, S, A, A, Tuple2]

    def getReplace(s: S): Boolean =
      lens.replace(lens.get(s))(s) == s

    def replaceGet(s: S, a: A): Boolean =
      lens.get(lens.replace(a)(s)) == a

    def replaceIdempotent(s: S, a: A): Boolean =
      lens.replace(a)(lens.replace(a)(s)) == lens.replace(a)(s)

    def modifyIdentity(s: S): Boolean =
      lens.modify(identity[A])(s) == s

    def composeModify(s: S, f: A => A, g: A => A): Boolean =
      lens.modify(g)(lens.modify(f)(s)) == lens.modify(f andThen g)(s)

    def consistentReplaceModify(s: S, a: A): Boolean =
      lens.replace(a)(s) == lens.modify(_ => a)(s)

  abstract class LensTests[S, A] extends Laws:
    def laws: LensLaws[S, A]

    def lens(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Lens",
        "get-replace" -> forAll((s: S) => laws.getReplace(s)),
        "replace-get" -> forAll((s: S, a: A) => laws.replaceGet(s, a)),
        "replace idempotent" ->
          forAll((s: S, a: A) => laws.replaceIdempotent(s, a)),
        "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
        "compose modify" ->
          forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
        "consistent replace-modify" ->
          forAll((s: S, a: A) => laws.consistentReplaceModify(s, a)),
      )

  // -------- Prism --------

  trait PrismLaws[S, A]:
    def prism: Optic[S, S, A, A, Either]

    /** Mirror of Monocle's `Prism.getOption`. */
    def getOption(s: S): Option[A] = prism.to(s).toOption

    def partialRoundTripOneWay(s: S): Boolean =
      getOption(s) match
        case Some(a) => prism.reverseGet(a) == s
        case None    => true

    def roundTripOtherWay(a: A): Boolean =
      getOption(prism.reverseGet(a)) == Some(a)

    def modifyIdentity(s: S): Boolean =
      prism.modify(identity[A])(s) == s

    def composeModify(s: S, f: A => A, g: A => A): Boolean =
      prism.modify(g)(prism.modify(f)(s)) == prism.modify(f andThen g)(s)

    def consistentGetOptionModifyId(s: S): Boolean =
      // Monocle: optional.getOption(prism.modify(identity)(s)) === optional.getOption(s)
      getOption(prism.modify(identity[A])(s)) == getOption(s)

  abstract class PrismTests[S, A] extends Laws:
    def laws: PrismLaws[S, A]

    def prism(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Prism",
        "partial round trip one way" ->
          forAll((s: S) => laws.partialRoundTripOneWay(s)),
        "round trip other way" ->
          forAll((a: A) => laws.roundTripOtherWay(a)),
        "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
        "compose modify" ->
          forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
        "consistent getOption / modify-id" ->
          forAll((s: S) => laws.consistentGetOptionModifyId(s)),
      )

  // -------- Optional --------

  trait OptionalLaws[S, A]:
    def optional: Optic[S, S, A, A, Affine]

    /** Mirror of Monocle's `Optional.getOption`. */
    def getOption(s: S): Option[A] =
      optional.to(s).affine.toOption.map(_._2)

    def modifyIdentity(s: S): Boolean =
      optional.modify(identity[A])(s) == s

    def composeModify(s: S, f: A => A, g: A => A): Boolean =
      optional.modify(g)(optional.modify(f)(s)) == optional.modify(f andThen g)(s)

    def consistentGetOptionModifyId(s: S): Boolean =
      getOption(optional.modify(identity[A])(s)) == getOption(s)

  abstract class OptionalTests[S, A] extends Laws:
    def laws: OptionalLaws[S, A]

    def optional(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Optional",
        "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
        "compose modify" ->
          forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
        "consistent getOption / modify-id" ->
          forAll((s: S) => laws.consistentGetOptionModifyId(s)),
      )

  // -------- Setter --------

  trait SetterLaws[S, A]:
    def setter: Optic[S, S, A, A, SetterF]

    def modifyIdentity(s: S): Boolean =
      setter.modify(identity[A])(s) == s

    def composeModify(s: S, f: A => A, g: A => A): Boolean =
      setter.modify(g)(setter.modify(f)(s)) == setter.modify(f andThen g)(s)

    def replaceIdempotent(s: S, a: A): Boolean =
      setter.replace(a)(setter.replace(a)(s)) == setter.replace(a)(s)

    def consistentReplaceModify(s: S, a: A): Boolean =
      setter.replace(a)(s) == setter.modify(_ => a)(s)

  abstract class SetterTests[S, A] extends Laws:
    def laws: SetterLaws[S, A]

    def setter(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Setter",
        "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
        "compose modify" ->
          forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
        "replace idempotent" ->
          forAll((s: S, a: A) => laws.replaceIdempotent(s, a)),
        "consistent replace-modify" ->
          forAll((s: S, a: A) => laws.consistentReplaceModify(s, a)),
      )

  // -------- Traversal (each, list-shaped) --------
  //
  // For `Traversal.each` the carrier is `Forget[T]`, which has the right
  // ForgetfulFunctor / ForgetfulFold instances for the modify-side laws.
  // We deliberately do NOT port `getAll` / `headOption` here — EO's
  // `Optic.all` returns the whole container wrapped (cartesian-product
  // semantics from `traverse(List(_))`), not the individual elements,
  // so the Monocle phrasing of those laws would not be testing what its
  // name suggests.

  trait TraversalLaws[T[_], A](using val FT: Functor[T]):
    def traversal: Optic[T[A], T[A], A, A, Forget[T]]

    def modifyIdentity(s: T[A]): Boolean =
      traversal.modify(identity[A])(s) == s

    def composeModify(s: T[A], f: A => A, g: A => A): Boolean =
      traversal.modify(g)(traversal.modify(f)(s)) ==
        traversal.modify(f andThen g)(s)

    def replaceIdempotent(s: T[A], a: A): Boolean =
      traversal.replace(a)(traversal.replace(a)(s)) == traversal.replace(a)(s)

    def consistentReplaceModify(s: T[A], a: A): Boolean =
      traversal.replace(a)(s) == traversal.modify(_ => a)(s)

  abstract class TraversalTests[T[_]: Functor, A] extends Laws:
    def laws: TraversalLaws[T, A]

    def traversal(using
        Arbitrary[T[A]], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Traversal",
        "modify identity" -> forAll((s: T[A]) => laws.modifyIdentity(s)),
        "compose modify" ->
          forAll((s: T[A], f: A => A, g: A => A) =>
            laws.composeModify(s, f, g)
          ),
        "replace idempotent" ->
          forAll((s: T[A], a: A) => laws.replaceIdempotent(s, a)),
        "consistent replace-modify" ->
          forAll((s: T[A], a: A) => laws.consistentReplaceModify(s, a)),
      )
