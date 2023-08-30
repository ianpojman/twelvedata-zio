ThisBuild / version := "0.2.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "twelvedata-zio"
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-json" % "0.5.0",
  "dev.zio" %% "zio-config-magnolia" % "4.0.0-RC14",
  "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC14",
  "dev.zio" %% "zio-streams" % "2.0.13",
  "dev.zio" %% "zio-http" % "3.0.0-RC2"
)


  // testing --------------------------------------------------

// for some reason ipv6 is enabled by default and is broken
Global / javaOptions += "-Djava.net.preferIPv4Stack=true"
Global / fork := true

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.0.15" % Test,
  "dev.zio" %% "zio-test-sbt"      % "2.0.15" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.0.15" % Test
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

libraryDependencies += "org.specs2" %% "specs2-core" % "5.2.0" % Test
