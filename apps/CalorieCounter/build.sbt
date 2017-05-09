packSettings

packMain := Map("CalorieCounter" -> "CalorieCounter")

name := "CalorieCounter"

version := "1.0"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.2.0"
)
