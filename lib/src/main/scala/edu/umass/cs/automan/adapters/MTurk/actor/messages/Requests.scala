package edu.umass.cs.automan.adapters.MTurk.actor.messages

import edu.umass.cs.automan.core.scheduler.Thunk

case class AcceptRequest(t: Thunk)
case class AcceptResponse()
case class CancelRequest(t: Thunk)
case class CancelResponse()
case class PostRequest(ts: List[Thunk], dual: Boolean, exclude_worker_ids: List[String])
case class PostResponse()