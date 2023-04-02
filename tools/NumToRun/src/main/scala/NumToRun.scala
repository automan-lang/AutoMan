import org.automanlang.core.policy.aggregation.AgreementSimulation

object NumToRun extends App {
  val NUMSIMS = 10000
  val target_conf = 0.95

  if (args.length != 3) {
    System.out.println("Usage: sbt \"run <# choices> <# trials> <# agree>\"")
    System.exit(1)
  }

  val choices = args(0).toInt
  var trials = args(1).toInt
  var agree = args(2).toInt
  var current_conf = 0.0

  while (current_conf < target_conf) {
    current_conf = AgreementSimulation.confidenceOfOutcome(choices, trials, agree, NUMSIMS)

    if (current_conf < target_conf) {
      trials += 1
      agree += 1
    }
  }

  println(
    trials +
    " trials are needed to answer a " +
    choices +
    "-option question where at least " +
    agree +
    " people initially agree for a confidence value of " +
    current_conf + "."
  )
}