import sbt._
import Keys._

object Common {
  def ScalaVersion = "2.10.4"
}

object HelloBuild extends Build {
    lazy val automan = Project(id = "automan",
                           		 base = file("libautoman"))

    lazy val simple_program = Project(id = "simple_program",
                           					  base = file("apps/simple_program")) dependsOn(automan)

		lazy val HowManyThings = Project(id = "HowManyThings",
                           					  base = file("apps/HowManyThings")) dependsOn(automan)
}
