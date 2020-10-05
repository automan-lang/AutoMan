package org.automanlang.core.logging.tables

import scala.slick.driver.H2Driver.simple._

object DBRadioButtonAnswer {
  val symbolStringMapper = MappedColumnType.base[Symbol, String](
  { sym => sym.toString().drop(1) },  // map Symbol to String
  { str => Symbol(str) }              // map String to Symbol
  )
}

class DBRadioButtonAnswer(tag: Tag) extends Table[(Int, Symbol, String)](tag, "DBRADIOBUTTONANSWER") {
  // implicit conversion for Symbol <-> String
  implicit val symbolColumnType = DBRadioButtonAnswer.symbolStringMapper

  def history_id = column[Int]("HISTORY_ID")
  def answer = column[Symbol]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}