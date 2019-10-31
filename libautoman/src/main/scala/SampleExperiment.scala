object SampleExperiment {
  /*<name> is <age> years old, single, outspoken, and very bright. <gender(name)> majored in <major>. As a student, <gender(name)> was deeply concerned with <issues>.

    Which is more probable?

    <name> is a <job>.
      <name> is a <job> and is active in the <related(issues)>.

        <name> = 10 (5 male, 5 female)
          <age> = 5 (21, 31, 41, 51, 61)
            <major> = 5 (chemistry, religion, english literature, philosophy, women's studies)
              <issues> = 10
                <demonstrations> = 10

                  <job> = 5 bank teller, almond paste mixer, tennis scout, lawyer, professor
*/

  def lindaProblem() = experiment (
    'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + gender(instance.name()) + " majored in " + 'major  +
      ". As a student,  " + gender(instance.name()) + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
    List('name + "is a " + 'job + ".", 'name + "is a " + 'job + " and is active in the " +  'issues + " movement."),
    Map(
      'name -> List("Linda", "Dan", "Emmie", "Joe"),
      'age -> List("21", "31", "41", "51", "61"),
      'major -> List("chemistry", "psychology", "english literature", "philosophy", "women's studies"),
      'issues -> List("discrimination and social justice", "fair wages", "animal rights", "white collar crime", "unemployed circus workers"),
      'demonstrations -> List("anti-nuclear", "anti-war", "pro-choice", "anti-abortion", "anti-animal testing"),
      'jobs -> List("bank teller", "almond paste mixer", "tennis scout", "lawyer", "professor")
    ),
    300,
    0.95
  )

  def lindaProblemInstance() = experimentInstance (
    'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + gender('name.toString()) + " majored in " + 'major  +
      ". As a student,  " + gender('name.toString()) + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
    List('name + "is a " + 'job + ".", 'name + "is a " + 'job + " and is active in the " +  'issues + " movement."),
    Map(
      'name -> "Linda",
      'age -> "31",
      'major -> "philosophy",
      'issue ->"discrimination and social justice",
      'demonstration -> "anti-nuclear",
      'job -> "bank teller"
    ),
    3,
    0.95
  )

  def gender(name: String): String ={
    if(name == "Linda" || name == "Emmie") "she"
    else if (name == "Dan" || name == "Joe") "he"
    else "they"
  }

  def experiment(text: String, choices: List[String], parameterization: Map[Symbol, List[String]], budget: BigDecimal, confidence_interval: BigDecimal): Unit ={

  }

  def experimentInstance(text: String, choices: List[String], parameterization: Map[Symbol, String], budget: BigDecimal, confidence_interval: BigDecimal): Unit = {

  }
}
