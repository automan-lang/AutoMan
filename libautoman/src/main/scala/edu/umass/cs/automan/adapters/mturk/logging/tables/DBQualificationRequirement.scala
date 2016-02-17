package edu.umass.cs.automan.adapters.mturk.logging.tables

import com.amazonaws.mturk.requester.Comparator
import scala.slick.driver.H2Driver.simple._

object DBQualificationRequirement {
  def comparatorMapper = MappedColumnType.base[Comparator, Int](
  {
    case Comparator.EqualTo => 0
    case Comparator.Exists => 1
    case Comparator.GreaterThan => 2
    case Comparator.GreaterThanOrEqualTo => 3
    case Comparator.LessThan => 4
    case Comparator.LessThanOrEqualTo => 5
    case Comparator.NotEqualTo => 6
  },
  {
    case 0 => Comparator.EqualTo
    case 1 => Comparator.Exists
    case 2 => Comparator.GreaterThan
    case 3 => Comparator.GreaterThanOrEqualTo
    case 4 => Comparator.LessThan
    case 5 => Comparator.LessThanOrEqualTo
    case 6 => Comparator.NotEqualTo
  }
  )
}

class DBQualificationRequirement(tag: Tag) extends Table[(Int, String, Int, Comparator, Boolean, Boolean, String)](tag, "DBQualificationRequirement") {
  implicit val comparatorMapper = DBQualificationRequirement.comparatorMapper

  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def qualificationTypeId = column[String]("qualificationTypeId")
  def integerValue = column[Int]("integerValue")
  def comparator = column[Comparator]("comparator")
  def requiredToPreview = column[Boolean]("requiredToPreview")
  def isDisqualification = column[Boolean]("isDisqualification")
  def HITTypeId = column[String]("HITTypeId")
  override def * = (id, qualificationTypeId, integerValue, comparator, requiredToPreview, isDisqualification, HITTypeId)
}
