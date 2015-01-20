import java.util.UUID

import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.adapters.Mock.events.TimedAnswer
import edu.umass.cs.automan.adapters.Mock.question.MockOption
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.core.question.RadioButtonQuestion
import org.scalatest.{Matchers, FlatSpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RadioButtonSpec extends FlatSpec with Matchers {
  "A RadioButtonSpec" should "return the most popular answer with the correct confidence and cost" in {
    // define options
    val cookiemonster = MockOption('cookiemonster, "Cookie Monster")
    val oscar = MockOption('oscar, "Oscar the Grouch")
    val kermit = MockOption('kermit, "Kermit")
    val spongebob = MockOption('spongebob, "Spongebob")
    val thecount = MockOption('thecount, "The Count")
    val options = Set(cookiemonster, oscar, kermit, spongebob, thecount)

    // define mock answers
    val question_id = UUID.randomUUID()
    val epoch = TimedAnswer(1,
      List(
        spongebob.question_id,
        kermit.question_id,
        spongebob.question_id,
        kermit.question_id,
        spongebob.question_id,
        spongebob.question_id,
        spongebob.question_id
      ).map { qid => question_id -> new RadioButtonAnswer(None, UUID.randomUUID().toString, qid)}
    )

    // init Mock backend
    val ma = MockAdapter { a =>
      a.answer_trace = List(epoch)
      a.use_memoization = false
    }

    // explicitly set confidence
    val target_confidence = 0.95

    // get question object reference
    var q_obj : RadioButtonQuestion = null

    // define simple RadioButton question & mock answers
    def AskEm(question: String) = ma.RadioButtonQuestion { q =>
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
    answer.value should === ('spongebob)

    // ensure that the confidence meets the user's bound
    answer.confidence should be >= target_confidence

    // we know that the correct amount is 5 * $0.06; is that what we paid?
    q_obj.final_cost should be (BigDecimal("0.30"))
  }
}
