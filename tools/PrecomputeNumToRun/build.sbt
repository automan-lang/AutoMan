packSettings

packMain := Map("PrecomputeNumToRun" -> "edu.umass.cs.automan.tools.PrecomputeNumToRun")

name := "AcceptAllFromDB"

version := "1.0"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs"        %% "automan" % "1.1.0-SNAPSHOT"
)