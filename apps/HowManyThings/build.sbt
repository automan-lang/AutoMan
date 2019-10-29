enablePlugins(PackPlugin)

name := "HowManyThings"

version := "0.3"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs"    %% "automan"      % "1.3.0-SNAPSHOT",
  //"com.amazonaws"   %  "aws-java-sdk"	% "1.10.23",
  "com.amazonaws"   % "aws-java-sdk" % "1.11.637",
  //"software.amazon.awssdk" % "bom" % "2.9.24" pomOnly(),
  "org.imgscalr"    %  "imgscalr-lib"	% "4.2",
  "net.ettinsmoor"  %% "bingerator"		% "0.2.4"
)
