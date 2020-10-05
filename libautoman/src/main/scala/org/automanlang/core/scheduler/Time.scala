package org.automanlang.core.scheduler

import java.text.SimpleDateFormat
import java.util.Date

import org.automanlang.core.mock.MockAnswer
import org.automanlang.core.util.Utilities

object Time {
  val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")   // for pretty-printing to logs
  def format(d: Date) : String = sdf.format(d)
  def incrTime(use_virt: Boolean)(current_time: Date)(tqueue: List[Date]) : Time = {
    if (use_virt) {
      tqueue match {
        case t :: ts => Time(t, ts, use_virt)
        case Nil => Time(Utilities.xSecondsFromDate(1, current_time), Nil, use_virt)
      }
    } else {
      Time(new Date(), Nil, use_virt)
    }
  }
  def initTickQueue[A](current_time: Date, ans: Iterable[MockAnswer[A]]) : List[Date] = {
    // convert time deltas into dates
    val dates = ans.map { ma => Utilities.xMillisecondsFromDate(ma.time_delta_in_ms, current_time) }

    // remove duplicate dates
    val dedup = Utilities.distinctBy(dates){ date => date.getTime }.toList

    // sort by date
    dedup.sortWith { (a,b) => a.compareTo(b) == -1 }
  }
  def timeoutDates(current_time: Date, tasks: List[Task]) : List[Date] = {
    val xMillisAway = tasks
      .map(_.timeout_in_s)
      .distinct
      .map(_.toLong * 1000)

    xMillisAway.map { ms => Utilities.xMillisecondsFromDate(ms, current_time) }
  }
}

case class Time(current_time: Date, virtual_times: List[Date], use_virt: Boolean) {
  def incrTime() : Time = {
    Time.incrTime(use_virt)(current_time)(virtual_times)
  }
  def addTimeoutsFor[A](newtasks: List[Task]) : Time = {
    // get dates for tasks
    val newdates = Time.timeoutDates(current_time, newtasks)

    // concat and sort
    val alldates = virtual_times ::: newdates

    // remove duplicate dates
    val dedup = Utilities.distinctBy(alldates){ date => date.getTime }.toList

    // sort
    val sorted = dedup.sortWith { (a,b) => a.compareTo(b) == -1 }

    // return new Time
    Time(current_time, sorted, use_virt)
  }
  override def toString() : String = {
    s"(current_time: $current_time, virtual_times: ${virtual_times.map(Time.format).mkString(", ")})"
  }
}
