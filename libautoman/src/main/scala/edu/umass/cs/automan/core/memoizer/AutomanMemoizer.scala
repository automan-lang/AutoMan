package edu.umass.cs.automan.core.memoizer

import net.java.ao._
import edu.umass.cs.automan.core.question.{FreeTextQuestion, CheckboxQuestion, RadioButtonQuestion, Question}
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class AutomanMemoizer(DBConnString: String, user: String, password: String) {
  Utilities.DebugLog("Startup...",LogLevel.INFO, LogType.MEMOIZER,null)
  private val _manager = new EntityManager(DBConnString, user, password)
  _manager.migrate(classOf[RadioButtonAnswerMemo], classOf[CheckboxAnswerMemo], classOf[FreeTextAnswerMemo])

  // returns all of the stored answers for a question with a given memo_hash
  def checkDB[Q <: Question](q: Q, dual: Boolean) : List[ScalarAnswer] = synchronized {
    q match {
      case rbq: RadioButtonQuestion => {
        if (!dual) { // There are never any RadioButton duals
          val memos = _manager.find[RadioButtonAnswerMemo,java.lang.Integer](classOf[RadioButtonAnswerMemo], "memoHash = ?", rbq.memo_hash(false))
          memos.map { memo =>
            val r = new RadioButtonAnswer(None, memo.getWorkerId, Symbol(memo.getAnswerValue.drop(1)) )
            r.custom_info = Some(memo.getCustomInfo)
            r.paid = memo.getPaidStatus
            r.memo_handle = memo
            r
          }.toList
        } else {
          List[RadioButtonAnswer]()
        }
      }
      case cbq: CheckboxQuestion => {
        val memos = _manager.find[CheckboxAnswerMemo,java.lang.Integer](classOf[CheckboxAnswerMemo], "memoHash = ?", cbq.memo_hash(dual))
        memos.map { memo =>
          val r = new CheckboxAnswer(None, memo.getWorkerId, memo.getAnswerValues.split(",").map(str => Symbol(str.drop(1))).toSet)
          r.custom_info = Some(memo.getCustomInfo)
          r.paid = memo.getPaidStatus
          r.memo_handle = memo
          r
        }.toList
      }
      case ftq: FreeTextQuestion => {
        val memos = _manager.find[FreeTextAnswerMemo,java.lang.Integer](classOf[FreeTextAnswerMemo], "memoHash = ?", ftq.memo_hash(false))
        memos.map { memo =>
          val r = new FreeTextAnswer(None, memo.getWorkerId, Symbol(memo.getAnswerValue.drop(1)))
          r.custom_info = Some(memo.getCustomInfo)
          r.paid = memo.getPaidStatus
          r.memo_handle = memo
          r
        }.toList
      }
    }
  }

  def writeAnswer[A <: Answer, Q <: Question](q: Q, a: A, is_dual: Boolean) : Unit = synchronized {
    a match {
      case rba: RadioButtonAnswer => {
        if (!is_dual) {
          val memo = _manager.create[RadioButtonAnswerMemo,java.lang.Integer](classOf[RadioButtonAnswerMemo])
          memo.setMemoHash(q.memo_hash(false))
          memo.setAnswerValue(rba.value.toString())
          a.custom_info match { case Some(ci) => memo.setCustomInfo(ci); case None => { /* do nothing */ }}
          memo.setPaidStatus(rba.paid)
          memo.setWorkerId(rba.worker_id)
          memo.save()
          rba.memo_handle = memo
        }
      }
      case cba: CheckboxAnswer => {
        val memo = _manager.create[CheckboxAnswerMemo,java.lang.Integer](classOf[CheckboxAnswerMemo])
        memo.setMemoHash(q.memo_hash(is_dual))
        memo.setAnswerValues(cba.values.map {ans => ans.toString}.reduceLeft(_ + "," + _))
        a.custom_info match { case Some(ci) => memo.setCustomInfo(ci); case None => { /* do nothing */ }}
        memo.setPaidStatus(cba.paid)
        memo.setWorkerId(cba.worker_id)
        memo.save()
        cba.memo_handle = memo
      }
      case fta: FreeTextAnswer => {
        val memo = _manager.create[FreeTextAnswerMemo,java.lang.Integer](classOf[FreeTextAnswerMemo])
        memo.setMemoHash(q.memo_hash(false))
        memo.setAnswerValue(fta.value.toString())
        a.custom_info match { case Some(ci) => memo.setCustomInfo(ci); case None => { /* do nothing */ }}
        memo.setPaidStatus(fta.paid)
        memo.setWorkerId(fta.worker_id)
        memo.save()
        fta.memo_handle = memo
      }
    }
  }

  def deleteAll[Q <: Question](q: Q) : Unit = synchronized {
    q match {
      case rbq: RadioButtonQuestion => {
        val e = _manager.find[RadioButtonAnswerMemo,java.lang.Integer](classOf[RadioButtonAnswerMemo], "memoHash = ?", rbq.memo_hash(false))
        e.foreach(_manager.delete(_))
      }
      case cbq: CheckboxQuestion => {
        val e1 = _manager.find[CheckboxAnswerMemo,java.lang.Integer](classOf[CheckboxAnswerMemo], "memoHash = ?", cbq.memo_hash(true))
        val e2 = _manager.find[CheckboxAnswerMemo,java.lang.Integer](classOf[CheckboxAnswerMemo], "memoHash = ?", cbq.memo_hash(false))
        e1.foreach(_manager.delete(_))
        e2.foreach(_manager.delete(_))
      }
      case ftq: FreeTextQuestion => {
        val e = _manager.find[FreeTextAnswerMemo,java.lang.Integer](classOf[FreeTextAnswerMemo], "memoHash = ?", ftq.memo_hash(false))
        e.foreach(_manager.delete(_))
      }
    }
  }
}