package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.SampleGrammar.buildString

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}

/**
  * A class representing a grammar
  *
  * @param _rules a map from Strings to Productions
  * @param _startSymbol the start symbol for the eventual iterations over the experiment space. Doesn't change.
  */

case class Grammar(_rules: Map[String, Production], _startSymbol: String){

  private var _curSym = _startSymbol // the current symbol we're working with, which may change
  private var _bases: Array[Int] = Array[Int]() // TODO automatically generate from rules?

  def rules = _rules
  //def rules_=(newRules: Map[String, edu.umass.cs.automan.core.grammar.Production]) = rules = newRules

  def startSymbol = _startSymbol

  def bases = _bases
  def bases_= (newBases: Array[Int]): Unit = _bases = newBases

  def curSymbol = _curSym
  def curSymbol_= (newSym: String): Unit = _curSym = newSym

  // Count the number of options possible in this grammar
  def count(soFar: Int, counted: mutable.HashSet[String]): Int = {
    val samp: Option[Production] = _rules.get(curSymbol) // get Production associated with symbol from grammar
    var opts = 0 // TODO will this only work if it starts with an addition?
    samp match {
      case Some(samp) => {
        opts = soFar + samp.count(this, counted)
      }
      case None => throw new Exception("Symbol could not be found")
    }
    opts
  }

  // bind variables to vals
  def bind(assignment: Array[Int], assignmentPos: Int, alreadyBound: Set[String]): Scope = {
    var curPos = assignmentPos
    var assigned = alreadyBound

    val samp: Option[Production] = _rules.get(_curSym) // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => {
            curSymbol = name.sample()
            bind(assignment, curPos, alreadyBound)
          }
          case choice: Choices => { // bind choicename to specified choice
            if(!alreadyBound.contains(_curSym)) {
              val choice = _rules.get(_curSym) // redundant?
              choice match { // will a name ever go to anything but a choice?
                case Some(prod) => {
                  prod match {
                    case choice: Choices => {
                      val newScope: Scope = new Scope(this, curPos)
                      newScope.assign(_curSym, choice.getOptions()(assignment(curPos)).sample())
                      newScope
                    }
                  }
                }
                case None => {
                  throw new Error("Name is invalid; there should be a choice here.")
                }
              }
            } else {
              val newScope = new Scope(this, curPos)
              newScope // return empty scope
            }
          }
          case opt: OptionProduction => {
            val newScope = new Scope(this, curPos)
            newScope
          }
          case nt: Sequence => { // The first case; combines all the bindings into one scope
            val newScope = new Scope(this, curPos)
            for(n <- nt.getList()) {
              n match {
                case name: Name => { // combine
                  curSymbol = name.sample() // TODO will this cause problems?
                  val toCombine = bind(assignment, curPos, assigned)
                  if(toCombine.getBindings().size == 1) { // indicates that we bound something
                    assigned = assigned + name.sample()
                    curPos += 1
                    newScope.combineScope(toCombine)
                    newScope.setPos(curPos + 1)
                  }
                }
                case p: Production => {}
              }
            }
            newScope
          }
        }
      }
      case None => throw new Exception(s"Symbol ${_curSym} could not be found")
    }
  }

  // build a string given a scope
  def buildString(scope: Scope, soFar: StringBuilder): StringBuilder = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    var generating: StringBuilder = soFar

    val samp: Option[Production] = _rules.get(curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
        samp match {
          case name: Name => {
            curSymbol = name.sample()
            buildString(scope, generating)
          }//render(g, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol
          case term: Terminal => generating.append(term.sample())
          //case break: OptionBreak => generating.append(break.sample())
          case choice: Choices => {
            if(scope.isBound(curSymbol)){
              //println(s"${startSymbol} is bound, looking up")
              //print(scope.lookup(startSymbol))
              generating.append(scope.lookup(curSymbol))
            } else {
              throw new Exception(s"Choice ${curSymbol} has not been bound")
            }
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  curSymbol = name.sample()
                  buildString(scope, generating)
                }
                case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
                case term: Terminal => generating.append(term.sample())
                //case break: OptionBreak => generating.append(break.sample())
                case p: Production => {
                  if(scope.isBound(curSymbol)){
                    generating.append(scope.lookup(curSymbol))
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
      case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
    }
  }

  // need to build the question body and options separately
  def buildQandOpts(scope: Scope, soFarBod: StringBuilder, soFarOpts: ListBuffer[StringBuilder], cur: StringBuilder, inBody: Boolean): (StringBuilder, ListBuffer[StringBuilder]) = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    var generatingBod: StringBuilder = soFarBod
    var generatingOpts: ListBuffer[StringBuilder] = new ListBuffer[StringBuilder]()
    generatingOpts ++= soFarOpts
    val curGen: StringBuilder = cur // what's currently being generated
    var inBod: Boolean = inBody // are we in the question body?

    val samp: Option[Production] = rules.get(curSymbol) // Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => {
            curSymbol = name.sample()
            buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
          }
          case term: Terminal => {
            curGen.append(term.sample())
            (generatingBod, generatingOpts)
          }
          case choice: Choices => {
            if(scope.isBound(curSymbol)){
              curGen.append(scope.lookup(curSymbol))
              (generatingBod, generatingOpts) // todo problem?
            } else {
              throw new Exception(s"Choice ${curSymbol} has not been bound")
            }
          }
          case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
            if(scope.isBound(fun.sample())){
              curGen.append(fun.runFun(scope.lookup(fun.sample())))
              (generatingBod, generatingOpts)
            } else {
              throw new Exception("Variable not bound")
            }
          }
          case opt: OptionProduction => {
            curGen.clear()
            opt.getText() match { // generate opt text
              case name: Name => {
                curSymbol = name.sample()
                curGen.append(buildString(scope, curGen))
              }
              case fun: Function => curGen.append(fun.runFun(scope.lookup(fun.sample())))
              case term: Terminal => curGen.append(term.sample())
                // todo sequence
              case p: Production => { // covers functions
                if(scope.isBound(curSymbol)){
                  curGen.append(scope.lookup(curSymbol))
                } else {
                  curGen.append(p.sample())
                }
              }
            }
            generatingOpts.append(new StringBuilder(curGen.toString()))
            curGen.clear()
            (generatingBod, generatingOpts)
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  curSymbol = name.sample()
                  rules.get(curSymbol) match {
                    case Some(symb) => {
                      symb match {
                        case opt: OptionProduction => {
                          if (inBod) {
                            inBod = false
                            generatingBod = new StringBuilder(curGen.toString())
                            val(bod, opts) = buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
                            generatingBod = bod
                            generatingOpts = opts
                          } else {
                            val(bod, opts) = buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
                            generatingBod = bod // does nothing first time around
                            generatingOpts = opts
                          }
                        }
                        case p: Production => {
                          val(bod, opts) = buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
                          generatingBod = bod // does nothing first time around
                          generatingOpts = opts
                        }
                      }
                    }
                    case None => { throw new Error(s"${curSymbol} could not be found.")}
                  }
                }
                case fun: Function => curGen.append(fun.runFun(scope.lookup(fun.sample())))
                case term: Terminal => curGen.append(term.sample())
                case p: Production => {
                  if(scope.isBound(curSymbol)){
                    curGen.append(scope.lookup(curSymbol))
                  } else {
                    curGen.append(p.sample())
                  }
                }
              }
            }
            if(inBod) generatingBod.append(curGen)
            //else generatingOpts += new StringBuilder(curGen.toString())
            (generatingBod, generatingOpts)
          }
        }
      }
      case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
    }
  }
}


