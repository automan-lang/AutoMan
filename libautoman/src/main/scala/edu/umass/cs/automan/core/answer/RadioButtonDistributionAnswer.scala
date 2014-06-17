package edu.umass.cs.automan.core.answer

import scala.collection.immutable.Bag

//import edu.umass.cs.automan.core.memoizer.RadioButtonAnswerMemo

class RadioButtonDistributionAnswer(override val worker_id_response_map: Map[String,Symbol])
  extends DistributionAnswer(worker_id_response_map: Map[String,Symbol]) {
  implicit val m1 = Bag.configuration.compact[Symbol]

  def responses: Bag[Symbol] =
    worker_id_response_map
      .foldLeft(Bag[Symbol].empty){(acc,rm) =>
        val response = rm._2
        acc.union(Bag(response))
    }
  override def final_answer(confidence: Option[Double]) : RadioButtonDistributionAnswer = {
    confidence match {
      case Some(c) => throw new AnswerConfidenceSuppliedException("A final RadioButtonDistributionAnswer should not have a confidence value.")
      case None => new RadioButtonDistributionAnswer(worker_id_response_map)
    }
  }
}
