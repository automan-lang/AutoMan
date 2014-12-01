packSettings

packMain := Map("simple_program" -> "simple_program")

name := "simple_program"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true

libraryDependencies += "edu.umass.cs.plasma"  %%  "automandebugger" % "0.1-SNAPSHOT"