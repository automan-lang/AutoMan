package edu.umass.cs.automan.core.memoizer

import scala.slick.driver.H2Driver.simple._

class AutomanMemoizer {
  val jdbc_conn_string = "jdbc:derby:AutoManMemoDB"

  Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver") withSession {
    implicit session =>
    // <- write queries here
  }
}
