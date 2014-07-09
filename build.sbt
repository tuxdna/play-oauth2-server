name := "play-oauth2-server"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "3.1.1",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.7.1",
  "com.typesafe.play" %% "play-slick" % "0.6.1",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.xerial" % "sqlite-jdbc" % "3.7.2"
)

play.Project.playScalaSettings

instrumentSettings
