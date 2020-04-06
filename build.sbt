import Dependencies._
import play.sbt.routes.RoutesKeys.routesGenerator
import sbtcrossproject.{CrossType, crossProject}

enablePlugins(JavaAppPackaging)

lazy val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  organization := "jp.skulking",
  organizationName := "skulking",
  scalaVersion := "2.13.0"
)

lazy val root = (project in file("."))
  .aggregate(server, client)

lazy val server = (project in file("skulking-server"))
  .settings(commonSettings)
  .settings(
    name := "skulking-server",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-http" % "10.1.11",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "com.vmunier" %% "scalajs-scripts" % "1.1.4",
      "org.specs2" %% "specs2-core" % "4.9.3" % Test,
      scalaTest % Test
    ),
    routesGenerator := InjectedRoutesGenerator,
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
  )
  .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
  .aggregate(client)
  .dependsOn(sharedJvm)

lazy val client = (project in file("skulking-client"))
  .settings(commonSettings)
  .enablePlugins(ScalaJSWeb, ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(
    name := "skulking-client",
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.0.0",
      "com.lihaoyi" %%% "scalatags" % "0.9.1",
      "be.doeraene" %%% "scalajs-jquery" % "1.0.0",
      "org.akka-js" %%% "akkajsactortyped" % "2.2.6.3",
      scalaTest % Test
    ),
    npmDependencies in Compile ++= Seq(
      "jquery" -> "2.1.3")
  ).dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %%% "play-json" % "2.8.1",
      scalaTest % Test
    ))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
