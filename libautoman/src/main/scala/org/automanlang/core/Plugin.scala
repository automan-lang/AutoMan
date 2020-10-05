package org.automanlang.core

import org.automanlang.core.logging.TaskSnapshot

trait Plugin {
  def startup(adapter: AutomanAdapter)
  def shutdown()
  def state_updates(tasks: List[TaskSnapshot[_]])
}
