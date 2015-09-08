name := "levar-cli"

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "com.typesafe" % "config" % "1.3.0")

enablePlugins(JavaAppPackaging)
