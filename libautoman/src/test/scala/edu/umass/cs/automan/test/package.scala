package edu.umass.cs.automan

import java.util.{UUID, Date}
import edu.umass.cs.automan.core.mock.MockAnswer
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import scala.reflect.ClassTag
import scala.util.Random

package object test {
  def newTask(question: Question, round: Int, timeout: Int, worker_timeout: Int, cost: BigDecimal, time_delta: Int) = {
    val now = new Date()
    Task(
      UUID.randomUUID(),
      question,
      round,
      timeout,
      worker_timeout,
      cost,
      now,
      SchedulerState.READY,
      from_memo = false,
      None,
      None,
      now
    )
  }

  def makeMocksNow[T](answers: List[T]) : List[MockAnswer[T]] = {
    answers.map(MockAnswer(_, 0))
  }

  def makeMocks[T](answer_time_pairs: List[(T,Int)]) : List[MockAnswer[T]] = {
    // make MockAnswers, converting times in seconds to times in milliseconds
    answer_time_pairs.map { case (a,t) => MockAnswer(a,t * 1000) }
  }

  def genAnswers[T : ClassTag](from_vector: Array[T], with_probabilities: Array[String], of_size: Int) : Array[T] = {
    genAnswers(from_vector, with_probabilities.map(BigDecimal(_)), of_size)
  }

  def genAnswers[T : ClassTag](from_vector: Array[T], with_probabilities: Array[BigDecimal], of_size: Int) : Array[T] = {
    assert(from_vector.length == with_probabilities.length)
    assert(with_probabilities.sum == BigDecimal(1.0))

    // make CDF
    val (cdf,_) = with_probabilities.foldLeft((Array.ofDim[BigDecimal](with_probabilities.length), 0)){ case ((arr, i), p) =>
      arr(i) = if (i == 0) { p } else { arr(i-1) + p }
      (arr, i+1)
    }

    // finds the index into the value vector given a p value
    def cdf_idx(p: BigDecimal, i: Int) : Int = {
      if(p > cdf(i)) {
        cdf_idx(p, i+1)
      } else {
        i
      }
    }


    // init rng
    val rng = new Random()

    // generate random values
    (0 until of_size).map { _ =>
      from_vector(cdf_idx(BigDecimal(rng.nextDouble()),0))
    }.toArray[T]
  }

  def compareDistributions[T](mock_distribution: List[T], output_distribution: Set[(String,T)]) : Boolean = {
    val output_answer_counts = output_distribution
      .toList
      .map(_._2)
      .foldLeft(Map.empty[T,Int]){ (acc: Map[T, Int], elem: T) =>
      if (acc.contains(elem)) {
        acc + (elem -> (acc(elem) + 1))
      } else {
        acc + (elem -> 1)
      }
    }

    val similarity_counts = mock_distribution
      .foldLeft(output_answer_counts){ (acc, elem) =>
      if (acc.contains(elem)) {
        acc + (elem -> (acc(elem) - 1))
      } else {
        acc + (elem -> -1)
      }
    }

    similarity_counts.foldLeft(0) { (acc,pair: (T, Int)) =>
      acc + pair._2
    } == 0
  }
}
