package edu.umass.cs.automan.adapters.googleads.forms.question

// old; remove when integrated with AutoMan and use GQuestionOption
object Choice {
  def choice(key: Symbol, text: String): Choice = new Choice(key, text)
  def choice(key: Symbol, text: String, url: String): Choice = new Choice(key, text, url)
}

class Choice(protected val _question_id: Symbol,
             protected val _question_text: String,
             protected val _url: String = "") {
  def question_id: Symbol = _question_id

  def question_text: String = _question_text

  def url: String = _url
}
