enablePlugins(PackPlugin)

name := "BananaQuestion"

version := "0.3"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.3.0-SNAPSHOT",
  "com.amazonaws"   %  "aws-java-sdk"	% "1.11.654",
  "org.imgscalr"    %  "imgscalr-lib"	% "4.2",
  "software.amazon.awssdk" % "mturk" % "2.9.5"
)
