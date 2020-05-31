name := "yaniv"
organization := "de.maxfriedrich"
version := "1.0-SNAPSHOT"

scalaVersion := "2.13.1"

lazy val game = (project in file("game"))
  .settings(name := "yaniv-game")
  .settings(
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.0",
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )

lazy val ai = (project in file("ai"))
  .settings(name := "yaniv-ai")
  .dependsOn(game)

lazy val rest = (project in file("rest"))
  .settings(name := "yaniv-rest")
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies += guice,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )
  .dependsOn(game)

lazy val root = (project in file("."))
  .aggregate(game, ai, rest)
