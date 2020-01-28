package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar
import edu.umass.cs.automan.core.grammar.SampleGrammar.{bind, buildString, render}

import scala.collection.mutable.ArrayBuffer

object Ranking {
  def product(l: Array[Int]): Int = {
    var toRet = 1
    if (l.length != 0) { //todo redundant?
      for (e <- l) {
        toRet *= e
      }
    }
    toRet

    //    if(l.length == 0) 1
    //    else l(0) * product(l.slice(1, l.length - 1))
  }

  // rank based on array
  def rank(vals: Array[Int], bases: Array[Int]): Int = {
    var toRet = 0
    for(i <- 0 to vals.length - 1){
      toRet += vals(i)*product(bases.slice(i+1,bases.length))
    }
    toRet
  }

  // unrank to array from int
  def unrank(rank: Int, bases: Array[Int]): ArrayBuffer[Int] = {
    var toRet: ArrayBuffer[Int] = new ArrayBuffer[Int]()
    for(i <- 0 to bases.length - 1){
      toRet += rank/product(bases.slice(i+1, bases.length))%bases(i)
    }
    toRet
  }

  // given a grammar, an int, and the bases, prints an experiment instance
  def renderInstance(grammar: Grammar, choice: Int, bases: Array[Int]): Unit = {
    val assignment = unrank(choice, bases) // get the assignment from the number
    grammar.curSymbol = grammar.startSymbol
    val scope = bind(grammar, assignment.toArray, 0, Set())
    grammar.curSymbol = grammar.startSymbol // TODO make this less ugly
    render(grammar, scope)
    println()
  }

  // given a grammar, an int, and the bases, creates a string of an experiment instance
  def buildInstance(grammar: Grammar, choice: Int, bases: Array[Int]): String = {
    val assignment = unrank(choice, bases) // get the assignment from the number
    grammar.curSymbol = grammar.startSymbol
    val scope = bind(grammar, assignment.toArray, 0, Set())
    grammar.curSymbol = grammar.startSymbol
    buildString(grammar, scope, new StringBuilder).toString()
  }

  def main(args: Array[String]): Unit = {
    val lindaBases = Array(4, 5, 6, 5, 5, 5, 5) // each number is number of possible assignments for that slot
    val total = product(lindaBases)

    println(s"total: ${total}")

    val xRank = rank(Array(3,3,4,4,2,2,3), lindaBases)
    println(xRank)

    val lRank = rank(Array(0,1,1,0,0,0,0), lindaBases) // the ranking for the classic Linda problem
    println(lRank)

    val grammar = getGrammar()
    grammar.curSymbol = grammar.startSymbol
    renderInstance(grammar, lRank, lindaBases)
    //grammar.curSymbol = grammar.startSymbol
    println(s"Build version:\n${buildInstance(grammar, lRank, lindaBases)}")
    grammar.curSymbol = grammar.startSymbol
    renderInstance(grammar, xRank, lindaBases)
    //grammar.curSymbol = grammar.startSymbol
    println(s"Build version:\n${buildInstance(grammar, xRank, lindaBases)}")
//    for (i <- 0 to total - 1) {
//      val values = unrank(i, lindaBases)
//      val r = rank(values.toArray, lindaBases)
//      assert(i == r)
//      println(s"${values} ${r}")
//    }
  }

  // returns the Linda grammar
  def getGrammar(): Grammar = {
    val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    // TODO: what if multiple params need same function?
    val articles = Map[String, String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )
    // The problem statement
    val lindaS = new Sequence(
      List(
        new Name("Name"),
        new Terminal(" is "),
        new Name("Age"),
        new Terminal(" years old, single, outspoken, and very bright. "),
        new grammar.Function(pronouns, "Name", true),
        new Terminal(" majored in "),
        new Name("Major"),
        new Terminal(". As a student, "),
        new grammar.Function(pronouns, "Name", false),
        new Terminal(" was deeply concerned with issues of "),
        new Name("Issue"),
        new Terminal(", and also participated in "),
        new Name("Demonstration"),
        new Terminal(" demonstrations.\nWhich is more probable?\n1. "),
        new Name("Name"),
        new Terminal(" is "),
        new grammar.Function(articles, "Job", false),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(".\n2. "),
        new Name("Name"),
        new Terminal(" is "),
        new grammar.Function(articles, "Job", false),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(" and is active in the "),
        new Name("Movement"),
        new Terminal(" movement.")
      )
    )
    val Linda = Grammar( // The grammar
      Map(
        "Start" -> new Name("lindaS"),
        "lindaS" -> lindaS,
        "Name" -> new Choices(
          List(
            new Terminal("Linda"),
            new Terminal("Dan"),
            new Terminal("Emmie"),
            new Terminal("Xavier the bloodsucking spider")
          )
        ),
        "Age" -> new Choices(
          List(
            new Terminal("21"),
            new Terminal("31"),
            new Terminal("41"),
            new Terminal("51"),
            new Terminal("61")
          )
        ),
        "Major" -> new Choices(
          List(
            new Terminal("chemistry"),
            new Terminal("psychology"),
            new Terminal("english literature"),
            new Terminal("philosophy"),
            new Terminal("women's studies"),
            new Terminal("underwater basket weaving")
          )
        ),
        "Issue" -> new Choices(
          List(
            new Terminal("discrimination and social justice"),
            new Terminal("fair wages"),
            new Terminal("animal rights"),
            new Terminal("white collar crime"),
            new Terminal("unemployed circus workers")
          )
        ),
        "Demonstration" -> new Choices(
          List(
            new Terminal("anti-nuclear"),
            new Terminal("anti-war"),
            new Terminal("pro-choice"),
            new Terminal("anti-abortion"),
            new Terminal("anti-animal testing")
          )
        ),
        "Job" -> new Choices(
          List(
            new Terminal("bank teller"),
            new Terminal("almond paste mixer"),
            new Terminal("tennis scout"),
            new Terminal("lawyer"),
            new Terminal("professor")
          )
        ),
        "Movement" -> new Choices(
          List(
            new Terminal("feminist"),
            new Terminal("anti-plastic water bottle"),
            new Terminal("pro-pretzel crisp"),
            new Terminal("pro-metal straw"),
            new Terminal("environmental justice")
          )
        )
      ), "Start"
    )
    Linda
  }
}
