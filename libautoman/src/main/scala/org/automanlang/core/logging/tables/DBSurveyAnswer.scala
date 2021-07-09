package org.automanlang.core.logging.tables

import scala.slick.driver.H2Driver.simple._

object DBSurveyAnswer {
  val symbolListAnyStringMapper = MappedColumnType.base[List[Any], String](
    { syms => syms.map(_.toString()).mkString },

    // TEMPORARY FIX

    { str => List(Set(Symbol("test"))) }
  )
}

class DBSurveyAnswer(tag: Tag) extends Table[(Int, List[Any], String)](tag, "DBSurveyANSWER") {
  // implicit conversion for Set[Symbol] <-> String
  implicit val symbolColumnType = DBSurveyAnswer.symbolListAnyStringMapper

  def history_id = column[Int]("HISTORY_ID")
  def answer = column[List[Any]]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}