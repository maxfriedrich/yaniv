name := "yaniv"
organization := "de.maxfriedrich"
version := "1.0-SNAPSHOT"

scalaVersion := "2.12.11"

lazy val game = (project in file("game"))
  .settings(name := "yaniv-game")
  .settings(
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.2" % Test
  )

lazy val ai = (project in file("ai"))
  .settings(name := "yaniv-ai")
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.2" % Test
  )
  .dependsOn(game)

lazy val rest = (project in file("rest"))
  .settings(name := "yaniv-rest")
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies += guice,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )
  .dependsOn(game, ai)

lazy val root = (project in file("."))
  .aggregate(game, ai, rest)
