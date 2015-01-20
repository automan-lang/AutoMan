import java.util.UUID
import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.adapters.Mock.events.TimedAnswer
import edu.umass.cs.automan.adapters.Mock.question.MockOption
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer.CheckboxAnswer
import edu.umass.cs.automan.core.question.CheckboxQuestion
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TimeoutHandlingSpec extends FlatSpec with Matchers {
  "Timeouts" should "cause AutoMan to respawn tasks until it gets an answer" in {
    // define options
    val cookiemonster = MockOption('cookiemonster, "Cookie Monster")
    val oscar = MockOption('oscar, "Oscar the Grouch")
    val kermit = MockOption('kermit, "Kermit")
    val spongebob = MockOption('spongebob, "Spongebob")
    val thecount = MockOption('thecount, "The Count")
    val options = Set(cookiemonster, oscar, kermit, spongebob, thecount)

    // define mock answers
    val question_id = UUID.randomUUID()
    val first_answers = List(
      Set(oscar.question_id),
      Set(spongebob.question_id, kermit.question_id),
      Set(spongebob.question_id)
    )
    val second_answers = List(
      Set(spongebob.question_id, kermit.question_id),
      Set(spongebob.question_id, kermit.question_id)
    )

    val first_epoch = TimedAnswer(65, first_answers.map {s => question_id -> new CheckboxAnswer(None, UUID.randomUUID().toString, s)})
    val second_epoch = TimedAnswer(91, second_answers.map {s => question_id -> new CheckboxAnswer(None, UUID.randomUUID().toString, s)})

    // init Mock backend
    val ma = MockAdapter { a =>
      a.answer_trace = List(first_epoch, second_epoch)
      a.use_memoization = false
    }

    // explicitly set confidence
    val target_confidence = 0.95

    // get question object reference
    var q_obj : CheckboxQuestion = null

    // define simple Checkbox question & mock answers
    def AskEm(question: String) = ma.CheckboxQuestion { q =>
      // set thunk timeout to 30 seconds
      q.worker_timeout_in_s = 30
      q.question_timeout_multiplier = 1

      q.id = question_id
      q.confidence = target_confidence
      q.text = question
      q.title = question
      q.options = options.toList
      q_obj = q
    }

    // run AutoMan
    val answer = automan(ma) {
      val future_answer = AskEm("Which one of these does not belong?")
      Await.result(future_answer, Duration.Inf)
    }

    // ensure that mock_answers == answers
    (answer.values == Set(spongebob.question_id, kermit.question_id)) should be (true)

    // ensure that the confidence meets the user's bound
    answer.confidence should be >= target_confidence

    // we know that the correct amount is:
    // first epoch: timeout, nothing gets paid, wage doubles
    // second epoch: 3 valid answers needed, 0.12 * 1 valid answer received, timeout, wage doubles
    // third epoch: 3 valid answers needed, 0.24 * 2 valid answers received
    q_obj.final_cost should be (BigDecimal("0.60"))
  }
}
