lazy val commonSettings = Seq(
  organization := "com.peoplepattern",
  scalaVersion := "2.11.7",
  scalacOptions in ThisBuild ++= Seq(
    "-unchecked",
    "-feature",
    "-deprecation",
    "-language:_",
    "-Xlint",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-target:jvm-1.8",
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

lazy val web = project
  .in(file("modeleval-web"))
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala)

lazy val cli = project
  .in(file("modeleval-cli"))
  .settings(commonSettings: _*)

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .aggregate(web, cli)

fork := true

publish := {}
