package edu.umass.cs.automan.core.grammar

import scala.collection.mutable
import scala.util.Random
import scala.util.control.Breaks._

object SampleGrammar {

  // Count the number of options possible in a given grammar
  def count(grammar: Grammar, soFar: Int, counted: mutable.HashSet[String]): Int = {
    val samp: Option[Production] = grammar.rules.get(grammar.curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
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

    val samp: Option[Production] = grammar.rules.get(grammar.curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => {
            grammar.curSymbol = name.sample()
            bind(grammar, assignment, curPos, alreadyBound)
          }//bind(grammar, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol // assigned or AlreadyBound?
          case choice: Choices => { // bind choicename to specified choice
            if(!alreadyBound.contains(grammar.curSymbol)) {
              val choice = grammar.rules.get(grammar.curSymbol) // redundant?
              choice match { // will a name ever go to anything but a choice?
                case Some(prod) => {
                  prod match {
                    case choice: Choices => {
                      val newScope: Scope = new Scope(grammar, curPos)
                      newScope.assign(grammar.curSymbol, choice.getOptions()(assignment(curPos)).sample())
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
                  grammar.curSymbol = name.sample() // TODO will this cause problems?
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
          //case p: edu.umass.cs.automan.core.grammar.Production => {}
        }
      }
      case None => throw new Exception(s"Symbol ${grammar.curSymbol} could not be found")
    }
  }

  def render(g: Grammar, scope: Scope): Unit = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    val samp: Option[Production] = g.rules.get(g.curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
        samp match {
          case name: Name => {
            g.curSymbol = name.sample()
            render(g, scope)
          } // edu.umass.cs.automan.core.grammar.Name becomes start symbol
          case term: Terminal => print(term.sample())
          //case break: OptionBreak => print(break.sample())
          case choice: Choices => {
            if(scope.isBound(g.curSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              print(scope.lookup(g.curSymbol))
            } else {
              throw new Exception(s"Choice ${g.curSymbol} has not been bound")
            }
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  g.curSymbol = name.sample()
                  render(g, scope)
                }
                case fun: Function => print(fun.runFun(scope.lookup(fun.sample())))
                case term: Terminal => print(term.sample())
                //case break: OptionBreak => print(break.sample())
                case p: Production => {
                  if(scope.isBound(g.curSymbol)){
                    print(scope.lookup(g.curSymbol))
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
      case None => throw new Exception(s"Symbol ${g.curSymbol} could not be found")
    }
  }

  def buildString(g: Grammar, scope: Scope, soFar: StringBuilder): StringBuilder = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    val generating: StringBuilder = soFar

    val samp: Option[Production] = g.rules.get(g.curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
        samp match {
          case name: Name => {
            g.curSymbol = name.sample()
            buildString(g,  scope, generating)
          }//render(g, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol
          case term: Terminal => generating.append(term.sample())
          //case break: OptionBreak => generating.append(break.sample())
          case choice: Choices => {
            if(scope.isBound(g.curSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              //print(scope.lookup(startSymbol))
              generating.append(scope.lookup(g.curSymbol))
            } else {
              throw new Exception(s"Choice ${g.curSymbol} has not been bound")
            }
          }
          case opt: OptionProduction => {
            generating.append(opt.sample())
          }
          case nonterm: Sequence => { // TODO sequence in sequence?
            //breakable {
              for (n <- nonterm.getList()) {
                n match {
                  case name: Name => {
                    g.curSymbol = name.sample()
                    buildString(g, scope, generating)
                  }
                  case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
                  case term: Terminal => generating.append(term.sample())
                  case opt: OptionProduction => generating.append(opt.sample())
                  //case optBreak: OptionBreak => generating.append(optBreak.sample())
                  case p: Production => {
                    if (scope.isBound(g.curSymbol)) {
                      generating.append(scope.lookup(g.curSymbol))
                    } else {
                      generating.append(p.sample())
                    }
                  }
                }
              }
            //}
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
      case None => throw new Exception(s"Symbol ${g.curSymbol} could not be found")
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
        //new OptionBreak(),
        new Name("Name"),
        new Terminal(" is "),
        new Function(articles, "Job", false),
        new Terminal(" "),
        new Name("Job"),
        new Terminal(".\n2. "),
        //new OptionBreak(),
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
    //val lindaScope = new edu.umass.cs.automan.core.grammar.Scope(Linda, 0)

    //sample(grammar, "Start")
    //println()
    //val scope = bind(Linda, "Start", Array(1,2,1,0,0,1,0,1,0,0), 0)
    println("Xavier:")
    val sSet: Set[String] = Set()
    val scope = bind(Linda, Array(3,3,4,4,2,2,3), 0, sSet)
    for(e <- scope.getBindings()) println(e)
    //Linda.resetStartSym
    Linda.curSymbol = Linda.startSymbol
    render(Linda, scope)
    println()

    println("OG Linda:")
    //Linda.resetStartSym
    Linda.curSymbol = Linda.startSymbol
    val scope2 = bind(Linda, Array(0,1,1,0,0,0,0), 0, sSet)
    for(e <- scope2.getBindings()) println(e)
    //Linda.resetStartSym
    Linda.curSymbol = Linda.startSymbol
    render(Linda, scope2)

    println("\nbuildString version: ")
    //Linda.resetStartSym
    Linda.curSymbol = Linda.startSymbol
    println(buildString(Linda, scope2, new StringBuilder))

    //Linda.resetStartSym
    Linda.curSymbol = Linda.startSymbol
    println()
    println("Linda count: "  + count(Linda, 0, new mutable.HashSet[String]()))
  }
}
