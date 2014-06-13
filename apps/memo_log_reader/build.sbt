packSettings

packMain := Map("memo_log_reader" -> "memo_log_reader")

name := "memo_log_reader"

version := "0.1"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true

libraryDependencies += "org.apache.derby" % "derby" % "10.10.1.1"

libraryDependencies += "net.java.dev.activeobjects" % "activeobjects" % "0.8.2"