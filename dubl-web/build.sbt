name := "dubl-web"

libraryDependencies ++= Seq(
  cache,
  ws,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.scalikejdbc" %% "scalikejdbc" % "2.1.0",
  "org.mindrot" % "jbcrypt" % "0.3m")
