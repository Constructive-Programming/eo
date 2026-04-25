addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")

// Format check gate for CI (`sbt scalafmtCheckAll scalafmtSbtCheck`
// in the workflow). The project ships a `.scalafmt.conf` pinned to
// 3.x; sbt-scalafmt honours that pin automatically.
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")

// `sbt-typelevel-ci-release` wires the Sonatype Central Portal flow
// (post-June-2025 OSSRH sunset): derives the version from git tags,
// signs artifacts with the configured GPG key, and publishes on
// `v*` tag push. Brings MiMa in through its transitive
// `sbt-typelevel-mima` dependency so binary-compat checks run on
// every CI build from 0.1.1 onward (0.1.0 has no previous version
// to compare against; see `mima.sbt`).
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "0.8.5")

// `sbt-typelevel-settings` contributes the curated scalac flag set
// (`-deprecation -feature -unchecked -Wunused:... -Wvalue-discard`,
// plus `-Xkind-projector:underscores` on 3.5+) and turns on `-Werror`
// in CI via `tlFatalWarnings`. Not transitively brought in by
// `-ci-release`, so we add it explicitly — without it each module is
// responsible for its own scalacOptions.
addSbtPlugin("org.typelevel" % "sbt-typelevel-settings" % "0.8.5")

// `sbt-scalafix` wires Scalafix into the build (`sbt scalafixAll`,
// `sbt scalafixAll --check`). Pinned to the same minor as the
// standalone CLI version listed in CLAUDE.md so devs and the build
// agree on rule semantics. Brings the SemanticDB compiler plugin
// transitively via `scalafixSemanticdb`; we wire it explicitly in
// build.sbt so every module exports SemanticDB consistently.
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.6")

// `sbt-typelevel-site` drives the Laika-based docs site. Pairs the
// mdoc-compiled markdown under `site/docs/` with the Helium theme
// configured from build.sbt. Pinned to the same 0.8.5 family as
// ci-release so they share plugin transitive versions.
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.5")
