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
//            if(rules.get(curSymbol).isInstanceOf[Option[OptionProduction]]){
//              if(inBod){
//                inBod = false
//                generatingBod = new StringBuilder(curGen.toString())
//                buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
//              } else {
//                buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
//              }
//            } else {
//              buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
//            }

            // TODO: check if name links to option prod
            // then check if in bod
            // if in bod, append, set inBod to false, and call,
            // if not, carry on
            //if(inBod) buildString(scope, )
            //(generatingBod, generatingOpts.toList)
            //buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod) // todo this is screwing it up?
            //(generatingBod, generatingOpts.toList)
          }//render(g, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol
          case term: Terminal => {
            curGen.append(term.sample())
            (generatingBod, generatingOpts)
          }
          //case break: OptionBreak => generating.append(break.sample())
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
//            if(inBod) { // hitting first opt, so save body // todo overriding!
//              generatingBod = new StringBuilder(curGen.toString()) // linked?
//              curGen.clear()
//              inBod = false
//              //curGen.append(buildString(scope, curGen))
//              //(generatingBod, generatingOpts.toList)
//              //buildQandOpts(scope, generatingBod, generatingOpts, cur, inBod) // bad bc looking up seq over and over
//            } else { // hitting subsequent opt, so save opt
//              //curGen.append(opt.sample()) // todo this has no effect
//              generatingOpts += new StringBuilder(curGen.toString()) // bad bc won't account for other productions //buildString(scope, curGen)
//              curGen.clear()
//              //curGen.append(buildString(scope, curGen))
//              //(generatingBod, generatingOpts.toList)
//              //buildQandOpts(scope, generatingBod, generatingOpts, cur, inBod)
//            }
            curGen.clear()
            opt.getText() match { // generate opt text
              case name: Name => {
                curSymbol = name.sample()
                //buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
                curGen.append(buildString(scope, curGen)) // todo uncommenting gets answers printed
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
            //buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod) // todo never updating symbol so infinite loop
            (generatingBod, generatingOpts) // todo as soon as we return everything is gone
          }
          case nonterm: Sequence => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => {
                  curSymbol = name.sample()
                  /**  */
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
                  /**  */

//                  val(bod, opts) = buildQandOpts(scope, generatingBod, generatingOpts, curGen, inBod)
//                  generatingBod = bod // does nothing first time around
//                  generatingOpts = opts
                  //buildString(scope, curGen) // todo uncommenting gets answers printed
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

//    val samp: Option[Production] = rules.get(curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
//    samp match {
//      case Some(samp) => {
//        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
//        samp match {
//          case name: Name => {
//            curSymbol = name.sample()
//            buildQandOpts(scope, generatingBod, generatingOpts, curGen)
//          }//render(g, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol
//          case term: Terminal => {
//            curGen.append(term.sample())
//            (generatingBod, generatingOpts)
//            //generatingBod.append(term.sample())
//          }
//          //case break: OptionBreak => generating.append(break.sample())
//          case choice: Choices => {
//            if(scope.isBound(curSymbol)){
//              //println(s"${startSymbol} is bound, looking up")
//              //print(scope.lookup(startSymbol))
//              curGen.append(scope.lookup(curSymbol))
//              (generatingBod, generatingOpts)
//            } else {
//              throw new Exception(s"Choice ${curSymbol} has not been bound")
//            }
//          }
//          case opt: OptionProduction => {
//            generatingBod.append(curGen)
//            generatingBod.clear()
//            generatingOpts +: new StringBuilder(opt.sample())
//            //generatingOpts += opt.sample() //List(new StringBuilder(opt.sample()))
//            (generatingBod, generatingOpts)
//            //generatingBod.append(opt.sample())
//          }
//          case nonterm: Sequence => { // TODO sequence in sequence?
//            //breakable {
//            for (n <- nonterm.getList()) {
//              n match {
//                case name: Name => {
//                  curSymbol = name.sample()
//                  curGen.append(buildString(scope, generatingBod))
//                }
//                case fun: Function => curGen.append(fun.runFun(scope.lookup(fun.sample())))
//                case term: Terminal => curGen.append(term.sample())
//                case opt: OptionProduction => curGen.append(opt.sample())
//                //case optBreak: OptionBreak => generating.append(optBreak.sample())
//                case p: Production => {
//                  if (scope.isBound(curSymbol)) {
//                    curGen.append(scope.lookup(curSymbol))
//                  } else {
//                    curGen.append(p.sample())
//                  }
//                }
//              }
//            }
//            //}
//            (generatingBod, generatingOpts)
//          }
//          case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
//            if(scope.isBound(fun.sample())){ // TODO: is this right?
//              curGen.append(fun.runFun(scope.lookup(fun.sample())))
//              (generatingBod, generatingOpts)
//            } else {
//              throw new Exception("Variable not bound")
//            }
//          }
//        }
//      }
//      case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
//    }
  }

//  // TODO these methods may have to move into a Question class
//  /**
//    * Builds body of a question (up to first OptionBreak)
//    * @param scope The scope, containing variable bindings
//    * @param soFar The string generated so far
//    * @return a StringBuilder representing the body of the question
//    */
//  def buildBody(scope: Scope, soFar: StringBuilder): StringBuilder = {
//    var generating: StringBuilder = soFar
//
//    val samp: Option[Production] = _rules.get(curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
//    samp match {
//      case Some(samp) => {
//        //println(s"${samp} is a LNT ${samp.isLeafNT()}")
//        samp match {
//          case name: Name => {
//            curSymbol = name.sample()
//            buildString(scope, generating)
//          }//render(g, name.sample(), scope) // edu.umass.cs.automan.core.grammar.Name becomes start symbol
//          case term: Terminal => generating.append(term.sample())
//          //case break: OptionBreak => break
//          case choice: Choices => {
//            if(scope.isBound(curSymbol)){
//              //println(s"${startSymbol} is bound, looking up")
//              //print(scope.lookup(startSymbol))
//              generating.append(scope.lookup(curSymbol))
//            } else {
//              throw new Exception(s"Choice ${curSymbol} has not been bound")
//            }
//          }
//          case nonterm: Sequence => {
//            breakable {
//              for (n <- nonterm.getList()) {
//                n match {
//                  case name: Name => {
//                    curSymbol = name.sample()
//                    buildString(scope, generating)
//                  }
//                  case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
//                  case term: Terminal => generating.append(term.sample())
//                  //case optBreak: OptionBreak => break
//                  case p: Production => {
//                    if (scope.isBound(curSymbol)) {
//                      generating.append(scope.lookup(curSymbol))
//                    } else {
//                      generating.append(p.sample())
//                    }
//                  }
//                }
//              }
//            }
//            generating
//          }
//          case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
//            if(scope.isBound(fun.sample())){ // TODO: is this right?
//              generating.append(fun.runFun(scope.lookup(fun.sample())))
//            } else {
//              throw new Exception("Variable not bound")
//            }
//          }
//        }
//      }
//      case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
//    }
//  }

//  /**
//    * Builds options for a Checkbox or Radio Button question
//    *
//    * @param scope The scope, containing variable bindings
//    * @param soFar The string generated so far
//    * @param opts The list of option strings generated so far
//    * @param inOpt Are we in an option? (Have we seen an OptionBreak yet?)
//    * @return A list of StringBuilders representing the list of question options
//    */
//  def buildOptions(scope: Scope, soFar: StringBuilder, opts: List[StringBuilder], inOpt: Boolean): List[StringBuilder] = {
//    val generating: StringBuilder = soFar
//    var opts: List[StringBuilder] = opts
//
//    val samp: Option[Production] = _rules.get(curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
//    if(inOpt){ // if we're in an Option, we need to generate everything
//      samp match {
//        case Some(samp) => {
//          samp match {
//            case name: Name => {
//              curSymbol = name.sample()
//              buildOptions(scope, generating, opts, true)
//            }
//            case term: Terminal => {
//              generating.append(term.sample())
//              buildOptions(scope, generating, opts, true)
//            }
//            //case break: OptionBreak => break
//            case choice: Choices => {
//              if(scope.isBound(curSymbol)){
//                generating.append(scope.lookup(curSymbol))
//                buildOptions(scope, generating, opts, true)
//              } else {
//                throw new Exception(s"Choice ${curSymbol} has not been bound")
//              }
//            }
//            case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
//              if(scope.isBound(fun.sample())){
//                generating.append(fun.runFun(scope.lookup(fun.sample())))
//                buildOptions(scope, generating, opts, true) // TODO: is this right?
//              } else {
//                throw new Exception(s"Variable ${fun.sample()} not bound")
//              }
//            }
//            case nonterm: Sequence => {
//              breakable {
//                for (n <- nonterm.getList()) {
//                  n match {
//                    case name: Name => {
//                      curSymbol = name.sample()
//                      buildOptions(scope, generating, opts, true)
//                    }
//                    case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
//                    case term: Terminal => generating.append(term.sample())
////                    case optBreak: OptionBreak => { // we've hit another option, so add this one and continue
////                      opts += generating.toString()
////                      buildOptions(scope, new StringBuilder, opts, true)
////                    }
//                    case p: Production => {
//                      if (scope.isBound(curSymbol)) {
//                        generating.append(scope.lookup(curSymbol))
//                      } else {
//                        generating.append(p.sample())
//                      }
//                    }
//                  }
//                }
//              }
//              opts
//            }
//          }
//        }
//        case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
//      }
//    } else { // if not, ignore everything till we get to an OptionBreak
//      samp match {
//        case Some(samp) => {
//          //println(s"${samp} is a LNT ${samp.isLeafNT()}")
//          samp match {
////            case optBreak: OptionBreak => { // we've hit our first option break TODO is this necessary? don't think it'll ever be hit
////              buildOptions(scope, soFar, opts, true)
////            }
//            case nonterm: Sequence => {
//              for (n <- nonterm.getList()) {
//                n match {
//                  //case optBreak: OptionBreak => { buildOptions(scope, soFar, opts, true)}
//                  case p: Production => { buildOptions(scope, soFar, opts, false) }
//                }
//              }
//              opts
//            }
//            case p: Production => { buildOptions(scope, soFar, opts, false) } // ignore all other Productions
//          }
//        }
//        case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
//      }
//    }
//  }

//  def resetStartSym: Unit = {
//    startSym = _startSymbol
//  }

}


