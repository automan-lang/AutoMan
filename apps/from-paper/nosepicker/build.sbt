name := "nosepicker"

version := "0.2"

organization := "org.automanlang"

scalaVersion := "2.12.12"

exportJars := true

libraryDependencies ++= Seq(
  "org.automanlang" 	%% "automan"	% "1.4.2",
  "au.com.bytecode"	% "opencsv"	% "2.4",
  "org.rogach"		%% "scallop"	% "3.4.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6"
)