enablePlugins(PackPlugin)

name := "SimpleFTDQuestion"

version := "1.0"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.3.0-SNAPSHOT"
)
