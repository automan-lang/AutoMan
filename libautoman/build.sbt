packSettings

name := "automan"

version := "0.4"

organization := "edu.umass.cs"

scalaVersion := Common.ScalaVersion

exportJars := true

resolvers += "Clojars" at "https://clojars.org/repo"

libraryDependencies ++= {
  val akkaVer   = "2.3.7"
  val sprayVer  = "1.3.2"
  Seq(
    "org.scala-lang.modules"     %%  "scala-xml"            % "1.0.2",
    "org.scalatest"              %%  "scalatest"            % "2.2.1"    % "test",
    "org.apache.derby"           %   "derby"                % "10.10.1.1",
    "net.java.dev.activeobjects" % "activeobjects"          % "0.8.2",
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
    "io.spray"                   %%  "spray-can"            % sprayVer,
    "io.spray"                   %%  "spray-routing"        % sprayVer,
    "io.spray"                   %%  "spray-testkit"        % sprayVer   % "test",
    "com.typesafe.akka"          %%  "akka-actor"           % akkaVer,
    "com.typesafe.akka"          %%  "akka-testkit"         % akkaVer    % "test",
    "org.specs2"                 %%  "specs2-core"          % "2.3.11" % "test"
//  "edu.umass.cs" % "mturkhitdecoupler_sjs0.5_2.10" % "1.0"
  )
}

//net.virtualvoid.sbt.graph.Plugin.graphSettings
