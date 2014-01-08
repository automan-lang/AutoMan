package edu.umass.cs.automan.adapters.MTurk.actor

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import edu.umass.cs.automan.adapters.MTurk.actor.messages._
import com.amazonaws.mturk.service.axis.RequesterService

trait MTurkAdapterProxy {
  def accept(r: AcceptRequest): Future[AcceptResponse]
  def cancel(r: CancelRequest): Future[CancelResponse]
  def post(r: PostRequest): Future[PostRequest]
}

class MTurkAdapterProxyImpl(backend: RequesterService, sleep_ms: Int) extends MTurkAdapterProxy {
  val actor = {
    val ctx = TypedActor.context
    ctx.actorOf(Props(new MTurkActor(backend, sleep_ms)))
  }

  implicit val timeout = Timeout(10, java.util.concurrent.TimeUnit.SECONDS)

  def accept(r: AcceptRequest): Future[AcceptResponse] =
    (actor ? r).mapTo[AcceptResponse]

  def cancel(r: CancelRequest): Future[CancelResponse] =
    (actor ? r).mapTo[CancelResponse]

  def post(r: PostRequest): Future[PostResponse] =
    (actor ? r).mapTo[PostResponse]
}