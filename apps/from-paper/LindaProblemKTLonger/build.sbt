enablePlugins(PackPlugin)

name := "LindaProblemKTLonger"

version := "0.1"

organization := "org.automanlang"

scalaVersion := "2.12.12"

exportJars := true

libraryDependencies ++= Seq(
  // NEED TO RUN `sbt publishLocal` on libautoman first
  "org.automanlang" %% "automan" % "1.4.3-SNAPSHOT"
)
