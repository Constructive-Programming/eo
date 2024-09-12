val scala3Version = "3.5.0"

val Typelevel = "org.typelevel"

lazy val cats = Typelevel %% "cats-core" % "2.12.0"
lazy val discipline = Typelevel %% "discipline-specs2" % "2.0.0"
lazy val scodec = "org.scodec" %% "scodec-core" % "2.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "cats-eo",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += cats,
    libraryDependencies += scodec % Test,
    libraryDependencies += discipline % Test
  )
