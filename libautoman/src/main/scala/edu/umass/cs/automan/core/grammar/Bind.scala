package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Expand.{Scope, Start, binding, ch, expand, nt, prettyPrint, ref, seq, term}
import edu.umass.cs.automan.core.grammar.Rank.{Grammar, generateBases}


object Bind {
  type OccurrenceMap = Map[Expression, (Int, Int)]

  // Scope is Map[(Name, Int), Expression] (int is level at which it was bound)
  /**
    * Generates a Scope that contains bindings from Names to Expressions based on the provided assignment array.
    * @param g The Grammar we are binding
    * @param assignment The assignment for the grammar instance we are generating the bindings for
    * @return A Scope containing the bindings
    */
  def bind(g: Grammar, assignment: Array[Int]): Scope = {
    bindHelper(g(Start), g, assignment, Map[Int, Expression](), Set[String]())._1
  }

  // Helper method to bind Ints (representing the index of an NT pointing to a Choice) to Expressions.
  // Returns Scope so far and text of names (without depth) already bound
  private def bindHelper(expr: Expression, g: Grammar, assignment: Array[Int], generatingScope: Scope, generatedNames: Set[String]): (Scope, Set[String]) = {
    val index = generatingScope.size
    var soFarScope = generatingScope
    var soFarNames = generatedNames

    expr match {
      case Ref(nt) => bindHelper(g(nt), g, assignment, generatingScope, soFarNames + nt.text)
      case OptionProduction(text) => bindHelper(text, g, assignment, generatingScope, soFarNames) // forward
      case Terminal(_) => (soFarScope, soFarNames) // ignore
      case Function(_,_,_) => (soFarScope, soFarNames) // ignore
      case Sequence(sentence) => { // bind each component
        for(e <- sentence) {
          val (newScope, newNames) = bindHelper(e, g, assignment, soFarScope, soFarNames)
          soFarScope = newScope
          soFarNames = newNames
        }
        (soFarScope, soFarNames)
      }
      case Choice(choices) => { // bind and go through binding options
        soFarScope = soFarScope + (index -> choices(assignment(index)))
        for(e <- choices) {
          val (newScope, newNames) = bindHelper(e, g, assignment, soFarScope, soFarNames)
          soFarScope = newScope
          soFarNames = newNames
        }
        (soFarScope, soFarNames)
      }
      case Binding(nt) => { // bind if we haven't seen before
        if(!(soFarNames contains nt.text)) {
          bindHelper(g(nt), g, assignment, generatingScope, soFarNames + nt.text)
        } else (soFarScope, soFarNames)
      }
    }
  }

  // Helper method to print Scopes nicely
  def prettyPrintScope(scope: Scope): String = {
    val toRet: StringBuilder = new StringBuilder
    for(s <- scope) {
      toRet.append(s._1 + " -> " + Expand.prettyPrintExpr(s._2) + "\n")
    }
    toRet.toString()
  }

}
