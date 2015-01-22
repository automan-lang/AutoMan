import java.util.UUID

import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.adapters.Mock.events.TimedAnswerPool
import edu.umass.cs.automan.adapters.Mock.question.MockOption
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.core.question.RadioButtonDistributionQuestion
import org.scalatest.{SequentialNestedSuiteExecution, Matchers, FlatSpec}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RadioButtonDistributionSpec extends FlatSpec with Matchers {
  "A RadioButtonDistributionSpec" should "return at least n answers and cost n*reward" in {
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
      spongebob.question_id,
      spongebob.question_id,
      cookiemonster.question_id,
      kermit.question_id,
      spongebob.question_id,
      thecount.question_id,
      oscar.question_id
    )
    val epoch = TimedAnswerPool(1, mock_answers.map { qid => question_id -> new RadioButtonAnswer(None, UUID.randomUUID().toString, qid)})

    val n = epoch.answers.size - 2

    // init Mock backend
    val ma = MockAdapter { a =>
      a.answer_trace = List(epoch)
      a.use_memoization = false
    }

    // question object
    var q_obj : RadioButtonDistributionQuestion = null

    // define simple RadioButton distribution question & mock answers
    def AskEm(question: String) = ma.RadioButtonDistributionQuestion { q =>
      q.id = question_id
      q.num_samples = n
      q.text = question
      q.title = question
      q.options = options.toList
      q_obj = q
    }

    // run AutoMan
    val answers = automan(ma) {
      val future_answer = AskEm("Which one of these does not belong?")
      Await.result(future_answer, Duration.Inf)
    }

    // ensure that all answers are in mock_answers
    answers.foreach { ans =>
      mock_answers.contains(ans.value) should be (true)
    }

    // ensure that the number of samples is correct
    answers.size should be (n)

    // ensure that we paid the correct amount
    q_obj.final_cost should be (q_obj.reward * BigDecimal(n))
  }
}
