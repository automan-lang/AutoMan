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
    val mock_answers = List('three, 'three, 'Three, Symbol("3"), 'four, 'one, 'three, Symbol("2"))

    val n = mock_answers.size - 1

    // init Mock backend
    val ma = MockAdapter()

    // explicitly set confidence
    val target_confidence = 0.95

    // define simple FreeText question & mock answers
    def AskEm(question: String) = ma.FreeTextDistributionQuestion { q =>
      q.mock_answers = mock_answers.map(new FreeTextAnswer(None, UUID.randomUUID().toString, _))
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
