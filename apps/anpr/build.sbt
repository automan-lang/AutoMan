packSettings

packMain := Map("anpr" -> "anpr")

name := "ANPR"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.7.5" exclude("org.apache.httpcomponents", "httpclient")


