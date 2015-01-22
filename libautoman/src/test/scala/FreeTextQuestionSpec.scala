import edu.umass.cs.automan.adapters.Mock.events.UntimedAnswerPool
import edu.umass.cs.automan.core.answer.FreeTextAnswer
import org.scalatest._
import edu.umass.cs.automan.adapters.Mock.MockAdapter
import edu.umass.cs.automan.automan
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.UUID

class FreeTextQuestionSpec extends FlatSpec with Matchers {
  "A FreeTextQuestion" should "return a single correct answer when given 2 identical answers" in {
    // define mock answers
    val question_id = UUID.randomUUID()
    val mock_answer = 'three
    val epoch = UntimedAnswerPool(List(mock_answer, mock_answer).map { s => question_id -> new FreeTextAnswer(None, UUID.randomUUID().toString, s)})

    // init Mock backend
    val ma = MockAdapter { a =>
      a.answer_trace = List(epoch)
      a.use_memoization = false
    }

    // explicitly set confidence
    val target_confidence = 0.95

    // define simple FreeText question & mock answers
    def AskEm(question: String) = ma.FreeTextQuestion { q =>
      q.id = question_id
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
