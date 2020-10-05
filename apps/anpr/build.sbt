enablePlugins(PackPlugin)

name := "ANPR"

version := "0.3"

organization := "org.automanlang"

scalaVersion := "2.12.12"

exportJars := true

libraryDependencies ++= Seq(
  "org.automanlang" %% "automan" % "1.4.0",
  "com.amazonaws"   % "aws-java-sdk" % "1.11.875"
)
