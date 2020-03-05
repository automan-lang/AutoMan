package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Production
import edu.umass.cs.automan.core.grammar.Grammar

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object Ranking {

  def product(l: Array[Int]): Int = {
    var toRet = 1
    if (l.length != 0) { //todo redundant?
      for (e <- l) {
        toRet *= e
      }
    }
    toRet

    //    if(l.length == 0) 1
    //    else l(0) * product(l.slice(1, l.length - 1))
  }

  /**
    * Generates bases for a given grammar for use in the mixed radix function.
    * This uses a breadth-first approach.
    * Assumes all Options associated with a Name.
    * @param g The grammar
    * @return The bases
    */
  def newGenerateBases(g: Grammar): List[Int] = {
    val q: mutable.Queue[Production] = new mutable.Queue[Production]()
    var bases: List[Int] = List[Int]() // the bases we're generating
    var counted: Set[String] = Set[String]() // the counted names
    var curSym: String = g.startSymbol

    var baseMap: Map[String, Int] = Map[String, Int]() // a map to keep track of which bases map to which strings

    counted = counted + curSym
//    // get first sequence
    val firstSeq = getSeq(g, curSym)
    println(firstSeq)
    //q += firstSeq
    q.enqueue(firstSeq)

    while(!q.isEmpty) {
      val samp: Production = q.dequeue()
      samp match {
        // We'll hit a Sequence first and in any Options. Enqueue all its children.
        case seq: Sequence => {
          println("first elem " + seq.sampleSpec(0))
          for(n <- seq.getList()) {
            q.enqueue(n)
            println(s"Enqueuing ${n.sample()} 52")
          }
        }
          // need to get symbol associated with Name and go from there
        case name: Name => {
          curSym = name.sample() // current name
          println(s"sampling ${curSym}")
          val prod = g.rules.get(curSym)
          prod match {
            case Some(p) => p match {
              // count choice if haven't seen yet
              case choice: Choices => {
                if (choice.isLeafNT(counted, curSym) && !counted.contains(curSym)) {
                  counted = counted + curSym
                  //bases :+ choice.count(g, counted)
                  val newBase = choice.count(g, counted)
                  println(s"${curSym} has base ${newBase}")
                  baseMap += curSym -> newBase
                  //bases = bases :+ newBase
                } else if (!counted.contains(curSym)) { // only re-enqueue if not LNT and haven't counted it
                  q.enqueue(name)
                  println(s"Enqueuing ${curSym} 69") // this is repeating
                }
              }
              // enqueue opt sequences
              case opt: OptionProduction => {
                //val optSeq = getSeq(g, opt.getText())
                val optSeq = opt.getText()
                assert(optSeq.isInstanceOf[Sequence])
                for (n <- optSeq.asInstanceOf[Sequence].getList()) {
                  q.enqueue(n)
                  println(s"Enqueuing ${curSym} 79")
                }
              }
            }
          }
        }
        case func: Function => { // todo condense with name case
          curSym = func.sample() // current name
          val prod = g.rules.get(curSym)
          prod match {
            case Some(p) => p match {
              // count choice if haven't seen yet
              case choice: Choices => {
                if (choice.isLeafNT(counted, curSym) && !counted.contains(curSym)) {
                  counted = counted + curSym
                  val newBase = choice.count(g, counted)
                  println(s"${curSym} has base ${newBase}")
                  baseMap += curSym -> newBase
                  //bases = bases :+ newBase
                } else {
                  q.enqueue(choice)
                }
              }
              // enqueue opt sequences
              case opt: OptionProduction => {
                //val optSeq = getSeq(g, opt.getText())
                val optSeq = opt.getText()
                assert(optSeq.isInstanceOf[Sequence])
                for (n <- optSeq.asInstanceOf[Sequence].getList()) {
                  q.enqueue(n)
                }
              }
            }
          }
        }
        case _ => {} // choices within OptProd going here
      }
    }

    // Add bases in order (determined by sequence)
    for(n <- firstSeq.asInstanceOf[Sequence].getList()) {
      val newBase = baseMap.get(n.sample())
      newBase match {
        case Some(n) => bases = bases :+ n
      }
      //bases = bases :+ baseMap.get(newBase)
    }
    bases
  }

  private def getSeq(g: Grammar, symbol: String): Production = {
    var seq: Production = null
    val firstName = g.rules.get(symbol)
    firstName match {
      case Some(n) => {
        val seqOpt = g.rules.get(n.sample())
        seqOpt match {
          case Some(s) => {
            seq = s
          }
        }
      }
    }
    assert(seq.isInstanceOf[Sequence])
    seq
  }

  /**
    * Generates bases for a given grammar for use in the mixed radix function.
    * Assumes all Options associated with a Name.
    * @param g The grammar
    * @param bases The list of bases so far
    * @param counted The counted names so far
    * @return A tuple of the bases and counted symbols
    */
  def generateBases(g: Grammar, bases: List[Int], counted: Set[String]): (List[Int], Set[String]) = {
    var soFarBases: List[Int] = bases
    var soFarCounted: Set[String] = counted

    val samp: Option[Production] = g.rules.get(g.curSymbol)
    samp match {
      case Some(s) => {
        s match {
          // This should only trigger when we get the start symbol
          case name: Name => {
            g.curSymbol = name.sample()
            generateBases(g, bases, counted)
          }
          // We'll hit a Sequence first and in any Options
          case seq: Sequence => {
            for(n <- seq.getList()) {
              n match {
                case name: Name => { // Sample name and run again
                  val toSamp = name.sample()
                  if(!soFarCounted.contains(toSamp)) { // only count if haven't seen it yet
                    soFarCounted = soFarCounted + toSamp // todo helper function?
                    g.curSymbol = toSamp
                    val (newBase, newCount) = generateBases(g, soFarBases, soFarCounted)
                    soFarBases = newBase
                    soFarCounted = newCount
                  }
                }
                case func: Function => { // todo combine with above if possible
                  val toSamp = func.sample()
                  if(!soFarCounted.contains(toSamp)) { // only count if haven't seen it yet
                    soFarCounted = soFarCounted + toSamp
                    g.curSymbol = toSamp
                    val (newBase, newCount) = generateBases(g, soFarBases, soFarCounted)
                    soFarBases = newBase
                    soFarCounted = newCount
                  }
                }
                case p: Production => {} // Nothing else counts
              }
            }
            (soFarBases, soFarCounted)
          }
          // If we've hit a Choices, we count those and add it to the bases
          case choice: Choices => {
            (soFarBases ++ List[Int](choice.count(g, soFarCounted)), soFarCounted)
          }
          // We came to an OptionProd through a Name
          case opt: OptionProduction => { // we have to get inside the option production
            val internalProd = opt.getText()
            internalProd match {
              case seq: Sequence => { // there should be a sequence inside every OptionProduction
                for(n <- seq.getList()) {
                  n match {
                    case name: Name => { // only thing we have to worry about here is names and functions
                      val toSamp = name.sample()
                      if(!soFarCounted.contains(toSamp)) { // only count if haven't seen it yet
                        soFarCounted = soFarCounted + toSamp
                        g.curSymbol = toSamp
                        val (newBases, newCount) = generateBases(g, soFarBases, soFarCounted)
                        soFarBases = newBases
                        soFarCounted = newCount
                      }
                      (soFarBases, soFarCounted)
                    }
                    case func: Function => { // todo deal with this
                      val toSamp = func.sample()
                      if(!soFarCounted.contains(toSamp)) { // only count if haven't seen it yet
                        soFarCounted = soFarCounted + toSamp
                        g.curSymbol = toSamp
                        val (newBases, newCount) = generateBases(g, soFarBases, soFarCounted)
                        soFarBases = newBases
                        soFarCounted = newCount
                      }
                      (soFarBases, soFarCounted)
                    }
                    case p: Production => { (soFarBases, soFarCounted) }
                  }
                }
                (soFarBases, soFarCounted)
              }
              case p: Production => { throw new Error("There should be a Sequence inside ever OptionProduction.")}
            }
          }
        }
      }
      case None => throw new Error(s"${g.curSymbol} could not be found.")
    }

  }

  // rank based on array
  def rank(vals: Array[Int], bases: Array[Int]): Int = {
    var toRet = 0
    for(i <- 0 to vals.length - 1){
      toRet += vals(i)*product(bases.slice(i+1,bases.length))
    }
    toRet
  }

  // unrank to array from int
  def unrank(rank: Int, bases: Array[Int]): ArrayBuffer[Int] = {
    var toRet: ArrayBuffer[Int] = new ArrayBuffer[Int]()
    for(i <- 0 to bases.length - 1){
      toRet += rank/product(bases.slice(i+1, bases.length))%bases(i)
    }
    toRet
  }

  // given a grammar, an int, and the bases, prints an experiment instance
//  def renderInstance(grammar: Grammar, choice: Int, bases: Array[Int]): Unit = {
//    val assignment = unrank(choice, bases) // get the assignment from the number
//    println(s"Assignment: ${assignment}")
//    grammar.curSymbol = grammar.startSymbol
//    val scope = grammar.bind(assignment.toArray, 0, Set())
//    grammar.curSymbol = grammar.startSymbol // TODO make this less ugly
//    grammar.render(grammar, scope)
//    println()
//  }

//  def newBuildInstance(grammar: Grammar, choice: Int, bod: TextProduction, opts: List[OptionProduction]): (StringBuilder, List[StringBuilder]) = {
//    grammar.curSymbol = grammar.startSymbol
//    val bases = generateBases(grammar, List[Int](), Set[String]()).toArray // todo only does for first sequence... make by sequence?
//    val assignment = unrank(choice, bases) // get the assignment from the number
//    grammar.curSymbol = grammar.startSymbol // TODO mak sure not resetting for options
//    val scope = grammar.bind(assignment.toArray, 0, Set())
//    grammar.curSymbol = grammar.startSymbol
//  }

  // given a grammar, an int, and the bases, creates a string of an experiment instance
  def buildInstance(grammar: Grammar, choice: Int): (StringBuilder, List[StringBuilder]) = {
    grammar.curSymbol = grammar.startSymbol
    val (bases, counted) = generateBases(grammar, List[Int](), Set[String]()) // todo fix for options
    //println(bases)
    val assignment = unrank(choice, bases.toArray) // get the assignment from the number todo fix
    //println(assignment)
    grammar.curSymbol = grammar.startSymbol // TODO make sure not resetting for options
    val (scope, p, s) = grammar.bind(assignment.toArray, 0, Set())
    grammar.curSymbol = grammar.startSymbol
    val (bod, opts) = grammar.buildQandOpts(scope, new StringBuilder, ListBuffer[StringBuilder](), new StringBuilder, true)
    (bod, opts.toList)
    //buildString(grammar, scope, new StringBuilder).toString()
  }

  def main(args: Array[String]): Unit = {
    val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    // TODO: what if multiple params need same function?
    val articles = Map[String, String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )
    // The problem statement
    val lindaBody = new Sequence(
      List(
        new Name("Name"),
        new Terminal(" is "),
        new Name("Age"),
        new Terminal(" years old, single, outspoken, and very bright. "),
        new Function(pronouns, "Name", true),
        new Terminal(" majored in "),
        new Name("Major"),
        new Terminal(". As a student, "),
        new Function(pronouns, "Name", false),
        new Terminal(" was deeply concerned with issues of "),
        new Name("Issue"),
        new Terminal(", and also participated in "),
        new Name("Demonstration"),
        new Terminal(" demonstrations.\nWhich is more probable?\n"),
        new Name("a"),
        new Name("b")
        //new Name("lindaOpt1"),
        //new Name("lindaOpt2")
      )
    )
    val opt1: OptionProduction =
      new OptionProduction(
        new Sequence(
          List(
            new Name("Name"),
            new Terminal(" is "),
            new Function(articles, "Job", false),
            new Terminal(" "),
            new Name("Job"),
            new Terminal(".")
          ))
      )
    val opt2: OptionProduction =
      new OptionProduction(
        new Sequence(
          List(
            new Name("Name"),
            new Terminal(" is "),
            new Function(articles, "Job", false),
            new Terminal(" "),
            new Name("Job"),
            new Terminal(" and is active in the "),
            new Name("Movement"),
            new Terminal(" movement.")
          ))
      )

    val grammar = Grammar( // The grammar
      Map(
        "Start" -> new Name("lindaS"),
        "lindaS" -> lindaBody,
        "Name" -> new Choices(
          List(
            new Terminal("Linda"),
            new Terminal("Dan"),
            new Terminal("Emmie"),
            new Terminal("Xavier the bloodsucking spider")
          )
        ),
        "Age" -> new Choices(
          List(
            new Terminal("21"),
            new Terminal("31"),
            new Terminal("41"),
            new Terminal("51"),
            new Terminal("61")
          )
        ),
        "Major" -> new Choices(
          List(
            new Terminal("chemistry"),
            new Terminal("psychology"),
            new Terminal("english literature"),
            new Terminal("philosophy"),
            new Terminal("women's studies"),
            new Terminal("underwater basket weaving")
          )
        ),
        "Issue" -> new Choices(
          List(
            new Terminal("discrimination and social justice"),
            new Terminal("fair wages"),
            new Terminal("animal rights"),
            new Terminal("white collar crime"),
            new Terminal("unemployed circus workers")
          )
        ),
        "Demonstration" -> new Choices(
          List(
            new Terminal("anti-nuclear"),
            new Terminal("anti-war"),
            new Terminal("pro-choice"),
            new Terminal("anti-abortion"),
            new Terminal("anti-animal testing")
          )
        ),
        "Job" -> new Choices(
          List(
            new Terminal("bank teller"),
            new Terminal("almond paste mixer"),
            new Terminal("tennis scout"),
            new Terminal("lawyer"),
            new Terminal("professor")
          )
        ),
        "Movement" -> new Choices(
          List(
            new Terminal("feminist"),
            new Terminal("anti-plastic water bottle"),
            new Terminal("pro-pretzel crisp"),
            new Terminal("pro-metal straw"),
            new Terminal("environmental justice")
          )
        ),
        "a" -> opt1,
        "b" -> opt2
      ), "Start",
      7
    )

    val practiceFunc = Map[String, String](
      "an ox" -> "an OXEN weighs ",
      "an ocarina" -> "an OCARINA weighs ",
      "an obelisk" -> "an OBELISK weighs "
    )

    val qSeq = new Sequence(
      List(
        new Terminal("How much does "),
        new Name("Object"),
        new Terminal(" weigh?"),
        new Name("a"),
        new Name("b"),
        new Name("c")
      )
    )
    val optASeq = new Sequence(
      List(
        //new Terminal("An "),
        new Name("Object"),
        new Terminal(" weighs 1 lb")
      )
    )
    val optBSeq = new Sequence(
      List(
        new Function(practiceFunc, "Object", true),
        new Terminal("1,000 lb")
      )
    )

    val estGrammar: Grammar = new Grammar(
      Map(
        "Start" -> new Name("Seq"),
        "Seq" -> qSeq,
        "Object" -> new Choices(
          List(
            new Terminal("an ox"),
            new Terminal("an ocarina"),
            new Terminal("an obelisk")
          )
        ),
        "a" -> new OptionProduction(new Terminal("1 lb")),//optASeq),
        "b" -> new OptionProduction(new Terminal("1,000 lb")),//optBSeq),
        "c" -> new OptionProduction(new Terminal("10,000 lb"))
        //"Options" -> optSeq // we need a name here
      ),
      "Start",
      7
    )
    //val estProd: EstimateQuestionProduction = new EstimateQuestionProduction(estGrammar, qSeq)
    val cbProd: CheckboxQuestionProduction = new CheckboxQuestionProduction(estGrammar, qSeq) // todo is opts necessary? may have made totext method too complicated

    val recSeq: Sequence = new Sequence(
      List(
        new Name("a"),
        new Name("b")
      )
    )

    val recGrammar: Grammar = Grammar(
      Map(
        "Start" -> new Name("Seq"),
        "Seq" -> recSeq,
        "a" -> new Choices(
          List(
            new Sequence(
              List(
                new Name("b"),
                new Name("a")
              )
            )
          )
        ),
        "b" -> new Choices(
          List(
            new Terminal("1"),
            new Terminal("2"),
            new Terminal("3")
          )
        )
      ),
      "Start",
      7
    )

    val recGrammar2: Grammar = Grammar(
      Map(
        "Start" -> new Name("Seq"),
        "Seq" -> recSeq,
        "a" -> new Choices(
          List(
            new Sequence(
              List(
                new Name("a"),
                new Name("b")
              )
            )
          )
        ),
        "b" -> new Choices(
          List(
            new Terminal("1"),
            new Terminal("2"),
            new Terminal("3")
          )
        )
      ),
      "Start",
      7
    )

   // println(cbProd.toQuestionText(0))
//    val (bod, opts) = cbProd.toQuestionText(0)
//    println(bod)
//    opts.map(println(_))

    //println(generateBases(recGrammar, List[Int](), Set[String]())._1)
    //recGrammar.curSymbol = recGrammar.startSymbol
    println(s"New GB recursive: ${newGenerateBases(recGrammar)}")
    println(s"New GB recursive: ${newGenerateBases(recGrammar2)}")
//    println(generateBases(grammar, List[Int](), Set[String]())._1)
//    println(s"New GB Linda: ${newGenerateBases(grammar)}")
//    println(rank(Array(0,1,3,0,0,0,0), Array(4,5,6,5,5,5,5)))
//    println(rank(Array(1,2), Array(4,3)))
//    println(unrank(rank(Array(1,2), Array(4,3)), Array(4,3)))

//    val lindaBase = generateBases(grammar, List[Int](), Set[String]())
//    println("Testing testBases method: " + lindaBase)
//    //val lindaBases = Array(4, 5, 6, 5, 5, 5, 5) // each number is number of possible assignments for that slot
//
//    val lindaBases = lindaBase.toArray
//    val total = product(lindaBases)
//
//    println(s"total: ${total}")
//
//    val xRank = rank(Array(3,3,4,4,2,2,3), lindaBases) // xavier is 70563
//    //println(xRank)
//
//    val lRank = rank(Array(0,1,1,0,0,0,0), lindaBases) // the ranking for the classic Linda problem
////    println(lRank) // 4375
////    println(unrank(lRank, lindaBases))
//
//    //val grammar = getGrammar()
//    // if it starts acting up reset start symbol
////    renderInstance(grammar, lRank, lindaBases)
////    println(s"Build version:\n${buildInstance(grammar, lRank)}")
////    renderInstance(grammar, xRank, lindaBases)
////    println(s"Build version:\n${buildInstance(grammar, xRank)}")
//
//    //grammar.curSymbol = grammar.startSymbol
//    val prod: EstimateQuestionProduction = new EstimateQuestionProduction(grammar, lindaBody)
//    println(prod.toQuestionText(0))
//    grammar.curSymbol = grammar.startSymbol
//    println(prod.toQuestionText(4375))
  }
}
