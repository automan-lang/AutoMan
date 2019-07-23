package edu.umass.cs.automan.adapters.googleads.forms.question

import edu.umass.cs.automan.core.question.QuestionOption

case class GQuestionOption(override val question_id: Symbol,
                           override val question_text: String,
                           image_url: String = "")
                           extends QuestionOption(question_id: Symbol, question_text: String) {

}
