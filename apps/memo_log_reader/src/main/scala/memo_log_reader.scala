import edu.umass.cs.automan.adapters.MTurk.SimpleMTurk
import edu.umass.cs.automan.core.memoizer.{ThunkMemo, RadioButtonAnswerMemo}
import net.java.ao._
import TupleType._

object memo_log_reader extends App {
  if (args.size != 5) {
    println("Usage: memo_log_reader <access key: string> <secret key: string> <sandbox mode: bool> <memo CSV path: string> <thunk CSV path: string>")
  }
  val access_key = args(0)
  val secret_key = args(1)
  val sandbox_mode = args(2).toBoolean
  val memo_csv = new log.CSV(args(3))
  val thunk_csv = new log.CSV(args(4))

  // DB parameters
  private val _memo_db_name: String = "AutomanMemoDB"
  private val _memo_conn_string: String = "jdbc:derby:" + _memo_db_name + ";create=true"
  private val _memo_user: String = ""
  private val _memo_pass: String = ""
  private val _thunk_db_name: String = "ThunkLogDB"
  private val _thunk_conn_string: String = "jdbc:derby:" + _thunk_db_name + ";create=true"
  private val _thunk_user: String = ""
  private val _thunk_pass: String = ""

  // start up AutomanMemoDB
  private val memo_db = new EntityManager(_memo_conn_string, _memo_user, _memo_pass)

  // start up ThunkLogDB
  private val thunk_db = new EntityManager(_thunk_conn_string, _thunk_user, _thunk_pass)

  // start up simple MTurk adapter
  private val mturk = new SimpleMTurk(access_key, secret_key, sandbox_mode).setup()

  // read data
  val answers = memo_db.find[RadioButtonAnswerMemo,java.lang.Integer](classOf[RadioButtonAnswerMemo])
  val thunk_memos = thunk_db.find[ThunkMemo,java.lang.Integer](classOf[ThunkMemo])

  // convert data
  val rbtuples = answers.map { answer => RBTuple.fromRadioButtonAnswerMemo(answer) }
  val ttuples = thunk_memos.map { thunk_memo => ThunkTuple.fromThunkMemo(thunk_memo) }

  // fetch HIT data from MTurk
  rbtuples.foreach { rbtuple =>
    System.err.println("Fetching data for HIT " + rbtuple.hit_id + "...")
    rbtuple.setHIT(mturk.backend.getHIT(rbtuple.hit_id))
  }

  // write data to CSVs
  RBTuple.writeHeaderToLog(memo_csv)
  rbtuples.foreach { rbtuple => rbtuple.writeLog(memo_csv) }

  ThunkTuple.writeHeaderToLog(thunk_csv)
  ttuples.foreach { ttuple => ttuple.writeLog(thunk_csv) }

  // close logs
  memo_csv.close()
  thunk_csv.close()
}
