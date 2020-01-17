enablePlugins(PackPlugin)

name := "simple_survey"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.4.0-SNAPSHOT"
)
