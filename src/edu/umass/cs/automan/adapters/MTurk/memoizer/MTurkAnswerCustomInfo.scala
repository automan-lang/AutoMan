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
      case Array(aid, hid) => {
        assignment_id = aid
        hit_id = hid
      }
      case _ => throw new Exception("Invalid MTurkAnswerCustomInfo string.")
    }
  }

  override def toString = assignment_id + ";" + hit_id
}
