package edu.umass.cs.automan.adapters.mturk

import java.util.{Calendar, Date, UUID}

import com.amazonaws.mturk.requester.{AssignmentStatus, QualificationType}
import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.logging.tables.DBAssignment
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.logging.tables.{DBQuestion, DBTask, DBTaskHistory}
import edu.umass.cs.automan.core.scheduler.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState

import scala.slick.driver.H2Driver.simple._

object DeleteAllQualifications extends App {
  // parse args
  val conf = Conf(args)

  println("start")

  println("init MTurkAdapter")

  // init adapter with no memo
  val a = MTurkAdapter { mt =>
    mt.access_key_id = conf.key()
    mt.secret_access_key = conf.secret()
    mt.sandbox_mode = conf.sandbox()
    mt.logging = LogConfig.NO_LOGGING
  }

  // get requester service handle
  val srv = a.requesterService match {
    case Some(x) => x
    case None =>
      println("Can't get handle to requester service.")
      sys.exit(1)
  }

  // get all of the qualifications
  val quals: Array[QualificationType] = srv.getAllQualificationTypes

  def delQual(qual: QualificationType) : Boolean = {
    try {
      val qtid = qual.getQualificationTypeId
      println("Deleting qualification ID: " + qtid)
      srv.disposeQualificationType(qtid)
      true
    } catch {
      case e: com.amazonaws.mturk.service.exception.InternalServiceException =>
        false
    }
  }

  // now delete them
  var sleep_interval_ms = 500
  quals.foreach { q =>
    var success = false
    while (!success) {
      Thread.sleep(sleep_interval_ms)
      success = delQual(q)
      if (!success) {
        sleep_interval_ms *= 2
      }
    }
  }

  println("done")

  a.close()

  sys.exit(0)
}
