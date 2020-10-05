package org.automanlang

import org.scalatest._

trait RetryOnceTest extends FlatSpec with Matchers with Retries {

  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry { super.withFixture(test) }
    else
      super.withFixture(test)
  }

  // To use:
  // Extend your test class with this trait,
  // import org.scalatest.tagobjects.Retryable
  // and then tag your test as below:
  //  "An empty Set" should "have size 0" taggedAs(Retryable) in {
  //    assert(Set.empty.size === 0)
  //  }
}