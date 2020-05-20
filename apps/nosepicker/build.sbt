name := "nosepicker"

version := "0.1"

organization := "edu.umass.cs"

scalaVersion := "2.11.7"

exportJars := true

libraryDependencies ++= Seq(
  "edu.umass.cs" %% "automan" % "1.4.0-SNAPSHOT",
  "au.com.bytecode"            % "opencsv"                % "2.4",
  "org.rogach" %% "scallop" % "3.4.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6"
)