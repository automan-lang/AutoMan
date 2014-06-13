import com.amazonaws.mturk.requester._
import edu.umass.cs.automan.adapters.MTurk.SimpleMTurk

object delete_old_quals extends App {
  if (args.size != 4) {
    println("Usage: delete_old_quals <access key: string> <secret key: string> <sandbox mode: bool> <qualification search string>")
  }
  val access_key = args(0)
  val secret_key = args(1)
  val sandbox_mode = args(2).toBoolean
  val query = args(3)

  val PAGE_SZ = 100

  // start up simple MTurk adapter
  private val mturk = new SimpleMTurk(access_key, secret_key, sandbox_mode).setup()

  def getAllHITs(status: ReviewableHITStatus) : List[HIT] = {
    // get all completed hits
    var hits = List[HIT]()
    var pagenum = 1
    var done = false
    while (!done) {
      System.err.print(".")
      val response = mturk.backend.getReviewableHITs(
        null,
        status,
        SortDirection.Ascending,
        GetReviewableHITsSortProperty.Enumeration,
        pagenum,
        PAGE_SZ
      )
      val hitarr = response.getHIT
      if (hitarr != null) {
        hits = hitarr.toList ::: hits
        pagenum += 1
      } else {
        done = true
      }
    }
    println()
    hits
  }

  def searchQualifications(query: String) : List[QualificationType] = {
    // get all completed hits
    var quals = List[QualificationType]()
    var pagenum = 1
    var done = false
    while (!done) {
      System.err.print(".")
      val response = mturk.backend.searchQualificationTypes(
        query,
        false,
        true,
        SortDirection.Ascending,
        SearchQualificationTypesSortProperty.Name, // wtf?
        pagenum,
        PAGE_SZ
      )
      val qarr = response.getQualificationType
      if (qarr != null) {
        quals = qarr.toList ::: quals
        pagenum += 1
      } else {
        done = true
      }
    }
    println()
    quals
  }

  var i = 0
  searchQualifications(query).foreach { qual =>
    val qt_id = qual.getQualificationTypeId
    println("Disposing of qualification type: " + qt_id)
    mturk.backend.disposeQualificationType(qt_id)
    i += 1
  }

  println("Disposed of " + i + " qualification types.")
}
