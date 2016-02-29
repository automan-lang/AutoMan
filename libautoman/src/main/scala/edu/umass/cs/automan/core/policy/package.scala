package edu.umass.cs.automan.core

import edu.umass.cs.automan.core.scheduler.Task

package object policy {
  def currentRound(tasks: List[Task]) : Int = {
    if (tasks.nonEmpty) {
      tasks.map(_.round).max
    } else {
      0
    }
  }

  def nextRound(tasks: List[Task], suffered_timeout: Boolean) : Int = {
    val round = currentRound(tasks)

    if (suffered_timeout) {
      round
    } else {
      round + 1
    }
  }
}
