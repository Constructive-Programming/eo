// MiMa setup — binary-compatibility checking, wired transitively
// through sbt-typelevel-ci-release.
//
// MiMa is ON from 1.0.0. There is no `tlMimaPreviousVersions` override
// here: sbt-typelevel's default computes the previous versions from git
// tags — every earlier non-prerelease `vX.Y.Z` tag that early-semver says
// the current version must stay binary-compatible with (same major, and
// major > 0). So 1.0.0 itself compares against nothing (every earlier tag
// is 0.x), and every later 1.x release is checked against 1.0.0 and each
// 1.x release after it. `mimaReportBinaryIssues` runs in CI.
//
// Exception: `cats-eo-kyo` is EXPERIMENTAL and opts out
// (`kyoIntegration / tlMimaPreviousVersions := Set.empty` in build.sbt):
// it builds on kyo 1.0.0-RC7, a release candidate. Lift that override once
// kyo 1.0.0 final ships.
//
// When a 1.x change trips MiMa, the default answer is: don't make it on
// 1.x. Accompany a genuinely internal change with a justified
// ProblemFilter, e.g.
//
//   ThisBuild / mimaBinaryIssueFilters ++= Seq(
//     ProblemFilters.exclude[MissingClassProblem]("dev.constructive.eo.internal.Foo"),
//   )
//
// Only exclude symbols that are genuinely internal (`private` or
// `private[pkg]`); exclusions on the public API are binary-compat
// breakage and belong in a major version bump instead.
//
// History. MiMa was disabled across the 0.x line: every 0.x minor was a
// deliberate breaking release, kept here for reference, newest first.
//
//   1.0.0 (breaking vs 0.18; the changes listed for 0.19 before 1.0 was
//         chosen, which was never released, ship here):
//         (a) `Optional` becomes a sealed abstract class with an
//         existential intermediate (`type M`, `X = (T, M)`) so every write
//         tears its source once; `new Optional(...)` is gone (use
//         `Optional.apply`) and `getOrModify` / `reverseGet` are methods
//         instead of constructor vals (source-compatible for callers,
//         binary-breaking).
//         (b) the legacy MultiFocus Function1 Grate kernel is removed —
//         tuple / representable / representableAt factories, zip /
//         zipWith, the Direct bridge (`forgetful2multifocusFunction1`) and
//         the short-lived `RepresentativeIndex` witness. Indexed is the
//         fixed-index replacement; its composition addresses the full
//         product grid, not the old diagonal. Source- and binary-breaking
//         for callers of the removed APIs. Container MultiFocus and
//         Traversal APIs are retained.
//         (c) `Optic.all` returns `List[A]` (every focus) instead of
//         `List[F[X, A]]` (binary-breaking descriptor change).
//         (d) built with Scala 3.9.0: the TASTy is 28.9, so downstream
//         compilers older than 3.9 cannot read it.
//   0.17: (a) jsoniter's string-path constructors keep the failure in the type
//         — `JsoniterPrism.fromPath` / `JsoniterTraversal.fromPath` return
//         `Either[String, _]`, the throwing `fromPath` is gone, and
//         `JsoniterTraversal.apply(path)` with it (source- and binary-
//         breaking; build from a path via `fromPath` and handle the `Left`).
//         (b) avro's `AvroVulcan.codec[A]` (the `using`-form) returns
//         `Either[Exception, AvroCodec[A]]`; the old total form is
//         `codec(schema)` (source- and binary-breaking for direct callers;
//         the opt-in given is unchanged).
//   0.15: additive (kyo Record optics + kyo-schema bridge incl.
//         Structure.Value optics), but cats-eo-kyo now REQUIRES JDK 25
//         (kyo 1.0.0-RC5+ ships Java-25-only bytecode).
//   0.13: `Affine` covariant in B; `Affine.Miss` drops its phantom B type
//         parameter (now `Miss[A] <: Affine[A, Nothing]`) and `widenB` is
//         deleted — retyping a miss is a plain upcast (source- and
//         binary-breaking for direct `Miss`/`widenB` users).
//   0.12: core Traversal constructors + Optional.readOnly/selectReadOnly
//         return types narrowed to the new concrete `Traversal` class /
//         `PickFold` (binary-breaking descriptor changes); `type
//         AffineFold` alias removed (source-breaking).
//   0.11: the same narrowing move for JsoniterPrism / JsoniterTraversal.
//   0.10: #77's ConfluentWire threadLocalStorage defaulted params are
//         binary-breaking.
//   0.6.0: kindlings 0.2.0 fully-qualifies derived Avro record names by
//         enclosing path (namespace) — a wire-format break vs 0.5.x.
//   Earlier: 0.2.0 JsonPrism → Affine `Optional` #31, 0.3.0 avro
//   field-naming #35, 0.4.0 additive #37/#38, 0.5.0 Confluent surface #41.
