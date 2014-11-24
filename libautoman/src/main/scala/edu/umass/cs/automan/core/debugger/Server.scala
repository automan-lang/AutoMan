package edu.umass.cs.automan.core.debugger

import java.util.Date
import akka.actor.Actor
import spray.routing._
import edu.umass.cs.automan.core.Utilities
import spray.json._
import edu.umass.cs.automan.core.debugger.DebugJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class Server extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  val tasks =
    Tasks(
      Array(
        Task(
          Info(
            "Test Task",
            "How many licks does it take to get to the Tootsie Roll center of a Tootsie Pop?",
            "Mr. Owl wants to know.",
            "RadioButtonQuestion",
            Utilities.dateToTimestamp(new Date()),
            0.95,
            34.3,
            24.1,
            5,
            7
          ),
          Array(
            PrevTimeout(
              1,
              Utilities.dateToTimestamp(new Date()),
              4,
              3
            ),
            PrevTimeout(
              2,
              Utilities.dateToTimestamp(new Date()),
              4,
              3
            )
          ),
          Array(
            CurrentTask(
              1,
              Utilities.dateToTimestamp(new Date()),
              "3"
            ),
            CurrentTask(
              2,
              Utilities.dateToTimestamp(new Date()),
              "2"
            ),
            CurrentTask(
              3,
              Utilities.dateToTimestamp(new Date()),
              "3"
            ),
            CurrentTask(
              4,
              Utilities.dateToTimestamp(new Date()),
              "3"
            )
          ),
          BudgetInfo(
            0.05,
            50.00,
            5.50,
            0.25,
            5.25
          )
        )
      )
    )

  val myRoute =
    path("") {
      get {
        complete {
          tasks.toJson.toString()
        }
      }
    }
}