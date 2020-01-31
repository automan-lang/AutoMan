package edu.umass.cs.automan.core.grammar

import scala.collection.mutable
import scala.util.Random

trait Production {
  def sample(): String
  def count(g: Grammar, counted: mutable.HashSet[String]): Int
  def toChoiceArr(g: Grammar): Option[Array[Range]]
}

// A set of choices, options for a value
class Choices(options: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    options(ran.nextInt(options.length)).sample()
  }

  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = {
    var c: Int = 0
    for (e <- options) {
      c = c + e.count(g, counted)
    } // choices are additive
    c
  }

  // return specific terminal given by index i
  def sampleSpec(i: Int): String = {
    assert(i < options.length)
    assert(i >= 0)
    options(i).sample() //TODO: should probably ensure array bounds
  }

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = {
    var n: Int = 0
    for (e <- options) {
      e.toChoiceArr(g) match {
        case Some(arr) => n += arr.length
        case None => {}
        //case Some(e) => n = n + (e.asInstanceOf[Array[Range]]).toChoiceArr(g).length // counting ranges
      }
    }
    Some(Array(0 to n - 1))
  }

  def getOptions(): List[Production] = options
}

// A terminal production
class Terminal(word: String) extends Production {
  override def sample(): String = {
    word
  }
  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = 1

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = {
    Some(Array(0 to 0))
    //toRet = toRet + (0 to 0)
  }
}

// A sequence, aka a combination of other terminals/choices and the ordering structure of each problem
class Sequence(sentence: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    sentence(ran.nextInt(sentence.length)).sample()
  }
  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = {
    var c: Int = 1 // TODO: is this ok?
    for (e <- sentence){
      c = c*e.count(g, counted)
    } // nonterminals are multiplicative
    c
  }

  // return specific terminal given by index i
  def sampleSpec(i: Int): String = {
    sentence(i).sample()
  }
  def getList(): List[Production] = sentence

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = {
    var choiceArr: Array[Range] = new Array[Range](sentence.length)
    for (e <- sentence) {
      e.toChoiceArr(g) match {
        case Some(arr) => {
          val newArr: Array[Range] = choiceArr ++ arr
          choiceArr = newArr
        }
        case None => {}
      }
      //choiceArr ++ newArr
    }
    Some(choiceArr)
  }
}

// A name associated with a edu.umass.cs.automan.core.grammar.Production
class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  def count(g: Grammar, counted: mutable.HashSet[String]): Int = {
    if(!counted.contains(n)){
      counted += n
      g.rules(this.sample()).count(g, counted) // TODO: get rid of this? null issues?
    } else 1
  }

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = {
    g.rules(this.sample()).toChoiceArr(g)
  }
}

// param is name of the edu.umass.cs.automan.core.grammar.Choices that this function applies to
// fun maps those choices to the function results
class Function(fun: Map[String,String], param: String, capitalize: Boolean) extends Production {
  override def sample(): String = param
  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = 1
  def runFun(s: String): String = { // "call" the function on the string
    if(capitalize) fun(s).capitalize
    else fun(s)
  }

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = Some(Array(0 to 0)) //None //Option[Array[Range]()] //Array(null) //TODO: right?
}

/**
  * QUESTION PRODUCTIONS
  */

abstract class QuestionProduction() extends Production { // TODO make prods take grammars?
  override def sample(): String

  override def count(g: Grammar, counted: mutable.HashSet[String]): Int

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = Some(Array(0 to 0))

  // returns tuple (body text, options list)
  def toQuestionText(g: Grammar, variation: Int): (String, List[String])
}

class EstimateQuestionProduction() extends QuestionProduction() {
  override def sample(): String = ""

  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = ???

  // todo grammar necessary?
  override def toQuestionText(g: Grammar, variation: Int): (String, List[String]) = {
    val body: String = Ranking.buildInstance(g, variation)
    (body, List[String]()) // no options for estimation
  }
}
