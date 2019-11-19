import scala.collection.immutable.HashMap
import scala.util.Random

trait Production {
  def sample(): String
  def count(g: Map[String,Production]): Int
}

// A set of choices, options for a value
class Choices(options: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    options(ran.nextInt(options.length)).sample()
  }
  override def count(g: Map[String,Production]): Int = {
    var c: Int = 0
    for (e <- options) {
      c = c + e.count(g)
    } // choices are additive
    c
  }
}

// A terminal production
class Terminal(word: String) extends Production {
  override def sample(): String = {
    word
  }
  override def count(g: Map[String,Production]): Int = 1
}

// A nonterminal, aka a combination of other terminals
class NonTerminal(sentence: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    sentence(ran.nextInt(sentence.length)).sample()
  }
  override def count(g: Map[String,Production]): Int = {
    var c: Int = 1 // TODO: is this ok?
    for (e <- sentence){
      c = c*e.count(g)
    } // nonterminals are multiplicative
    c
  }
  def getList(): List[Production] = sentence
}

// A name associated with a Production
class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  def count(g: Map[String,Production]): Int = {
    g(this.sample()).count(g) // TODO: will null cause issues?
  }
}

// param is name of the Choices that this function applies to
// fun maps those choices to the function results
class Function(fun: Map[String,String], param: String) extends Production {
  override def sample(): String = ???
  override def count(g: Map[String, Production]): Int = 1
  def runFun(s: String): String = { // "call" the function on the string
    fun(s)
  }
  def getParam: String = {
    param
  }
}

object SampleGrammar {

  // Sample a string from the grammar
  def sample(g: Map[String,Production], startSymbol: String, scope: Scope): Unit = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    val samp: Option[Production] = g get startSymbol // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => sample(g, name.sample(), scope) // Name becomes start symbol
          case term: Terminal => {
            print(term.sample())
          }
          case choice: Choices => {
            if(scope.isBound(startSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              print(scope.lookup(startSymbol))
            } else {
              throw new Exception(s"Choice ${startSymbol} has not been bound")
            }
            //print(choice.sample(scope))
          }
          case nonterm: NonTerminal => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => sample(g, name.sample(), scope)
                case p: Production => {
                  if(scope.isBound(startSymbol)){
                    print(scope.lookup(startSymbol))
                  } else {
                    print(p.sample())
                  }
                }
              }
            }
          }
          case func: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
            if(scope.isBound(startSymbol)){
              print(func.runFun(scope.lookup(startSymbol)))
            } else {
              print("something's not right")
            }
          }
        }
      }
      case None => throw new Exception(s"Symbol ${startSymbol} could not be found")
    }
  }

  // bind variables
  def bind(grammar: Map[String, Production], startSymbol: String, scope: Scope): Unit ={
    val samp: Option[Production] = grammar get startSymbol // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => bind(grammar, name.sample(), scope) // Name becomes start symbol
          case choice: Choices => {
            if(!(scope.isBound(startSymbol))){
              val binding = choice.sample()
              scope.assign(startSymbol, binding)
              println(scope.toString())
            }
          }
          case p: Production => {}
//          case nonterm: NonTerminal => { // TODO: refactor
//            for(n <- nonterm.getList()) {
//              n match {
//                case name: Name => sample(grammar, name.sample(), scope)
//                }
//              }
//            }
          }
        }
      case None => throw new Exception(s"Symbol ${startSymbol} could not be found")
      }
    }

  // Count the number of options possible in a given grammar
  //TODO: keep track of which vars have been counted?
  def count(g: Map[String, Production], startSymbol: String, soFar: Int): Int = {
    val samp: Option[Production] = g get startSymbol // get Production associated with symbol from grammar
    var opts = 0
    samp match {
      case Some(samp) => {
        opts = soFar + samp.count(g)
      }
      case None => throw new Exception("Symbol could not be found")
    }
    opts
  }

  def main(args: Array[String]): Unit = {
    val G = new NonTerminal(
      List(
        new Name("A"),//, nameMap = nameMap + ("A" -> A)),
        new Terminal(" is "),
        new Name("B"),// nameMap = nameMap + ("B" -> B)),
        new Terminal(" years old.")
      )
    )

    val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    // TODO: what if multiple params need same function?
    val articles = Map[String,String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )

    // simple grammar
    val grammar = {
      Map(
        "Start" -> new Name("G"),
        "G" -> G,
        "A" -> new Choices(
          List(
            new Terminal("Linda"),
            new Terminal("Dan"),
            new Terminal("Emmie")
          )
      ),
        "B" -> new Choices(
          List(
            new Terminal("21"),
            new Terminal("31"),
            new Terminal("41"),
            new Terminal("51"),
            new Terminal("61")
          )
        )
      )
    }

    // new, complex grammar for the Linda Problem
    val lindaG = new NonTerminal(
      List(
        new Name("Name"),
        new Terminal(" is "),
        new Name("Age"),
        new Terminal(" years old, single, outspoken, and very bright. "),
        new Function(pronouns, "Name"),
        new Terminal(" majored in "),
        new Name("Major"),
        new Terminal(". As a student, "),
        new Function(pronouns, "Name"),
        new Terminal(" was deeply concerned with issues of "),
        new Name("Issue"),
        new Terminal(", and also participated in "),
        new Name("Demonstration"),
        new Terminal(" demonstrations.\nWhich is more probable?\n1. "),
        new Name("Name"),
        new Terminal(" is "),
        new Function(articles, "Job"),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(".\n2. "),
        new Name("Name"),
        new Terminal(" is "),
        new Function(articles, "Job"),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(" and is active in the "),
        new Name("Movement"),
        new Terminal(" movement.")
      )
    )
    val Linda = {
      Map(
        "Start" -> new Name("lindaG"),
        "lindaG" -> lindaG,
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
        )//,
//        "Article" -> new Function(
//
//        )
      )
    }
    //var scopeMap: Map[String,String] = Map
    val lindaScope = new Scope(Linda)

    //sample(grammar, "Start")
    //println()
    bind(Linda, "Start", lindaScope)
    //sample(Linda, "Start", lindaScope)
    //functions(Linda, lindaScope)

    println()
    println("Count: " + G.count(grammar))
    println("Linda count: " + lindaG.count(Linda)) //TODO: make sure this is how we want to call count
  }
}

//val linda = new Experiment(
//  'name +  " is " + 'age + "years old, single, outspoken, and very bright. " + 'name_gender + " majored in " + 'major  +
//    ". As a student,  " + 'name_gender + " was deeply concerned with issues of " + 'issues + ", and also participated in " + 'demonstrations + "demonstrations.\n\nWhich is more probable?",
//  List('name + " is a " + 'job + ".", 'name + " is a " + 'job + " and is active in the " +  'issues + " movement."),
//  Map(
//    'name -> nameList,
//    //'name_gender -> genderMap(nameList),//genderMap(Symbol.valueFromKey("name")),
//    'age -> List("21", "31", "41", "51", "61"),
//    'major -> List("chemistry", "psychology", "english literature", "philosophy", "women's studies"),
//    'issues -> List("discrimination and social justice", "fair wages", "animal rights", "white collar crime", "unemployed circus workers"),
//    'demonstrations -> List("anti-nuclear", "anti-war", "pro-choice", "anti-abortion", "anti-animal testing"),
//    'jobs -> List("bank teller", "almond paste mixer", "tennis scout", "lawyer", "professor")
//  ),
