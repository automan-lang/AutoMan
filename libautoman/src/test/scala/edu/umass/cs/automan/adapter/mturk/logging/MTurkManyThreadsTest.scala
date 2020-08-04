package edu.umass.cs.automan.adapter.mturk.logging

import java.util.UUID

import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.logging.LogLevelDebug
import org.scalatest.tagobjects.Slow
import edu.umass.cs.automan.test._
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random
import org.scalatest.Ignore

@Ignore
class MTurkManyThreadsTest extends FlatSpec with Matchers {

  "A freetext program with many threads" should "work" taggedAs Slow in {
    val urls = Random.alphanumeric.take(1000).mkString.grouped(10).toList

    implicit val a = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    def plateTxt(url: String) = freetext (
      budget = 5.00,
      title = "Transcribe these license plates.",
      text = "Foobitty foobitty foo? http://" + url,
      allow_empty_pattern = true,
      pattern = "XXXXXXXXX",
      dont_reject = true,
      mock_answers = makeMocks(Utilities.randomPermute(List(url, url, url + "z", url)))
    )

    automan(a, test_mode = true, in_mem_db = true) {
      // get plate texts from image URLs
      val plate_texts = urls.map { url =>
        (url, plateTxt(url))
      }

      // print out results
      plate_texts.foreach { case (url, outcome) =>
        outcome.answer match {
          case Answer(ans, _, _, _, _) =>
            println(url + ": " + ans)
            ans should be (url)
          case _ => fail()
        }
      }
    }
  }
}
