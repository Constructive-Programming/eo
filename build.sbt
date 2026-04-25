val scala3Version = "3.8.3"

// ---- Publishing metadata -------------------------------------------
//
// `sbt-typelevel-ci-release` derives the project version from git
// tags (`v0.1.0` → `0.1.0`, untagged commits → a snapshot). The
// fields below are ThisBuild-scoped so every published sub-module
// inherits them.
//
// TODO before 0.1.0 tag:
//   1. Register the `dev.constructive` namespace on Sonatype
//      Central Portal, add the DNS TXT record it asks for on the
//      `constructive.dev` domain, wait for verification.
//   2. The repository lives at
//      `https://github.com/Constructive-Programming/eo` (derived
//      automatically for `scmInfo`); change `homepage` below if the
//      project moves to a different host.
//   3. Generate a project GPG key, upload to keys.openpgp.org,
//      configure GitHub Secrets (see docs/ci-secrets.md).

ThisBuild / tlBaseVersion := "0.1"
ThisBuild / organization := "dev.constructive"
ThisBuild / organizationName := "Constructive"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / homepage := Some(url("https://github.com/Constructive-Programming/eo"))
ThisBuild / developers := List(
  tlGitHubDev("kryptt", "Rodolfo Hansen")
)

// MiMa starts *enforcing* binary compatibility from 0.1.1 onward.
// 0.1.0 is the first publish, so there is no previous version to
// compare against.
ThisBuild / tlMimaPreviousVersions := Set.empty

// The minimum Java runtime we support (`-java-output-version 17` on the
// scalac side, `javacOptions --release 17` on the javac side). JDK 25+
// no longer accepts `--release 8`, and our CI matrix tests only on 17
// and 21 — so 17 is the honest floor. Downstream consumers on older
// JDKs must use `cats-eo 0.1.x`-era artifacts compiled with a pre-25
// toolchain.
ThisBuild / tlJdkRelease := Some(17)

// GitHub Actions matrix: JDK 17 (LTS) and JDK 21 (current LTS).
// Scala version comes from the scalaVersion ThisBuild setting
// below via `crossScalaVersions`.
ThisBuild / scalaVersion := scala3Version
ThisBuild / crossScalaVersions := Seq(scala3Version)
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21"),
)
// Run `sbt test` + `sbt doc` + scalafmt check on every PR. MiMa
// is wired transitively through sbt-typelevel-ci-release. The
// scalafix check runs via `--check` so any rule drift fails CI
// without rewriting files; devs run `sbt scalafixAll` locally to
// auto-fix.
ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Sbt(
    List("scalafmtCheckAll", "scalafmtSbtCheck"),
    name = Some("Check formatting"),
  ),
  WorkflowStep.Sbt(
    List("scalafixAll --check"),
    name = Some("Check scalafix"),
  ),
)

// -------------------------------------------------------------------
// Scalafix wiring. SemanticDB exports are required by the semantic
// rules (RemoveUnused, OrganizeImports). Rule set lives in
// `.scalafix.conf` at the repo root; typelevel-scalafix is brought in
// via `scalafixDependencies` so its cats-module rules
// (TypelevelMapSequence, TypelevelAs) are discoverable by name.
//
// `-Wunused:all` is the broadest unused-warning surface; it
// supersedes the narrower `-Wunused:implicits,explicits,imports,
// locals,params,privates` that sbt-typelevel-settings 0.8.5 ships,
// adding `unused pattern bindings` and `unused nowarn annotations`.
//
// `tlFatalWarnings := true` flips warnings-as-errors from CI-only
// (the plugin default) to always-on. Surgical `-Wconf` silences in
// `commonSettings` / `scala3LibrarySettings` continue to work — they
// whitelist specific known-noisy patterns rather than blanket-
// suppressing.
// -------------------------------------------------------------------
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies +=
  "org.typelevel" %% "typelevel-scalafix" % "0.5.0"
ThisBuild / scalacOptions += "-Wunused:all"
ThisBuild / tlFatalWarnings := true

// -------------------------------------------------------------------
// Bump hardcoded GitHub Action versions that sbt-typelevel 0.8.5
// pins to older releases:
//
//   - actions/upload-artifact           v5 → v7
//   - actions/download-artifact         v6 → v8
//   - scalacenter/sbt-dependency-submission  v2 → v3
//
// Dependabot repeatedly tries to update these by hand-editing
// ci.yml, which conflicts with sbt-typelevel's `githubWorkflowCheck`
// gate. Rewriting the generated steps here keeps the emitted ci.yml
// and Dependabot's expectations aligned.
//
// Remove this block (and re-run `sbt githubWorkflowGenerate`) when
// sbt-typelevel ships a release that already pins these newer
// versions upstream — see
// https://github.com/typelevel/sbt-typelevel/releases. The hardcoded
// references today live in:
//   - GenerativePlugin.scala (upload @ v5, download @ v6)
//   - WorkflowStep.scala     (sbt-dependency-submission @ v2)
// -------------------------------------------------------------------

def bumpActionVersion(
    owner: String,
    repo: String,
    newRef: String,
): WorkflowStep => WorkflowStep = {
  case s: WorkflowStep.Use =>
    s.ref match {
      case UseRef.Public(o, r, _) if o == owner && r == repo =>
        s.withRef(UseRef.Public(owner, repo, newRef))
      case _ => s
    }
  case other => other
}

ThisBuild / githubWorkflowGeneratedUploadSteps ~= { steps =>
  steps.map(bumpActionVersion("actions", "upload-artifact", "v7"))
}

ThisBuild / githubWorkflowGeneratedDownloadSteps ~= { steps =>
  steps.map(bumpActionVersion("actions", "download-artifact", "v8"))
}

ThisBuild / githubWorkflowAddedJobs ~= { jobs =>
  jobs.map { job =>
    if (job.id == "dependency-submission")
      job.withSteps(
        job
          .steps
          .map(
            bumpActionVersion("scalacenter", "sbt-dependency-submission", "v3")
          )
      )
    else job
  }
}

// -------------------------------------------------------------------
// Drop the legacy `SONATYPE_CREDENTIAL_HOST` env var that
// sbt-typelevel-sonatype-ci-release 0.8.5 still emits on the
// `Publish` step (see TypelevelSonatypeCiReleasePlugin.scala:66).
// The host is no longer parameterised: TypelevelSonatypePlugin pins
// `publishTo` to https://central.sonatype.com/... unconditionally.
// Carrying the env var means we'd also need a matching repo secret
// for it, which is a constant masquerading as a secret. Remove this
// override when sbt-typelevel ships a release that drops the env var
// upstream.
// -------------------------------------------------------------------
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("tlCiRelease"),
    name = Some("Publish"),
    env = Map(
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
    ),
  )
)

val Typelevel = "org.typelevel"
val ScalaCheckOrg = "org.scalacheck"
val Optics = "dev.optics"
val Kubuszok = "com.kubuszok"
val Circe = "io.circe"

lazy val cats = Typelevel %% "cats-core" % "2.13.0"
lazy val disciplineCore = Typelevel %% "discipline-core" % "1.7.0"
lazy val discipline = Typelevel %% "discipline-specs2" % "2.0.0"
lazy val scalacheck = ScalaCheckOrg %% "scalacheck" % "1.17.1"
lazy val monocle = Optics %% "monocle-core" % "3.3.0"
lazy val hearth = Kubuszok %% "hearth" % "0.3.0"
lazy val kindlingsCats = Kubuszok %% "kindlings-cats-derivation" % "0.1.0"
lazy val kindlingsCirce = Kubuszok %% "kindlings-circe-derivation" % "0.1.0"
lazy val circe = Circe %% "circe-core" % "0.14.10"
lazy val circeParser = Circe %% "circe-parser" % "0.14.10"

lazy val commonSettings = Seq(
  // `version` is NOT set here — sbt-typelevel-ci-release derives it
  // from the current git tag (`tlBaseVersion` above gives the
  // untagged snapshot prefix).
  scalaVersion := scala3Version,
  // `-groups` renders @group tags as Scaladoc sections. Applied on
  // every published module so any `@group Constructors` /
  // `@group Operations` / `@group Instances` bucket on an optic
  // companion shows up as a collapsible section in the API docs.
  Compile / doc / scalacOptions ++= Seq("-groups"),
  // Silence unchecked-type-test warnings that bubble up from Hearth's
  // kindlings-cats-derivation macro expansions. The generated match
  // patterns reference our abstract enum cases ("the type test for
  // Problem.X cannot be checked at runtime"); we don't own that
  // library's code and the warning is a Hearth-side concern rather
  // than a cats-eo bug.
  Test / scalacOptions += "-Wconf:src=.*/cats-derivation/.*:silent",
)

// Library-appropriate scalac options layered on top of the baseline set
// that sbt-typelevel-settings contributes automatically (`-deprecation
// -feature -unchecked -Wunused:implicits,explicits,imports,locals,params,privates
// -Wvalue-discard -Xkind-projector:underscores`, plus `-Werror` in CI
// via `tlFatalWarnings`). Applied to published modules.
//
// `-opt` was ported from Scala 2 to Scala 3.8.3 in January 2026 and is
// safe on published artifacts: it only rewrites bytecode within a
// method (box elim, null-check folding, dead-code elim, copy
// propagation) and touches neither TASTy nor call boundaries, so
// downstream binary-compat checks see nothing. `-opt-inline` DOES cross
// method/class boundaries and is reserved for [[scala3CoreSettings]]
// only, with the `<sources>` scope so nothing from cats / JDK is baked
// into our jar (a future cats release can't be invalidated by stale
// inline copies). Laws / generics / circe stay on `-opt` only.
//
// `-Yretain-trees` keeps full ASTs in published TASTy so downstream
// callers' `inline def` expansions can see through our bodies.
// `-Wsafe-init` catches `val` init-order bugs in companion objects,
// zero runtime cost.
//
// Deferred: `-language:strictEquality` (SIP-67). Enabling it requires
// threading `CanEqual` witnesses through every `equals(that: Any)`
// that currently compares match-type-valued fields (Affine's Fst/Snd,
// PSVec's leaf-structural equality, ...). Tracked for a later pass
// once we decide how CanEqual should flow through the existential
// optic hierarchy.
lazy val scala3LibrarySettings = Seq(
  scalacOptions ++= Seq(
    "-opt", // method-local JVM bytecode optimizer
    "-Yretain-trees", // downstream `inline def` / macros see our bodies
    "-Wsafe-init", // catch `val` init-order bugs in companion objects
    "-Yexplicit-nulls", // non-nullable reference types by default
    // `-Yexplicit-nulls` + `-Xcheck-macros` + Hearth's `lens[S](_.field)`
    // expansion trip a Scala 3 compiler bug at quote expansion time
    // ("Missing symbol position ... method productIterator / productPrefix").
    // The emitted code still works and the tests pass; we silence the
    // internal compiler-bug note until Hearth / Scala 3 patch the
    // interaction.
    "-Wconf:msg=Missing symbol position:silent",
  )
)

// Pure library modules with no dependency on another artifact's
// internals can additionally enable `-opt-inline:<sources>` — inlines
// only between files compiled together in the current sub-project,
// leaving external libraries untouched so their future bug-fixes keep
// flowing through. Applied to `core/` only; `circeIntegration/`
// deliberately omits it (crosses into circe, where we must NOT bake
// internals). `generics/` also omits it — inlining across macro
// expansions is both unnecessary and risks leaking compiler-internals
// into user bytecode.
lazy val scala3CoreSettings = scala3LibrarySettings ++ Seq(
  scalacOptions += "-opt-inline:<sources>"
)

// Macro-bearing modules additionally enable `-Xcheck-macros` so
// malformed quotes surface at macro-compile time rather than as NPEs
// in downstream builds.
lazy val scala3MacroSettings = scala3LibrarySettings ++ Seq(
  scalacOptions += "-Xcheck-macros"
)

lazy val root: Project = project
  .in(file("."))
  .aggregate(core, laws, tests, generics, circeIntegration)
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-root",
    publish / skip := true,
  )

lazy val core: Project = project
  .in(file("core"))
  .settings(commonSettings *)
  .settings(scala3CoreSettings *)
  .settings(
    name := "cats-eo",
    libraryDependencies += cats,
    // Minimal Test dep so core can run its own in-module smoke specs
    // (currently just FoldSpec) without needing the cross-module
    // `tests` project. The richer suites all live in cats-eo-tests.
    libraryDependencies += discipline % Test,
  )

lazy val laws: Project = project
  .in(file("laws"))
  .dependsOn(LocalProject("core"))
  .settings(commonSettings *)
  .settings(scala3LibrarySettings *)
  .settings(
    name := "cats-eo-laws",
    libraryDependencies += cats,
    libraryDependencies += disciplineCore,
    libraryDependencies += scalacheck,
  )

lazy val tests: Project = project
  .in(file("tests"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("laws"),
    LocalProject("generics") % Test,
  )
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-tests",
    publish / skip := true,
    libraryDependencies += discipline % Test,
    // circe-core powers the JsonOptic demo spec ported from the
    // `unthreaded` branch -- the behaviour specs there traverse a
    // circe Json AST to show how Optic.modify composes over a
    // recursive parser output.
    libraryDependencies += circe % Test,
    libraryDependencies += circeParser % Test,
    // Kubuszok's kindlings library provides Hearth-powered derivation
    // for cats typeclasses (Show / Eq / Monoid / ...) and circe codecs.
    // Used by the motivating CRUD round-trip example to keep the
    // typeclass boilerplate out of the way of the optics story.
    libraryDependencies += kindlingsCats % Test,
    libraryDependencies += kindlingsCirce % Test,
  )

// Auto-derivation of optics for product / sum types via quoted macros,
// built on Mateusz Kubuszok's `hearth` macro-commons library. Kept out
// of `core` so agents that only want the hand-written optics don't
// pull in a macro dep.
lazy val generics: Project = project
  .in(file("generics"))
  .dependsOn(LocalProject("core"), LocalProject("laws") % Test)
  .settings(commonSettings *)
  .settings(scala3MacroSettings *)
  .settings(
    name := "cats-eo-generics",
    libraryDependencies += cats,
    libraryDependencies += hearth,
    libraryDependencies += discipline % Test,
    // The Hearth-quote-driven lens macro emits a `'{ (x, a) => ... }`
    // lambda whose `a` is only referenced from inside a nested splice,
    // which `-Wunused:explicits` can't follow across the quote/splice
    // boundary (no amount of `val _ = a` in the quote body or `@nowarn`
    // on the enclosing def dislodges it). Silence the false positive
    // for this one file only.
    Compile / scalacOptions += "-Wconf:src=.*/LensMacro\\.scala:silent",
  )

// Cross-representation optics that bridge a native Scala source type
// (a case class) and its circe-serialised form (JsonObject). The
// flagship is `JsonLens[S, A]` — a polymorphic lens whose write path
// lets you `transform` a JsonObject field in place without
// round-tripping through S. Kept in its own module so projects that
// only want the base library don't pull in circe.
lazy val circeIntegration: Project = project
  .in(file("circe"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("generics"),
    LocalProject("laws") % Test,
  )
  .settings(commonSettings *)
  .settings(scala3LibrarySettings *)
  .settings(
    name := "cats-eo-circe",
    libraryDependencies += cats,
    libraryDependencies += circe,
    // Compile-scope: the `Json | String` overloads on JsonPrism /
    // JsonFieldsPrism / JsonTraversal / JsonFieldsTraversal parse String
    // inputs via io.circe.parser.parse before proceeding with the Json
    // path. Parse failures surface via JsonFailure.ParseFailed.
    libraryDependencies += circeParser,
    libraryDependencies += kindlingsCirce % Test,
    libraryDependencies += discipline % Test,
  )

// Docs site — Laika + mdoc via sbt-typelevel-site. Rooted at
// `site/` on disk, named `docs` in sbt so `sbt docs/tlSite` /
// `sbt docs/mdoc` / `sbt docs/tlSitePreview` read naturally. Keeps
// docs out of the root aggregator so plain `sbt compile` / `sbt
// test` stay fast.
lazy val docs: Project = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(
    LocalProject("core"),
    LocalProject("generics"),
    LocalProject("circeIntegration"),
    LocalProject("laws"),
  )
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-docs",
    publish / skip := true,
    // kindlingsCirce is only in `circeIntegration`'s Test scope, so
    // docs examples that want circe Codec derivation need it
    // surfaced here as a Compile-scope dep.
    libraryDependencies += kindlingsCirce,
    // Point mdoc at the sub-project's own `docs/` directory. The
    // plugin's default resolves to the ROOT `docs/` directory,
    // which already contains internal notes (`plans/`,
    // `solutions/`, `ci-secrets.md`) that Laika should not ingest.
    mdocIn := (ThisBuild / baseDirectory).value / "site" / "docs",
    // mdoc variable substitutions — site pages can reference
    // `@VERSION@` to always display the current version.
    mdocVariables ++= Map(
      "VERSION" -> tlBaseVersion.value
    ),
    // Helium theme config — the plugin needs a home-link URL on
    // the top nav; without it Laika fails the site build with
    // "No target for home link found".
    tlSiteHelium ~= {
      _.site.topNavigationBar(
        homeLink = laika
          .helium
          .config
          .ImageLink
          .external(
            "https://github.com/Constructive-Programming/eo",
            laika.ast.Image.external(""),
          )
      )
    },
  )

// Benchmarks deliberately stay OUT of the root aggregator: they're a
// JMH harness rather than a test, and we don't want `sbt test` or
// `sbt compile` to drag the JMH machinery / monocle dep in. Run them
// explicitly with e.g. `sbt benchmarks/Jmh/run -i 5 -wi 3 -f 1`.
lazy val benchmarks: Project = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .dependsOn(
    LocalProject("core"),
    LocalProject("circeIntegration"),
  )
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-benchmarks",
    publish / skip := true,
    libraryDependencies += monocle,
    // circe-parser for the Json round-trip bench; kindlings for the
    // Codec derivation used by the JsonPrism bench fixture.
    libraryDependencies += circeParser,
    libraryDependencies += kindlingsCirce,
  )
