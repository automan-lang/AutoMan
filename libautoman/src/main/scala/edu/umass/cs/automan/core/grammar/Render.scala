package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Expand.{Scope, Start}
import edu.umass.cs.automan.core.grammar.Rank.Grammar

object Render {

  /**
    * Creates a string of the grammar instance specified by the Scope.
    *
    * @param scope   The Scope containing the variable bindings
    * @param grammar The Grammar that we're working with
    * @return A String that is the desired derivation of the Grammar
    */
  def render(scope: Scope, grammar: Grammar): String = {
    //renderHelper(grammar(Start), scope, new StringBuilder, Map[Name, (Int, Int)](), 0, 0)
    renderHelper(grammar(Start), grammar, scope, new StringBuilder, true, -1, Map[Name, String]())._1.toString()
  }

  // Helper method to generate a String for an Expression
  def renderHelper(expr: Expression, g: Grammar, scope: Scope, generating: StringBuilder, doAppend: Boolean, index: Int, boundVars: Map[Name, String]): (StringBuilder, Int, Map[Name, String]) = {
    var instanceSoFar = generating
    var position = index
    var boundVarsSoFar = boundVars

    expr match {
      case Ref(nt) => {
        renderHelper(g(nt), g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
      } // forward
      case Terminal(value) => { // append if we're on the right branch
        if (doAppend) instanceSoFar.append(value)
        (instanceSoFar, position, boundVarsSoFar)
      }
      case Sequence(sentence) => { // call on each part of seq
        for (e <- sentence) {
          val (newStr, newInd, newBound) = renderHelper(e, g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
          instanceSoFar = newStr
          position = newInd
          boundVarsSoFar = newBound // updating fine here
        }
        (instanceSoFar, position, boundVarsSoFar)
      }
      case Choice(choices) => { // call on each Choice but only append correct one
        position += 1
        for (e <- choices) {
          var doApp = false
          if (doAppend && scope(position) == e) { // if scope points to this option we're on a valid branch // pos - 1?
            doApp = true
          }
          val (newStr, newInd, newBound) = renderHelper(e, g, scope, instanceSoFar, doApp, position, boundVarsSoFar) // pos + 1?
          instanceSoFar = newStr
          position = newInd
          boundVarsSoFar = newBound
        }
        (instanceSoFar, position, boundVarsSoFar)
      }
      case Binding(nt) => {
        val startStr = instanceSoFar.toString() // save string before adding binding so we know what was added

        if (!(boundVarsSoFar contains nt)) { // haven't seen this binding before // todo doAppend?
          val (newStr, newInd, newBound) = renderHelper(g(nt), g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
          val added: String = (newStr diff startStr).toString()//newStr.toString.toSeq.diff(startStr) // figure out what was added
          instanceSoFar = newStr
          position = newInd
          boundVarsSoFar = newBound + (nt -> added) // update bindings
        } else {
          instanceSoFar = instanceSoFar.append(boundVarsSoFar(nt))
        }
        (instanceSoFar, position, boundVarsSoFar)
      }
    }
  }
}