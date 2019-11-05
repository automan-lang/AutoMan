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

class Name(n: String, nameMap: Map[String, Production]) extends Production {
  override def sample(): String = nameMap(n).sample() // or sample(p)?
  def getName(): String = n
  def getMap(): Map[String, Production] = nameMap
}


object SampleGrammar {
  def sample(g: Map[String,Production], isFirst: Boolean, startSymbol: String, curProd: Option[Production]): Option[Production] = {
    // find start
    // sample symbol associated with it
    // build string by sampling each symbol

    if(isFirst) {
      val firstSamp: Option[Production] = g get "Start"
      firstSamp
//      firstSamp match {
//        case Some(firstSamp) => {
//          //println(prod.sample()) // Sampling G, a nonterminal
//          firstSamp
//        }
//        case None => throw new Exception("Need a start indicator")
//      }
      //println(firstSamp.sample())
    } else if(curProd.isInstanceOf[Name]){
      val prodMap = curProd.asInstanceOf[Name].getMap()
      val nonTerm = prodMap(startSymbol)

      for (e <- nonTerm.asInstanceOf[NonTerminal].getList()) {
        print(e.sample)
      }
      Some(nonTerm)
    } else {
        None
      }
    }
    //curProd
      //    else {
//      for(item <- g){
//        item.sample()
//      }
//    }

  def main(args: Array[String]): Unit = {
    var nameMap = new HashMap[String, Production]
    val A = new Choices(
      List(
        new Terminal("Linda"),
        new Terminal("Dan"),
        new Terminal("Emmie")
      )
    )

    val B = new Choices(
      List(
        new Terminal("21"),
        new Terminal("31"),
        new Terminal("41"),
        new Terminal("51"),
        new Terminal("61")
      )
    )

    val G = new NonTerminal(
      List(
        new Name("A", nameMap = nameMap + ("A" -> A)),
        new Terminal(" is "),
        new Name("B", nameMap = nameMap + ("B" -> B)),
        new Terminal(" years old.")
      )
    )

    nameMap += ("G" -> G)

    val grammar = {
      Map(
        "Start" -> new Name("G", nameMap),
        "G" -> G,
        "A" -> A,
        "B" -> B
      )
    }

    val startProd: Option[Production] = sample(grammar, true, "Start", None)
    //for(e <- G.getList()) print(e.sample())
    println()
    sample(grammar, false, startProd.asInstanceOf[Name].getName(), startProd)
    //startProd.sample()
  }
}
