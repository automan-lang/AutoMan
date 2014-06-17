package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.DistributionAnswer
import edu.umass.cs.automan.core.question.{DistributionQuestion, Question}

abstract class DistributionValidationStrategy[Q <: DistributionQuestion](question: Q)
  extends ValidationStrategy[Q](question) {
  def is_done = throw new NotImplementedError("DistributionValidationStrategy not yet implemented.")
  def select_answer(question: Question) : DistributionAnswer = {
    // TODO
    throw new NotImplementedError("DistributionValidationStrategy not yet implemented.")
  }
}