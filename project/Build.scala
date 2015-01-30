import sbt._
import Keys._

object Common {
  def ScalaVersion = "2.11.4"
  def DeleteRecursive(f: File) {
    if (f.isDirectory) {
      for(subf <- f.listFiles()) {
        DeleteRecursive(subf)
      }
    }
    if (!f.delete()) {
      // don't fail, just tell the user that we could not succeed
      System.err.println("Could not delete file: " + f)
    }
  }
}

object AutoManBuild extends Build {
  lazy val memoClean = TaskKey[Unit]("memo-clean", "Deletes AutoMan's memoization database.")

  lazy val root =
    Project(id = "automan", base = file("libautoman"))
      .settings(
        memoClean := {
          val memo_db = new File("AutoManMemoDB")
          val thunk_db = new File("ThunkLogDB")
          val derby_log = new File("derby.log")
          if (memo_db.exists()) {
            println(String.format("Removing %s", memo_db.toString))
            Common.DeleteRecursive(memo_db)
          }
          if (thunk_db.exists()) {
            println(String.format("Removing %s", thunk_db.toString))
            Common.DeleteRecursive(thunk_db)
          }
          if (derby_log.exists()) {
            println(String.format("Removing %s", derby_log.toString))
            Common.DeleteRecursive(derby_log)
          }
        }
      )

  lazy val SimpleProgram =
    Project(id = "SimpleProgram",
      base = file("apps/simple_program")
    ) dependsOn root

  lazy val SimpleCheckboxProgram =
    Project(id = "SimpleCheckboxProgram",
      base = file("apps/simple_checkbox_program")
    ) dependsOn root

  lazy val SimpleCBDQuestion =
    Project(id = "SimpleCBDQuestion",
      base = file("apps/SimpleCBDQuestion")
    ) dependsOn root

  lazy val SimpleFTDQuestion =
    Project(id = "SimpleFTDQuestion",
      base = file("apps/SimpleFTDQuestion")
    ) dependsOn root

  lazy val SimpleSurvey =
    Project(id = "SimpleSurvey",
      base = file("apps/SimpleSurvey")
    ) dependsOn root

  lazy val HowManyThings =
    Project(id = "HowManyThings",
      base = file("apps/HowManyThings")
    ) dependsOn root

  lazy val BananaQuestion =
    Project(id = "BananaQuestion",
      base = file("apps/banana_question")
    ) dependsOn root

  lazy val LicensePlateReader =
    Project(id = "LicensePlateReader",
      base = file("apps/license_plate_reader")
    ) dependsOn root

  lazy val ANPR =
    Project(id = "ANPR",
      base = file("apps/anpr")
    ) dependsOn root
}
