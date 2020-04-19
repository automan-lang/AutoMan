package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Expand.{Scope, Start, binding, ch, expand, nt, ref, seq, term, opt, fun}
import edu.umass.cs.automan.core.grammar.Rank.Grammar

object Render {

  /**
    * Creates a string of the experiment instance specified by the Scope.
    * @param scope The Scope containing the variable bindings
    * @param grammar The Grammar that we're working with
    * @return A String representing the body of the question and an array representing the question options
    */
  def renderInstance(scope: Scope, grammar: Grammar): (String, Array[String]) = {
    //renderHelper(grammar(Start), scope, new StringBuilder, Map[Name, (Int, Int)](), 0, 0)
    //renderHelper(grammar(Start), grammar, scope, new StringBuilder, true, -1, Map[Name, String]())._1.toString()
    val (instArr, choiceArrs, binMap, _) = firstRender(grammar(Start), grammar, scope, Array[Expression](), Array[Array[Expression]](), true, -1, Map[Name, String]())
    secondRender(instArr, choiceArrs, binMap)
  }

  // returns list of terms and functions for body, lists for each opt, map of bindings to the strings they map to, and position
  def firstRender(expr: Expression, g: Grammar, scope: Scope, generatingBod: Array[Expression], generatingOpts: Array[Array[Expression]], doAppend: Boolean, index: Int, boundVars: Map[Name, String]): (Array[Expression], Array[Array[Expression]], Map[Name, String], Int) = {
    var bodSoFar = generatingBod
    var optsSoFar = generatingOpts
    var boundVarsSoFar = boundVars
    var position = index

    expr match {
      case Ref(nt) => {
        firstRender(g(nt), g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
      }
      case t: Terminal => { // append if we're on the right branch
        if(doAppend) bodSoFar = bodSoFar :+ t
        (bodSoFar, optsSoFar, boundVarsSoFar, position)
      }
      case f: Function => {
        if(doAppend) bodSoFar = bodSoFar :+ f
        (bodSoFar, optsSoFar, boundVarsSoFar, position)
      }
      case Sequence(sentence) => { // call on each part of seq
        for(e <- sentence) {
          val (newBod, newOpts, newBound, newInd) = firstRender(e, g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
          bodSoFar = newBod
          optsSoFar = newOpts
          boundVarsSoFar = newBound
          position = newInd
        }
        (bodSoFar, optsSoFar, boundVarsSoFar, position)
      }
      case Choice(choices) => { // call on each Choice but only append correct one
        position += 1
        for (e <- choices) {
          var doApp = false
          if (doAppend && scope(position) == e) { // if scope points to this option we're on a valid branch // pos - 1?
            doApp = true
          }

          val (newBod, newOpts, newBound, newInd) = firstRender(e, g, scope, bodSoFar, optsSoFar, doApp, position, boundVarsSoFar)
          bodSoFar = newBod
          optsSoFar = newOpts
          boundVarsSoFar = newBound
          position = newInd
        }
        (bodSoFar, optsSoFar, boundVarsSoFar, position)
      }
      case OptionProduction(text) => {
        val (newOpt, newBound, newInd) = renderHelper(text, g, scope, Array[Expression](), doAppend, position, boundVarsSoFar) // pos or index?
        boundVarsSoFar = newBound
        position = newInd
        (bodSoFar, optsSoFar :+ newOpt, boundVarsSoFar, position)
      }
      case Binding(nt) => {
        var startArr = Array[Expression]() // save expression list before adding binding so we know what was added
        for(e <- bodSoFar) startArr = startArr :+ e

        if (!(boundVarsSoFar contains nt)) { // haven't seen this binding before
          val (newBod, newOpts, newBound, newInd) = firstRender(g(nt), g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
          val added: Array[Expression] = newBod.diff(startArr)// figure out what was added

          val toAdd = new StringBuilder()
          for(e <- added) { // Bindings cannot map to Functions for now
            assert(e.isInstanceOf[Terminal])
            toAdd.append(e.asInstanceOf[Terminal].toText)
          }

          bodSoFar = newBod
          optsSoFar = newOpts
          boundVarsSoFar = newBound //+ (nt -> toAdd)
          boundVarsSoFar += (nt -> toAdd.toString())
          position = newInd
          // val added: String = newStr.toString.toSeq.diff(startStr).unwrap// figure out what was added
        } else { // if have seen, look up binding and add to instance
          bodSoFar = bodSoFar :+ term(boundVarsSoFar(nt)) //instanceSoFar.append(boundVarsSoFar(nt))
        }
        (bodSoFar, optsSoFar, boundVarsSoFar, position)
      }
    }
  }

  // Generates a String representing the question body and an Array of Strings representing the question options
  def secondRender(bodArr: Array[Expression], optsArr: Array[Array[Expression]], bindingMap: Map[Name, String]): (String, Array[String]) = {
    val bod = secondRenderHelper(bodArr, bindingMap)
    val opts = optsArr.map(secondRenderHelper(_, bindingMap))
    (bod, opts)
  }

  // Helper method to generate a string from the Expression arrays by appending Terminals and calling Functions
  def secondRenderHelper(instArr: Array[Expression], bindingMap: Map[Name, String]): String = {
    val instance = new StringBuilder
    for(e <- instArr) {
      e match {
        case Terminal(value) => {
          instance.append(value)
        }
        case Function(_, param, _) => {
          instance.append(e.asInstanceOf[Function].runFun(bindingMap(param)))
        }
      }
    }
    instance.toString()
  }

  // Helper method to generate a String for an Expression
  // Returns the current string being generated, the index we're at in the grammar, and a Map for bindings from their Name to the String generated
  // Mostly used to generate OptProds
  def renderHelper(expr: Expression, g: Grammar, scope: Scope, generating: Array[Expression], doAppend: Boolean, index: Int, boundVars: Map[Name, String]): (Array[Expression], Map[Name, String], Int) = {
    var instanceSoFar = generating
    var boundVarsSoFar = boundVars
    var position = index

    expr match {
      case Ref(nt) => {
        renderHelper(g(nt), g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
      }
      case t: Terminal => { // append if we're on the right branch
        if(doAppend) instanceSoFar = instanceSoFar :+ t
        (instanceSoFar, boundVarsSoFar, position)
      }
      case f: Function => {
        if(doAppend) instanceSoFar = instanceSoFar :+ f
        (instanceSoFar, boundVarsSoFar, position)
      }
      case OptionProduction(text) => { // if hit another OP, terminate
        renderHelper(text, g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
      }
      case Sequence(sentence) => { // call on each part of seq
        for(e <- sentence) {
          val (newInst, newBound, newInd) = renderHelper(e, g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
          instanceSoFar = newInst
          boundVarsSoFar = newBound
          position = newInd
        }
        (instanceSoFar, boundVarsSoFar, position)
      }
      case Choice(choices) => { // call on each Choice but only append correct one
        position += 1
        for (e <- choices) {
          var doApp = false
          if (doAppend && scope(position) == e) { // if scope points to this option we're on a valid branch // pos - 1?
            doApp = true
          }

          val (newInst, newBound, newInd) = renderHelper(e, g, scope, instanceSoFar, doApp, position, boundVarsSoFar)
          instanceSoFar = newInst
          boundVarsSoFar = newBound
          position = newInd
        }
        (instanceSoFar, boundVarsSoFar, position)
      }

      case Binding(nt) => {
        var startArr = Array[Expression]() // save expression list before adding binding so we know what was added
        for(e <- instanceSoFar) startArr = startArr :+ e

        if (!(boundVarsSoFar contains nt)) { // haven't seen this binding before
          val (newInst, newBound, newInd) = renderHelper(g(nt), g, scope, instanceSoFar, doAppend, position, boundVarsSoFar)
          val added: Array[Expression] = newInst.diff(startArr)// figure out what was added

          val toAdd = new StringBuilder()
          for(e <- added) { // Bindings cannot map to Functions for now
            assert(e.isInstanceOf[Terminal])
            toAdd.append(e.asInstanceOf[Terminal].toText)
          }

          instanceSoFar = newInst
          boundVarsSoFar = newBound //+ (nt -> toAdd)
          boundVarsSoFar += (nt -> toAdd.toString())
          position = newInd
          // val added: String = newStr.toString.toSeq.diff(startStr).unwrap// figure out what was added
        } else { // if have seen, look up binding and add to instance
          instanceSoFar = instanceSoFar :+ term(boundVarsSoFar(nt)) //instanceSoFar.append(boundVarsSoFar(nt))
        }
        (instanceSoFar, boundVarsSoFar, position)
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
    (body, opts)
  }

  // Helper method to print an instance
  def prettyPrintInstance(body: String, opts: Array[String]): Unit = {
    println(body)
    opts.foreach(println(_))
    println()
  }

  // Generates examples of a provided grammar. Generates (limit) examples, or all of them if no limit is supplied.
  def dryRun(g: Grammar, depth: Int, limit: Option[Int]): Array[(String, Array[String])] = {
    var generated = Array[(String, Array[String])]()

    val expandedG = expand(g, depth)
    val bases = Rank.generateBases(expandedG)

    val numToGenerate: Int = { // figure out how many instances to generate
      limit match {
        case Some(i) => i
        case None => bases.product
      }
    }

    // Generate each instance and append
    for (i <- 0 until numToGenerate) {
      val assignment = Rank.unrank(i, bases)
      val scope = Bind.bind(expandedG, assignment)
      generated = generated :+ renderInstance(scope, expandedG)
    }

    generated
  }

  def main(args: Array[String]): Unit = {
    // Linda!
    val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    val articles = Map[String,String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )

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
        term("Dan"),
        term("Emmie"),
        term("Xavier the bloodsucking spider")
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
        term("almond paste mixer"),
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

    val (body1, opts1) = buildInstance(lindaG, 435245353, 2)
    println(prettyPrintInstance(body1, opts1))
    //val expLindaG = expand(lindaG, 2)
//    //println(Expand.prettyPrint(expLindaG))
    //val lindaBases = Rank.generateBases(expLindaG)
//    //println("Linda: ")
//    //println("bases: " + lindaBases.mkString(" "))
//    val lindaAssignment = Rank.unrank(0, lindaBases)
//    //println("assignment: " + lindaAssignment.mkString(" "))
//    val lindaScope = Bind.bind(expLindaG, lindaAssignment)
//    //println(Bind.prettyPrintScope(lindaScope))
//    val (body, opts) = renderInstance(lindaScope, expLindaG)
//    prettyPrintInstance(body, opts)
//    val variation = Rank.rank(Array(3, 3, 5, 0, 4, 1, 2), lindaBases) // todo make build method to take array
//    val (body1, opts1) = buildInstance(lindaG, variation, 2)
//    prettyPrintInstance(body1, opts1)
    //val instances: Array[(String, Array[String])] = dryRun(lindaG, 2, None)
//    for(e <- instances){
//      e match {
//        case (bod, opts) => {
//          prettyPrintInstance(bod, opts)
//        }
//      }
//    }
  }
}