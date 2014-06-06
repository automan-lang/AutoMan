name := "HowManyThings"

version := "0.2"

scalaVersion := Common.ScalaVersion

exportJars := true

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.7.5" exclude("org.apache.httpcomponents", "httpclient")

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

libraryDependencies += "net.ettinsmoor" % "bingerator_2.10" % "0.2.2"

//net.virtualvoid.sbt.graph.Plugin.graphSettings
