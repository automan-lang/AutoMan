packSettings

packMain := Map("SimpleSurvey" -> "SimpleSurvey")

name := "SimpleSurvey"

version := "0.1"

organization := "edu.umass.cs"

scalaVersion := "2.11.4"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "0.5-SNAPSHOT"
)
