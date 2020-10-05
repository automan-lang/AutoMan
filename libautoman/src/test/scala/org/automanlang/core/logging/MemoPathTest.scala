package org.automanlang.core.logging

import java.io.File

import org.scalatest._
import java.util.UUID
import org.automanlang.test._
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.mock.MockSetup

class MemoPathTest extends FlatSpec with Matchers {

  "A checkbox program" should "correctly recall answers when a database path is specified" in {
    // cleanup if last test's run left junk laying around
    val db_path = "MemoPathTestDB.mv.db"
    val db_f = new File(db_path)
    if (db_f.exists) {
      db_f.delete()
    }

    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      database_path = db_path,
      log_verbosity = LogLevelDebug()
    )

    def which_one(text: String) = checkbox (
      confidence = confidence,
      budget = 8.00,
      text = text,
      options = List(
        choice('oscar, "Oscar the Grouch"),
        choice('kermit, "Kermit the Frog"),
        choice('spongebob, "Spongebob Squarepants"),
        choice('cookie, "Cookie Monster"),
        choice('count, "The Count")
      ),
      mock_answers = makeMocksAt(List(Set('spongebob,'count),Set('spongebob),Set('count,'spongebob),Set('count,'spongebob)), 0)
    )

    def which_one2(text: String) = checkbox (
      confidence = confidence,
      budget = 8.00,
      text = text,
      options = List(
        choice('oscar, "Oscar the Grouch"),
        choice('kermit, "Kermit the Frog"),
        choice('spongebob, "Spongebob Squarepants"),
        choice('cookie, "Cookie Monster"),
        choice('count, "The Count")
      ),
      mock_answers = List()
    )

    // test_mode MUST be false here in order to preserve
    // the user-defined DB name above
    automan(mt, test_mode = false, in_mem_db = false) {
      which_one("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(ans, cost, conf, _, _) =>
          println(s"Answer: $ans, cost: ${'$' + cost.toString}, confidence: $conf")
          (ans == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost >= BigDecimal(2) * BigDecimal(0.06)
            && cost <= BigDecimal(3) * BigDecimal(0.06)) should be(true)
        case _ =>
          fail()
      }

      which_one2("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(ans, cost, conf, _, _) =>
          println(s"Answer: $ans, cost: ${'$' + cost.toString}, confidence: $conf")
          (ans == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0)) should be(true)
        case _ =>
          fail()
      }
    }

    // delete database
    mt.memo_delete()
  }
}
