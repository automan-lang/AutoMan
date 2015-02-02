package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._

class RadioButtonAnswer(tag: Tag) extends Table[(UUID, UUID, Symbol)](tag, "RADIOBUTTONANSWER") {
  // implicit conversion for Symbol <-> String
  implicit val symbolColumnType = MappedColumnType.base[Symbol, String](
    { sym => sym.toString() },  // map Symbol to String
    { str => Symbol(str) }      // map String to Symbol
  )

  def id = column[UUID]("RADIOBUTTONANSWER_ID", O.PrimaryKey, O.DBType("UUID"))
  def thunk_id = column[UUID]("THUNK_ID", O.DBType("UUID"))
  def answer = column[Symbol]("ANSWER")
  override def * = (id, thunk_id, answer)
}