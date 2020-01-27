import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

trait Production {
  def sample(): String
  def count(g: Grammar, counted: mutable.HashSet[String]): Int
  def isLeafNT(): Boolean
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
    options(i).sample() //TODO: should probably ensure array bounds
  }

  // A leafNonTerminal must have at least one terminal, and other elements must also be LNTs
  override def isLeafNT(): Boolean = {
    var containsTerm = false
    var allNT = true

    for(e <- options) {
      if (e.isInstanceOf[Terminal]) containsTerm = true
      else if(!e.isLeafNT()) allNT = false
    }
    containsTerm && allNT
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

  override def isLeafNT(): Boolean = true

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

  override def isLeafNT(): Boolean = false // should be irrelevant

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

// A name associated with a Production
class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  def count(g: Grammar, counted: mutable.HashSet[String]): Int = {
    if(!counted.contains(n)){
      counted += n
      g.rules(this.sample()).count(g, counted) // TODO: get rid of this? null issues?
    } else 1
  }

  override def isLeafNT(): Boolean = false

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = {
    g.rules(this.sample()).toChoiceArr(g)
  }
}

// param is name of the Choices that this function applies to
// fun maps those choices to the function results
class Function(fun: Map[String,String], param: String, capitalize: Boolean) extends Production {
  override def sample(): String = param
  override def count(g: Grammar, counted: mutable.HashSet[String]): Int = 1
  def runFun(s: String): String = { // "call" the function on the string
    if(capitalize) fun(s).capitalize
    else fun(s)
  }
  override def isLeafNT(): Boolean = false

  override def toChoiceArr(g: Grammar): Option[Array[Range]] = Some(Array(0 to 0)) //None //Option[Array[Range]()] //Array(null) //TODO: right?
}

object SampleGrammar {

  // Count the number of options possible in a given grammar
  def count(grammar: Grammar, soFar: Int, counted: mutable.HashSet[String]): Int = {
    val samp: Option[Production] = grammar.rules.get(grammar.startSymbol) // get Production associated with symbol from grammar
    var opts = 0
    samp match {
      case Some(samp) => {
        opts = soFar + samp.count(grammar, counted)
      }
      case None => throw new Exception("Symbol could not be found")
    }
    opts
  }

  def bind(grammar: Grammar, assignment: Array[Int], assignmentPos: Int, alreadyBound: Set[String]): Scope = {
    var curPos = assignmentPos
    var assigned = alreadyBound

    val samp: Option[Production] = grammar.rules.get(grammar.startSymbol) // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => {
            grammar.startSymbol = name.sample()
            bind(grammar, assignment, curPos, alreadyBound)
          }//bind(grammar, name.sample(), scope) // Name becomes start symbol // assigned or AlreadyBound?
          case choice: Choices => { // bind choicename to specified choice
            if(!alreadyBound.contains(grammar.startSymbol)) {
              val choice = grammar.rules.get(grammar.startSymbol) // redundant?
              choice match { // will a name ever go to anything but a choice?
                case Some(prod) => {
                  prod match {
                    case choice: Choices => {
                      val newScope: Scope = new Scope(grammar, curPos)
                      newScope.assign(grammar.startSymbol, choice.getOptions()(assignment(curPos)).sample())
                      //curPos += 1
                      //                    curPos += 1
                      //                    newScope.setPos(curPos)
                      newScope
                    }
                    //                    instance += choice.getOptions()(params(choiceIndex)).sample() //TODO: make sure array isn't out of bounds
                    //                    choiceIndex += 1
                  }
                }
                case None => {
                  throw new Error("Name is invalid; there should be a choice here.")
                }
              }
            } else {
              val newScope = new Scope(grammar, curPos)
              newScope // return empty scope
            }
          }
          case nt: Sequence => { // The first case; combines all the bindings into one scope
            val newScope = new Scope(grammar, curPos)
            for(n <- nt.getList()) {
              n match {
                case name: Name => { // combine
                  grammar.startSymbol = name.sample() // TODO will this cause problems?
                  val toCombine = bind(grammar, assignment, curPos, assigned)
                  if(toCombine.getBindings().size == 1) { // indicates that we bound something
                    assigned = assigned + name.sample()
                    curPos += 1
                    newScope.combineScope(toCombine)
                    newScope.setPos(curPos + 1)
                  }

                  //curPos += 1
                }
                case p: Production => {}
              }
            }
            newScope
          }
          //case p: Production => {}
        }
      }
      case None => throw new Exception(s"Symbol ${grammar.startSymbol} could not be found")
    }
  }

  def render(g: Grammar, scope: Scope): Unit = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    val samp: Option[Production] = g.rules.get(g.startSymbol) // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
        samp match {
          case name: Name => {
            g.startSymbol = name.sample()
            render(g, scope)
          } // Name becomes start symbol
          case term: Terminal => {
            print(term.sample())
          }
          case choice: Choices => {
            if(scope.isBound(g.startSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              print(scope.lookup(g.startSymbol))
            } else {
              throw new Exception(s"Choice ${g.startSymbol} has not been bound")
            }
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  g.startSymbol = name.sample()
                  render(g, scope)
                }
                case fun: Function => print(fun.runFun(scope.lookup(fun.sample())))
                case term: Terminal => {
                  print(term.sample())
                }
                case p: Production => {
                  if(scope.isBound(g.startSymbol)){
                    print(scope.lookup(g.startSymbol))
                  } else {
                    print(p.sample())
                  }
                }
              }
            }
          }
          case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
            if(scope.isBound(fun.sample())){ // TODO: is this right?
              print(fun.runFun(scope.lookup(fun.sample())))
            } else {
              print("Variable not bound")
            }
          }
        }
      }
      case None => throw new Exception(s"Symbol ${g.startSymbol} could not be found")
    }
  }

  def buildString(g: Grammar, scope: Scope, soFar: StringBuilder): StringBuilder = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    var generating: StringBuilder = soFar

    val samp: Option[Production] = g.rules.get(g.startSymbol) // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
        samp match {
          case name: Name => {
            g.startSymbol = name.sample()
            buildString(g,  scope, generating)
          }//render(g, name.sample(), scope) // Name becomes start symbol
          case term: Terminal => {
            generating.append(term.sample())
            //print(term.sample())
          }
          case choice: Choices => {
            if(scope.isBound(g.startSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              //print(scope.lookup(startSymbol))
              generating.append(scope.lookup(g.startSymbol))
            } else {
              throw new Exception(s"Choice ${g.startSymbol} has not been bound")
            }
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  g.startSymbol = name.sample()
                  buildString(g, scope, generating)
                }
                case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
                case term: Terminal => generating.append(term.sample())
                case p: Production => {
                  if(scope.isBound(g.startSymbol)){
                    generating.append(scope.lookup(g.startSymbol))
                  } else {
                    generating.append(p.sample())
                  }
                }
              }
            }
            generating
          }
          case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
            if(scope.isBound(fun.sample())){ // TODO: is this right?
              generating.append(fun.runFun(scope.lookup(fun.sample())))
            } else {
              throw new Exception("Variable not bound")
            }
          }
        }
      }
      case None => throw new Exception(s"Symbol ${g.startSymbol} could not be found")
    }
  }

  def main(args: Array[String]): Unit = {

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

    // The problem statement
    val lindaS = new Sequence(
      List(
        new Name("Name"),
        new Terminal(" is "),
        new Name("Age"),
        new Terminal(" years old, single, outspoken, and very bright. "),
        new Function(pronouns, "Name", true),
        new Terminal(" majored in "),
        new Name("Major"),
        new Terminal(". As a student, "),
        new Function(pronouns, "Name", false),
        new Terminal(" was deeply concerned with issues of "),
        new Name("Issue"),
        new Terminal(", and also participated in "),
        new Name("Demonstration"),
        new Terminal(" demonstrations.\nWhich is more probable?\n1. "),
        new Name("Name"),
        new Terminal(" is "),
        new Function(articles, "Job", false),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(".\n2. "),
        new Name("Name"),
        new Terminal(" is "),
        new Function(articles, "Job", false),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(" and is active in the "),
        new Name("Movement"),
        new Terminal(" movement.")
      )
    )
    val Linda = Grammar(// the grammar
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
      ),
      "Start"
    )
    //val lindaScope = new Scope(Linda, 0)

    //sample(grammar, "Start")
    //println()
    //val scope = bind(Linda, "Start", Array(1,2,1,0,0,1,0,1,0,0), 0)
    println("Xavier:")
    val sSet: Set[String] = Set()
    val scope = bind(Linda, Array(3,3,4,4,2,2,3), 0, sSet)
    for(e <- scope.getBindings()) println(e)
    Linda.resetStartSym
    render(Linda, scope)
    println()

    println("OG Linda:")
    Linda.resetStartSym
    val scope2 = bind(Linda, Array(0,1,1,0,0,0,0), 0, sSet)
    for(e <- scope2.getBindings()) println(e)
    Linda.resetStartSym
    render(Linda,  scope2)

    println("\nmakeString version: ")
    Linda.resetStartSym
    println(buildString(Linda, scope2, new StringBuilder))

    Linda.resetStartSym
    println()
    println("Linda count: "  + count(Linda, 0, new mutable.HashSet[String]()))

    //println(s"New count: ${newCount}")
//    val instance = getInstance(Linda, choiceArr, lindaScope,  Array(0,1,3,0,0,0,0,0,0,0))//,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0))
//    println("Classic instance: ")
//    for(s <- instance) print(s)
//
//    val newInstance = getInstance(Linda, choiceArr, lindaScope,  Array(3,0,5,4,2,3,1,3,1,3))
//    println("New instance: ")
//    for(s <- newInstance) print(s)
    //println("Instance: " + getInstance(Linda, choiceArr, lindaScope,  Array(0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)).toString)
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
