package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType

import scala.collection.mutable
import scala.util.Random

trait Production {
  def sample(): String
  def count(grammar: Grammar, counted: Set[String], name: String): Int
  def toChoiceArr(grammar: Grammar): Option[Array[Range]]
  def isLeafProd(): Boolean // Certain prods can terminate
  def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean // Checking if prod and everything it maps to are LNTs
}

trait TextProduction extends Production{}

/**
  * A Choice is a Production that contains multiple options that it could evaluate to. Which one it evaluates to is
  * determined by an assignment.
  * @param options The options the Choice can choose between
  */
class Choices(options: List[Production]) extends TextProduction {
  override def sample(): String = {
    val samp: StringBuilder = new StringBuilder()
    for (o <- options) {
      samp.addString(new StringBuilder(o.sample()))
    }
    samp.toString()
  }

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = {
    var c: Int = 0
    var mapsToTerm = false
    var mapsToSelf = false
    // todo counter in case of multiple recursive cases?

    for (e <- options) { // if an option contains the name that brought us here, it should be k
      if (e.isLeafNT(counted, name, grammar)) { // if LNT, safe to count
        mapsToTerm = true
        c += e.count(grammar, counted, name)
      } else if (checkMapsToSelf(e, counted, name, grammar)) mapsToSelf = true
    }

    if(mapsToSelf && !mapsToTerm) c = 1 // if infinite, count is 1
    else if(mapsToSelf && mapsToTerm) c += grammar.maxDepth // if recursive but finite, add k
    c
  }

  // return specific terminal given by index i
  def sampleSpec(i: Int): String = {
    assert(i < options.length)
    assert(i >= 0)
    options(i).sample()
  }

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = {
    var n: Int = 0
    for (e <- options) {
      e.toChoiceArr(grammar) match {
        case Some(arr) => n += arr.length
        case None => {}
        //case Some(e) => n = n + (e.asInstanceOf[Array[Range]]).toChoiceArr(g).length // counting ranges
      }
    }
    Some(Array(0 to n - 1))
  }

  def getOptions(): List[Production] = options

  override def isLeafProd(): Boolean = false

  // A leafNonTerminal must map to all LNTs
  // name is name that maps to this Choices
  def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = {
    var allNT = true

    for (e <- options) {
      if (!e.isLeafNT(counted, name, grammar)) { // if e is LNT, great
        allNT = false
      } // checking if counted contains should be irrelevant bc checking if names are contained in Name.isLeafNT
    }
    allNT
  }

  // returns whether the specified Production maps to itself and a terminal or LNT
  def mapsToSelfAndTerm(p: Production, counted: Set[String], name: String, grammar: Grammar): Boolean = {
    var mapsToSelf = false
    var mapsToTerm = false

    //for(e <- options) {
      if(p.isInstanceOf[Sequence]) { // if there's a sequence option, have to look at things inside
        for(l <- p.asInstanceOf[Sequence].getList()) {
          if(l.sample() == name) mapsToSelf = true
          if(l.isLeafNT(counted, name, grammar)) mapsToTerm = true // names only LNTs if counted...  TODO get grammar somehow?
        }
      } else {
        if(p.sample() == name) mapsToSelf = true
        if(p.isLeafNT(counted, name, grammar)) mapsToTerm = true
      }
    //}
    mapsToSelf && mapsToTerm
  }

  def checkMapsToSelf(p: Production, counted: Set[String], name: String, grammar: Grammar): Boolean = {
    if(p.isInstanceOf[Sequence]) { // if there's a sequence option, have to look at things inside
      for(l <- p.asInstanceOf[Sequence].getList()) {
        if(l.sample() == name) return true
      }
    } else {
      if(p.sample() == name) return true
    }
    false
  }

  // Returns true if the specified Production maps to itself and no terminals/LNTs (and so is infinite)
  def isInfinite(p: Production, counted: Set[String], name: String, grammar: Grammar): Boolean = {
    var mapsToTerm = false
    var mapsToSelf = false

    //for(e <- options) {
      if(p.isInstanceOf[Sequence]) { // if there's a sequence option, have to look at things inside
        for(l <- p.asInstanceOf[Sequence].getList()) {
          if(l.sample() == name) mapsToSelf = true
        }
      } else {
        if(p.sample() == name) mapsToSelf = true
        if(p.isLeafNT(counted, name, grammar)) mapsToTerm = true
      }
    //}
    mapsToSelf && !mapsToTerm
  }
}

/**
  * A Terminal is a Production that contains a String.
  * @param word The String this Terminal is associated with
  */
class Terminal(word: String) extends TextProduction {
  override def sample(): String = {
    word
  }
  override def count(grammar: Grammar, counted: Set[String], name: String): Int = 1

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = {
    Some(Array(0 to 0))
    //toRet = toRet + (0 to 0)
  }

  override def isLeafProd(): Boolean = true

  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = true
}


/**
  * A Sequence is a nonterminal, i.e. a combination of terminals and other Productions. It links Productions together.
  * in a sensible structure.
  *
  * @param sentence A List of Productions
  */
class Sequence(sentence: List[Production]) extends TextProduction {
  override def sample(): String = {
    val samp: StringBuilder = new StringBuilder()
    for (o <- sentence) {
      samp.addString(new StringBuilder(o.sample()))
    }
    samp.toString()
  }

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = {
    var c: Int = 1 // TODO: is this ok?
    for (e <- sentence){
      if(!counted.contains(e.sample())) c = c*e.count(grammar, counted, name) // counting b here
      //else c = c*g.maxDepth
    } // nonterminals are multiplicative
    c
  }

  // return specific terminal given by index i
  def sampleSpec(i: Int): String = {
    sentence(i).sample()
  }
  def getList(): List[Production] = sentence

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = {
    var choiceArr: Array[Range] = new Array[Range](sentence.length)
    for (e <- sentence) {
      e.toChoiceArr(grammar) match {
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

  override def isLeafProd(): Boolean = false

  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = {
    var allNT = true

    for (e <- sentence) {
      if(!counted.contains(e.sample()) || e.sample() == name) allNT = false //todo contains or ==?
//      if (!e.isLeafNT(counted, name, grammar)) { // if e is LNT, great
//        if(!counted.contains(name) || e.sample() == name) allNT = false
//      }
    }
    allNT
  }
}

/**
  * A Name is a Production. It is used to map to another Production so that we can look them up.
  * @param n The text associated with the Name
  */
class Name(n: String) extends TextProduction {
  override def sample(): String = n // sample returns name for further lookup
  def count(grammar: Grammar, counted: Set[String], name: String): Int = {
    var c: Set[String] = counted

    if(!counted.contains(n)){
      c = c + n
      grammar.rules(this.sample()).count(grammar, c, name)
    } else grammar.maxDepth // todo or 1?
  }

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = {
    grammar.rules(this.sample()).toChoiceArr(grammar)
  }

  override def isLeafProd(): Boolean = false

  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = {
    val linkedProd = grammar.rules.get(name)
    linkedProd match {
      case Some(l) => {
        if (l.isLeafNT(counted, name, grammar)) return true
        else return false
      }
      case None => throw new Error("Name not found")
    }
    false
  }
}

/**
  * Functions are Productions with a value that varies based on the value of an associated Name.
  * A sample Function could map names to pronouns, or nouns to articles.
  * @param fun The mapping of Names to values
  * @param param The Name the Function is associated with
  * @param capitalize Whether or not to capitalize the value
  */
class Function(fun: Map[String,String], param: String, capitalize: Boolean) extends TextProduction {
  override def sample(): String = param
  override def count(grammar: Grammar, counted: Set[String], name: String): Int = 1

  /// "Call" the function on the string
  def runFun(s: String): String = {
    if(capitalize) fun(s).capitalize
    else fun(s)
  }

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = Some(Array(0 to 0))
  override def isLeafProd(): Boolean = true
  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = {
    if(counted.contains(param)) true
    else false
  }
}

/**
  * A Variable is a Production similar to a Choice, but permanently bound to the same Production in the recursive case.
  */
class Variable(name: Name, options: List[Production]) extends TextProduction {
  override def sample(): String = ???

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = ???

  override def isLeafProd(): Boolean = ???

  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = ???
}

/**
  * QUESTION PRODUCTIONS
  */

abstract class QuestionProduction(g: Grammar) extends Production { // TODO make prods take grammars?
  var _questionType: QuestionType

  override def sample(): String

  override def count(grammar: Grammar, counted: Set[String], name: String): Int

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = Some(Array(0 to 0))

  // returns tuple (body text, options list)
  def toQuestionText(variation: Int): (String, List[String])

  def questionType: QuestionType = _questionType

  def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = false
}

class OptionProduction(text: TextProduction) extends Production {
  override def sample(): String = text.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = text.count(grammar, counted, name)

  def getText() = text

  override def toChoiceArr(grammar: Grammar): Option[Array[Range]] = text.toChoiceArr(grammar)

  override def isLeafProd(): Boolean = false

  override def isLeafNT(counted: Set[String], name: String, grammar: Grammar): Boolean = false
}

class EstimateQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.EstimationQuestion

  override def sample(): String = body.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  // todo grammar necessary?
  override def toQuestionText(variation: Int): (String, List[String]) = {
    //val body: String = Ranking.buildInstance(g, variation) // todo where does body come in?
    //(body, List[String]()) // no options for estimation
    //val bod: (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()} // todo just ""?
    (bodS, optsS)
    //bod.toString()
  }

  override def isLeafProd(): Boolean = false

  //override def isLeafNT(counted: Set[String]): Boolean = false
}

// todo we can actually get rid of the body param now...
class CheckboxQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.CheckboxQuestion

  override def sample(): String = {
    body.sample()
  }

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
//    val body: String = Ranking.buildInstance(g, variation) // todo where does body come in?
//    val options: List[String] =
//    (body, List[String]()) // no options for estimation
    //Ranking.buildInstance(g, variation)
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()}
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}

class CheckboxesQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.CheckboxDistributionQuestion

  override def sample(): String = {
    body.sample()
  }

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()}
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}

class FreetextQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.FreeTextQuestion

  override def sample(): String = body.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()} //todo get rid of opts
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}

class FreetextsQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.FreeTextQuestion

  override def sample(): String = body.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()}
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}

class RadioQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.RadioButtonQuestion

  override def sample(): String = body.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()}
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}

class RadiosQuestionProduction(g: Grammar, body: TextProduction) extends QuestionProduction(g) {
  override var _questionType: QuestionType = QuestionType.RadioButtonDistributionQuestion

  override def sample(): String = body.sample()

  override def count(grammar: Grammar, counted: Set[String], name: String): Int = ???

  override def toQuestionText(variation: Int): (String, List[String]) = {
    val (bod, opts): (StringBuilder, List[StringBuilder]) = Ranking.buildInstance(g, variation)
    val bodS = bod.toString()
    val optsS = opts.map(_.toString())//{for(e <- opts) e.toString()}
    (bodS, optsS)
  }

  override def isLeafProd(): Boolean = false
}