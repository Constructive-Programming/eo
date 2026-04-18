// MiMa filter file.
//
// 0.1.0 has no previous version to compare against, so this file
// is empty. Starting with 0.1.1, every binary-breaking change must
// be accompanied by a ProblemFilter entry here explaining the
// justification.
//
// Format (per MiMa docs):
//
//   ThisBuild / mimaBinaryIssueFilters ++= Seq(
//     ProblemFilters.exclude[MissingClassProblem]("eo.internal.Foo"),
//   )
//
// Only exclude symbols that are genuinely internal (`private` or
// `private[pkg]`) or that shipped briefly in a non-0.1.x release.
// Exclusions on the public 0.1.x API are binary-compat breakage
// and should be a 0.2.0 concern instead.
