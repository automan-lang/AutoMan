/**
  * A class representing a grammar
  * @param _rules a map from Strings to Productions
  * @param _startSymbol the start symbol for the eventual iterations over the experiment space. Doesn't change.
  */

case class Grammar(_rules: Map[String, Production], _startSymbol: String){

  private var curSym = _startSymbol // the current symbol we're working with, which may change

  def rules = _rules
  //def rules_=(newRules: Map[String, Production]) = rules = newRules

  def startSymbol = _startSymbol

  def curSymbol = curSym
  def curSymbol_= (newSym: String): Unit = curSym = newSym
//  def resetStartSym: Unit = {
//    startSym = _startSymbol
//  }

}
