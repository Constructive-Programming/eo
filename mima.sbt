// MiMa setup — binary-compatibility checking, wired transitively
// through sbt-typelevel-ci-release.
//
// MiMa is currently DISABLED across the still-evolving 0.x line:
// every minor so far has been a deliberate breaking release, and
// cats-eo-avro has no published baseline anyway. Breaking-change
// history, newest first:
//
//   next: `Affine` covariant in B; `Affine.Miss` drops its phantom B type
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
//
// Re-enable once the API is stable: set this to the published baseline
// line, and accompany every subsequent binary-breaking change with a
// justified ProblemFilter, e.g.
//
//   ThisBuild / mimaBinaryIssueFilters ++= Seq(
//     ProblemFilters.exclude[MissingClassProblem]("dev.constructive.eo.internal.Foo"),
//   )
//
// Only exclude symbols that are genuinely internal (`private` or
// `private[pkg]`); exclusions on the public API are binary-compat
// breakage and belong in a version bump instead.
ThisBuild / tlMimaPreviousVersions := Set.empty
