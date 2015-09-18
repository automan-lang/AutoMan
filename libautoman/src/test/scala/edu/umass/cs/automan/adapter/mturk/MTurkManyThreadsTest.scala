package edu.umass.cs.automan.adapter.mturk

import java.util.UUID
import edu.umass.cs.automan.test._
import org.scalatest.{Matchers, FlatSpec}
import scala.util.Random
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.core.answer.Answer

class MTurkManyThreadsTest extends FlatSpec with Matchers {

  "A freetext program with many threads" should "work" in {
    val urls = Random.alphanumeric.take(1000).mkString.grouped(10).toList

    implicit val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    def plateTxt(url: String)(implicit a: MTurkAdapter) = a.FreeTextQuestion { q =>
      q.budget = 5.00
      q.text = "Foobitty foobitty foo? http://" + url
      q.allow_empty_pattern = true
      q.pattern = "XXXXXXXXX"
      q.dont_reject = true
      q.mock_answers = makeMocks(Utilities.randomPermute(List(url, url, url + "z", url)))
    }

    automan(a, test_mode = true) {
      // get plate texts from image URLs
      val plate_texts = urls.map { url =>
        (url, plateTxt(url))
      }

      // print out results
      plate_texts.foreach { case (url, outcome) =>
        outcome.answer match {
          case Answer(ans, _, _) =>
            println(url + ": " + ans)
            ans should be (url)
          case _ => fail()
        }
      }
    }
  }
}
