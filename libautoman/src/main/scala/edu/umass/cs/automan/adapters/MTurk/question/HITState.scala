package edu.umass.cs.automan.adapters.MTurk.question

object HITState extends Enumeration {
  type HITState = Value
  val READY,  // HIT is ready to post
  RUNNING,    // HIT has been posted
  RESOLVED    // all assignments have been retrieved
  = Value
}

import HITState._