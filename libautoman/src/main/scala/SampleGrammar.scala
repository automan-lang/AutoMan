import scala.collection.immutable.HashMap
import scala.util.Random

trait Production {
  def sample(): String
  def count(): Int
}

class Choices(options: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    options(ran.nextInt(options.length)).sample()
  }
  override def count(): Int = {
    var c = 0
    for (e <- options) c += e.count() // choices are additive
    c
  }
}

class Terminal(word: String) extends Production {
  override def sample(): String = {
    word
  }
  override def count(): Int = 1
}

class NonTerminal(sentence: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    sentence(ran.nextInt(sentence.length)).sample()
  }
  override def count(): Int = {
    var c = 0
    for (e <- sentence) c *= e.count() // nonterminals are multiplicative
    c
  }
  def getList(): List[Production] = sentence
}

class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  override def count(): Int = 0 //count() // want to look up what it's associated with but how?
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

  def count(g: Map[String, Production], startSymbol: String, soFar: Int): Int = {
    val samp: Option[Production] = g get startSymbol // get Production associated with symbol from grammar
    samp match {
      case Some(samp) => {
        samp match {
          case name: Name => count(g, name.sample(), soFar) // Name becomes start symbol, no count incremented
          case term: Terminal => term.count()
          case choice: Choices => choice.count()
          case nonterm: NonTerminal => nonterm.count()
        }
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
    println()
    println("Count: " + G.count())
    println("count method: " + count(grammar, "Start", 0))
    //print(sample(grammar, "Start"))

    //val startProd: Option[Production] = sample(grammar, "Start")
    //for(e <- G.getList()) print(e.sample())
    //println()
    //sample(grammar, startProd.asInstanceOf[Name].getName(), startProd)
    //startProd.sample()
  }
}
