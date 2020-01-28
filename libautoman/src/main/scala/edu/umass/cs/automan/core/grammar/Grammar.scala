package edu.umass.cs.automan.core.grammar

/**
  * A class representing a grammar
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
                  buildString(_rules, scope, generating)
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
//  def resetStartSym: Unit = {
//    startSym = _startSymbol
//  }

}
