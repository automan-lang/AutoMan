enablePlugins(PackPlugin)

name := "CalorieCounter"

version := "1.1"

organization := "org.automanlang"

scalaVersion := "2.12.12"

exportJars := true

libraryDependencies ++= Seq(
  "org.automanlang" %% "automan" % "1.4.0"
)
