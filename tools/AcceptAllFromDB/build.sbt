packSettings

packMain := Map("AcceptAllFromDB" -> "edu.umass.cs.automan.adapters.mturk.AcceptAllFromDB")

name := "AcceptAllFromDB"

version := "1.0"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs"        %% "automan" % "1.1.0-SNAPSHOT",
  "org.rogach"          %% "scallop" % "0.9.5",
  "com.typesafe.slick"  %% "slick"   % "2.1.0",
  "com.h2database"      %  "h2"      % "1.4.189"
)
