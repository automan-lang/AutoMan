packSettings

packMain := Map("anprlib" -> "anprlib")

name := "ANPR"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.2-SNAPSHOT",
  "com.amazonaws" % "aws-java-sdk" % "1.7.5" exclude("org.apache.httpcomponents", "httpclient")
)
