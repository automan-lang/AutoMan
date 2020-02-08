package edu.umass.cs.automan.core.policy.aggregation

import java.io.ObjectInputStream

object PrecompTable {
  def load(resource_name: String) : Option[PrecompTable] = {
    try {
      val is = new ObjectInputStream(getClass.getResourceAsStream(resource_name))
      val table = is.readObject().asInstanceOf[PrecompTable]
      is.close()
      Some(table)
    } catch {
      case t:Throwable => None
    }

  }
}

class PrecompTable(val possibilities_sz: Int, val reward_sz: Int) extends Serializable {
  private val _store = Array.fill[Int](possibilities_sz * reward_sz)(0)

  // np is numOpts: number of question possibilities(?)
  private def computeIndex(np: Int, reward: BigDecimal) : Int = {
    assert(np >= 2) // todo failing here

    // convert reward to cents
    val cents: Int = (reward * BigDecimal(100)).toInt

    // adjust np
    val npadj = np - 2

    // compute index (-1 is to start at zero)
    val index = (reward_sz * npadj) + cents - 1

    assert(index < possibilities_sz * reward_sz)

    index
  }

  def addEntry(np: Int, reward: BigDecimal, num_to_run: Int) : Unit = {
    assert(num_to_run != 0)
    _store(computeIndex(np, reward)) = num_to_run
  }
  def getEntryOrNone(np: Int, reward: BigDecimal) : Option[Int] = {
    if (computeIndex(np, reward) < possibilities_sz * reward_sz) {
      val output = _store(computeIndex(np, reward))
      assert(output != 0)
      Some(output)
    } else {
      None
    }
  }
}
