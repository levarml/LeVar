name := "levar-web"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.scalatestplus" %% "play" % "1.4.0" % "test",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.scalikejdbc" %% "scalikejdbc" % "2.1.0")

publish := {}
