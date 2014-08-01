package edu.umass.cs.automan.adapters.MTurk.memoizer

class MTurkAnswerCustomInfo {
  var assignment_id: String = _
  var hit_id: String = _

  def this(AssignmentId: String, HITId: String) {
    this()
    assignment_id = AssignmentId
    hit_id = HITId
  }

  def parse(s: String) {
    s.split(";") match {
      case Array(assn_str, hit_str) => {
        assn_str.split(":") match {
          case Array(_, aid) => assignment_id = aid.drop(1) // get rid of leading space
          case _ => throw new Exception("Invalid MTurkAnswerCustomInfo string.")
        }
        hit_str.split(":") match {
          case Array(_, hid) => hit_id = hid.drop(1) // get rid of leading space
          case _ => throw new Exception("Invalid MTurkAnswerCustomInfo string.")
        }
      }
      case _ => throw new Exception("Invalid MTurkAnswerCustomInfo string.")
    }
  }

  override def toString = "assignment_id: " + assignment_id + ";" + "hit_id: " + hit_id
}
