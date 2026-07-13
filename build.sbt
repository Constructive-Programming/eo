val scala3Version = "3.8.4"

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

ThisBuild / tlBaseVersion := "0.8"
ThisBuild / organization := "dev.constructive"
ThisBuild / organizationName := "Constructive"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / homepage := Some(url("https://github.com/Constructive-Programming/eo"))
ThisBuild / developers := List(
  tlGitHubDev("kryptt", "Rodolfo Hansen")
)

// MiMa stays disabled for 0.6.0 — a deliberate breaking release: the kindlings
// 0.2.0 migration fully-qualifies derived Avro record names by enclosing path
// (namespace), so records written/read through the derived codecs carry
// different on-wire schema names than 0.5.x — a wire-format break. (0.6.0 also
// raises the derivation macro timeout to 30s and pins jackson to CVE-patched
// 2.21.5.) Earlier 0.x lines: 0.2.0 JsonPrism → Affine `Optional` #31, 0.3.0
// avro field-naming #35, 0.4.0 additive #37/#38, 0.5.0 Confluent surface #41.
// Kept off across the still-evolving 0.x line — and cats-eo-avro has no
// published baseline anyway. Re-enable (set to the published 0.6.x line) once
// the API is stable.
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
    List(
      "scalafmtCheckAll",
      "scalafmtSbtCheck",
      "benchmarks/scalafmtCheck",
      "benchmarks/Test/scalafmtCheck",
    ),
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
// Raise kindlings' per-derivation macro-expansion budget from the 2s default (which a loaded CI
// runner intermittently trips: `deriveAsObject timed out after 2000ms`) to 30s. One namespace per
// kindlings module (circe / cats / avro derivation); read by kindlings 0.3.x's `DerivationTimeout`.
// Comma-separated so Scala's `-Xmacro-settings` MultiStringSetting splits them.
ThisBuild / scalacOptions +=
  "-Xmacro-settings:circeDerivation.timeout=30,catsDerivation.timeout=30,avroDerivation.timeout=30"
ThisBuild / tlFatalWarnings := false

// `unused-code-plugin` (xuwei-k) ships a Scalafix `SyntacticRule`
// (`WarnUnusedCode`) that finds unused PUBLIC classes/objects/methods.
// It complements stdlib `RemoveUnused`, which only catches
// private/local definitions. We use the WARN variant — never the
// REMOVE / ERROR variant — because cats-eo's public optic
// constructors are intentionally part of the published API even when
// no internal call site invokes them yet (downstream users are the
// consumers).
//
// `excludePath` patterns: tests / benchmarks / circe-test fixtures
// host top-level helpers that look unused across compilation but are
// referenced via reflection (specs2 discovery, JMH @Benchmark) or
// only inside their owning module's test runs (which the plugin's
// scan can't see).
ThisBuild / unusedCodeConfig ~= { c =>
  c.copy(
    excludePath = c.excludePath ++ Set(
      "glob:**/src/test/**",
      "glob:**/benchmarks/**",
    ),
    excludeMainMethod = false,
    dialect = unused_code.Dialect.Scala3,
  )
}

// -------------------------------------------------------------------
// Bump hardcoded GitHub Action versions that sbt-typelevel 0.8.6
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

// sbt-typelevel 0.8.x still generates `actions/checkout@v6`; we run @v7
// (Dependabot's bump). `githubWorkflowCheck` enforces ci.yml == the
// generated output, so pin v7 at the source — in the shared job setup —
// rather than hand-editing the YAML (which the check would reject). Drop
// this once the plugin ships a release that generates checkout@v7.
ThisBuild / githubWorkflowJobSetup ~= { steps =>
  steps.map(bumpActionVersion("actions", "checkout", "v7"))
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
    // sbt-typelevel 0.8.x pins the steward-validate job to temurin@11,
    // but scala-steward releases are now compiled for JDK 17 — the
    // coursier-resolved launcher dies with UnsupportedClassVersionError
    // on 11. Bump the matrix AND the literal setup step (whose `if:` is
    // hardcoded to temurin@11 and would otherwise never fire, leaving
    // the runner-default JDK 11). Drop when the plugin bumps upstream.
    else if (job.id == "validate-steward")
      job
        .withJavas(List(JavaSpec.temurin("17")))
        .withSteps(job.steps.map {
          case s: WorkflowStep.Use if s.id.exists(_.startsWith("setup-java")) =>
            s.withParams(s.params.updated("java-version", "17"))
              .withId(Some("setup-java-temurin-17"))
              .withName(Some("Setup Java (temurin@17)"))
              .withCond(Some("matrix.java == 'temurin@17'"))
          case s => s
        })
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
val ApacheAvro = "org.apache.avro"
val FasterXmlJackson = "com.fasterxml.jackson.core"
val Plokhotnyuk = "com.github.plokhotnyuk.jsoniter-scala"

lazy val cats = Typelevel %% "cats-core" % "2.13.0"
lazy val kyoData = "io.getkyo" %% "kyo-data" % "0.19.0"
lazy val disciplineCore = Typelevel %% "discipline-core" % "1.7.0"
lazy val discipline = Typelevel %% "discipline-specs2" % "2.0.0"
lazy val scalacheck = ScalaCheckOrg %% "scalacheck" % "1.19.0"
lazy val monocle = Optics %% "monocle-core" % "3.3.0"
// droste — the recursion-scheme baseline for the schemes benchmarks (pattern
// functor + Fix encoding). Benchmark-only; never a published dependency.
lazy val drosteCore = "io.higherkindness" %% "droste-core" % "0.9.0-M3"
// kindlings 0.3.x (all three) ship a configurable macro-expansion timeout
// (`DerivationTimeout`, default 5s) and pull hearth 0.4.0 + kindlings-derivation-commons.
// We raise it to 30s via `-Xmacro-settings:{circe,cats,avro}Derivation.timeout=30`
// (see the `ThisBuild / scalacOptions` above) so a loaded CI runner stops tripping the old
// hardcoded 2s budget (the recurring `deriveAsObject timed out after 2000ms` flake).
// NB kindlings fully-qualifies derived Avro record names (namespace = enclosing path) —
// established in 0.2.0, unchanged in 0.3.x.
lazy val hearth = Kubuszok %% "hearth" % "0.4.0"
lazy val kindlingsCats = Kubuszok %% "kindlings-cats-derivation" % "0.3.0"
lazy val kindlingsCirce = Kubuszok %% "kindlings-circe-derivation" % "0.3.0"
lazy val kindlingsAvro = Kubuszok %% "kindlings-avro-derivation" % "0.3.0"
lazy val circe = Circe %% "circe-core" % "0.14.16"
lazy val circeParser = Circe %% "circe-parser" % "0.14.16"
// Pin apache-avro 1.12.1 explicitly even though kindlings-avro-derivation
// brings it transitively — keeps the reachable runtime jar visible in
// dependency reports. cats-eo-avro touches `IndexedRecord` /
// `GenericData` / `Schema` directly on the hot path.
lazy val avro = ApacheAvro % "avro" % "1.12.1"
// Force jackson to 2.21.5 — `apache-avro 1.12.1` brings `jackson-databind
// 2.20.0` (and `jackson-core`) transitively, inside the CVE-affected
// `>= 2.19.0, < 2.21.5` range (four GHSA dependabot alerts: two
// PolymorphicTypeValidator/allowlist bypasses, an InetSocketAddress SSRF, and
// a @JsonIgnoreProperties case-insensitive bypass). 2.21.5 was the first
// release patched against all four. Do NOT bump to 2.22.0: it REGRESSED the
// @JsonIgnoreProperties case-insensitive fix (CVE-2026-54515, dependabot
// alert #7; re-fixed only in the unreleased 2.22.1) — the 0.6.1 bulk Steward
// upgrade briefly did, re-opening the alert. `.scala-steward.conf` pins the
// 2.21.x series; lift both pins together once 2.22.1 is on Central.
// Overrides apply via
// `commonSettings.dependencyOverrides` across every module so any future
// jackson-pulling transitive (e.g. a kindlings bump) inherits the safe
// versions automatically. eo never enables polymorphic/default typing, so the
// PTV bypasses aren't reachable here — this just keeps the dep tree clean.
lazy val jacksonCore = FasterXmlJackson % "jackson-core" % "2.21.5"
lazy val jacksonDatabind = FasterXmlJackson % "jackson-databind" % "2.21.5"
// jsoniter-scala — high-perf JSON codec (~5–10× circe on hot paths).
// Used by `eo-jsoniter` to back byte-cursor JSON optics that decode
// directly from `Array[Byte]` without allocating a runtime AST. The
// `-macros` artifact ships `JsonCodecMaker` so callers can derive a
// `JsonValueCodec[A]` per focus type at compile time.
lazy val jsoniterCore = Plokhotnyuk %% "jsoniter-scala-core" % "2.38.17"
lazy val jsoniterMacros = Plokhotnyuk %% "jsoniter-scala-macros" % "2.38.17"

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
  // Pin jackson-core + jackson-databind at the CVE-patched 2.21.5 across
  // every module — see the `jacksonCore` / `jacksonDatabind` defs above.
  dependencyOverrides ++= Seq(jacksonCore, jacksonDatabind),
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
  .aggregate(
    core,
  )
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
    libraryDependencies += kyoData,
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

// Recursion schemes as composable optics: `cata` (a Getter on Plated), `ana`
// (a Review), and a FUSED `hylo` (Getter[Seed, A], no intermediate S), plus the
// materializing `ana.cross(cata)` composition via core's `cross` combinator.
// `core`-only main dependency; tests use generics' `plate[S]` and circe's
// `platedJson`. See docs/plans/2026-06-09-001-feat-schemes-module-plan.md.
lazy val schemes: Project = project
  .in(file("schemes"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("generics") % Test,
    LocalProject("circeIntegration") % Test,
  )
  .settings(commonSettings *)
  .settings(scala3LibrarySettings *)
  .settings(
    name := "cats-eo-schemes",
    libraryDependencies += cats,
    libraryDependencies += discipline % Test,
    libraryDependencies += scalacheck % Test,
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

// Cross-representation optics that bridge a native Scala source type
// (a case class) and its Apache Avro on-the-wire form (`IndexedRecord`
// / `Array[Byte]`). The flagship is `AvroPrism[S, A]` — a Prism whose
// write path lets you `transform` an `IndexedRecord` field in place
// without round-tripping through S. Mirrors `circeIntegration`'s
// architecture decisions; built on Mateusz Kubuszok's
// `kindlings-avro-derivation` for the codec surface (the
// `AvroEncoder[A]` / `AvroDecoder[A]` / `AvroSchemaFor[A]` triplet,
// macro-derived via Hearth). cats-eo-avro wraps the triplet in its own
// `AvroCodec[A]` shorthand so user code summons one thing per type.
// Kept in its own module so projects that only want the base library
// don't pull in kindlings / avro.
lazy val avroIntegration: Project = project
  .in(file("avro"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("generics"),
    LocalProject("laws") % Test,
  )
  .settings(commonSettings *)
  .settings(scala3LibrarySettings *)
  .settings(
    name := "cats-eo-avro",
    libraryDependencies += cats,
    libraryDependencies += kindlingsAvro,
    // Pin apache-avro explicitly even though kindlings-avro-derivation
    // brings it transitively — the optic surface uses `IndexedRecord` /
    // `GenericData` directly for hot-path walks, so the dep is part
    // of our reachable API.
    libraryDependencies += avro,
    // circe is Optional: only the `dev.constructive.eo.avro.circe` sub-package (the structural
    // Avro ↔ circe bridge, `AvroJson`) touches it, and its API surface *names* `io.circe.Json` —
    // so any caller already depends on circe directly to write the call site. Optional keeps it
    // off downstream classpaths; avro-only users never load the bridge's classfiles.
    libraryDependencies += circe % Optional,
    libraryDependencies += discipline % Test,
  )

// Byte-cursor JSON optics over `Array[Byte]`, backed by
// jsoniter-scala. Reuses the existing `Affine` carrier — the optic
// shape `Optic[Array[Byte], Array[Byte], A, A, Affine]` keeps the
// source bytes in the structural leftover (Hit's `snd` carries
// `(bytes, span)`, Miss's `fst` carries `bytes` for pass-through) and
// writes splice into the recorded spans. No runtime AST allocation:
// the path scanner walks raw bytes and the focus is decoded via
// `JsonValueCodec[A]` only when the user reads it. avroIntegration is
// Test-scoped for the cross-format Avro-bytes → JSON-bytes bridge spec.
lazy val jsoniterIntegration: Project = project
  .in(file("jsoniter"))
  .dependsOn(
    LocalProject("core"),
    LocalProject("laws") % Test,
    LocalProject("avroIntegration") % Test,
  )
  .settings(commonSettings *)
  .settings(scala3LibrarySettings *)
  .settings(
    name := "cats-eo-jsoniter",
    libraryDependencies += cats,
    libraryDependencies += jsoniterCore,
    // -macros is Test-scoped: the optic factory only needs
    // `JsonValueCodec[A]` evidence at the call site, and the spec
    // derives codecs for fixtures via JsonCodecMaker.make.
    libraryDependencies += jsoniterMacros % Test,
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
    LocalProject("avroIntegration"),
    LocalProject("jsoniterIntegration"),
    LocalProject("laws"),
    LocalProject("schemes"),
  )
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-docs",
    publish / skip := true,
    // Disable sbt-typelevel-site's default ci.yml "Publish site" step.
    // That step pushes the rendered site to a `gh-pages` branch via
    // `peaceiris/actions-gh-pages`, but GitHub Pages is disabled on
    // this repo — `gh-pages` is consumed by nothing. Production is
    // served from Cloudflare Pages via .github/workflows/deploy-site.yml.
    // Leaving the default on means every push to main also pushes a
    // dead site to gh-pages, churns Dependabot on the peaceiris
    // version, and burns CI minutes.
    tlSitePublishBranch := None,
    tlSitePublishTags := false,
    // Laika's input tree is mdoc's output by default. Append a
    // `laika-static/` sibling so we can ship hand-authored CSS / JS
    // (under `static/`) without putting them through mdoc, which
    // only knows how to process Markdown.
    Laika / sourceDirectories += baseDirectory.value / "laika-static",
    // kindlingsCirce is only in `circeIntegration`'s Test scope, so
    // docs examples that want circe Codec derivation need it
    // surfaced here as a Compile-scope dep.
    libraryDependencies += kindlingsCirce,
    // kindlingsAvro is only in `avroIntegration`'s test/runtime path
    // through the codec triplet — surface it here so avro.md mdoc
    // blocks can summon `AvroEncoder.derived` / `AvroDecoder.derived`
    // / `AvroSchemaFor.derived` against the live module classpath.
    libraryDependencies += kindlingsAvro,
    // jsoniter-scala-macros is `Test`-scoped on jsoniterIntegration; surface
    // it here so jsoniter.md mdoc blocks can derive `JsonValueCodec[A]` via
    // `JsonCodecMaker.make` against the live classpath.
    libraryDependencies += jsoniterMacros,
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
    // Helium theme config — aligns eo.constructive.dev with the
    // constructive.dev brand identity:
    //   - Indigo accent (#3b3b8c light / #7878d6 dark) replacing the
    //     Typelevel-default red/blue.
    //   - System-sans body, JetBrains-Mono code stack — matches the
    //     $cp-sans / $cp-mono CSS variables in the main site.
    //   - OS-driven dark mode (prefers-color-scheme), augmented by a
    //     manual toggle injected from site/laika/js/cp-theme-toggle.js.
    //   - Top nav: keep the Helium default home link (the small
    //     home icon), and add an explicit pair of icons on the right
    //     — constructive.dev favicon (back to the brand site) first,
    //     GitHub source link second. To keep the favicon ahead of
    //     the GitHub icon in display order, we suppress the GitHub
    //     IconLink that `GenericSiteSettings` auto-derives from
    //     `scmInfo` (it would otherwise prepend and force the wrong
    //     order) and re-add it ourselves inside this call.
    scmInfo := None,
    tlSiteHelium ~= {
      import laika.ast.{Image, Path}
      import laika.helium.config.{HeliumIcon, IconLink, ImageLink, TextLink}
      import laika.theme.config.Color
      _.all
        .themeColors(
          primary = Color.hex("3b3b8c"),
          primaryMedium = Color.hex("8585c5"),
          primaryLight = Color.hex("eaeaf5"),
          secondary = Color.hex("5e5eb8"),
          text = Color.hex("23272e"),
          background = Color.hex("ffffff"),
          bgGradient = (Color.hex("ffffff"), Color.hex("ffffff")),
        )
        .site
        .darkMode
        .themeColors(
          primary = Color.hex("7878d6"),
          primaryMedium = Color.hex("5e5eb8"),
          primaryLight = Color.hex("2d333b"),
          secondary = Color.hex("a3a3e6"),
          text = Color.hex("e6edf3"),
          background = Color.hex("1f242b"),
          bgGradient = (Color.hex("1f242b"), Color.hex("23272e")),
        )
        // NB: Helium.fontFamilies wraps the value in
        //   "<value>", sans-serif    /  "<value>", monospace
        // which breaks any comma-separated stack we pass. The
        // --body-font / --header-font / --code-font CSS vars are
        // overridden in site/docs/static/cp-theme.css instead.
        .site
        .topNavigationBar(
          // Home link: project name as a text link to the docs root.
          // Helium's default home link (`DynamicHomeLink.default`)
          // expects a configured landing page; we don't have one.
          homeLink = TextLink.internal(Path.Root / "index.md", "cats-eo"),
          // Right-side nav: constructive.dev favicon (back to brand
          // site) first, GitHub source link second. Order matters —
          // the auto-derived IconLink from scmInfo was suppressed
          // above (scmInfo := None) so this list is the full set in
          // the rendered order.
          navLinks = Seq(
            // Merged scaladoc (the `unidocs` project) — mounted at
            // /api/ by deploy-site.yml AFTER the Laika render, so
            // Laika never sees it: the link must be external (an
            // internal one would fail validation). Root-relative so
            // previews and production both resolve.
            TextLink.external("/api/", "API"),
            // Benchmark history chart — a static page in laika-static/,
            // so an internal link (preview-deployment-safe).
            TextLink.internal(Path.Root / "bench" / "index.html", "bench history"),
            ImageLink.external(
              "https://www.constructive.dev/",
              Image.external("https://www.constructive.dev/assets/img/favicon.svg"),
            ),
            IconLink.external(
              "https://github.com/Constructive-Programming/eo",
              HeliumIcon.github,
            ),
          ),
        )
        .site
        .internalCSS(Path.Root / "static" / "cp-theme.css")
        .site
        .internalJS(Path.Root / "static" / "cp-theme-toggle.js")
    },
  )

// Merged scaladoc across the published modules, self-hosted on the
// docs site: deploy-site.yml runs `unidocs/unidoc` and copies the
// output into the Cloudflare Pages tree under `/api/` (the "API"
// top-nav link in `tlSiteHelium` above points there root-relatively,
// so previews and production both resolve). Not published to Maven —
// the site is the delivery channel — and not in the root aggregate,
// so `sbt test` / `sbt compile` don't pay for it.
lazy val unidocs: Project = project
  .in(file("unidocs"))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name := "cats-eo-api-docs",
    publish / skip := true,
    // Same `-groups` the per-module docs get (commonSettings), so
    // @group buckets render as sections in the merged tree too.
    ScalaUnidoc / unidoc / scalacOptions ++= Seq("-groups"),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      core,
      laws,
      generics,
      schemes,
      circeIntegration,
      avroIntegration,
      jsoniterIntegration,
    ),
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
  )
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-benchmarks",
    publish / skip := true,
    libraryDependencies += monocle,
  )

// Single source of truth for the JMH invocation (the config that the per-class
// annotations pin — see JmhDefaults' Phase-4 note for why that preamble can't be
// DRY'd). Append a filter: `sbt "bench .*OrderAvroBench.*"`.
addCommandAlias("bench", "benchmarks/Jmh/run -i 5 -wi 3 -f 3 -t 1")
addCommandAlias("benchQuick", "benchmarks/Jmh/run -i 3 -wi 2 -f 1 -t 1")
