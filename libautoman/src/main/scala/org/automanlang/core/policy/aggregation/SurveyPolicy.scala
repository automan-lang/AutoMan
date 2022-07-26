package org.automanlang.core.policy.aggregation

import org.apache.commons.math3.stat.inference.TestUtils.tTest
import org.automanlang.core.info.QuestionType
import org.automanlang.core.logging.{DebugLog, LogLevelInfo, LogType}
import org.automanlang.core.question._
import org.automanlang.core.scheduler.Task
import org.automanlang.core.util.lapjv

class SurveyPolicy(question: FakeSurvey)
  extends SurveyVectorPolicy(question) {

  DebugLog("Policy: survey policy", LogLevelInfo(), LogType.STRATEGY, question.id)

  private[automanlang] def create_samples(sample_size: Int, radixes: Array[Double]): Array[Array[Double]] = {
    val r = new java.util.Random

    // number of question * number of sample_size. Also transposed
    radixes.map(radix => {
      Array.fill(sample_size) {
        r.nextDouble(radix)
      }
    })
  }

  private[automanlang] def earth_movers(samples1: Array[Array[Double]], samples2: Array[Array[Double]], sample_size: Int, question_types: Array[QuestionType.QuestionType], radixes: Array[Double]): Double = {
    // N*N matrix where distance[x][y] denotes distance between x-th sample in samples1 and y-th sample in samples2
    val distances = Array.ofDim[Double](sample_size, sample_size)
    // Memoizes and eliminates unnecessary computations. (z, valueInX) => Array of contributed cost (of length sample_size)
    val memoMap: scala.collection.mutable.Map[(Int, Double), Array[Double]] = scala.collection.mutable.Map()

    for (z <- question_types.indices) {
      // radio and checkbox question: distance = 1 if different else 0
      // TODO: radio => XOR distance
      if (question_types(z) == QuestionType.RadioButtonQuestion || question_types(z) == QuestionType.CheckboxQuestion) {
        // TODO: .par?
        samples1(z).zipWithIndex.foreach { case (value, x) =>
          if (memoMap.contains(z, value)) {
            distances(x) = distances(x).zip(memoMap((z, value))).map { case (left, right) => left + right }
          } else {
            val dis = samples2(z).map(v => if (v == value) 0.0 else 1.0)
            memoMap.put((z, value), dis)
            distances(x) = distances(x).zip(dis).map { case (left, right) => left + right }
          }
        }
      } else if (question_types(z) == QuestionType.EstimationQuestion) {
        // TODO: .par?
        samples1(z).zipWithIndex.foreach { case (value, x) =>
          if (memoMap.contains(z, value)) {
            distances(x) = distances(x).zip(memoMap((z, value))).map { case (left, right) => left + right }
          } else {
            val radix = radixes(z)
            val dis = samples2(z).map(v => math.abs(v - value) / radix)
            memoMap.put((z, value), dis)
            distances(x) = distances(x).zip(dis).map { case (left, right) => left + right }
          }
        }
      } else {
        throw new NotImplementedError("EMD Distance: Question Type not supported")
      }
    }

    // Run Jonker-Volgenant Algorithm to solve the minimum-cost assignment problem
    // The cost shall be the Earth Mover's Distance between two samples
    val assignment = lapjv.execute(distances)
    assignment.zipWithIndex.map { case (j, i) => distances(i)(j) }.sum
  }

  private[automanlang] def processTasks(tasks: List[Task]): (Array[Array[Double]], Array[Double]) = {
    val answers = tasks.map(task => {
      val answers = task.answer.get.asInstanceOf[question.A]
      val row: Array[Double] = answers.zipWithIndex.map { case (answer, q) =>
        val ques = question.questions(q)
        ques.getQuestionType match {
          case QuestionType.CheckboxQuestion =>
            // Symbol.to_string -> 0...n
            val optionMap = ques.asInstanceOf[CheckboxQuestion].options.
              zipWithIndex.map { case (o, index) => o.question_id.toString() -> index }.toMap

            val bitVector = answer.asInstanceOf[CheckboxQuestion#A].map(a => 1 << optionMap(a.toString()))
            bitVector.sum.toDouble
          case QuestionType.RadioButtonQuestion =>
            // Symbol.to_string -> 0...n
            val optionMap = ques.asInstanceOf[RadioButtonQuestion].options.
              zipWithIndex.map { case (o, index) => o.question_id.toString() -> index }.toMap

            optionMap(answer.asInstanceOf[RadioButtonQuestion#A].toString()).toDouble
          case QuestionType.EstimationQuestion =>
            answer.asInstanceOf[EstimationQuestion#A]
          case _ =>
            throw new NotImplementedError("SurveyPolicy: FreeTextQuestion is not supported for now.")
        }
      }.toArray
      row
    }).toArray.transpose // note that here we transpose the matrix to make it column-major

    // Ideally radixes should be Array[Int]. However EstimateQuestion may lead to radix of type Double
    val radixes: Array[Double] = question.questions.zipWithIndex.map { case (q, i) =>
      q.getQuestionType match {
        case QuestionType.CheckboxQuestion =>
          // there are 2^n options in total, where n is # of options
          (1 << q.asInstanceOf[CheckboxQuestion].options.length).toDouble
        case QuestionType.RadioButtonQuestion =>
          q.asInstanceOf[RadioButtonQuestion].options.length.toDouble
        case QuestionType.EstimationQuestion =>
          // For EQ, we should decrement everything by min value to simplify calculation
          val min = answers(i).min
          answers(i) = answers(i).map(v => v - min)
          answers(i).max
        case _ =>
          throw new NotImplementedError("SurveyPolicy: FreeTextQuestion is not supported for now.")
      }
    }.toArray

    (answers, radixes)
  }

  // Algorithm to determine if more answers are needed for the survey
  // Uses the earth-mover's distance algorithm
  private[automanlang] def survey_algorithm(question_types: Array[QuestionType.QuestionType], radixes: Array[Double], iterations: Int, sample_size: Int, test_samples: Array[Array[Double]]): Boolean = {
    val distancesRandom = Array.ofDim[Double](sample_size)
    val distancesTest = Array.ofDim[Double](sample_size)

    (0 until iterations).foreach(i => {
      val samples1 = create_samples(sample_size, radixes)
      val samples2 = create_samples(sample_size, radixes)

      // Calculate distance
      val dist_random = earth_movers(samples1, samples2, sample_size, question_types, radixes)
      distancesRandom(i) = dist_random

      val dist_test = earth_movers(samples1, test_samples, sample_size, question_types, radixes)
      distancesTest(i) = dist_test
    })

    // p-value returned by Welch's two-tailed t-Test. Does not assume equal variance.
    // Approximates degree of freedom by Welch-Satterthwaite equation.
    // Null Hypothesis: two population means are equal.
    val pValue = tTest(distancesRandom, distancesTest)
    DebugLog(s"Policy: the p-value for different means is $pValue.", LogLevelInfo(), LogType.STRATEGY, question.id)

    // 95% confidence to reject null hypothesis in favor of alternative hypothesis
    // such that the two have different means
    pValue <= 0.05
  }

  override def is_done(tasks: List[Task], num_comparisons: Int): (Boolean, Int) = {
    // What is this for?
    val done = completed_workerunique_tasks(tasks).size
    if (done < tasks.size) {
      return (false, num_comparisons)
    }

    // Do not terminate less than sample_size
    if (done < question.sample_size) {
      return (false, num_comparisons + 1)
    }

    // Turn the answers into their number representations
    val (answerMatrix, radixes) = processTasks(tasks)
    val sample_size = answerMatrix(0).length // note here that answers is transposed
    val question_types: Array[QuestionType.QuestionType] = question.questions.map(_.getQuestionType).toArray

    // Run the algorithm
    val result = survey_algorithm(question_types, radixes, iterations = 1000, sample_size, answerMatrix)

    // TODO: how much should we increase sample_size by if survey_algorithm does not pass?

    // Terminate ONLY when fulfilled at least sample_size and pass survey_algorithm of statistical significance
    (result, num_comparisons + 1)
  }

  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    // additional number needed to reach num_samples
    math.max(question.sample_size - outstanding_tasks(tasks).size - answered_tasks(tasks).size, 0)
  }
}
