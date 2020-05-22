name := """yaniv"""
organization := "de.maxfriedrich"
version := "1.0-SNAPSHOT"

scalaVersion := "2.13.1"

lazy val game = (project in file("game"))
  .withId("yaniv-game")
  .settings(
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.0",
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )

lazy val rest = (project in file("rest"))
  .withId("yaniv-rest")
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies += guice
  )
  .dependsOn(game)
