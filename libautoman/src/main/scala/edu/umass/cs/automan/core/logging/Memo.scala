package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.info.QuestionType._
import edu.umass.cs.automan.core.logging.tables.{DBRadioButtonAnswer, DBThunkHistory}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.SchedulerState._
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import scala.slick.driver.DerbyDriver
import scala.slick.driver.DerbyDriver.simple._
import scala.slick.jdbc.meta.MTable

object Memo {
  def sameThunks[A](ts1: List[Thunk[A]], ts2: List[Thunk[A]]) : Boolean = {
    val t1_map = ts1.map { t => t.thunk_id -> t }.toMap
    ts2.foldLeft (true) { case (acc, t) =>
      acc && t1_map.contains(t.thunk_id) && sameThunk(t1_map(t.thunk_id), t)
    }
  }
  def sameThunk[A](t1: Thunk[A], t2: Thunk[A]) : Boolean = {
    // this is split into separate statements
    // to make debugging easier
    val c1 = t1.thunk_id == t2.thunk_id
    val c2 = t1.question == t2.question
    val c3 = t1.timeout_in_s == t2.timeout_in_s
    val c4 = t1.worker_timeout == t2.worker_timeout
    val c5 = t1.cost == t2.cost
    val c6 = t1.created_at == t1.created_at
    val c7 = t1.state == t2.state
    val c8 = t1.worker_id == t2.worker_id
    val c9 = t1.answer == t2.answer
    val is_same = c1 && c2 && c3 && c4 && c5 && c6 && c7 && c8 && c9
    is_same
  }
}

class Memo(log_config: LogConfig.Value) {
  // implicit conversions
  implicit val javaUtilDateMapper = DBThunkHistory.javaUtilDateMapper
  implicit val symbolStringMapper = DBRadioButtonAnswer.symbolStringMapper

  // typedefs
  type DBThunk = (UUID, UUID, BigDecimal, Date, Int, Int)
  type DBThunkHistory =(UUID, Date, SchedulerState)
  type DBQuestion = (UUID, String, QuestionType)
  type DBRadioButtonAnswer = (Int, Symbol, String)
  type DBSession = DerbyDriver.backend.Session

  // connection string
  protected[automan] val jdbc_conn_string = "jdbc:derby:AutoManMemoDB;create=true"

  // TableQuery aliases
  protected[automan] val dbThunk = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunk]
  protected[automan] val dbThunkHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunkHistory]
  protected[automan] val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  protected[automan] val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]

  // Thunk cache
  protected var all_thunk_ids = Map[UUID,SchedulerState.Value]()

  // get DB handle
  val db_opt = log_config match {
    case LogConfig.NO_LOGGING => None
    case _ => {
      Some(Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver"))
    }
  }

  protected[automan] def init() : Unit = {
    init_database_if_required(List())
  }

  /**
   * Run table definitions if this is the first time the database is run.  Overring subclasses
   * should provide their DDLs as a list to this method instead of overriding it.
   * @param ddls Slick Table definitions.
   */
  protected def init_database_if_required(ddls: List[DerbyDriver.SchemaDescription]) : Unit = {
    val base_ddls: DerbyDriver.DDL = dbThunk.ddl ++ dbThunkHistory.ddl ++ dbQuestion.ddl ++ dbRadioButtonAnswer.ddl
    val all_ddls: DerbyDriver.DDL = if (ddls.nonEmpty) {
      base_ddls ++ ddls.tail.foldLeft(ddls.head){ case (acc,ddl) => acc ++ ddl }
    } else {
      base_ddls
    }

    all_thunk_ids = db_opt match {
      case Some(db) => {
        val tables = db.withSession { implicit session => MTable.getTables(None, None, None, None).list.map(_.name.name)}
        if (!tables.contains(dbQuestion.baseTableRow.tableName)) {
          db.withSession { implicit s => all_ddls.create }
          Map.empty
        } else {
          // prepopulate cache with all of the thunk_ids from the database
          db withSession { implicit s => getAllThunksMap }
        }
      }
      case None => Map.empty
    }
  }

  protected[automan] def allThunksQuery() = {
    // subquery: get thunk_id -> most recent state change time
    val MSQ = dbThunkHistory.groupBy(_.thunk_id).map{ case (thunk_id,row) => thunk_id -> row.map(_.state_change_time).max }

    // get latest thunk histories
    val THS = for {
    th <- dbThunkHistory
    m <- MSQ
    if th.thunk_id === m._1 && th.state_change_time === m._2
  } yield th

    // join with thunk
    val TS_THS = dbThunk join THS on (_.thunk_id === _.thunk_id)

    // join with question
    dbQuestion join TS_THS on (_.id === _._1.question_id)
  }

  protected[automan] def getAllThunksMap(implicit session: DBSession) : Map[UUID,SchedulerState.Value] = {
    allThunksQuery().map { case (dbquestion, (dbthunk, dbthunkhistory)) =>
      dbthunk.thunk_id -> dbthunkhistory.scheduler_state
    }.list.toMap
  }

  /**
   * Restore all Thunks from the database given a question's memo_hash.
   * @param q An AutoMan question.
   * @tparam A The data type of the Answer.
   * @return A list of Thunks.
   */
  def restore[A](q: Question[A]) : List[Thunk[A]] = {
    db_opt match {
      case Some(db) => {
        val QS_TS_THS = allThunksQuery()

          // filter by memo_hash
        val fQS_TS_THS = QS_TS_THS.filter(_._1.memo_hash === q.memo_hash)

          // LEFT join with answers
          // ((DBQuestion, (DBThunk, DBThunkHistory)), DBAnswerKind)
        val A_QS_TS_THS = q.getQuestionType match {
          case RadioButtonQuestion => {
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbthunk, dbthunkhistory)), dbradiobuttonanswer) =>
                ( dbthunk.thunk_id,
                  dbthunk.timeout_in_s,
                  dbthunk.worker_timeout_in_s,
                  dbthunk.cost,
                  dbthunk.creation_time,
                  dbthunkhistory.scheduler_state,
                  true,
                  dbradiobuttonanswer.worker_id.?,
                  dbradiobuttonanswer.answer.?,
                  dbthunkhistory.state_change_time
                )
            }
          }
          case _ => throw new NotImplementedError()
        }

          // execute query
        val results = db.withSession { implicit s => A_QS_TS_THS.list }

          // make and return thunks
        results.map {
          case (thunk_id,
                timeout_in_s,
                worker_timeout_in_s,
                cost,
                created_at,
                state,
                from_memo,
                worker_id_opt,
                answer_opt,
                state_changed_at) =>
            Thunk[A](
              thunk_id,
              q,
              timeout_in_s,
              worker_timeout_in_s,
              cost,
              created_at,
              state,
              from_memo = true,
              worker_id_opt,
              answer_opt.asInstanceOf[Option[A]],
              state_changed_at
            )
        }
      }
      case None => List.empty
    }
  }

  protected[automan] def questionInDB(memo_hash: String)(implicit db: DBSession) : Boolean = {
    dbQuestion.filter(_.memo_hash === memo_hash).firstOption match {
      case Some(q) => true
      case None => false
    }
  }

  protected[automan] def needsUpdate[A](ts: List[Thunk[A]]) : List[InsertUpdateOrSkip[Thunk[A]]] = {
    ts.map { t =>
      if (!all_thunk_ids.contains(t.thunk_id)) {
        Insert(t)
      } else if (all_thunk_ids(t.thunk_id) != t.state) {
        Update(t)
      } else {
        Skip(t)
      }
    }
  }

  protected[automan] def thunk2ThunkTuple[A](ts: List[Thunk[A]]) : List[(UUID, UUID, BigDecimal, Date, Int, Int)] = {
    ts.map(t => (t.thunk_id, t.question.id, t.cost, t.created_at, t.timeout_in_s, t.worker_timeout))
  }

  protected[automan] def thunk2ThunkHistoryTuple[A](ts: List[Thunk[A]]) : List[(Int, UUID, Date, SchedulerState)] = {
    ts.map(t => (1, t.thunk_id, new Date(), t.state))
  }

  protected[automan] def thunk2ThunkAnswerTuple[A](ts: List[Thunk[A]], histories: Seq[(UUID, Int)]) : List[(Int, A, String)] = {
    val history_dict = histories.toMap
    ts.flatMap { t =>
      t.answer match {
        case Some(ans) =>
          assert(history_dict.contains(t.thunk_id))
          assert(t.worker_id != None)
          Some(history_dict(t.thunk_id), ans, t.worker_id.get)
        case None => None
      }
    }
  }

  protected[automan] def insertAnswerTable[A](ts: List[Thunk[A]], histories: Seq[(UUID, Int)])(implicit session: DBSession) = {
    assert(ts.size != 0)
    ts.head.question.getQuestionType match {
      case RadioButtonQuestion =>
        dbRadioButtonAnswer ++= thunk2ThunkAnswerTuple(ts, histories).asInstanceOf[List[(Int, Symbol, String)]]
      case RadioButtonDistributionQuestion => ???
      case CheckboxQuestion => ???
      case CheckboxDistributionQuestion => ???
      case FreeTextQuestion => ???
      case FreeTextDistributionQuestion => ???
    }
  }

  /**
   * Updates the database given a complete list of Thunks.
   * @param ts A non-empty list of Thunks.
   * @tparam A The data type of the Answer.
   */
  def save[A](q: Question[A], ts: List[Thunk[A]]) : Unit = {
    assert(ts.size != 0)  // should never be given zero thunks

    db_opt match {
      case Some(db) =>
        db.withTransaction { implicit session =>
          synchronized {
            // is the question even in the database?
            if (!questionInDB(q.memo_hash)) {
                // create dbQuestion record for this memo_hash
                dbQuestion += (q.id, q.memo_hash, q.getQuestionType)
            }

            // determine which records need to be inserted/updated/ignored
            val (inserts, updates) = needsUpdate (ts).foldLeft ((List.empty[Thunk[A]], List.empty[Thunk[A]] ) ) {
              case (acc, ius) => ius match {
                case Insert(t) => (t :: acc._1, acc._2)
                case Update(t) => (acc._1, t :: acc._2)
                case Skip(t) => acc
              }
            }

            // do bulk insert for new thunks
            dbThunk ++= thunk2ThunkTuple(inserts)

            // do bulk insert for all thunk histories (inserts and updates)
            dbThunkHistory ++= thunk2ThunkHistoryTuple(inserts ::: updates)

            // Derby can only return a single item, so we were not able to
            // get a thunk_id -> history_map in the previous step;
            // instead we query the table we just inserted into
            val t_ids = ts.map(_.thunk_id).toSet
            val histories = dbThunkHistory.filter(_.thunk_id inSet t_ids).map(th => (th.thunk_id, th.history_id)).list

            // do bulk insert for all answered thunks
            insertAnswerTable(ts, histories)

            // update the cache
            all_thunk_ids = ts.map { t =>
              t.thunk_id -> t.state
            }.toMap
          }
        }
      case None => ()
    }
  }

  /**
   * This call deletes all records stored in all of the Memo
   * database's tables.
   */
  def wipeDatabase() : Unit = {
    db_opt match {
      case Some(db) =>
        db.withTransaction { implicit session =>
          dbQuestion.delete
          dbThunk.delete
          dbThunkHistory.delete
          dbRadioButtonAnswer.delete
        }
      case None => ()
    }
  }
}
