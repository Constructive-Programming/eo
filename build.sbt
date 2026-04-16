val scala3Version = "3.8.3"

val Typelevel     = "org.typelevel"
val ScalaCheckOrg = "org.scalacheck"
val Optics        = "dev.optics"
val Kubuszok      = "com.kubuszok"

lazy val cats           = Typelevel     %% "cats-core"         % "2.13.0"
lazy val disciplineCore = Typelevel     %% "discipline-core"   % "1.7.0"
lazy val discipline     = Typelevel     %% "discipline-specs2" % "2.0.0"
lazy val scalacheck     = ScalaCheckOrg %% "scalacheck"        % "1.17.1"
lazy val monocle        = Optics        %% "monocle-core"      % "3.3.0"
lazy val hearth         = Kubuszok      %% "hearth"            % "0.3.0"

lazy val commonSettings = Seq(
  version      := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
)

lazy val root: Project = project
  .in(file("."))
  .aggregate(core, laws, tests, generics)
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
  .dependsOn(LocalProject("core"), LocalProject("laws"))
  .settings(commonSettings *)
  .settings(
    name := "cats-eo-tests",
    publish / skip := true,
    libraryDependencies += discipline % Test,
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
