packSettings

name := "automan"

version := "0.4"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true

resolvers += "Clojars" at "https://clojars.org/repo"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies += "org.apache.derby" % "derby" % "10.10.1.1"

libraryDependencies += "net.java.dev.activeobjects" % "activeobjects" % "0.8.2"

libraryDependencies += "commons-codec" % "commons-codec" % "1.4"

libraryDependencies += "log4j" % "log4j" % "1.2.17"

libraryDependencies += "org.clojars.zaxtax" % "java-aws-mturk" % "1.6.2" exclude("org.apache.commons","not-yet-commons-ssl") exclude("apache-xerces","xercesImpl") exclude("apache-xerces","resolver") exclude("apache-xerces","xml-apis") exclude("velocity","velocity") exclude("org.apache.velocity","velocity") exclude("commons-beanutils","commons-beanutils")

libraryDependencies += "ca.juliusdavies" % "not-yet-commons-ssl" % "0.3.11"

libraryDependencies += "xerces" % "xercesImpl" % "2.9.1"

libraryDependencies += "org.apache.velocity" % "velocity" % "1.6.2"

libraryDependencies += "commons-beanutils" % "commons-beanutils-core" % "1.7.0"

//libraryDependencies += "edu.umass.cs" % "mturkhitdecoupler_sjs0.5_2.10" % "1.0"

//net.virtualvoid.sbt.graph.Plugin.graphSettings
