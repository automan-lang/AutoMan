package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.SampleGrammar.buildString

import scala.util.control.Breaks.{break, breakable}

/**
  * A class representing a grammar
  *
  * @param _rules a map from Strings to Productions
  * @param _startSymbol the start symbol for the eventual iterations over the experiment space. Doesn't change.
  */

case class Grammar(_rules: Map[String, Production], _startSymbol: String){

  private var curSym = _startSymbol // the current symbol we're working with, which may change

  def rules = _rules
  //def rules_=(newRules: Map[String, edu.umass.cs.automan.core.grammar.Production]) = rules = newRules

  def startSymbol = _startSymbol

  def curSymbol = curSym
  def curSymbol_= (newSym: String): Unit = curSym = newSym

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
          case break: OptionBreak => generating.append(break.sample())
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
                case break: OptionBreak => generating.append(break.sample())
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

  /**
    * Builds body of a question (up to first OptionBreak)
    * @param scope The scope, containing variable bindings
    * @param soFar The string generated so far
    * @return a StringBuilder representing the body of the question
    */
  def buildBody(scope: Scope, soFar: StringBuilder): StringBuilder = {
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
          //case break: OptionBreak => break
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
            breakable {
              for (n <- nonterm.getList()) {
                n match {
                  case name: Name => {
                    curSymbol = name.sample()
                    buildString(scope, generating)
                  }
                  case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
                  case term: Terminal => generating.append(term.sample())
                  case optBreak: OptionBreak => break
                  case p: Production => {
                    if (scope.isBound(curSymbol)) {
                      generating.append(scope.lookup(curSymbol))
                    } else {
                      generating.append(p.sample())
                    }
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

  def buildOptions(scope: Scope, soFar: StringBuilder, opts: List[StringBuilder], inOpt: Boolean): List[StringBuilder] = {
    val generating: StringBuilder = soFar
    var opts: List[StringBuilder] = opts

    val samp: Option[Production] = _rules.get(curSymbol) // get edu.umass.cs.automan.core.grammar.Production associated with symbol from grammar
    if(inOpt){ // if we're in an Option, we need to generate everything
      samp match {
        case Some(samp) => {
          samp match {
            case name: Name => {
              curSymbol = name.sample()
              buildOptions(scope, generating, opts, true)
            }
            case term: Terminal => {
              generating.append(term.sample())
              buildOptions(scope, generating, opts, true)
            }
            //case break: OptionBreak => break
            case choice: Choices => {
              if(scope.isBound(curSymbol)){
                generating.append(scope.lookup(curSymbol))
                buildOptions(scope, generating, opts, true)
              } else {
                throw new Exception(s"Choice ${curSymbol} has not been bound")
              }
            }
            case fun: Function => { // need to look up element in grammar via getParam, find its binding, then use that in runFun
              if(scope.isBound(fun.sample())){
                generating.append(fun.runFun(scope.lookup(fun.sample())))
                buildOptions(scope, generating, opts, true) // TODO: is this right?
              } else {
                throw new Exception(s"Variable ${fun.sample()} not bound")
              }
            }
            case nonterm: Sequence => {
              breakable {
                for (n <- nonterm.getList()) {
                  n match {
                    case name: Name => {
                      curSymbol = name.sample()
                      buildOptions(scope, generating, opts, true)
                    }
                    case fun: Function => generating.append(fun.runFun(scope.lookup(fun.sample())))
                    case term: Terminal => generating.append(term.sample())
                    case optBreak: OptionBreak => { // we've hit another option, so add this one and continue
                      opts += generating.toString()
                      buildOptions(scope, new StringBuilder, opts, true)
                    }
                    case p: Production => {
                      if (scope.isBound(curSymbol)) {
                        generating.append(scope.lookup(curSymbol))
                      } else {
                        generating.append(p.sample())
                      }
                    }
                  }
                }
              }
              opts
            }
          }
        }
        case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
      }
    } else { // if not, ignore everything till we get to an OptionBreak
      samp match {
        case Some(samp) => {
          //println(s"${samp} is a LNT ${samp.isLeafNT()}")
          samp match {
            case optBreak: OptionBreak => { // we've hit our first option break TODO is this necessary? don't think it'll ever be hit
              buildOptions(scope, soFar, opts, true)
            }
            case nonterm: Sequence => {
              for (n <- nonterm.getList()) {
                n match {
                  case optBreak: OptionBreak => { buildOptions(scope, soFar, opts, true)}
                  case p: Production => { buildOptions(scope, soFar, opts, false) }
                }
              }
              opts
            }
            case p: Production => { buildOptions(scope, soFar, opts, false) } // ignore all other Productions
          }
        }
        case None => throw new Exception(s"Symbol ${curSymbol} could not be found")
      }
    }
  }

//  def resetStartSym: Unit = {
//    startSym = _startSymbol
//  }

}
