lazy val commonSettings = Seq(
  organization := "com.peoplepattern",
  scalaVersion := "2.11.7",
  scalacOptions in ThisBuild ++= Seq(
    "-unchecked",
    "-feature",
    "-deprecation",
    "-language:_",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-target:jvm-1.7",
    "-encoding",
    "UTF-8"
  ),
  resolvers := Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.5" % "test"
  ),
  fork := true
) ++ scalariformSettings

lazy val core = project
  .in(file("levar-core"))
  .settings(commonSettings: _*)

lazy val web = project
  .in(file("levar-web"))
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala)
  .dependsOn(core)

lazy val cli = project
  .in(file("levar-cli"))
  .settings(commonSettings: _*)
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .aggregate(core, web, cli)

fork := true

publish := {}
