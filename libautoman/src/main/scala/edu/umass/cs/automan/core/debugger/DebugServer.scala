package edu.umass.cs.automan.core.debugger

import akka.actor.Actor
import spray.routing._
import edu.umass.cs.automan.core.AutomanAdapter
import spray.json._
import edu.umass.cs.automan.core.debugger.DebugJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class DebugServer[T <: AutomanAdapter](a: T) extends Actor with DebugService[T] {
  val adapter = a

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(routes)
}

// this trait defines our service behavior independently from the service actor
trait DebugService[T <: AutomanAdapter] extends HttpService {
  val adapter: T

  val routes =
    path("") {
      get {
        complete {
          adapter.debug_info.toJson.toString()
        }
      }
    } ~
    path("debugger") {
      getFromResource("webdebugger/index.html")
    }
}