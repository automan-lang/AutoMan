package edu.umass.cs.automan.adapters.googleads

object DSL extends edu.umass.cs.automan.core.DSL {
  type GQuestionOption = edu.umass.cs.automan.adapters.googleads.question.GQuestionOption

  def gads(): GoogleAdsAdapter = ???

  def choice(key: Symbol, text: String)(implicit ga: GoogleAdsAdapter): GQuestionOption = {
    ga.Option(key, text)
  }

  def choice(key: Symbol, text: String, image_url: String)(implicit ga: GoogleAdsAdapter): GQuestionOption = {
    ga.Option(key, text, image_url)
  }
}
