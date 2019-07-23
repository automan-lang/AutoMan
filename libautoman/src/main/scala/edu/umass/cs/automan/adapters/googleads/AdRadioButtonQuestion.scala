package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.ads._
import edu.umass.cs.automan.adapters.googleads.forms._
import edu.umass.cs.automan.adapters.googleads.forms.question.Choice._
import edu.umass.cs.automan.adapters.googleads.forms.question._

import scala.io.StdIn._

object AdRadioButtonQuestion extends App {
  //------------------------------ irrelevant word generator code --------------------------
/*  val dict = scala.io.Source.fromFile("/usr/share/dict/american-english").getLines.toArray
  val rand = new Random

  def word: String = {
    val w = dict(rand.nextInt(dict.length)) + " "
    if (w.length < 10) w.replaceAll("'s", "") else word
  }*/
  //----------------------------------------------------------------------------------------

  def radio(budget: BigDecimal,
            title: String,
            question: String,
            choices: List[Choice],
            min_answers: Int,
            ad_title: String,
            ad_subtitle: String,
            ad_descript: String,
            ad_keywords: List[String] = Nil,
            cpc: BigDecimal = 0.3): Unit = {
    val form = Form(title)
    form.setDescription("This question is part of ongoing computer science research at Williams College.\n" +
                        "All answers are fully anonymous and we will not collect any personal information.\n" +
                        "To find out more about Williams Computer Science visit https://csci.williams.edu/")

    val radiobutton = GRadioButtonQuestion(
      form.id,
      question,
      choices,
    )
    val form_url = form.getPublishedUrl
    println("Form is published: " + form_url)

    println("Enter production account id: ")
    val accountId = readLine()
    val acc = Account(accountId.toLong)
    val camp = acc.createCampaign(budget/2, title)
    //camp.restrictEnglish()

    val ad = camp.createAd(
      ad_title,
      ad_subtitle,
      ad_descript,
      form_url,
      ad_keywords
    )

    camp.setCPC(cpc)
    println("Ad is published. Enabled status: " + ad.is_enabled)

    val t0 = System.currentTimeMillis()
    wait()

    def wait () {
      if(ad.is_approved) {
        go()
      } else {
        Thread.sleep(30*1000)
        println("Awaiting approval")
        println("Elapsed time: " + (System.currentTimeMillis() - t0)/1000.0 + " seconds.")
        wait()
      }
    }

    def go (): Unit = {
      if(form.getResponses.length < min_answers) {
        Thread.sleep(10 * 1000)
        try{
          form.getResponses.foreach((x : String) => println("Response: " + x))
          form.shuffle()
        }
        catch {
          case _ : ScriptError => go()
          case _ : Throwable => println("Could not get responses"); go()
        }
        println("Elapsed time: " + (System.currentTimeMillis() - t0) / 1000.0 + " seconds.")
        go()
      }
    }

    camp.pause()
    println("Final responses:")
    form.getResponses.foreach((x : String) => println(x))
  }

  //------------------Test--------------------------
  radio(1,
    "Question 53",
    "Which planet is closest to Earth?",
    List(choice('venus,"Venus"),
         choice('mars,"Mars"),
         choice('saturn,"Saturn"),
         choice('neptune,"Neptune"),

    ),
    5,
    "Assist Crowdsourcing Research",
    "Input Your Expertise",
    "Answer just one quick question to assist computer science research",
    ad_keywords = List(
      "programming",
      "crowd","science",
      "research",
      "volunteering",
      "quiz",
      "college")
    )
  //-------------------------------------------------
}


