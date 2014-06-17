package edu.umass.cs.automan.core.answer

import scala.collection.immutable.Bag

// worker_id_response_map is a map from worker_id -> answer_value
abstract class DistributionAnswer(val worker_id_response_map: Map[String,Symbol])
  extends Answer {
  override def toString = worker_id_response_map.toString()
  def responses: Bag[Symbol]
  def comparator = 'AlwaysAccept
  override def final_answer(confidence: Option[Double]): DistributionAnswer
}
