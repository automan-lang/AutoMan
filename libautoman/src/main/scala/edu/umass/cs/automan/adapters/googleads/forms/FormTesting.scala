package edu.umass.cs.automan.adapters.googleads.forms

object FormTesting extends App{

  val form = Form(Symbol("10oJbexfqJi9Az5oEZn3ViVLWSprrt4RlPyvlgW8DDdQ"))
  go()

  def go() {
    try {
      form.getResponses.foreach((x : String) => println("Response: " + x)); go()
    }
    catch {
      case _: ScriptError => println("Could not get responses"); go()
      case x : Throwable => println("in responses:" + x.toString); go()
    }
    try {
      form.shuffle(); go()
    }
    catch {
      case _: ScriptError => println("Could not shuffle"); go()
      case x : Throwable => println("in shuffle:" + x.toString); go()
    }
    println("exited")
  }

  //form.setDescription("This question is part of ongoing computer science research at Williams College.\n" +
  //  "All answers are fully anonymous and we will not collect any personal information.\n" +
  //  "To find out more about Williams Computer Science visit https://csci.williams.edu/")

  //val camp = Campaign(1373958703,2073836611)
  //camp.setCPC(0.2)

  //Project.setup("New Form Library", "198608039773")
}