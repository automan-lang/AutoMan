import sbt._
import Keys._

object Common {
  def ScalaVersion = "2.10.4"
}

object AutoManBuild extends Build {
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
}
