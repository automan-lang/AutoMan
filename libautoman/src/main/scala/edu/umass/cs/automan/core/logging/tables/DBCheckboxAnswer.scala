package edu.umass.cs.automan.core.logging.tables

import scala.slick.driver.SQLiteDriver.simple._

object DBCheckboxAnswer {
  val symbolStringMapper = MappedColumnType.base[Set[Symbol], String](
    { syms => syms.map(_.toString()).mkString },        // map Set[Symbol] to String
    { str => str.split("'").tail.map(Symbol(_)).toSet } // map String to Set[Symbol]
  )
}

class DBCheckboxAnswer(tag: Tag) extends Table[(Int, Set[Symbol], String)](tag, "DBCHECKBOXANSWER") {
  // implicit conversion for Set[Symbol] <-> String
  implicit val symbolColumnType = DBCheckboxAnswer.symbolStringMapper

  def history_id = column[Int]("HISTORY_ID")
  def answer = column[Set[Symbol]]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}