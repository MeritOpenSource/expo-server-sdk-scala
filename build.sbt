name := "ExpoClient"

version := "0.1"

scalaVersion := "2.12.6"

organization := "com.merits"
homepage := Some(url("https://merits.com/"))
//scmInfo := Some(ScmInfo(url("https://github.com/Merit")))
lazy val myProject = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
headerLicense := Some(HeaderLicense.MIT("2020", "Merit International Inc."))

libraryDependencies += "com.twitter" %% "finagle-http" % "19.8.0"
libraryDependencies += "com.twitter" %% "finagle-base-http" % "19.8.0"
libraryDependencies += "io.circe" %% "circe-core" % "0.9.0"
libraryDependencies += "io.circe" %% "circe-generic" % "0.9.0"
libraryDependencies += "io.circe" %% "circe-parser" % "0.9.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.0.0"