//enablePlugins(PackPlugin)

name := "linda_test_survey"

version := "0.1"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.4.0-SNAPSHOT",
  "au.com.bytecode"            % "opencsv"                % "2.4"
)