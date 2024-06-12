ThisBuild / version := "0.3-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

val zioVersion = "2.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "twelvedata-zio"
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-json" % "0.6.2",
  "dev.zio" %% "zio-config-magnolia" % "4.0.2",
  "dev.zio" %% "zio-config-typesafe" % "4.0.2",
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-http" % "3.0.0-RC2"
)


  // testing --------------------------------------------------

// for some reason ipv6 is enabled by default and is broken
Global / javaOptions += "-Djava.net.preferIPv4Stack=true"
Global / fork := true

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt"      % zioVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

libraryDependencies += "org.specs2" %% "specs2-core" % "5.2.0" % Test
