import java.util.UUID

import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.adapters.Mock.events.TimedAnswer
import edu.umass.cs.automan.adapters.Mock.question.MockOption
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer.{Answer, CheckboxAnswer}
import edu.umass.cs.automan.core.question.CheckboxDistributionQuestion
import org.scalatest.{Matchers, FlatSpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CheckboxDistributionSpec extends FlatSpec with Matchers {
  "A CheckboxDistributionSpec" should "return at least n answers and cost n*reward" in {
    // define options
    val cookiemonster = MockOption('cookiemonster, "Cookie Monster")
    val oscar = MockOption('oscar, "Oscar the Grouch")
    val kermit = MockOption('kermit, "Kermit")
    val spongebob = MockOption('spongebob, "Spongebob")
    val thecount = MockOption('thecount, "The Count")
    val options = Set(cookiemonster, oscar, kermit, spongebob, thecount)

    // define mock answers
    val question_id = UUID.randomUUID()
    val mock_answers = List(
      Set(oscar.question_id),
      Set(spongebob.question_id, kermit.question_id),
      Set(spongebob.question_id),
      Set(kermit.question_id, spongebob.question_id),
      Set(spongebob.question_id, kermit.question_id),
      Set(thecount.question_id, kermit.question_id, spongebob.question_id, oscar.question_id),
      Set(spongebob.question_id, kermit.question_id),
      Set(spongebob.question_id, kermit.question_id)
    )
    val epoch = TimedAnswer(1, mock_answers.map {s => question_id -> new CheckboxAnswer(None, UUID.randomUUID().toString, s)})

    val n = epoch.answers.size

    // init Mock backend
    val ma = MockAdapter { a =>
      a.answer_trace = List(epoch)
      a.use_memoization = false
    }

    // question object
    var q_obj : CheckboxDistributionQuestion = null

    // define simple Checkbox distribution question & mock answers
    def AskEm(question: String) = ma.CheckboxDistributionQuestion { q =>
      q.id = question_id
      q.num_samples = n
      q.text = question
      q.title = question
      q.options = options.toList
      q_obj = q
    }

    // run AutoMan
    val answers = automan(ma) {
      val future_answer = AskEm("Which characters do you know?")
      Await.result(future_answer, Duration.Inf)
    }

    // ensure that all answers are in mock_answers
    answers.foreach { ans =>
      mock_answers.contains(ans.values) should be (true)
    }

    // ensure that the number of samples is correct
    answers.size should be (n)

    // ensure that we paid the correct amount
    q_obj.final_cost should be (q_obj.reward * BigDecimal(n))
  }
}
