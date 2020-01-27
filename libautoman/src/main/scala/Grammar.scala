/**
  * A class representing a grammar
  * @param _rules a map from Strings to Productions
  * @param _startSymbol the start symbol for the eventual iterations over the experiment space
  */

case class Grammar(_rules: Map[String, Production], _startSymbol: String){

  var startSym = _startSymbol

  def rules = _rules
  //def rules_=(newRules: Map[String, Production]) = rules = newRules

  def startSymbol = startSym
  def startSymbol_= (newSym: String): Unit = startSym = newSym
  def resetStartSym: Unit = {
    startSym = _startSymbol
  }

}
