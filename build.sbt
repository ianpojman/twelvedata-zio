ThisBuild / version := "0.1.11-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "twelvedata-zio"
  )

libraryDependencies += "org.specs2" %% "specs2-core" % "5.2.0" % Test
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-json" % "0.5.0",
  "dev.zio" %% "zio-config-magnolia" % "4.0.0-RC14",
  "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC14",
  "dev.zio" %% "zio-http" % "3.0.0-RC1",
  "dev.zio" %% "zio-streams" % "2.0.13"
)
