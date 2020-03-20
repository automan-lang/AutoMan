package edu.umass.cs.automan.core.grammar
import edu.umass.cs.automan.core.grammar.{Expression}

object Expand {

  type Scope = Map[String, String]

  type KCount = Map[String, Int] // Maps nonterminal name to count

  val ε = Terminal("")

  /**
    * Expands an Expression into a tree with max depth k. Expands recursive Expressions until they reach the depth parameter, k.
    * @param expr The Expression to be expanded
    * @param k The maximum depth to expand to
    * @return A tree version of the Expression
    */
  def expand(expr: Expression, k: Int): Expression = {
    expandHelper(expr, Map[String, Int](), k)
  }

  /**
    * A helper method to expand Expressions.
    * @param expr The Expression to be expanded
    * @param kc The NonTerminals that have been expanded and at which count
    * @param k The maximum depth to be expanded
    * @return A tree version of the Expression
    */
  private def expandHelper(expr: Expression, kc: KCount, k: Int): Expression = {
    expr match {
      case NonTerminal(name, expr) => { // if we've hit the max depth, map to empty string
        if(kc.contains(name) && kc.get(name) == k) {
          ε
        } else {
          // rename this NonTerminal to NonTerminal_i
          // where i is the current depth
          if(!kc.contains(name)) { // first time we've seen name
            val newKC = kc + (name -> 1)
            val newExpr = expandHelper(expr, newKC, k)
            freshName(name, newExpr, 0)
          } else { // we've seen name before so get correct depth
            val curDepth: Option[Int] = kc.get(name)
            curDepth match {
              case Some(d) => {
                //val curDepth = d + 1
                val newKC = kc + (name -> (d + 1))
                val newExpr = expandHelper(expr, newKC, k)
                freshName(name, newExpr, d)
              }
              case None => { // this should never trigger
                throw new Error(s"${name} could not be found")
              }
            }
          }
        }
      }
      case term: Terminal => {
        term
      }
      case Choice(exprs) => {
        Choice(exprs.map(expandHelper(_, kc, k)))
      }
      case Sequence(exprs) => {
        Sequence(exprs.map(expandHelper(_, kc, k)))
      }
      case Binding(name, expr) => {
        Binding(name, expandHelper(expr, kc, k))
      }
    }
  }

  def freshName(name: String, expr: Expression, i: Int): Expression = {
    NonTerminal(name + "_" + i.toString(), expr)
  }
}
