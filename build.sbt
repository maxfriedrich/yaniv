name := """yaniv"""
organization := "de.maxfriedrich"

version := "1.0-SNAPSHOT"

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

scalaVersion := "2.13.1"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "de.maxfriedrich.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "de.maxfriedrich.binders._"
