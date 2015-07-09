package edu.umass.cs.automan.core

import edu.umass.cs.automan.core.logging.TaskSnapshot

trait Plugin {
  def startup(adapter: AutomanAdapter)
  def shutdown()
  def state_updates(tasks: List[TaskSnapshot[_]])
}
