package edu.umass.cs.automan.core.logging.tables

import scala.slick.driver.H2Driver.simple._

object DBMultiEstimationAnswer {
  val estimateArrayMapper = MappedColumnType.base[Array[Double], String](
    { arr => arr.map(_.toString()).mkString("/") }, // map Array[Double] to String
    { str => str.split("/").map(_.toDouble) }       // map String to Array[Double]
  )
}

class DBMultiEstimationAnswer(tag: Tag) extends Table[(Int, Array[Double], String)](tag, "DBMULTIESTIMATIONANSWER") {
  // implicit conversion for Array[Double] <-> String
  implicit val symbolColumnType = DBMultiEstimationAnswer.estimateArrayMapper

  def history_id = column[Int]("HISTORY_ID")
  def answers = column[Array[Double]]("ANSWERS")
  def worker_id = column[String]("WORKER_ID")
  override def * = (history_id, answers, worker_id)
}