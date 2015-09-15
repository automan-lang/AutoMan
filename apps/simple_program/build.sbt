packSettings

packMain := Map("simple_program" -> "simple_program")

name := "simple_program"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := "2.11.4"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "0.5-SNAPSHOT"
)
