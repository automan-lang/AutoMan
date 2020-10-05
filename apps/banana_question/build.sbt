enablePlugins(PackPlugin)

name := "BananaQuestion"

version := "0.4"

organization := "org.automanlang"

scalaVersion := "2.12.12"

exportJars := true

libraryDependencies ++= Seq(
  "org.automanlang" %% "automan" % "1.4.0",
  "com.amazonaws"   % "aws-java-sdk" % "1.11.875",
  "org.imgscalr"    %  "imgscalr-lib"	% "4.2",
  "software.amazon.awssdk" % "mturk" % "2.15.2"
)
