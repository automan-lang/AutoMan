import edu.umass.cs.automan.adapters.Mock.events.Epoch
import edu.umass.cs.automan.core.answer.FreeTextAnswer
import org.scalatest._
import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.automan
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.UUID

class FreeTextDistributionSpec extends FlatSpec with Matchers {
  "A FreeTextDistributionSpec" should "return at least n answers" in {
    // define mock answers
    val question_id = UUID.randomUUID()
    val mock_answers = List('three, 'three, 'Three, Symbol("3"), 'four, 'one, 'three, Symbol("2"))
    val epoch = Epoch(30, mock_answers.map { s => question_id -> new FreeTextAnswer(None, UUID.randomUUID().toString, s)})

    val n = epoch.answers.size - 1

    // init Mock backend
    val ma = MockAdapter { a =>
      a.mock_answers = List(epoch)
      a.use_memoization = false
    }

    // explicitly set confidence
    val target_confidence = 0.95

    // define simple FreeText distribution question & mock answers
    def AskEm(question: String) = ma.FreeTextDistributionQuestion { q =>
      q.id = question_id
      q.num_samples = n
      q.text = question
      q.title = question
    }

    // run AutoMan
    val answers = automan(ma) {
      val future_answer = AskEm("How many licks does it take to get to the Tootsie Roll Center of a Tootsie Pop?")
      Await.result(future_answer, Duration.Inf)
    }

    // ensure that all answers are in mock_answers
    answers.foreach { ans =>
      mock_answers.contains(ans.value) should be (true)
    }

    // ensure that the number of samples is correct
    answers.size should be (n)
  }
}
