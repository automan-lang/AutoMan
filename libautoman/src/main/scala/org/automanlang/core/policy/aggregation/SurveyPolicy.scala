package org.automanlang.core.policy.aggregation

import com.github.tototoshi.csv._
import org.automanlang.core.info.QuestionType
import org.automanlang.core.logging.{DebugLog, LogLevelInfo, LogType}
import org.automanlang.core.scheduler.Task
import org.automanlang.core.question._
import org.automanlang.core.util.lapjv
import org.automanlang.core.util.stat

import scala.math._
import java.io.{BufferedWriter, File, FileWriter}

class SurveyPolicy(question: FakeSurvey)
  extends SurveyVectorPolicy(question) {

  DebugLog("Policy: survey policy", LogLevelInfo(), LogType.STRATEGY, question.id)

  private[automanlang] def create_samples(sample_size: Int, radixes: Array[Double]): Array[Array[Double]] = {
    val r = new java.util.Random

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
            val dis = samples2(z).map(v => abs(v - value) / radix)
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

    val meanRandom = distancesRandom.sum / sample_size
    val meanTest = distancesTest.sum / sample_size

    // standard deviation of the random data
    // ==> shouldn't this be pooled std dev?
    val std = stat.standardDeviation(distancesRandom)

    // ==> shouldn't there be well-defined threshold of rejecting null hypothesis? (depending on degrees of freedom)
    val threshold = 3
    val stdAway = (meanTest - meanRandom) / std

    //    println("random distances")
    //    println(randomDistances.mkString(","))
    //    println("test distances")
    //    println(testDistances.mkString(","))
    //    println("meanRandom")
    //    println(meanRandom)
    //    println("meanTest")
    //    println(meanTest)
    //    println(std)
    //    println(stdAway)
    println(stdAway)

    stdAway > threshold
  }

  override def is_done(tasks: List[Task], num_comparisons: Int): (Boolean, Int) = {

    val done = completed_workerunique_tasks(tasks).size

    if (done < tasks.size) {
      return (false, num_comparisons)
    }

    var sample_size = 0

    // Turn the answers into their number representations
    val task: FakeSurvey = tasks.head.question.asInstanceOf[FakeSurvey]

    // TODO: may need better way to express this
    var filename = "output.csv"

    // TODO: way to get around this magical string?
    if (task.csv_output != "Output not specified.") {
      filename = task.csv_output
    }

    val out = new BufferedWriter(new FileWriter(filename))
    val writer = CSVWriter.open(new File(filename + ".survey-policy"))

    // get the questions
    val questions = task.questions

    // create the array of possibilities
    var possibilities: Array[Array[String]] = Array()

    // type of question
    var question_types: Array[String] = Array()

    // radixes
    var radixes: Array[Int] = Array()

    // index for which estimate is being looked at. Need for figuring out radix
    var estimate_radix_index = 0

    // keep track of the smallest and largest estimate
    var estimate_radix_largest: Array[Int] = Array()

    // figure out the format
    questions.foreach {
      case chx: CheckboxQuestion =>
        val arr = chx.return_response_possibilities()
        possibilities = possibilities :+ arr
        question_types = question_types :+ "checkbox"
        radixes = radixes :+ arr.length
      case rad: RadioButtonQuestion =>
        val arr = rad.return_response_possibilities()
        possibilities = possibilities :+ arr
        question_types = question_types :+ "radio"
        radixes = radixes :+ arr.length
      case _: EstimationQuestion =>
        possibilities = possibilities :+ null
        question_types = question_types :+ "estimate"
        radixes = radixes :+ 0
        estimate_radix_largest = estimate_radix_largest :+ -99999999
      case _ =>
        possibilities = possibilities :+ null
        question_types = question_types :+ "other"
        radixes = radixes :+ -1
    }

    // All the information, to be used for CSV file
    var l: List[List[String]] = List()

    // Just the number representations
    var numberReps: Array[Array[Int]] = Array()

    // For each task, analyze the answer, if it exists
    tasks.foreach(task => {

      val workerId = task.worker_id
      val cost = task.cost
      val time = task.state_changed_at

      estimate_radix_index = 0

      val answer = task.answer

      answer match {
        case Some(t: List[Any]) =>
          // If the answer exists
          sample_size = sample_size + 1

          // A list that stores all the information for each question of the survey
          var ar = List[String]()

          // include workerId in the output
          workerId match {
            case Some(value) =>
              ar = ar :+ value
            case None =>
              ar = ar :+ "undefined"
          }

          // include cost and time in the output
          ar = ar :+ cost.toString
          ar = ar :+ time.toString

          // Number representation, as a string. E.g. "[1,5,3]"
          var placeString = ""
          // Number representation, as array
          var placeArray = Array[Int]()
          // Index of which question in the survey answer we're on
          var placeIndex = 1
          // Number of questions in the answer
          val placeSize = t.size

          // for sorting symbols
          implicit val symbolOrdering: Ordering[Symbol] = Ordering.by(_.name)

          // format each individual answer within the bigger survey answer
          t.foreach(x => {

            x match {
              // Checkbox answer
              case s: Set[Symbol] =>
                // Aggregate into a single string
                var str = ""
                var index = 1
                val size = s.size

                // sort
                val ss = collection.immutable.SortedSet[Symbol]() ++ s

                ss.foreach(sym => {
                  // concatenate all the symbols into one string
                  str = str + sym.name
                  if (index < size) {
                    str = str + ", "
                  }
                  index = index + 1
                })

                // calculate number representation
                val place = possibilities(placeIndex - 1).indexWhere(_ == str)
                placeString = placeString + place
                str = "[" + str + "]"
                ar = ar :+ str
                placeArray = placeArray :+ place

              // Radio answer
              case sym: Symbol =>
                ar = ar :+ sym.name
                val place = possibilities(placeIndex - 1).indexWhere(_ == sym.name)
                placeString = placeString + place
                placeArray = placeArray :+ place
              // Estimate
              case d: Double =>
                val intValue = d.toInt
                ar = ar :+ d.toString
                placeString = placeString + d.toString
                placeArray = placeArray :+ intValue

                // Figure out radix
                val biggestSoFar = estimate_radix_largest(estimate_radix_index)
                if (intValue > biggestSoFar) {
                  estimate_radix_largest(estimate_radix_index) = intValue
                }
                estimate_radix_index = estimate_radix_index + 1
              case _ =>
                ar = ar :+ x.toString
                //placeArray = placeArray :+ x.toString()
            }

            if (placeIndex < placeSize) placeString = placeString + ", "
            placeIndex = placeIndex + 1
          })
          placeString = "[" + placeString + "]"
          ar = ar :+ placeString
          l = ar :: l
          numberReps = numberReps :+ placeArray
        case _ =>
          println("None")
      }

    })

    // Edit radixes to take into account estimates
    var i = 0
    for (j <- radixes.indices) {
      val current = radixes(j)
      if (current == 0) {
        radixes(j) = estimate_radix_largest(i) + 1
        i = i + 1
      }
    }

    //    println("question_types")
    //    println(question_types.mkString(","))
    //    println("radixes")
    //    println(radixes.mkString(","))
    //    println("sample size")
    //    println(sample_size)
    //    println("number reps")
    //    numberReps.foreach(r => {
    //      println(r.mkString(","))
    //    })

    // Run the algorithm
    //val result = survey_algorithm(question_types, radixes, iterations = 10000, sample_size, numberReps)

    //println(result)

    val listOfRecords = l.reverse
    writer.writeAll(listOfRecords)
    out.close()

    //((done >= question.sample_size) && result, num_comparisons + 1)
    (done >= question.sample_size, num_comparisons + 1)

  }

  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    //println("running num_to_run")
    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_tasks(tasks).size, 0)
  }
}
