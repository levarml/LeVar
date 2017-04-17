import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "levar-cli"

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "com.typesafe" % "config" % "1.3.0",
  "org.scalanlp" %% "breeze" % "0.11.2")

enablePlugins(JavaAppPackaging)
