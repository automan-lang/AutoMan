package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Expand.{Scope, Start, binding, ch, expand, nt, ref, seq, term, opt}
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
  // Returns the current string being generated, the index we're at in the grammar, and a Map for bindings from their Name to the String generated
  def renderHelper(expr: Expression, g: Grammar, scope: Scope, generating: StringBuilder, doAppend: Boolean, index: Int, boundVars: Map[Name, String]): (StringBuilder, Int, Map[Name, String]) = {
    var instanceSoFar = generating
    var position = index
    var boundVarsSoFar = boundVars

    expr match {
      case Ref(nt) => {
        renderHelper(g(nt), g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
      } // forward
      case OptionProduction(text) => { // if hit another OP, terminate
        renderHelper(text, g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
      }
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

  def renderInstance(scope: Scope, grammar: Grammar): (String, Array[String]) = {
    val(body, opts, _, _) = renderInstanceHelper(grammar(Start), grammar, scope, new StringBuilder, Array[StringBuilder](), true, -1, Map[Name, String]())
    (body.toString, opts.map(_.toString()))
  }

  // Helper method to generate a String for an Expression
  // Returns the current string being generated, the index we're at in the grammar, and a Map for bindings from their Name to the String generated
  def renderInstanceHelper(expr: Expression, g: Grammar, scope: Scope, generatingBod: StringBuilder, generatingOpts: Array[StringBuilder], doAppend: Boolean, index: Int, boundVars: Map[Name, String]): (StringBuilder, Array[StringBuilder], Int, Map[Name, String]) = {
    var bodSoFar = generatingBod
    var optsSoFar = generatingOpts
    var position = index
    var boundVarsSoFar = boundVars

    expr match {
      case Ref(nt) => {
        renderInstanceHelper(g(nt), g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
      } // forward
      case OptionProduction(text) => {
        val (newOpt, newInd, newBound) = renderHelper(expr, g, scope, new StringBuilder, doAppend, index, boundVarsSoFar)
        position = newInd
        boundVarsSoFar = newBound
        //optsSoFar += newOpt
        (bodSoFar, optsSoFar :+ newOpt, position, boundVarsSoFar)
      }
      case Terminal(value) => { // append if we're on the right branch
        if (doAppend) bodSoFar.append(value)
        (bodSoFar, optsSoFar, position, boundVarsSoFar)
      }
      case Sequence(sentence) => { // call on each part of seq
        for (e <- sentence) {
          val (newStr, newOpts, newInd, newBound) = renderInstanceHelper(e, g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
          bodSoFar = newStr
          optsSoFar = newOpts
          position = newInd
          boundVarsSoFar = newBound // updating fine here
        }
        (bodSoFar, optsSoFar, position, boundVarsSoFar)
      }
      case Choice(choices) => { // call on each Choice but only append correct one
        position += 1
        for (e <- choices) {
          var doApp = false
          if (doAppend && scope(position) == e) { // if scope points to this option we're on a valid branch // pos - 1?
            doApp = true
          }
          val (newStr, newOpts, newInd, newBound) = renderInstanceHelper(e, g, scope, bodSoFar, optsSoFar, doApp, position, boundVarsSoFar) // pos + 1?
          bodSoFar = newStr
          optsSoFar = newOpts
          position = newInd
          boundVarsSoFar = newBound
        }
        (bodSoFar, optsSoFar, position, boundVarsSoFar)
      }
      case Binding(nt) => {
        val startStr = bodSoFar.toString() // save string before adding binding so we know what was added

        if (!(boundVarsSoFar contains nt)) { // haven't seen this binding before // todo doAppend?
          val (newStr, newOpts, newInd, newBound) = renderInstanceHelper(g(nt), g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
          val added: String = (newStr diff startStr).toString()//newStr.toString.toSeq.diff(startStr) // figure out what was added
          bodSoFar = newStr
          optsSoFar = newOpts
          position = newInd
          boundVarsSoFar = newBound + (nt -> added) // update bindings
        } else {
          bodSoFar = bodSoFar.append(boundVarsSoFar(nt))
        }
        (bodSoFar, optsSoFar, position, boundVarsSoFar)
      }
    }
  }

  /**
    * Expands, binds, and generates a given instance of a grammar.
    * @param g The Grammar
    * @param variation The variation to generate
    * @param depth The depth to expand the Grammar to
    * @return A tuple of the body of the instance and any question options
    */
  def buildInstance(g: Grammar, variation: Int, depth: Int): (String, Array[String]) = {
    val expandedG = expand(g, depth)
    val bases = Rank.generateBases(expandedG)
    val assignment = Rank.unrank(variation, bases)
    val scope = Bind.bind(expandedG, assignment)
    val (body, opts) = renderInstance(scope, expandedG)
    (body.toString, opts.map(_.toString))
  }

  def prettyPrintInstance(body: String, opts: Array[String]) = {
    println(body)
    opts.map(println(_))
  }

  def main(args: Array[String]): Unit = {
    // Linda!
    val lindaG = Map[Name, Expression](
      Start -> ref("A"),
      nt("A") -> seq(Array(
        binding(nt("Name")),
        term(" is "),
        binding(nt("Age")),
        term("  years old, single, outspoken, and very bright. She majored in "),
        binding(nt("Major")),
        term(". As a student, she was deeply concerned with issues of "),
        binding(nt("Issue")),
        term(", and also participated in "),
        binding(nt("Demonstration")),
        term(" demonstrations.\n Which is more probable?\n"),
        opt(seq(Array(
          binding(nt("Name")),
          term(" is a "),
          binding(nt("Job")),
          term(".")))),
        //term("\n"),
        opt(seq(Array(
          binding(nt("Name")),
          term(" is a "),
          binding(nt("Job")),
          term(" and is active in the "),
          binding(nt("Movement")),
          term(" movement.")
        )))
      )),
      nt("Name") -> ch(Array(
        term("Linda"),
        term("Emmie"),
        term("Cheryl"),
        term("Little Miss Muffet")
      )),
      nt("Age") -> ch(Array(
        term("21"),
        term("31"),
        term("41"),
        term("51"),
        term("61")
      )),
      nt("Major") -> ch(Array(
        term("chemistry"),
        term("psychology"),
        term("english literature"),
        term("philosophy"),
        term("women's studies"),
        term("underwater basket weaving")
      )),
      nt("Issue") -> ch(Array(
        term("discrimination and social justice"),
        term("fair wages"),
        term("animal rights"),
        term("white collar crime"),
        term("unemployed circus workers")
      )),
      nt("Demonstration") -> ch(Array(
        term("anti-nuclear"),
        term("anti-war"),
        term("pro-choice"),
        term("anti-abortion"),
        term("anti-animal testing")
      )),
      nt("Job") -> ch(Array(
        term("bank teller"),
        term("semiretired almond paste mixer"),
        term("tennis scout"),
        term("lawyer"),
        term("professor")
      )),
      nt("Movement") -> ch(Array(
        term("feminist"),
        term("anti-plastic water bottle"),
        term("pro-pretzel crisp"),
        term("pro-metal straw"),
        term("environmental justice")
      ))
    )

    val expLindaG = expand(lindaG, 2)
    val lindaBases = Rank.generateBases(expLindaG)
    println("Linda: ")
    //println("bases: " + lindaBases.mkString(" "))
    val lindaAssignment = Rank.unrank(0, lindaBases)
    println("assignment: " + lindaAssignment.mkString(" "))
    val lindaScope = Bind.bind(expLindaG, lindaAssignment)
    val (body, opts) = renderInstance(lindaScope, expLindaG)
    prettyPrintInstance(body, opts)
  }
}