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
  buildInfoKeys := Seq[BuildInfoKey](version),
  buildInfoPackage := "levar.build",
  bintrayOrganization := Some("peoplepattern"),
  bintrayRepository := "levar",
  publishMavenStyle := false,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("http://peoplepattern.github.io/LeVar")),
  crossScalaVersions := Seq("2.10.6", "2.11.7"),
  releaseCrossBuild := true,
  fork := true
) ++ scalariformSettings

lazy val core = project
  .in(file("levar-core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)

lazy val web = project
  .in(file("levar-web"))
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala)
  .dependsOn(core)

lazy val client = project
  .in(file("levar-client"))
  .settings(commonSettings: _*)
  .dependsOn(core)

lazy val cli = project
  .in(file("levar-cli"))
  .settings(commonSettings: _*)
  .dependsOn(client)

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .settings(
    site.settings,
    ghpages.settings,
    git.remoteRepo := "git@github.com:peoplepattern/LeVar.git",
    site.jekyllSupport()
  )
  .aggregate(core, web, client, cli)

fork := true

publish := {}
