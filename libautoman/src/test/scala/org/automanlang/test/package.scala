package org.automanlang

import java.util.{UUID, Date}
import org.automanlang.core.mock.MockAnswer
import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.{SchedulerState, Task}
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

  /**
    * Create a list of timed mock answers, with roughly the proportion
    * specified as duplicates.
    * @param proportion The fraction of responses that should be duplicates.
    * @param answers A list of valid answers.
    * @tparam T The type of the answer.
    * @return A list of mock answers.
    */
  def makeDuplicateMocks[T](proportion: Double, answers: T*) : List[MockAnswer[T]] = {
    assert(proportion > 0 && proportion < 1)

    val recip = (1/proportion).toInt

    // pad answers
    val (_,_,mocks) =
      (0 until (answers.size * (1.0 + proportion)).toInt)
      .foldLeft((0,0,List[MockAnswer[T]]())) {
        case ((time,ptr,newAnswers),i) =>
          // + 1 is to avoid the conditional being true the first time around
          // when 'last' is undefined
          if ((i + 1) % recip == 0) {
            // retrieve the last mock answer so that we may
            // reuse both the answer itself and the worker_id
            val last = newAnswers.head
            (time + 1, ptr, MockAnswer(last.answer, time, last.worker_id) :: newAnswers)
          } else {
            (time + 1, ptr + 1, MockAnswer(answers(ptr), time, UUID.randomUUID()) :: newAnswers)
          }
      }
    mocks
  }

  def makeMocks[T](answers: T*) : List[MockAnswer[T]] = {
    makeMocks(answers.toList)
  }

  def makeMocksWithDelta[T](answers: List[T], delta_in_sec: Int) : List[MockAnswer[T]] = {
    val (_, mocks) = answers.foldLeft(0,List[MockAnswer[T]]()){
      case ((t,xs),x) => (t + delta_in_sec * 1000, MockAnswer(x, t, UUID.randomUUID()) :: xs)
    }
    mocks
  }

  def makeMocks[T](answers: List[T]) : List[MockAnswer[T]] = {
    makeMocksWithDelta(answers, 1)
  }

  def makeMultiMocks[T : ClassTag](answers: List[T], dim: Int) : List[MockAnswer[Array[T]]] = {
    val answerses = (0 until dim).map { d => Random.shuffle(answers).toArray }.toArray
    makeMocksWithDelta[Array[T]](
      answers.indices.map { i =>
        (0 until dim).map { j => answerses(j)(i) }.toArray
      }.toList,
      1
    )
  }

  def makeMocksAt[T](answers: List[T], time: Long) : List[MockAnswer[T]] = {
    answers.map(MockAnswer(_,time, UUID.randomUUID()))
  }

  def makeTimedMocks[T](answer_time_pairs: List[(T,Int)]) : List[MockAnswer[T]] = {
    // make MockAnswers, converting times in seconds to times in milliseconds
    answer_time_pairs.map { case (a,t) => MockAnswer(a,t * 1000, UUID.randomUUID()) }
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
