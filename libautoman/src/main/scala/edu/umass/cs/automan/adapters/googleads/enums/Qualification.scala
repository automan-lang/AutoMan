package edu.umass.cs.automan.adapters.googleads.enums

import com.google.ads.googleads.v2.enums.GenderTypeEnum.GenderType

trait Qualification[T] {
  def Value: T = _
}

trait Language extends Qualification[Int]

case object ENGLISH extends Language {
  override def Value  = 1000
}

case object SPANISH extends Language

case object FRENCH extends Language

trait Gender extends Qualification[GenderType]

case object MALE extends Gender {
  override def Value = GenderType.MALE
}

case object FEMALE extends Gender {
  override def Value = GenderType.FEMALE
}

case object UNDETERMINED extends Gender {
  override def Value = GenderType.UNDETERMINED
}
