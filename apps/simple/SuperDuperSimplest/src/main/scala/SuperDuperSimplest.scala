/* This is the application shown in the online tutorial.
 *
 * It has two small issues:
 *  - Without special configuration, MTurk requires that tasks
 *    have at least 10 HITs.  For this question, the minimum
 *    number of HITs is 3, so this version is a little
 *    inefficient.
 *  - If AutoMan is unable to deliver a high-confidence answer,
 *    it will not be clear to the user, since the `println`
 *    method calls the `toString` method on the return value
 *    of `which_one()`.
 *
 * A better, but slightly more complicated version of this same
 * program can be found in the apps/simple/SimpleRadioButton
 * folder.  It solves the above problems by
 *  - providing special configuration, and
 *  - by pattern matching on the result.
 *
 * Caveats aside, this application does work and is relatively
 * easy to understand.
 */

import org.automanlang.adapters.mturk.DSL._

object SuperDuperSimplest extends App {

  implicit val a = mturk (
    access_key_id = args(0),
    secret_access_key = args(1),
    sandbox_mode = true
  )

  def which_one() = radio (
    budget = 5.00,
    text = "Which one of these does not belong?",
    options = (
      choice('oscar, "Oscar the Grouch", "https://tinyurl.com/y2nf2h76"),
      choice('kermit, "Kermit the Frog", "https://tinyurl.com/yxh2emmr"),
      choice('spongebob, "Spongebob Squarepants", "https://tinyurl.com/y3uv2oew"),
      choice('cookiemonster, "Cookie Monster", "https://tinyurl.com/y68x9zvx"),
      choice('thecount, "The Count", "https://tinyurl.com/y6na5a8a")
    )
  )

  automan(a) {
    println("Answer is: " + which_one())
  }
}