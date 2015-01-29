package edu.umass.cs.automan.core.strategy
import edu.umass.cs.automan.core.scheduler.{SchedulerResult, SchedulerState, Thunk}
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

abstract class ScalarValidationStrategy[A](question: Question[A])
  extends ValidationStrategy[A](question) {

  var _confidence: Double = 0.95
  var _selected_answer: Option[A] = None

  def confidence: Double = _confidence
  def confidence_=(c: Double) { _confidence = c }
  def current_confidence(thunks: List[Thunk[A]]) : Double
  def is_confident(thunks: List[Thunk[A]]) : Boolean
  def is_done(thunks: List[Thunk[A]]) = is_confident(thunks)
  def select_answer(thunks: List[Thunk[A]]) : Option[SchedulerResult[A]] = {
    val rt = completed_workerunique_thunks(thunks) // only retrieved and memo-recalled; only earliest submission per-worker

    if (rt.size == 0) {
      None
    }

    // group by answer (which is actually an Option[A] because Thunk.answer is Option[A])
    val groups: Map[Option[A], List[Thunk[A]]] = rt.groupBy(_.answer)
    
    Utilities.DebugLog("Groups = " + groups, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // find answer of the largest group
    val gsymb: Option[A] = groups.maxBy { case(opt, as) => as.size }._1

    Utilities.DebugLog("Most popular answer is " + gsymb, LogLevel.INFO, LogType.STRATEGY,_computation_id)
    Utilities.DebugLog("classOf Thunk.answer is " + groups(gsymb).head.answer.get.getClass, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // return the top result
    _selected_answer = Some(groups(gsymb).head.answer.get)
    Some(SchedulerResult(_selected_answer.get, ???, current_confidence(thunks)))
  }

  override def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( _.answer.get == answer) // note that we accept all of a worker's matching submissions
                                           // even if we have to accept duplicate submissions
      case None => throw new PrematureValidationCompletionException("thunks_to_accept", this.getClass.toString)
    }
  }

  override def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( _.answer.get != answer)
      case None => throw new PrematureValidationCompletionException("thunks_to_reject", this.getClass.toString)
    }
  }
}