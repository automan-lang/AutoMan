package org.automanlang.core.grammar

import Rank.Grammar

object Expand {

  // Scope is Map from an NT Name, the level at which it was bound, and how many times it has occurred at that level
  // to an Expression
  //type Scope = Map[(Name, Int, Int), Expression]
  type Scope = Map[Int, Expression] // Scope maps from an NT index to an Expression

  type KCount = Map[String, Int] // Maps nonterminal name to count

  type RefMap = Map[String, Expression] // Maps strings (Expression Ref names) to Expressions

  val ε: Terminal = Terminal("")

  val Start = new Name("Start", None)

  /**
    * Turns an infinite grammar into a finite grammar
    * @param g The grammar to be expanded
    * @param k The max depth to expand the grammar to
    * @return A finite grammar with depth at most k
    */
  def expand(g: Grammar, k: Int): Grammar = {
    expandLHSHelper(Start, Map[String, Int](), g, k)._2
  }

  // Helper method to generate Expressions with new Names
  // xp
  private def expandLHSHelper(lhs: Name, kc: KCount, g: Grammar, k: Int): (Expression, Grammar) = {
    val (newLHS, newKC) = lhs.freshName(kc, k) // get new LHS and update kc
    if(newLHS.isEpsilon()) { // if done (NT_k) map to ε
      (ε, g + (newLHS -> ε))
    } else {
      // expand RHS
      val (newRHS, newG) = expandRHSHelper(newLHS, g(lhs), g, newKC, k)
      // update grammar
      val updatedGrammar = newG + (newLHS -> newRHS)
      (newRHS, updatedGrammar)
    }
  }

  // Helper method to actually expand Expressions
  // exxp
  private def expandRHSHelper(lhs: Name, expr: Expression, g: Grammar, kc: KCount, k: Int): (Expression, Grammar) = {
    // expand RHS
    expr match {
      case Ref(nt) => {
        // get new NT name but don't update kcount
        val (newNT, _) = nt.freshName(kc, k)
        // eval LHS with original name
        val (_, newG) = expandLHSHelper(nt, kc, g, k)
        // return updated Ref and g
        (Ref(newNT), newG)
      }
      case Terminal(value) => (expr, g)
      case Choice(choices) => {
        // eval all expressions in choice
        val(newG, els) = choices.foldLeft(g, Array[Expression]()) {
          (acc, expr) => {
            acc match {
              case (gacc, eacc) => {
                // expand expression
                val (newE, newG): (Expression, Grammar) = expandRHSHelper(lhs, expr, g, kc, k)
                // merge updated grammar
                val newGacc = merge(gacc, newG)
                // add updated expression to list
                val newExprs: Array[Expression] = eacc :+ newE
                (newGacc, newExprs)
              }
            }
          }
        }
        // return an updated Choice and updated grammar
        (Choice(els), newG)
      }
      case Sequence(sentence) => {
        // eval all expressions in choice
        val(newG, els) = sentence.foldLeft(g, Array[Expression]()) {
          (acc, expr) => {
            acc match {
              case (gacc, eacc) => {
                // expand expression
                val (newE, newG) = expandRHSHelper(lhs, expr, g, kc, k)
                // merge updated grammar
                val newGacc = merge(gacc, newG)
                // add updated expression to list
                val newExprs = eacc :+ newE
                (newGacc, newExprs)
              }
            }
          }
        }
        // return an updated Choice and updated grammar
        (Sequence(els), newG)
      }
      case Binding(nt) => {
        val (newNT, _) = nt.freshName(kc, k)
        // evaluate LHS with binding name
        val (_, newG) = expandLHSHelper(nt, kc, g, k) //todo figure out if should be new name?
        (Binding(newNT), newG)
      }
      case OptionProduction(text) => {
        // expand expression
        val (newE, newG) = expandRHSHelper(lhs, text, g, kc, k)
        (OptionProduction(newE.asInstanceOf[TextExpression]), newG) // todo fix hacky crap
      }
      case Function(fun, param, capitalize) => {
        param match {
          case nt: Name => {
            val (newParam, _) = nt.freshName(kc, k)
            (Function(fun, newParam, capitalize), g)
          }
        }
      }
    }
  }

  // Helper method to merge two maps
  private def merge[A, B](m1: Map[A, B], m2: Map[A, B]): Map[A, B] = {
    //    var toRet: Map[A, B] = Map[A,B]()
    //    toRet = Seq.concat(m1.toSeq, m2.toSeq).toMap
    Seq.concat(m1.toSeq, m2.toSeq).toMap
    //new Map(Seq.concat(m1.toSeq, m2.toSeq))
  }

  def prettyPrint(g: Grammar): String = {
    val sb = new StringBuilder()
    for (kvp <- g) {
      val lhs = kvp._1.fullname()
      val rhs = kvp._2
      sb.append(s"${lhs} ::= ${prettyPrintHelper(rhs)}\n")
    }
    sb.toString
  }

  def prettyPrintExpr(e: Expression): String = {
    prettyPrintHelper(e)
  }

  // Helper method to generate string for single expression
  private def prettyPrintHelper(e: Expression): String = {
    e match {
      //case Ref(name, maybeInt) => fullname(new Name(name, maybeInt)) // todo figure out syntax so not annoying
      case Ref(nt) => nt.fullname()
      case Terminal(value) => value
      case Choice(choices) => choices.map(prettyPrintHelper(_)).mkString( " | ")
      case Sequence(sentence) => sentence.map(prettyPrintHelper(_)).mkString( "")
      //case Binding(name, maybeInt) => "Var(" + fullname(new Name(name, maybeInt)) + ")"
      case Binding(nt) => "Var(" + nt.fullname() + ")"
      case OptionProduction(text) => s"Option(${prettyPrintHelper(text)})\n"
      case Function(_, param, _) => s"fun(${param.fullname()})"
    }
  }

  // simple constructors
  def nt(name: String): Name = { new Name(name, None) }
  def ref(name: String): Ref = { Ref(nt(name)) }
  def binding(name: Name): Binding = { Binding(name) }
  def ch(exprs: Array[Expression]): Choice = { Choice(exprs) }
  def term(literal: String): Terminal = { Terminal(literal) }
  def seq(exprs: Array[Expression]): Sequence = { Sequence(exprs) }
  def opt(text: TextExpression): OptionProduction = {OptionProduction(text)}
  def fun(fun: Map[String, String], param: Name, capitalize: Boolean): Function = { Function(fun, param, capitalize) }
}
