package org.automanlang.core.logging.tables

import scala.slick.driver.H2Driver.simple._

object DBHugoAnswer {
  val symbolSetTupleStringMapper = MappedColumnType.base[(Set[Symbol], Set[Symbol]), String](
    { syms => syms._1.map(_.toString()).mkString + "..." + syms._2.map(_.toString()).mkString },        // map Set[Symbol] to String

    // TEMPORARY FIX

    { str => (str.split("'").tail.map(Symbol(_)).toSet, Set(Symbol("test"))) } // map String to Set[Symbol]
  )
}

class DBHugoAnswer(tag: Tag) extends Table[(Int, (Set[Symbol], Set[Symbol]), String)](tag, "DBHUGOANSWER") {
  // implicit conversion for Set[Symbol] <-> String
  implicit val symbolColumnType = DBHugoAnswer.symbolSetTupleStringMapper

  def history_id = column[Int]("HISTORY_ID")
  def answer = column[(Set[Symbol], Set[Symbol])]("ANSWER")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answer, worker_id)
}