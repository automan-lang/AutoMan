// METADATA
name := "automan"

version := "1.4.3-SNAPSHOT"

organization := "org.automanlang"

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
    "org.apache.logging.log4j"   % "log4j-core"             % "2.17.1",
    "org.specs2"                 %% "specs2-core"           % "4.10.2" % Test,
    "com.typesafe.slick"         %% "slick"                 % "2.1.0",
    "software.amazon.awssdk"     % "mturk"                  % "2.17.134",
    "com.amazonaws"              % "aws-java-sdk-mechanicalturkrequester" % "1.11.875",
    "com.h2database"             % "h2"                     % "1.4.189",
    "org.slf4j"                  % "slf4j-nop"              % "1.6.4",
    "org.apache.logging.log4j"   % "log4j-core"             % "2.13.0",
    "au.com.bytecode"            % "opencsv"                % "2.4"
  )
}

// CUSTOM TASKS
val memoClean = TaskKey[Unit]("memo-clean", "Deletes AutoMan memo database files.")
val hashAsConstant = TaskKey[Unit]("hash-as-constant", "Creates a Scala source file containing the git hash for HEAD.")

// memoClean task
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

// gitHash task
val gitHash = TaskKey[String]("githash", "Gets the git hash of HEAD.")

gitHash := {
  import scala.sys.process._
  ("git rev-parse HEAD" !!).replace("\n","")
}

hashAsConstant := {
  import java.io._

  val path = "src/main/scala/org/automanlang/core/util/GitHash.scala"

  val clazz =
    "package org.automanlang.core.util\n\n" +
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
sonatypeBundleDirectory := (ThisBuild / baseDirectory).value / target.value.getName / "sonatype-staging" / s"${version.value}"

// this setting bundles all artifacts into a single JAR
publishTo := sonatypePublishToBundle.value

// set account profile name
sonatypeProfileName := "org.automanlang"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// license
licenses := Seq("GPLv2" -> url("https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"))

// SCM
homepage := Some(url("https://automan-lang.github.io/"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/automan-lang/AutoMan"),
    "scm:git@github.com:automan-lang/AutoMan.git"
  )
)
developers := List(
  Developer(
    id="dbarowy",
    name="Daniel W. Barowy",
    email="dbarowy@cs.williams.edu",
    url=url("https://www.cs.williams.edu/~dbarowy")
  )
)

// don't trap System.exit() calls-- really quit SBT
trapExit := false