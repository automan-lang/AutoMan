package edu.umass.cs.automan.core.logging.tables

import scala.slick.driver.DerbyDriver.simple._

class RadioButtonAnswer(tag: Tag) extends Table[(Int, Symbol, String)](tag, "RADIOBUTTONANSWER") {
  // implicit conversion for Symbol <-> String
  implicit val symbolColumnType = MappedColumnType.base[Symbol, String](
    { sym => sym.toString() },  // map Symbol to String
    { str => Symbol(str) }      // map String to Symbol
  )

  def history_id = column[Int]("HISTORY_ID")
  def answer = column[Symbol]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}