import sbt._
import Keys._

object Common {
  def ScalaVersion = "2.10.4"
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

  lazy val automan =
    Project(id = "automan",
      base = file("libautoman")
    )

  lazy val simple_program =
    Project(id = "simple_program",
      base = file("apps/simple_program")
    ) dependsOn automan

  lazy val HowManyThings =
    Project(id = "HowManyThings",
      base = file("apps/HowManyThings")
    ) dependsOn automan

  lazy val memo_log_reader =
    Project(id = "memo_log_reader",
      base = file("apps/memo_log_reader")
    ) dependsOn automan

  lazy val pay_unpaid_workers =
    Project(id = "pay_unpaid_workers",
      base = file("apps/pay_unpaid_workers")
    ) dependsOn automan

  lazy val delete_old_quals =
    Project(id = "delete_old_quals",
      base = file("apps/delete_old_quals")
    ) dependsOn automan

  lazy val root =
    Project(id = "automan_project", base = file("."))
      .settings(
        memoClean := {
          val memo_db = new File("AutomanMemoDB")
          val thunk_db = new File("ThunkLogDB")
          val derby_log = new File("derby.log")
          if (memo_db.exists()) {
            println(String.format("Removing %s", memo_db.toPath.toString))
            Common.DeleteRecursive(memo_db)
          }
          if (thunk_db.exists()) {
            println(String.format("Removing %s", thunk_db.toPath.toString))
            Common.DeleteRecursive(thunk_db)
          }
          if (derby_log.exists()) {
            println(String.format("Removing %s", derby_log.toPath.toString))
            Common.DeleteRecursive(derby_log)
          }
        }
      )
}
