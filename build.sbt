val scala3Version = "3.8.3"

val Typelevel     = "org.typelevel"
val ScalaCheckOrg = "org.scalacheck"

lazy val cats           = Typelevel     %% "cats-core"         % "2.13.0"
lazy val disciplineCore = Typelevel     %% "discipline-core"   % "1.7.0"
lazy val discipline     = Typelevel     %% "discipline-specs2" % "2.0.0"
lazy val scalacheck     = ScalaCheckOrg %% "scalacheck"        % "1.17.1"

lazy val commonSettings = Seq(
  version      := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
)

lazy val root: Project = project
  .in(file("."))
  .aggregate(core, laws, tests)
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
