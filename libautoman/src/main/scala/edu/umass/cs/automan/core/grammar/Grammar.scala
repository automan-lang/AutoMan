package edu.umass.cs.automan.core.grammar

//import edu.umass.cs.automan.core.grammar.SampleGrammar.buildString

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}

/**
  * A class representing a grammar
  *
  * @param _rules a map from Strings to Productions
  * @param _startSymbol the start symbol for the eventual iterations over the experiment space. Doesn't change.
  */

case class Grammar(_rules: Map[String, Production], _startSymbol: String, _maxDepth: Int){

  private var _curSym = _startSymbol // the current symbol we're working with, which may change
  private var _bases: Array[Int] = Array[Int]() // TODO automatically generate from rules?
  private val _depthParam = _maxDepth

  def rules = _rules
  //def rules_=(newRules: Map[String, edu.umass.cs.automan.core.grammar.Production]) = rules = newRules

  def startSymbol = _startSymbol

  def maxDepth = _depthParam

  def bases = _bases
  def bases_= (newBases: Array[Int]): Unit = _bases = newBases

  def curSymbol = _curSym
  def curSymbol_= (newSym: String): Unit = _curSym = newSym

  // Count the number of options possible in this grammar
  def count(soFar: Int, counted: Set[String]): Int = {
    val samp: Option[Production] = _rules.get(curSymbol) // get Production associated with symbol from grammar
    var opts = 0 // TODO will this only work if it starts with an addition?
    samp match {
      case Some(samp) => {
        opts = soFar + samp.count(this, counted, curSymbol)
      }
      case None => throw new Exception("Symbol could not be found")
    }
    opts
  }

  /**
    * Binds Names to Choice selections based on assignment
    * @param assignment The variable assignments
    * @param assignmentPos Where are we in the assignment array?
    * @param bound The Names already bound to selections
    * @return A tuple: the Scope containing the bindings, the new assignment position, and what's been bound.
    */
  def bind(assignment: Array[Int], assignmentPos: Int, bound: Set[String]): (Scope, Int, Set[String]) = {
    //var curAssignment = assignment
    var curPos = assignmentPos
    var curBound = bound

    val samp: Option[Production] = rules.get(curSymbol)
    samp match {
      case Some(s) => {
        s match {
          // This should only trigger when we get the start symbol
          case name: Name => {
            curSymbol = name.sample()
            bind(assignment, curPos, curBound)
          }
          // We'll hit a Sequence first and in any Options
          case seq: Sequence => {
            val newScope = new Scope(this, curPos)
            for (n <- seq.getList()) {
              n match {
                case name: Name => { // Sample name and run again
                  val toSamp = name.sample()
                  if(!curBound.contains(toSamp)){
                    curBound = curBound + toSamp
                    curSymbol = toSamp
                    val (toCombine, newPos, newBound) = bind(assignment, curPos, curBound)
                    if(toCombine.getBindings().size > 0){ // indicates something bound, so we're going to combine them
                      newScope.combineScope(toCombine)
                      curPos = newPos
                      curBound = newBound
                    }
                  }
                }
                case func: Function => { //todo combine with above
                  val toSamp = func.sample()
                  if(!curBound.contains(toSamp)){
                    curBound = curBound + toSamp // adding to curBound here
                    curSymbol = toSamp
                    val (toCombine, newPos, newBound) = bind(assignment, curPos, curBound)
                    if(toCombine.getBindings().size > 0){ // indicates something bound, so we're going to combine them
                      newScope.combineScope(toCombine)
                      curPos = newPos
                      curBound = newBound
                    }
                  }
                }
                case p: Production => {} // Nothing else matters
              }
            }
            (newScope, curPos, curBound)
          }
          // Choices trigger bindings. Make Scope containing assignment of this name
            // to the value specified by the assignment in the assignment array
          case choice: Choices => {
            val toRetScope = new Scope(this, curPos)
            // if choice is
            // if(choice)
            //choice.getOptions()
//            if(choice.mapsToSelfAndTerm(Set[String](), curSymbol)) {
//              //
//            }

            // todo this may just be a name now
            val toSamp = choice.getOptions()(assignment(curPos)) // assignment out of bounds when using k
            println(s"toSamp ${toSamp}")
            toSamp match {
              case seq: Sequence => {
                toRetScope.assign(curSymbol, seq.sample()) // need to assign to whole seq
//                for(p <- seq.getList()) {
//                  toRetScope.assign(curSymbol, p.sample()) // todo this seems wrong
//                }
              }
              case p: Production => {
                toRetScope.assign(curSymbol, toSamp.sample())
              }
            }
            curPos = curPos + 1 // incrementing curPos here
            (toRetScope, curPos, curBound)
          }
          // We came to an OptionProd through a Name
          case opt: OptionProduction => {
            val internalProd = opt.getText()
            internalProd match {
              case seq: Sequence => { // there should be a sequence inside every OptionProduction
                val newScope = new Scope(this, curPos)
                for(n <- seq.getList()) {
                  n match {
                    case name: Name => { // Sample name and run again
                      val toSamp = name.sample()
                      if(!curBound.contains(toSamp)){
                        curBound = curBound + toSamp
                        curSymbol = toSamp
                        val (toCombine, newPos, newBound) = bind(assignment, curPos, curBound)
                        if(toCombine.getBindings().size > 0){ // indicates something bound, so we're going to combine them
                          newScope.combineScope(toCombine)
                          curPos = newPos
                          curBound = newBound
                        }
                      }
                    }
                    case func: Function => { //todo combine with above
                      val toSamp = func.sample()
                      if(!curBound.contains(toSamp)){
                        curBound = curBound + toSamp // adding to curBound here
                        curSymbol = toSamp
                        val (toCombine, newPos, newBound) = bind(assignment, curPos, curBound)
                        if(toCombine.getBindings().size > 0){ // indicates something bound, so we're going to combine them
                          newScope.combineScope(toCombine)
                          curPos = newPos
                          curBound = newBound
                        }
                      }
                    }
                    case p: Production => {} // Nothing else matters
                  }
                }
                (newScope, curPos, curBound)
              }
              case p: Production => { throw new Error("There should be a Sequence inside ever OptionProduction.")}
            }
          }
        }
      }
      case None => throw new Error(s"${curSymbol} could not be found.")
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
                /** if stuff breaks it was here  */
              case nonterm: Sequence => {
                for (n <- nonterm.getList()) {
                  n match {
                    case name: Name => {
                      curSymbol = name.sample()
                      val (bod, opts) = buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
                      generatingBod = bod // does nothing first time around
                      generatingOpts = opts
                    }
                    case fun: Function => curGen.append(fun.runFun(scope.lookup(fun.sample())))
                    case term: Terminal => curGen.append(term.sample())
                    case p: Production => {
                      if (scope.isBound(curSymbol)) {
                        curGen.append(scope.lookup(curSymbol))
                      } else {
                        curGen.append(p.sample())
                      }
                    }
                  }
                }
              }
                /**  */
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


