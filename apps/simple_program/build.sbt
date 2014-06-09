packSettings

//import com.github.retronym.SbtOneJar._

//oneJarSettings

//packageOptions in oneJar ++=
//  Seq(sbt.Package.ManifestAttributes(
//    new java.util.Attributes.Name("one-jar.verbose") -> "some-file.txt"))

name := "simple_program"

packMain := Map("simple_program" -> "simple_program")

version := "0.2"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true
