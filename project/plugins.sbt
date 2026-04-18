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
