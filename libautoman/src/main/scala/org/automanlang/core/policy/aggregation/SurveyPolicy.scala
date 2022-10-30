package org.automanlang.core.policy.aggregation

import org.apache.commons.math3.stat.StatUtils
import org.automanlang.core.answer.{SurveyAnswers, SurveyTaskMetadata}
import org.automanlang.core.info.QuestionType
import org.automanlang.core.logging.{DebugLog, LogLevelInfo, LogType}
import org.automanlang.core.question._
import org.automanlang.core.scheduler.Task
import org.automanlang.core.util.{lapjv, stat}

class SurveyPolicy(question: FakeSurvey)
  extends SurveyVectorPolicy(question) {

  DebugLog("Policy: survey policy", LogLevelInfo(), LogType.STRATEGY, question.id)

  private[automanlang] def create_samples(sample_size: Int, radixes: Array[Int]): Array[Array[Int]] = {
    val r = new java.util.Random

    // number of question * number of sample_size. Also transposed
    radixes.map(radix => {
      Array.fill(sample_size) {
        r.nextInt(radix)
      }
    })
  }

  private[automanlang] def earth_movers(samples1: Array[Array[Int]], samples2: Array[Array[Int]], sample_size: Int, question_types: Array[QuestionType.QuestionType], radixes: Array[Int]): Double = {
    // N*N matrix where distance[x][y] denotes distance between x-th sample in samples1 and y-th sample in samples2
    val distances = Array.ofDim[Double](sample_size, sample_size)
    // Memoizes and eliminates unnecessary computations. (z, valueInX) => Array of contributed cost (of length sample_size)
    val memoMap: scala.collection.mutable.Map[(Int, Int), Array[Double]] = scala.collection.mutable.Map()

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
            val dis = samples2(z).map(v => math.abs(v.toDouble - value.toDouble) / radix)
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
//    val assignment = new HungarianAlgorithm(distances).execute()
    assignment.zipWithIndex.map { case (j, i) => distances(i)(j) }.sum
  }

  private[automanlang] def processTasks(tasks: List[Task]): (Array[Array[Int]], Array[Int]) = {
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
            answer.asInstanceOf[EstimationQuestion#A].toDouble
          case _ =>
            throw new NotImplementedError("SurveyPolicy: FreeTextQuestion is not supported for now.")
        }
      }.toArray
      row
    }).toArray.transpose // note that here we transpose the matrix to make it column-major

    // Ideally radixes should be Array[Int]. However EstimateQuestion may lead to radix of type Double
    val radixes: Array[Int] = question.questions.zipWithIndex.map { case (q, i) =>
      q.getQuestionType match {
        case QuestionType.CheckboxQuestion =>
          // there are 2^n options in total, where n is # of options
          1 << q.asInstanceOf[CheckboxQuestion].options.length
        case QuestionType.RadioButtonQuestion =>
          q.asInstanceOf[RadioButtonQuestion].options.length
        case QuestionType.EstimationQuestion =>
          // For EQ, we should decrement everything by min value to simplify calculation
          val q1 = StatUtils.percentile(answers(i), 0, answers(i).length, 25)
          val q3 = StatUtils.percentile(answers(i), 0, answers(i).length, 75)
          val IQR = q3 - q1

          val upper_bound = q3 + 1.5 * IQR
          val lower_bound = q1 - 1.5 * IQR

          // digitize the answers matrix for EQ
          answers(i) = answers(i).map(v => {
            if (v < lower_bound) {
              0.0
            } else if (v >= upper_bound) {
              21.0
            } else {
              // digitize into bins 1...20
              math.floor((v-lower_bound) / upper_bound * 20) + 1
            }
          })

          // 0 to 21
          22
        case _ =>
          throw new NotImplementedError("SurveyPolicy: FreeTextQuestion is not supported for now.")
      }
    }.toArray

    // Convert double to int
    (answers.map(ans => ans.map(a => a.toInt)), radixes)
  }

  // Algorithm to determine if more answers are needed for the survey
  // Uses the earth-mover's distance algorithm
  private[automanlang] def survey_algorithm(question_types: Array[QuestionType.QuestionType], radixes: Array[Int], iterations: Int, sample_size: Int, test_samples: Array[Array[Int]]): Boolean = {
    val distancesRandom = Array.ofDim[Double](iterations)
    val distancesTest = Array.ofDim[Double](iterations)

    (0 until iterations).foreach(i => {
      val samples1 = create_samples(sample_size, radixes)
      val samples2 = create_samples(sample_size, radixes)

      // Calculate distance
      val dist_random = earth_movers(samples1, samples2, sample_size, question_types, radixes)
      distancesRandom(i) = dist_random

      val dist_test = earth_movers(samples1, test_samples, sample_size, question_types, radixes)
      distancesTest(i) = dist_test
    })

    DebugLog(s"Policy: the mean distance of random vs random is ${distancesRandom.sum / distancesRandom.length}.", LogLevelInfo(), LogType.STRATEGY, question.id)
    DebugLog(s"Policy: the mean distance of test vs random ${distancesTest.sum / distancesTest.length}.", LogLevelInfo(), LogType.STRATEGY, question.id)

    // p-value returned by Welch's two-tailed t-Test. Does not assume equal variance.
    // Approximates degree of freedom by Welch-Satterthwaite equation.
    // Null Hypothesis: two population means are equal.
    // val pValue = tTest(distancesRandom, distancesTest)

    val dValue = stat.cohen_d(distancesRandom, distancesTest)
    DebugLog(s"Policy: the Cohen's d for different means is $dValue.", LogLevelInfo(), LogType.STRATEGY, question.id)

    // whether the Cohen's d exceeds the predefined threshold
    dValue >= question.d_threshold
  }

  override def is_done(tasks: List[Task], num_comparisons: Int): (Boolean, Int) = {
    // What is this for?
    val done = completed_workerunique_tasks(tasks).size
    if (done < tasks.size) {
      return (false, num_comparisons)
    }

    // Do not terminate less than sample_size
    if (done < question.sample_collect_size) {
      return (false, num_comparisons + 1)
    }

    // Turn the answers into their number representations
    val (answerMatrix, radixes) = processTasks(tasks)
    val sample_size = answerMatrix(0).length // note here that answers is transposed
    val question_types: Array[QuestionType.QuestionType] = question.questions.map(_.getQuestionType).toArray

    // Run the algorithm
    val result = survey_algorithm(question_types, radixes, iterations = 1000, sample_size, answerMatrix)

    // TODO: how much should we increase sample_size by if survey_algorithm does not pass?
    if (!result) {
      question.sample_size = (question.sample_size * 2).toInt
    }

    // Terminate ONLY when fulfilled at least sample_size and pass survey_algorithm of statistical significance
    (result, num_comparisons + 1)
  }

  /**
   * This method runs our denoise algorithm to calculate a noise score for all
   * the tasks. It returns the noise score and labels whether the current task
   * is likely a noisy answer (bots or lazy human workers) based on an a priori
   * (previously configured) percentage.
   *
   * Note that this percentage seems to be a stable number found by many
   * different researchers and published works.
   *
   * @param tasks The complete list of tasks.
   * @param num_comparisons The number of times is_done has been called.
   *  @return Top answer
   */
  override def select_answer(tasks: List[Task], num_comparisons: Int): Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)

    // Turn the answers into their number representations
    val (answerMatrix, radixes) = processTasks(tasks)
    val question_types: Array[QuestionType.QuestionType] = question.questions.map(_.getQuestionType).toArray
    val sample_size = answerMatrix(0).length

    // A map of score given question index and radix (answer value/range)
    val radixScores = radixes.indices.map(i => {
      val radix = radixes(i)

      // bin count of all radixes in answers
      var count = Array.ofDim[Int](radix)
      count = count.map(_ => 0)
      answerMatrix(i).foreach(x => {
        count(x) = count(x) + 1
      })

      // calculate scores based on bin count
      val score = count.map(count => {
        // np.power(np.e, (1/rad - bincount_q[:rad]/SAMPLE_SIZE)*rad)
        math.exp(1 - count * radix / sample_size)
      })

      if (question_types(i) == QuestionType.EstimationQuestion) {
        // punish lower & upper outlier (first and last radix) with score 2 * e
        score(0) = 2 * math.exp(1)
        score(score.length-1) = 2 * math.exp(1)
      }

      score
    }).toArray

    // set different scores based on question indices and which bin they are in
    // (i.e. the radix they have). Then, sum across questions
    val subScores = answerMatrix.zipWithIndex.map{ case (answers, i) =>
      answers.map(ans => {
        radixScores(i)(ans)
      })
    }.transpose
    val scores = subScores.map(s => s.sum)

    // score threshold. Tasks with scores greater than which are likely noises
    val score_threshold = scores.sortWith(_ > _)(math.floor(question.noise_percentage * scores.length).toInt)

    // sum the score across questions for a given Task
    assert(scores.length == valid_tasks.length, "Denoise Algorithm: scores have different length from valid_tasks")

    // filtered dist
    val distribution: Set[(String, Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
    val metadatas: Map[String, SurveyTaskMetadata] = valid_tasks.zipWithIndex.map { case (t, i) =>
      (
        t.worker_id.get,
        SurveyTaskMetadata(t.worker_id.get, t.cost, scores(i),
          likely_noise = scores(i) >= score_threshold)
      )
    }.toMap

    // raw distribution
    val dist = getDistribution(tasks)
    val cost: BigDecimal = valid_tasks.filterNot(_.from_memo).foldLeft(BigDecimal(0)) { case (acc, t) => acc + t.cost }

    SurveyAnswers(distribution, metadatas, cost, question, dist).asInstanceOf[Question#AA]
  }


  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    // additional number needed to reach num_samples
    math.max(question.sample_collect_size - outstanding_tasks(tasks).size - answered_tasks(tasks).size, 0)
  }
}
