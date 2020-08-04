// METADATA
name := "automan"

version := "1.4.0-SNAPSHOT"

organization := "edu.umass.cs"

licenses := Seq("GPL-2.0" -> url("http://opensource.org/licenses/GPL-2.0"))

homepage := Some(url("http://github.com/dbarowy/AutoMan"))

scalaVersion := "2.12.12"

exportJars := true

// SUPPORTED SCALA VERSIONS
crossScalaVersions := Seq("2.11.7")

// DEPENDENCIES
libraryDependencies := {
  val akkaVer   = "2.3.7"
  val sprayVer  = "1.3.2"
  Seq(
    "org.scala-lang.modules"     %% "scala-xml"             % "2.0.0-M1",
    "org.scalatest"              %% "scalatest"             % "3.0.8" % Test,
    "log4j"                      % "log4j"                  % "1.2.17",
    "org.specs2"                 %% "specs2-core"           % "4.10.2" % Test,
    "com.typesafe.slick"         %% "slick"                 % "2.1.0",
    "software.amazon.awssdk"     % "mturk"                  % "2.9.5",
    "com.amazonaws"              % "aws-java-sdk-mechanicalturkrequester" % "1.11.637",
    "com.h2database"             % "h2"                     % "1.4.189",
    "org.slf4j"                  % "slf4j-nop"              % "1.6.4",
    "org.apache.logging.log4j"   % "log4j-core"             % "2.13.0",
    "au.com.bytecode"            % "opencsv"                % "2.4"
  )
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

val gitHash = TaskKey[String]("githash", "Gets the git hash of HEAD.")

gitHash := {
  import scala.sys.process._
  ("git rev-parse HEAD" !!).replace("\n","")
}

val hashAsConstant = TaskKey[Unit]("hash-as-constant", "Creates a Scala source file containing the git hash for HEAD.")

hashAsConstant := {
  import java.io._

  val path = "src/main/scala/edu/umass/cs/automan/core/util/GitHash.scala"

  val clazz =
    "package edu.umass.cs.automan.core.util\n\n" +
    "object GitHash {\n" +
    "  val value = \"" + gitHash.value + "\"\n" +
    "}"

  val pw = new PrintWriter(new File(path))
  pw.write(clazz)
  pw.close()
}

// TEST CONFIGURATION
parallelExecution in Test := false

// MODIFY BUILD
compile := ((compile in Compile) dependsOn hashAsConstant).value

// GENERATING JAR
enablePlugins(PackPlugin)

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

trapExit := false