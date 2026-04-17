val scala3Version = "3.8.3"

val Typelevel     = "org.typelevel"
val ScalaCheckOrg = "org.scalacheck"
val Optics        = "dev.optics"
val Kubuszok      = "com.kubuszok"
val Circe         = "io.circe"

lazy val cats           = Typelevel     %% "cats-core"         % "2.13.0"
lazy val disciplineCore = Typelevel     %% "discipline-core"   % "1.7.0"
lazy val discipline     = Typelevel     %% "discipline-specs2" % "2.0.0"
lazy val scalacheck     = ScalaCheckOrg %% "scalacheck"        % "1.17.1"
lazy val monocle        = Optics        %% "monocle-core"      % "3.3.0"
lazy val hearth         = Kubuszok      %% "hearth"            % "0.3.0"
lazy val kindlingsCats  = Kubuszok      %% "kindlings-cats-derivation"  % "0.1.0"
lazy val kindlingsCirce = Kubuszok      %% "kindlings-circe-derivation" % "0.1.0"
lazy val circe          = Circe         %% "circe-core"        % "0.14.10"
lazy val circeParser    = Circe         %% "circe-parser"      % "0.14.10"

lazy val commonSettings = Seq(
  version      := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
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
    libraryDependencies += kindlingsCats  % Test,
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
  .settings(
    name := "cats-eo-generics",
    libraryDependencies += cats,
    libraryDependencies += hearth,
    libraryDependencies += discipline % Test,
  )

// Cross-representation optics that bridge a native Scala source type
// (a case class) and its circe-serialised form (JsonObject). The
// flagship is `JsonLens[S, A]` — a polymorphic lens whose write path
// lets you `transform` a JsonObject field in place without
// round-tripping through S. Kept in its own module so projects that
// only want the base library don't pull in circe.
lazy val circeIntegration: Project = project
  .in(file("circe"))
  .dependsOn(LocalProject("core"), LocalProject("generics"))
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-circe",
    libraryDependencies += cats,
    libraryDependencies += circe,
    libraryDependencies += circeParser       % Test,
    libraryDependencies += kindlingsCirce    % Test,
    libraryDependencies += discipline        % Test,
  )

// Benchmarks deliberately stay OUT of the root aggregator: they're a
// JMH harness rather than a test, and we don't want `sbt test` or
// `sbt compile` to drag the JMH machinery / monocle dep in. Run them
// explicitly with e.g. `sbt benchmarks/Jmh/run -i 5 -wi 3 -f 1`.
lazy val benchmarks: Project = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .dependsOn(LocalProject("core"))
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-benchmarks",
    publish / skip := true,
    libraryDependencies += monocle,
  )
