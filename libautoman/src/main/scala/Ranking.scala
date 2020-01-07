import scala.collection.mutable.ArrayBuffer

object Ranking {
  def product(l: Array[Int]): Int = {
    var toRet = 1
    if (l.length != 0) { //todo redundant?
      //toRet = 1
      for (e <- l) {
        toRet *= e
      }
    }
    toRet

    //    if(l.length == 0) 1
    //    else l(0) * product(l.slice(1, l.length - 1))
  }

  def rank(vals: Array[Int], bases: Array[Int]): Int = {
    var toRet = 0
    for(i <- 0 to vals.length - 1){
      toRet += vals(i)*product(bases.slice(i+1,bases.length - 1))
    }
    toRet
  }

  //todo something funky happening here
  def unrank(rank: Int, bases: Array[Int]): ArrayBuffer[Int] = {
    var toRet: ArrayBuffer[Int] = new ArrayBuffer[Int]()
    for(i <- 0 to bases.length - 1){
      toRet += rank/product(bases.slice(i+1, bases.length - 1))%bases(i) // todo: make sure order of operations ok
    }
    toRet
  }

  def main(args: Array[String]): Unit = {
    val bases = Array(1, 2, 3, 4)
    val total = product(bases)

    println(s"total: ${total}")

    for (i <- 0 to total) {
      var values = unrank(i, bases)
      var r = rank(values.toArray, bases)
      //assert(i == r)
      println(s"${values} ${r}")
    }
  }
}
