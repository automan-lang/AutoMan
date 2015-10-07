// METADATA
name := "automan"

version := "1.0.0"

organization := "edu.umass.cs"

licenses := Seq("GPL-2.0" -> url("http://opensource.org/licenses/GPL-2.0"))

homepage := Some(url("http://github.com/dbarowy/AutoMan"))

scalaVersion := "2.11.7"

exportJars := true

// SUPPORTED SCALA VERSIONS
crossScalaVersions := Seq("2.10.6", "2.11.7")

// DEPENDENCIES
libraryDependencies := {
  val akkaVer   = "2.3.7"
  val sprayVer  = "1.3.2"
  Seq(
    "org.scalatest"              %%  "scalatest"            % "2.2.1"    % "test",
    "commons-codec"              % "commons-codec"          % "1.4",
    "log4j"                      % "log4j"                  % "1.2.17",
    "net.ettinsmoor"         % "java-aws-mturk"         % "1.6.2"
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

// add scala-xml if scala major version >= 11
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
    case _ =>
      libraryDependencies.value
  }
}

// CUSTOM TASKS
val memoClean = TaskKey[Unit]("memo-clean", "Deletes AutoMan memo database files.")

memoClean := {
  val dbs = Seq.concat(
    (baseDirectory.value ** "*.mv.db").get,
    (baseDirectory.value ** "*.trace.db").get
  )
  dbs.foreach { f: File =>
    println(s"Deleting: ${f.getName}")
    f.delete()
  }
}

// GENERATING JAR
packSettings

// TESTING
concurrentRestrictions in Global := Seq(
  Tags.limit(Tags.Test, 1)
)

parallelExecution in Test := false

// MAVEN

// yes, we want Maven artifacts
publishMavenStyle := true

// specify repository
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

// don't publish test artifacts
publishArtifact in Test := false

// don't include optional repositories
pomIncludeRepository := { _ => false }

// POM body
pomExtra := (
  <scm>
    <url>git@github.com:dbarowy/AutoMan.git</url>
    <connection>scm:git:git@github.com:dbarowy/AutoMan.git</connection>
  </scm>
  <developers>
    <developer>
      <id>dbarowy</id>
      <name>Daniel Barowy</name>
      <url>http://people.cs.umass.edu/~dbarowy</url>
    </developer>
  </developers>)
