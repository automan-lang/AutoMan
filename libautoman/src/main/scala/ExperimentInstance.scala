class ExperimentInstance(val param_selections: Map[Symbol, String],
                         override val text: String,
                         override val choices: List[String],
                         override val parameterization: Map[Symbol, List[String]],
                         override val budget: Double,
                         override val confidence_interval: Double
                        ) extends Experiment(text, choices, parameterization, budget, confidence_interval) {



}

object ExperimentInstanceMain {

}
