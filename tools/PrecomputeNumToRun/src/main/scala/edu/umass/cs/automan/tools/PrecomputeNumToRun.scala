package edu.umass.cs.automan.tools

import java.io.{ObjectOutputStream, FileOutputStream}
import edu.umass.cs.automan.core.policy.aggregation.{PrecompTable, AdversarialPolicy}

object PrecomputeNumToRun extends App {
  val output_filename = "PossibilitiesTable.dat"
  val num_possibilities = 1000
  val num_rewards = 25

  val table = new PrecompTable(num_possibilities, num_rewards)

  // compute table
  for (np <- 2 to (num_possibilities + 1);
       reward_cents <- 1 to num_rewards
      ) {
    val q = new StubQuestion(np)
    val policy = new AdversarialPolicy(q)

    val reward: BigDecimal =
      ( BigDecimal(reward_cents)
        / BigDecimal(100)
      ).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
    val ntr: Int = policy.num_to_run_fallback(Nil, 0, reward)

    table.addEntry(np, reward, ntr)
  }

  // serialize
  val os = new ObjectOutputStream(new FileOutputStream(output_filename))
  os.writeObject(table)
  os.close()
}
