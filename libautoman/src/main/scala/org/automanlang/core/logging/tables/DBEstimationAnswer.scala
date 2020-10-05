package org.automanlang.core.logging.tables

import scala.slick.driver.H2Driver.simple._

class DBEstimationAnswer(tag: Tag) extends Table[(Int, Double, String)](tag, "DBESTIMATIONANSWER") {
  def history_id = column[Int]("HISTORY_ID")
  def answer = column[Double]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}