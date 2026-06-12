# Phase 7 — Law-scan (model: opus — MANDATORY, every run; promotion is user-gated)

The holistic pass the first two runs never performed: they only considered
law promotion opportunistically, per-mutant, and surfaced zero candidates.
This phase reads ACROSS the whole suite and the algebra, and must end with a
ranked candidate list — possibly empty, but only with a written justification
("scanned N property shapes across M files; all instances of existing laws
because ...").

## Where candidates hide (scan all of these)

1. **Repeated property shapes across instances.** The same `forAll` body
   appearing for two+ carriers/families, differing only in types, is a law
   wearing a property costume. (`CheckAllHelpers` exists because the repo
   already de-duplicated *registrations*; this scan de-duplicates the
   *statements*.)
2. **Phase 6 leftovers.** Folds that wanted a parameterized helper across
   instance types — a helper generic enough to parameterize is usually a law.
3. **The algebra's unfilled obligations.** The composition matrix is a
   category: where are associativity (`(a ∘ b) ∘ c ≅ a ∘ (b ∘ c)` at carrier
   and read/write level) and identity (`Iso.id` composition) witnesses? Every
   carrier with `map` is a functor — is composition pinned? Fusion equalities
   the docs assert (`hylo` = `ana` then `cata`)? Naturality of carrier
   transformations? Check what `cats-eo-laws` ships against what the
   structures claim, and list the gaps even when no test exists yet.
4. **Behaviour specs asserting algebra.** `OpticsBehaviorSpec`-style examples
   that verify `composed.get == inner.get(outer.get(s))` ad-hoc are law
   instances; the matrix spec pins TYPES, but who pins the SEMANTICS of each
   composition cell?

## Vetting (before the user sees a candidate)

1. **State it formally**: name, signature, the equality, which existing laws
   nearly-imply it. Derivable from existing laws → redundant, drop it.
2. **Counterexample search**: run the candidate as a scratch `forAll` against
   EVERY instance shape currently registered at any `checkAll` site. A
   failure is reported verbatim either way — it means not-a-law OR an
   unlawful instance, and both findings go to the user.
3. **Blast radius**: which existing registrations inherit it (predicted
   kills), what clients gain, MiMa impact (`cats-eo-laws` is published —
   adding concrete trait methods is normally compatible; verify with
   `mimaReportBinaryIssues`, vacuous before 0.1.1).

## The gate (NEVER autonomous)

Present via AskUserQuestion: formal statement, evidence (N instance shapes ×
M samples, zero counterexamples), placement, predicted kills, options
**promote / keep as tests-only property / reject**. The user judges validity
(universally true for the abstraction, not incidentally true for today's
instances) and worth (a contract maintained forever). Unattended runs queue
candidates in state `pending_promotions` and continue.

## On approval — promotion mechanics

1. Law method → the matching trait in `laws/src/main/scala/.../laws/`
   (family laws at the root, `laws.data` carriers, `laws.typeclass`
   typeclasses, `laws.eo` EO-specific). Scaladoc states the equality and why
   it holds.
2. Ruleset wiring → the sibling `.discipline` `*Tests` class. Every existing
   `checkAll` site inherits it with zero test-file changes — the stage-3
   payoff.
3. `sbt test` — a newly-failing registered instance halts everything: either
   the law is wrong or the instance is unlawful; back to the user.
4. `sbt mimaReportBinaryIssues`.
5. Re-run mutation for affected modules — promotion kills are measured like
   any candidate.
6. Now-redundant tests-only properties are deleted (a stage-2→3 promotion is
   also a consolidation); surface the new law in the site docs laws listing.
