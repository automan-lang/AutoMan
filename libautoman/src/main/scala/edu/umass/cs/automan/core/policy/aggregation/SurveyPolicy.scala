package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.{Answers, IncompleteAnswers, OverBudgetAnswers, SurveyAnswers}
import edu.umass.cs.automan.core.question.{Question, Response, Survey}
import edu.umass.cs.automan.core.scheduler.Task

import scala.collection.immutable

abstract class SurveyPolicy(survey: Survey)
  extends AggregationPolicy(survey) {

  def is_done(tasks: List[Task], num_comparisons: Int) = {
    val done = completed_workerunique_tasks(tasks).size // how many tasks per unique worker
    // num_comparisons is irrelevant for termination based
    // on a fixed sample size but bump anyway for consistency
    (done >= survey.sample_size, num_comparisons + 1) // cheating
  }
  def rejection_response(tasks: List[Task]): String = {
    "We can only accept a single answer per worker."
  }

  def select_answer(tasks: List[Task], num_comparisons: Int) : Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks) // tasks unfiltered, valid_tasks filtered
    // distribution of quality-controlled answers
    val temp0: Seq[Task] = valid_tasks.filter(_.worker_id.isDefined) // filter out undefined worker ids
    val temp: Map[String, Seq[Task]] = temp0.groupBy(_.worker_id.get) // t -> t.worker_id (tasks by worker id)
    val temp1: Set[Map[String, Question#A]] = temp.map {
      case (worker_id, tasks) => {
         tasks.foldLeft(Map[String, Question#A]())((acc, t) => acc + (t.question.name -> t.answer.get)) // <question id, answer>
      }
    }.toSet
//    val temp1: Set[(String, Question#A)] = temp.map{
//      case(worker_id, tasks) => {
//        tasks.foldLeft(Set[(String, Question#A)]())((acc, t) => acc += (t.question.name, t.answer.get))
//      }
//    }
  // Set[(String, Question#A)]
    val dist = getSurveyDistribution(tasks) // raw distribution (not quality-controlled)
    val cost: BigDecimal = valid_tasks.filterNot(_.from_memo).foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }
    SurveyAnswers(temp1, cost, survey, dist).asInstanceOf[Question#AA]
  }

  // takes tasks, creates array of Responses with worker id, (question name, answer string)
  def getSurveyDistribution(tasks: List[Task]) : Array[Response[Set[(String,Question#A)]]] = {
    // distribution
    tasks.flatMap { t =>
      (t.answer,t.worker_id) match {
        case (Some(ans),Some(worker)) => {
          val toAdd: Set[(String,Question#A)] = Set((t.question.name,ans))
          Some(Response(toAdd,worker))
        }
        case _ => None
      }
    }.toArray
  }

//  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal, num_comparisons: Int) : Question#AA = {
//    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)
//    if (valid_tasks.isEmpty) {
//      OverBudgetAnswers(need, have, question).asInstanceOf[Question#AA]
//    } else {
//      val distribution: Set[(String, Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
//      val cost: BigDecimal = valid_tasks.map { t => t.cost }.foldLeft(BigDecimal(0)) { (acc, c) => acc + c }
//      // distribution
//      val dist = getDistribution(tasks)
//      IncompleteAnswers(distribution, cost, question, dist).asInstanceOf[Question#AA]
//    }
//  }

  override def tasks_to_accept(tasks: List[Task]): List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    completed_workerunique_tasks(tasks)
      .filter { t =>
        not_final(t) &&
          !cancels.contains(t)
      }
  }
  override def tasks_to_reject(tasks: List[Task]): List[Task] = {
    val accepts = tasks_to_accept(tasks).toSet
    tasks.filter { t =>
      !accepts.contains(t) &&
        not_final(t)
    }
  }

}
