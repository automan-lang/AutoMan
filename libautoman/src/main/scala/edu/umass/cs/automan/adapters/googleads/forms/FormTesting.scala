package edu.umass.cs.automan.adapters.googleads.forms

import java.util

import edu.umass.cs.automan.adapters.googleads.DSL.choice
import edu.umass.cs.automan.adapters.googleads.GoogleAdsAdapter
import edu.umass.cs.automan.adapters.googleads.question.{GQuestionOption, GRadioButtonQuestion}
import edu.umass.cs.automan.adapters.googleads.util.{Authenticate, Service}

import scala.collection.mutable

object FormTesting extends App{
  //Authenticate.setup()
  //Authenticate.scriptRevamp()

  val form = Form(Symbol("1KuqpqGaC7dAIcrb689f3q8I0wajvfh11EN7Q6Pvxi4o"))
  //val form2 = Service.formRetry(() => Form(Symbol("1KuqpqGaC7dAIcrb689f3q8I0wajvfh11EN7Q6Pvxi4")))
  Service.formRetry(() => println(form.getPublishedUrl))
  //Service.formRetry(() => println(form2.getPublishedUrl))

  //val gRBQ = new GRadioButtonQuestion()

  /*
  gRBQ.answers_=(mutable.Queue.empty)
  gRBQ.form_=(form)
  gRBQ.text_=("q1")
  gRBQ.options_=(List(
    GQuestionOption('oscar, "Oscar the Grouch"),
    GQuestionOption('kermit, "Kermit the Frog"),
    GQuestionOption('spongebob, "Spongebob Squarepants"),
    GQuestionOption('cookiemonster, "Cookie Monster"),
    GQuestionOption('thecount, "The Count")
  ))
  gRBQ.create()

  println(gRBQ.item_id)
  gRBQ.answer()

  val gRBQ2 = new GRadioButtonQuestion()

  gRBQ2.answers_=(mutable.Queue.empty)
  gRBQ2.form_=(form)
  gRBQ2.text_=("q2")
  gRBQ2.options_=(List(
    GQuestionOption('oscar, "Oscar the Grouch"),
    GQuestionOption('kermit, "Kermit the Frog"),
    GQuestionOption('spongebob, "Spongebob Squarepants"),
    GQuestionOption('cookiemonster, "Cookie Monster"),
    GQuestionOption('thecount, "The Count")
  ))
  gRBQ2.create()

  println(gRBQ2.item_id)
  gRBQ2.answer()
  go(Nil,Nil)

  def go(l : List[Symbol], l3 : List[Symbol]) {
    gRBQ.answer()
    gRBQ2.answer()
    val l2 : List[Symbol] = gRBQ.answers_dequeue() match
    {
        case Some(s) => s :: l
        case None => l
    }
    val l4 : List[Symbol] = gRBQ2.answers_dequeue() match
    {
      case Some(s) => s :: l3
      case None => l3
    }
    println(l2)
    println(l4)
    Thread.sleep(1000)
    go(l2,l4)
  }

  //form.setDescription("This question is part of ongoing computer science research at Williams College.\n" +
  //  "All answers are fully anonymous and we will not collect any personal information.\n" +
  //  "To find out more about Williams Computer Science visit https://csci.williams.edu/")

  //val camp = Campaign(1373958703,2073836611)
  //camp.setCPC(0.2)

  //Project.setup("New Form Library", "198608039773")
  */
}