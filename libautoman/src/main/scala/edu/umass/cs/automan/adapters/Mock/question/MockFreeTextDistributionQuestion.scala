package edu.umass.cs.automan.adapters.Mock.question

import java.security.MessageDigest
import edu.umass.cs.automan.core.answer.FreeTextAnswer
import edu.umass.cs.automan.core.question.FreeTextDistributionQuestion
import org.apache.commons.codec.binary.Hex

class MockFreeTextDistributionQuestion extends FreeTextDistributionQuestion {
  override def memo_hash: String = {
    val hash_string = this.pattern + this.text + this.image_alt_text + this.image_url + this.title + this.question_type.toString
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(hash_string.getBytes)))
  }
}
