import scala.collection.immutable.HashMap

class Experiment(val text: String, val choices: List[String], val parameterization: Map[Symbol, List[String]], val budget: Double, val confidence_interval: Double) {
  //  val text: String
  //  var choices: List[String]
  //  var parameterization: Map[Symbol, List[String]]
  //  var budget: Double
  //  var confidence_interval: Double

  def getText() = text

  def getChoices(): List[String] = choices

  def getParameterization(): Map[Symbol, List[String]] = parameterization

  def getBudget(): Double = budget

  def getConfidence(): Double = confidence_interval

  def genderMap(names: List[String]): Map[String, String] = {
    var nameMap = new HashMap[String, String]()

    for (n <- names) {
      nameMap += (n -> gender(n))
      //nameMap(n) = gender(n)
    }
    nameMap
  }

  def gender(name: String): String = {
    if (name == "Linda" || name == "Emmie") "she"
    else if (name == "Dan" || name == "Joe") "he"
    else "they"
  }
}

  object ExperimentMain {
    def main(args: Array[String]): Unit = {
      val nameList = List("Linda", "Dan", "Emmie", "Joe")

      val linda = new Experiment(
        'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + 'name_gender + " majored in " + 'major  +
          ". As a student,  " + 'name_gender + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
        List('name + " is a " + 'job + ".", 'name + " is a " + 'job + " and is active in the " +  'issues + " movement."),
        Map(
          'name -> nameList,
          //'name_gender -> genderMap(nameList),//genderMap(Symbol.valueFromKey("name")),
          'age -> List("21", "31", "41", "51", "61"),
          'major -> List("chemistry", "psychology", "english literature", "philosophy", "women's studies"),
          'issues -> List("discrimination and social justice", "fair wages", "animal rights", "white collar crime", "unemployed circus workers"),
          'demonstrations -> List("anti-nuclear", "anti-war", "pro-choice", "anti-abortion", "anti-animal testing"),
          'jobs -> List("bank teller", "almond paste mixer", "tennis scout", "lawyer", "professor")
        ),
        300,
        0.95
      )
      println(s"Question: ${linda.getText()}\n Options: ${linda.getChoices()}\n Params: ${linda.getParameterization}\n Budget: ${linda.getBudget}\n Confidence: ${linda.getConfidence()}")

//      val lindaInstance = new ExperimentInstance(
//        Map(
//          'name -> "Linda",
//          'age -> "31",
//          'major -> "philosophy",
//          'issue ->"discrimination and social justice",
//          'demonstration -> "anti-nuclear",
//          'job -> "bank teller"
//        ),
//        'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + this.gender('name.toString()) + " majored in " + 'major  +
//            ". As a student,  " + this.gender('name.toString()) + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
//          List('name + "is a " + 'job + ".", 'name + "is a " + 'job + " and is active in the " +  'issues + " movement."),
//        Map(
//          'name -> nameList,
//          //'name_gender -> genderMap(nameList),//genderMap(Symbol.valueFromKey("name")),
//          'age -> List("21", "31", "41", "51", "61"),
//          'major -> List("chemistry", "psychology", "english literature", "philosophy", "women's studies"),
//          'issues -> List("discrimination and social justice", "fair wages", "animal rights", "white collar crime", "unemployed circus workers"),
//          'demonstrations -> List("anti-nuclear", "anti-war", "pro-choice", "anti-abortion", "anti-animal testing"),
//          'jobs -> List("bank teller", "almond paste mixer", "tennis scout", "lawyer", "professor")
//        ),
//          3,
//          0.95)
    }
  }
//gender(instance.name())
//  object SampleExperiment() = Experiment (
//    'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + gender(instance.name()) + " majored in " + 'major  +
//      ". As a student,  " + 'name_gender + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
//    List('name + "is a " + 'job + ".", 'name + "is a " + 'job + " and is active in the " +  'issues + " movement."),
//    Map(
//      'name -> List("Linda", "Dan", "Emmie", "Joe"),
//      'name_gender -> genderMap('name),
//      'age -> List("21", "31", "41", "51", "61"),
//      'major -> List("chemistry", "psychology", "english literature", "philosophy", "women's studies"),
//      'issues -> List("discrimination and social justice", "fair wages", "animal rights", "white collar crime", "unemployed circus workers"),
//      'demonstrations -> List("anti-nuclear", "anti-war", "pro-choice", "anti-abortion", "anti-animal testing"),
//      'jobs -> List("bank teller", "almond paste mixer", "tennis scout", "lawyer", "professor")
//    ),
//    300,
//    0.95
//  )
//
//  def lindaProblemInstance() = experimentInstance (
//    'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + gender('name.toString()) + " majored in " + 'major  +
//      ". As a student,  " + gender('name.toString()) + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
//    List('name + "is a " + 'job + ".", 'name + "is a " + 'job + " and is active in the " +  'issues + " movement."),
//    Map(
//      'name -> "Linda",
//      'age -> "31",
//      'major -> "philosophy",
//      'issue ->"discrimination and social justice",
//      'demonstration -> "anti-nuclear",
//      'job -> "bank teller"
//    ),
//    3,
//    0.95
//  )


//  def experimentInstance(text: String, choices: List[String], parameterization: Map[Symbol, String], budget: BigDecimal, confidence_interval: BigDecimal): Unit = {
//
//  }
