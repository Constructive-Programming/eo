addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.8")

// `sbt-stryker4s` drives mutation testing. Mutation testing was
// previously evaluated and dropped because cats-eo was almost entirely
// type-level (a whole-project run found a single mutable runtime
// expression). The `schemes` module's runtime machinery and core's
// opaque-carrier dispatch changed that, so it's back — run on demand /
// at release rather than as a per-PR gate (see `mutationAll` in
// build.sbt and site/docs/quality-assurance.md).
// Stryker runs each module's OWN `Test / test` against that module's
// mutants. This build invokes it as `project <module>; stryker` (see
// `mutationAll` in build.sbt and the quality.yml release sweep).
// Upstream fixed the multi-module invocation in 0.20.4 by scoping the
// task to the invoking project, so `<module>/stryker` is correct too on
// 1.x; the current-project form is kept because the alias switches
// projects anyway and the borrowed-tests `set` lines are expressed with
// `project <m>;` as well. Before 0.20.4 the module-scoped form resolved
// `loadedTestFrameworks` from the aggregating root project (no test
// deps), so every mutant came back NoCoverage.
// The Scala 3 dialect is still auto-derived from scalaVersion. Bumped
// 0.20.4 → 1.1.1: v1 has no removed or renamed features, the sbt
// setting keys used in build.sbt (`strykerReporters`,
// `strykerExcludedMutations`, `strykerThresholdsBreak`) are unchanged,
// and 1.x requires sbt >= 1.11.2 (we run 1.13.0).
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "1.1.1")

// Format check gate for CI (`sbt scalafmtCheckAll scalafmtSbtCheck`
// in the workflow). The project ships a `.scalafmt.conf` pinned to
// 3.x; sbt-scalafmt honours that pin automatically.
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

// `sbt-typelevel-ci-release` wires the Sonatype Central Portal flow
// (post-June-2025 OSSRH sunset): derives the version from git tags,
// signs artifacts with the configured GPG key, and publishes on
// `v*` tag push. Brings MiMa in through its transitive
// `sbt-typelevel-mima` dependency so binary-compat checks run on
// every CI build from 0.1.1 onward (0.1.0 has no previous version
// to compare against; see `mima.sbt`).
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "0.8.7")

// `sbt-typelevel-settings` contributes the curated scalac flag set
// (`-deprecation -feature -unchecked -Wunused:... -Wvalue-discard`,
// plus `-Xkind-projector:underscores` on 3.5+) and turns on `-Werror`
// in CI via `tlFatalWarnings`. Not transitively brought in by
// `-ci-release`, so we add it explicitly — without it each module is
// responsible for its own scalacOptions.
addSbtPlugin("org.typelevel" % "sbt-typelevel-settings" % "0.8.7")

// `sbt-scalafix` wires Scalafix into the build (`sbt scalafixAll`,
// `sbt scalafixAll --check`). Pinned to the same minor as the
// standalone CLI version listed in CLAUDE.md so devs and the build
// agree on rule semantics. Brings the SemanticDB compiler plugin
// transitively via `scalafixSemanticdb`; we wire it explicitly in
// build.sbt so every module exports SemanticDB consistently.
//
// 0.14.7's OrganizeImports normalises multi-line import selectors WITHOUT a
// trailing comma. scalafix fully owns imports (sorting, grouping, and now
// trailing commas), so scalafmt is set to `trailingCommas = keep`
// (.scalafmt.conf) to stay out of import formatting — the two no longer fight.
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.9")

// `sbt-typelevel-site` drives the Laika-based docs site. Pairs the
// mdoc-compiled markdown under `site/docs/` with the Helium theme
// configured from build.sbt. Pinned to the same 0.8.5 family as
// ci-release so they share plugin transitive versions.
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.7")

// `unused-code-plugin` from xuwei-k contributes the `WarnUnusedCode` /
// `ErrorUnusedCode` / `RemoveUnusedCode` Scalafix `SyntacticRule`s,
// which surface unused PUBLIC classes/objects/methods (the stdlib
// `RemoveUnused` rule only catches private/local ones). Configuration
// for the rule lives on `ThisBuild / unusedCodeConfig` in `build.sbt`;
// the rule itself ships as `_2.13` only but loads inside the Scalafix
// classloader so Scala 3 sources are parsed via `Dialect.Scala3`.
addSbtPlugin("com.github.xuwei-k" % "unused-code-plugin" % "0.5.5")

// `sbt-unidoc` merges scaladoc across the published modules into one
// tree (the `unidocs` project in build.sbt). We self-host the output —
// deploy-site.yml copies it into the Cloudflare Pages tree under
// `/api/`.
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.6.1")
