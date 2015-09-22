package edu.umass.cs.automan.core.logging.tables

import scala.slick.driver.H2Driver.simple._

class DBFreeTextAnswer(tag: Tag) extends Table[(Int, String, String)](tag, "DBFREETEXTANSWER") {
  def history_id = column[Int]("HISTORY_ID")
  def answer = column[String]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}