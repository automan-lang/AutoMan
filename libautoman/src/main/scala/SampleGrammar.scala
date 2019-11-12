import scala.collection.immutable.HashMap
import scala.util.Random

trait Production {
  def sample(): String
  def count(g: Map[String,Production]): Int
}

class Choices(options: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    options(ran.nextInt(options.length)).sample()
  }
  override def count(g: Map[String,Production]): Int = {
    var c: Int = 0
    for (e <- options) {
      println(s"adding ${e.count(g)}")
      c = c + e.count(g)
    } // choices are additive
    c
  }
}

class Terminal(word: String) extends Production {
  override def sample(): String = {
    word
  }
  override def count(g: Map[String,Production]): Int = 1
}

class NonTerminal(sentence: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    sentence(ran.nextInt(sentence.length)).sample()
  }
  override def count(g: Map[String,Production]): Int = {
    var c: Int = 1 // TODO: is this ok?
    for (e <- sentence){
      c = c*e.count(g)
//      e match {
//        case name: Name => c = c*name.count(g)
//        case term: Terminal => c = c*term.count(g)
//        case choice: Choices => c = c*choice.count(g)
//        case nonterm: NonTerminal => c = c*nonterm.count(g)
//      }
       // need to call count(params) bc otherwise names just default to 0
      // but can't access count here
      //println("counting nonterminal: " + c)
    } // nonterminals are multiplicative
    c
  }
  def getList(): List[Production] = sentence
}

class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  def count(g: Map[String,Production]): Int = {
    g(this.sample()).count(g)
  } //count() // want to look up what it's associated with but how?
}


object SampleGrammar {
  def sample(g: Map[String,Production], startSymbol: String): Unit = { //, soFar: String): String = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol
    val samp: Option[Production] = g get startSymbol // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => sample(g, name.sample()) // Name becomes start symbol
          case term: Terminal => print(term.sample())
          case choice: Choices => print(choice.sample())
          case nonterm: NonTerminal => {
            for(n <- nonterm.getList()) {
              n match {
                case name: Name => sample(g, name.sample())
                case p: Production => print(p.sample())
              }
            }
          }
        }
      }
      case None => throw new Exception("Symbol could not be found")
    }
  }

  var counter = 0;
  def count(g: Map[String, Production], startSymbol: String): Unit = {
    val samp: Option[Production] = g get startSymbol // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        counter += samp.count(g)
//        samp match {
//          case name: Name => counter += name.count(g)//count(g, name.sample()) // Name becomes start symbol, no count incremented
//          case term: Terminal => counter += term.count(g)
//          case choice: Choices => counter += choice.count(g)
//          case nonterm: NonTerminal => counter += nonterm.count(g)
        //}
      }
      case None => throw new Exception("Symbol could not be found")
    }
  }

  def main(args: Array[String]): Unit = {
    val G = new NonTerminal(
      List(
        new Name("A"),//, nameMap = nameMap + ("A" -> A)),
        new Terminal(" is "),
        new Name("B"),// nameMap = nameMap + ("B" -> B)),
        new Terminal(" years old.")
      )
    )

    //nameMap += ("G" -> G)

    val grammar = {
      Map(
        "Start" -> new Name("G"),
        "G" -> G,
        "A" -> new Choices(
          List(
            new Terminal("Linda"),
            new Terminal("Dan"),
            new Terminal("Emmie")
          )
      ),
        "B" -> new Choices(
          List(
            new Terminal("21"),
            new Terminal("31"),
            new Terminal("41"),
            new Terminal("51"),
            new Terminal("61")
          )
        )
      )
    }

    sample(grammar, "Start")
    val A = new Choices(
      List(
        new Terminal("Linda"),
        new Terminal("Dan"),
        new Terminal("Emmie")
      )
    )

    val B = new NonTerminal(
      List(
        A,
        A
      )
    )

    println()
    println("A count: " + A.count(grammar))
    println("B count: " + B.count(grammar))
    println("Count: " + G.count(grammar))
    println("count method: " + count(grammar, "Start"))
    println("counter: " + counter)
    //print(sample(grammar, "Start"))

    //val startProd: Option[Production] = sample(grammar, "Start")
    //for(e <- G.getList()) print(e.sample())
    //println()
    //sample(grammar, startProd.asInstanceOf[Name].getName(), startProd)
    //startProd.sample()
  }
}
