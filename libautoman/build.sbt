packSettings

name := "automan"

version := "0.5-SNAPSHOT"

organization := "edu.umass.cs"

scalaVersion := "2.11.4"

exportJars := true

resolvers += "Clojars" at "https://clojars.org/repo"

libraryDependencies ++= {
  val akkaVer   = "2.3.7"
  val sprayVer  = "1.3.2"
  Seq(
    "org.scala-lang.modules"     %%  "scala-xml"            % "1.0.2",
    "org.scalatest"              %%  "scalatest"            % "2.2.1"    % "test",
    "commons-codec"              % "commons-codec"          % "1.4",
    "log4j"                      % "log4j"                  % "1.2.17",
    "org.clojars.zaxtax"         % "java-aws-mturk"         % "1.6.2"
      exclude("org.apache.commons","not-yet-commons-ssl")
      exclude("apache-xerces","xercesImpl")
      exclude("apache-xerces","resolver")
      exclude("apache-xerces","xml-apis")
      exclude("velocity","velocity")
      exclude("org.apache.velocity","velocity")
      exclude("commons-beanutils","commons-beanutils"),
    "ca.juliusdavies"            % "not-yet-commons-ssl"    % "0.3.11",
    "xerces"                     % "xercesImpl"             % "2.9.1",
    "org.apache.velocity"        % "velocity"               % "1.6.2",
    "commons-beanutils"          % "commons-beanutils-core" % "1.7.0",
    "org.specs2"                 %%  "specs2-core"          % "2.3.11" % "test",
    "com.typesafe.slick"         %% "slick"                 % "2.1.0",
    "com.h2database"             % "h2"                     % "1.4.189",
    "org.slf4j"                  % "slf4j-nop"              % "1.6.4"
  )
}

concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1)
)

parallelExecution in Test := false

