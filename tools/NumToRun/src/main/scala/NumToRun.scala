import edu.umass.cs.automan.core.policy.aggregation.AgreementSimulation

object NumToRun extends App {
  val choices = args(0).toInt
  var trials = args(1).toInt
  var agree = args(2).toInt
  var conf = 0.0
  
  val NUMSIMS = 10000
  val target_conf = 0.95
  
  while (conf < target_conf) {
    conf = AgreementSimulation.confidenceOfOutcome(choices, trials, agree, NUMSIMS)
    
    if (conf < target_conf) {
      trials += 1
      agree += 1
    }
  }
  
  println(trials + " are needed to answer a " + choices + "-option question where " + agree + " people initially agree for a confidence value of " + conf + ".")
}