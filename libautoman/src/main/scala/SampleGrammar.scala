import scala.collection.immutable.HashMap
import scala.util.Random

trait Production {
  def sample(): String
}

class Choices(options: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    options(ran.nextInt(options.length)).sample()
  }
}

class Terminal(word: String) extends Production {
  override def sample(): String = {
    //print(word)
    word
  }
}

class NonTerminal(sentence: List[Production]) extends Production {
  override def sample(): String = {
    val ran = new Random()
    sentence(ran.nextInt(sentence.length)).sample()
  }
  def getList(): List[Production] = sentence
}

class Name(n: String) extends Production {
  override def sample(): String = n // sample returns name for further lookup
  //override def sample(): String = nameMap(n).sample() // or sample(p)?
  //def getName(): String = n
  //def getMap(): Map[String, Production] = nameMap
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
              //sample(g, n)
              //print(n.sample())
            }
          }
        }
      }
      case None => throw new Exception("Symbol could not be found")
    }

    //    //if(isFirst) {
    //      val firstSamp: Option[Production] = g get "Start"
    //      firstSamp
    ////      firstSamp match {
    ////        case Some(firstSamp) => {
    ////          //println(prod.sample()) // Sampling G, a nonterminal
    ////          firstSamp
    ////        }
    ////        case None => throw new Exception("Need a start indicator")
    ////      }
    //      //println(firstSamp.sample())
    //    } else if(curProd.isInstanceOf[Name]){
    //      val prodMap = curProd.asInstanceOf[Name].getMap()
    //      val nonTerm = prodMap(startSymbol)
    //
    //      for (e <- nonTerm.asInstanceOf[NonTerminal].getList()) {
    //        print(e.sample)
    //      }
    //      Some(nonTerm)
    //    } else {
    //        None
    //      }
    //    }
    //curProd
    //    else {
    //      for(item <- g){
    //        item.sample()
    //      }
    //    }

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
    //print(sample(grammar, "Start"))

    //val startProd: Option[Production] = sample(grammar, "Start")
    //for(e <- G.getList()) print(e.sample())
    //println()
    //sample(grammar, startProd.asInstanceOf[Name].getName(), startProd)
    //startProd.sample()
  }
}
