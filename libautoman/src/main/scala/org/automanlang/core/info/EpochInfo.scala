package org.automanlang.core.info

import java.util.Date

import org.automanlang.core.scheduler.Task
import org.automanlang.core.scheduler.Task

case class EpochInfo(start_time: Date, end_time: Date, tasks: List[Task], tasks_needed_to_agree: Int, prop_that_agree: Int)