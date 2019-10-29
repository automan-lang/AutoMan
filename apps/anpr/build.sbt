enablePlugins(PackPlugin)

name := "ANPR"

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.3.0-SNAPSHOT",
  "com.amazonaws"   % "aws-java-sdk" % "1.11.637"
  //"com.amazonaws" % "aws-java-sdk" % "1.7.5" exclude("org.apache.httpcomponents", "httpclient")
)
