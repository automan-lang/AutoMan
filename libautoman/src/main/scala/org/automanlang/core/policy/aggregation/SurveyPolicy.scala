package org.automanlang.core.policy.aggregation

import org.automanlang.core.logging.{LogLevelInfo, DebugLog, LogType}
import org.automanlang.core.question.MixedQuestion
import org.automanlang.core.scheduler.Task
import org.automanlang.core.question._
import org.automanlang.core.answer._
import org.automanlang.core.util.Stopwatch

import scala.math._

import java.io.{BufferedWriter, FileWriter}
import collection.JavaConverters._
import au.com.bytecode.opencsv.CSVWriter

class SurveyPolicy(question: MixedQuestion)
  extends MixedPolicy(question) {

  DebugLog("Policy: survey policy",LogLevelInfo(),LogType.STRATEGY, question.id)

  private[automanlang] def standard_deviation(samples: Array[Double]) : Double = {

    val mean = samples.sum/samples.length
    val stdDev = Math.sqrt((samples.map( _ - mean)
      .map(t => t*t).sum)/samples.length)
    stdDev

  }

  private[automanlang] def create_samples(sample_size: Int, radixes: Array[Int]): Array[Array[Int]] = {

    var samples : Array[Array[Int]] = Array()
    var length = radixes.length

    val r = new scala.util.Random

    for (x <- 0 until sample_size) {

      var sample : Array[Int] = Array()

      for (y <- 0 until length) {

        // for each item, randomly choose
        val radix = radixes(y)
        val newNumber = r.nextInt(radix)

        sample = sample :+ newNumber

      }
      samples = samples :+ sample
    }
    samples
  }

  private[automanlang] def random_v_random(iterations: Int, sample_size: Int, radixes: Array[Int], question_types: Array[String]): Array[Double] = {

    var distances: Array[Double] = Array()

    for (x <- 0 until iterations) {

      // Create the random samples
      val samples1 = create_samples(sample_size, radixes)
      val samples2 = create_samples(sample_size, radixes)


      // Calculate distance
      val dist = earth_movers(samples1, samples2, sample_size, radixes.length, question_types, radixes)
      distances = distances :+ dist

    }

    distances

  }

  private[automanlang] def test_v_random(test_samples: Array[Array[Int]], iterations: Int, sample_size: Int, radixes: Array[Int], question_types: Array[String]): Array[Double] = {

    var distances: Array[Double] = Array()

    for (x <- 0 until iterations) {

      // Create the random samples
      val samples1 = create_samples(sample_size, radixes)

      // Calculate distance
      val dist = earth_movers(samples1, test_samples, sample_size, radixes.length, question_types, radixes)

      distances = distances :+ dist

    }

    distances

  }

  private[automanlang] def earth_movers(samples1: Array[Array[Int]], samples2: Array[Array[Int]], sample_size: Int, complexity: Int, question_types: Array[String], radixes: Array[Int]): Double = {

    // Data is stored in this format: [((5,3),8), ((2,2),10)] where the inner tuple is the row and column in the "table" of this entry, and the other number is the distance
    var data: Array[Tuple2[Tuple2[Int, Int], Double]] = Array()

    // exhaustively compute the distance between every pair
    for (x <- 0 until sample_size) {
      for (y <- 0 until sample_size) {
        val s1 = samples1(x)
        val s2 = samples2(y)

        var total = 0.0

        // Calculate Euclidean distance, but normalize between 0 and 1
        for (z <- 0 until complexity) {

          // First, check what kind of question is being compared here
          val q_type = question_types(z)

          val number1 = s1(z)
          val number2 = s2(z)

          q_type match {

            // for checkbox and radio questions, same is 0 and different is 1
            case "checkbox" => {
              if (number1 != number2) {
                total = total + 1
              }
            }

            case "radio" => {
              if (number1 != number2) {
                total = total + 1
              }
            }

            case "estimate" => {
              // Figure out the "percentile" of each number
              // The radix is the maximum possible number
              val radix = radixes(z)
              val per1 = number1.toFloat / (radix - 1)
              val per2 = number2.toFloat / (radix - 1)
              val diff = per1 - per2
              total = total + (diff * diff)

            }

          }



// Version without normalizing
//          val diff = s1(z) - s2(z)
//          total = total + (diff * diff)
        }

        data = data :+ ((x,y),sqrt(total))
      }
    }

    // ordering
    implicit val dataOrdering: Ordering[Tuple2[Tuple2[Int, Int], Double]] = Ordering.by(_._2)

    scala.util.Sorting.quickSort(data)

    // keep track of which rows and columns have been "removed"
    var rows_used : Array[Int] = Array()
    var columns_used: Array[Int] = Array()

    // number of items chosen
    var number_chosen = 0

    // total distance
    var distance = 0.0

    // current index
    var index = 0

    // Calculate earth-mover's distance
    while (number_chosen < sample_size) {

      // Get the current one
      val current = data(index)

      val row = current._1._1
      val column = current._1._2

      // Check to see if the row or column has been chosen yet
      if (!(rows_used contains row) && !(columns_used contains column)) {

        distance = distance + current._2
        number_chosen = number_chosen + 1
        rows_used = rows_used :+ row
        columns_used = columns_used :+ column

      }
      index = index + 1

    }

    distance

  }

  // Algorithm to determine if more answers are needed for the survey
  // Uses the earth-mover's distance algorithm
  private[automanlang] def survey_algorithm(question_types: Array[String], radixes: Array[Int], iterations: Int, sample_size: Int, test_samples: Array[Array[Int]]): Boolean = {

    val complexity = radixes.length

    val randomDistances = random_v_random(iterations, sample_size, radixes, question_types)
    val testDistances = test_v_random(test_samples, iterations, sample_size, radixes, question_types)

    val randomSum = randomDistances.sum
    val meanRandom = randomSum / randomDistances.length

    val testSum = testDistances.sum
    val meanTest = testSum / testDistances.length

    // standard deviation of the random data
    val std = standard_deviation(randomDistances)

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

  override def is_done(tasks: List[Task], num_comparisons: Int) : (Boolean, Int) = {

    val done = completed_workerunique_tasks(tasks).size

    if (done < tasks.size ) {
      return (false, num_comparisons)
    }

    var sample_size = 0

    // Turn the answers into their number representations
    val task : SurveyQuestion = tasks.head.question.asInstanceOf[SurveyQuestion]

    // get the file name
    val filename = "test.csv"

    val out = new BufferedWriter(new FileWriter(filename))
    val writer = new CSVWriter(out)

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

    // keep track of the largest estimate
    var estimate_radix_largest : Array[Int] = Array()

    // figure out the format
    questions.foreach(q => {

      q match {
        case chx: CheckboxQuestion => {
          var arr = chx.return_response_possibilities()
          possibilities = possibilities :+ arr
          question_types = question_types :+ "checkbox"
          radixes = radixes :+ arr.length
        }
        case rad: RadioButtonQuestion => {
          var arr = rad.return_response_possibilities()
          possibilities = possibilities :+ arr
          question_types = question_types :+ "radio"
          radixes = radixes :+ arr.length
        }
        case est: EstimationQuestion => {
          possibilities = possibilities :+ null
          question_types = question_types :+ "estimate"
          radixes = radixes :+ 0
          estimate_radix_largest = estimate_radix_largest :+ 0
        }
        case _ => {
          possibilities = possibilities :+ null
          question_types = question_types :+ "other"
          radixes = radixes :+ -1
        }
      }

    })

    // All the information, to be used for CSV file
    var l: List[Array[String]] = List()

    // Just the number representations
    var numberReps: Array[Array[Int]] = Array()

    // For each task, analyze the answer, if it exists
    tasks.foreach(task => {

      var workerId = task.worker_id

      estimate_radix_index = 0

      var answer = task.answer

      answer match {
        case Some(t: List[Any]) => {
          // If the answer exists
          sample_size = sample_size + 1

          // An array that stores all the information for each question of the survey
          var ar = Array[String]()

          workerId match {
            case Some(value) => {
              ar = ar :+ value
            }
            case None => {
              ar = ar :+ "undefined"
            }
          }

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
              case s: Set[Symbol] => {
                // Aggregate into a single string
                var str = ""
                var index = 1
                var size = s.size

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
              }

              // Radio answer
              case sym: Symbol => {
                ar = ar :+ sym.name
                val place = possibilities(placeIndex - 1).indexWhere(_ == sym.name)
                placeString = placeString + place
                placeArray = placeArray :+ place
              }
              // Estimate
              case d: Double => {
                val intValue = d.toInt
                ar = ar :+ d.toString()
                placeString = placeString + d.toString()
                placeArray = placeArray :+ intValue

                // Figure out radix
                val soFar = estimate_radix_largest(estimate_radix_index)
                if (intValue > soFar) {
                  estimate_radix_largest(estimate_radix_index) = intValue
                }
                estimate_radix_index = estimate_radix_index + 1
              }
              case _ => {
                ar = ar :+ x.toString()
                //placeArray = placeArray :+ x.toString()
              }
            }

            if (placeIndex < placeSize) placeString = placeString + ", "
            placeIndex = placeIndex + 1
          })
          placeString = "[" + placeString + "]"
          ar = ar :+ placeString
          l = ar :: l
          numberReps = numberReps :+ placeArray
        }
        case _ => {
          println("None")
        }
      }

    })

    // Edit radixes to take into account estimates
    var i = 0
    for (j <- 0 until radixes.length) {
      var current = radixes(j)
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

    val listOfRecords = l.reverse.asJava
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
