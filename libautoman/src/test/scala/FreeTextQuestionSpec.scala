import edu.umass.cs.automan.core.answer.FreeTextAnswer
import org.scalatest._
import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.automan
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.UUID

class FreeTextQuestionSpec extends FlatSpec with Matchers {
  "A FreeTextQuestion" should "return a single correct answer when given 2 identical answers" in {
    // init Mock backend
    val ma = MockAdapter()

    // define mock answers
    val mock_answer = 'three
    val mock_answers = List(mock_answer, mock_answer)

    // explicitly set confidence
    val target_confidence = 0.95

    // define simple FreeText question & mock answers
    def AskEm(question: String) = ma.FreeTextQuestion { q =>
      q.mock_answers = mock_answers.map(new FreeTextAnswer(None, UUID.randomUUID().toString, _))
      q.confidence = target_confidence
      q.text = question
      q.title = question
    }

    // run AutoMan
    val answer = automan(ma) {
      val future_answer = AskEm("How many licks does it take to get to the Tootsie Roll Center of a Tootsie Pop?")
      Await.result(future_answer, Duration.Inf)
    }

    // ensure that mock_answers == answers
    answer.value should be theSameInstanceAs mock_answer

    // ensure that the confidence meets the user's bound
    answer.confidence should be >= target_confidence
  }
}
