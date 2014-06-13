import com.amazonaws.mturk.requester.AssignmentStatus
import edu.umass.cs.automan.adapters.MTurk.SimpleMTurk
import edu.umass.cs.automan.adapters.MTurk.memoizer.MTurkAnswerCustomInfo
import edu.umass.cs.automan.core.memoizer.{ThunkMemo, RadioButtonAnswerMemo}
import net.java.ao._

object pay_unpaid_workers extends App {
  if (args.size != 3) {
    println("Usage: pay_unpaid_workers <access key: string> <secret key: string> <sandbox mode: bool>")
  }
  val access_key = args(0)
  val secret_key = args(1)
  val sandbox_mode = args(2).toBoolean

  // DB parameters
  private val _memo_db_name: String = "AutomanMemoDB"
  private val _memo_conn_string: String = "jdbc:derby:" + _memo_db_name + ";create=true"
  private val _memo_user: String = ""
  private val _memo_pass: String = ""

  // start up AutomanMemoDB
  private val memo_db = new EntityManager(_memo_conn_string, _memo_user, _memo_pass)

  // start up simple MTurk adapter
  private val mturk = new SimpleMTurk(access_key, secret_key, sandbox_mode).setup()

  // read data
  val answers = memo_db.find[RadioButtonAnswerMemo,java.lang.Integer](classOf[RadioButtonAnswerMemo])

  // find all unpaid workers and pay them
  answers.filter { answer => !answer.getPaidStatus }
         .foreach { answer =>
           val ci = new MTurkAnswerCustomInfo()
           ci.parse(answer.getCustomInfo)

           // double-check that the assignment hasn't already been approved
           val astat = mturk.backend.getAssignment(ci.assignment_id).getAssignment.getAssignmentStatus
           if (astat != AssignmentStatus.Approved) {
             val hit = mturk.backend.getHIT(ci.hit_id)
             System.err.println(
               String.format("Approving payment of %s for assignment %s for worker %s.",
                 hit.getReward.getAmount.toString,
                 ci.assignment_id,
                 answer.getWorkerId
               )
             )
             mturk.backend.approveAssignment(ci.assignment_id, "Thanks.")
           }
         }
}
