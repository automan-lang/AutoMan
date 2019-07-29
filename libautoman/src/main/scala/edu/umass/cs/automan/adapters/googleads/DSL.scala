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

  // Instead of writing a list, let the user supply a "big tuple"
  implicit def product2OptionList(p: Product) : List[GQuestionOption] = p.productIterator.toList.asInstanceOf[List[GQuestionOption]]
  implicit def tupSymbString2MTQuestionOption(opt: (Symbol, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2)
  implicit def tupStrURL2MTQuestionOption(opt: (String, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(opt._1), opt._1, opt._2)
  implicit def tupWithURL2MTQuestionOption(opt: (Symbol, String, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2, opt._3)
  implicit def str2MTQuestionOption(s: String)(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(s), s)
}
