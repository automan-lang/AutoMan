import sbt._
import Keys._

object HelloBuild extends Build {
    // lazy val root = Project(id = "all",
    //                         base = file("."))
    // 										aggregate(automan, simple_program)
    // 										dependsOn(automan, simple_program)

    lazy val automan = Project(id = "automan",
                           		 base = file("libautoman"))

    lazy val simple_program = Project(id = "simple_program",
                           					  base = file("apps/simple_program")) dependsOn(automan)
}
