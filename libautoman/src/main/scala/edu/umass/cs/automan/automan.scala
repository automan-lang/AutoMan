package edu.umass.cs.automan

import java.util.UUID

import edu.umass.cs.automan.core.AutomanAdapter

object automan {
  def apply[T](a: AutomanAdapter, test_mode: Boolean = false, in_mem_db: Boolean = false)(block: => T): T = {
    if (test_mode) {
      a.database_path = "AutoManMemoDB_TEST_" + UUID.randomUUID()
    }
    a.in_memory_db = in_mem_db
    val output = block // block is evaluated here
    a.close()
    if (test_mode) {
      a.memo_delete()
    }
    output
  }
}
