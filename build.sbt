val scala3Version = "3.1.2"

val Typelevel = "org.typelevel"

lazy val discipline = Typelevel %% "discipline-specs2" % "2.0.0"

lazy val cats = Typelevel %% "cats-core" % "2.7.0"

lazy val shapeless = Typelevel %% "shapeless3-deriving" % "3.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "cats-eo",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += cats,
    libraryDependencies += shapeless,
    libraryDependencies += discipline % Test
  )
