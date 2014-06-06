import com.github.retronym.SbtOneJar._

oneJarSettings

name := "automan"

version := "0.3"

scalaVersion := Common.ScalaVersion

exportJars := true

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies += "org.apache.derby" % "derby" % "10.10.1.1"

libraryDependencies += "net.java.dev.activeobjects" % "activeobjects" % "0.8.2"

libraryDependencies += "commons-codec" % "commons-codec" % "1.4"

libraryDependencies += "axis" % "axis" % "1.4"

libraryDependencies += "commons-httpclient" % "commons-httpclient" % "3.1"

libraryDependencies += "log4j" % "log4j" % "1.2.17"

//net.virtualvoid.sbt.graph.Plugin.graphSettings

