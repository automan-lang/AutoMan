package edu.umass.cs.automan.core.info

import java.util.Date
import edu.umass.cs.automan.core.scheduler.Thunk

case class EpochInfo(start_time: Date, end_time: Date, thunks: List[Thunk[_]], thunks_needed_to_agree: Int, prop_that_agree: Int)