package org.automanlang.adapters.mturk

import com.amazonaws.services.mturk.model.QualificationRequirement

import scala.language.implicitConversions
import org.automanlang.adapters.mturk.question.MTFakeSurvey
import org.automanlang.adapters.mturk.mock.MockSetup
import org.automanlang.core.{AutomanAdapter, MagicNumbers}
import org.automanlang.core.logging.LogConfig.LogConfig
import org.automanlang.core.logging.{LogLevel, LogLevelInfo}
import org.automanlang.core.policy.aggregation.MinimumSpawnPolicy
import org.automanlang.core.question.{FakeSurvey, Question}
import scala.collection.immutable.ListMap

object DSL extends org.automanlang.core.DSL {
  type MTQuestionOption = org.automanlang.adapters.mturk.question.MTQuestionOption
  
  def mturk(access_key_id: String,
            secret_access_key: String,
            sandbox_mode: Boolean = false,
            database_path: String = null,
            use_mock: MockSetup = null,
            logging: LogConfig = LogConfig.TRACE_MEMOIZE,
            log_verbosity: LogLevel = LogLevelInfo()) : MTurkAdapter = {
    val initf = (mt: MTurkAdapter) => {
      // mandatory parameters
      mt.access_key_id = access_key_id
      mt.secret_access_key = secret_access_key

      // mandatory with sane defaults
      mt.logging = logging
      mt.log_verbosity = log_verbosity
      mt.sandbox_mode = sandbox_mode

      // optional parameters
      if (database_path != null) { mt.database_path = database_path }
      if (use_mock != null) { mt.use_mock = use_mock }
    }

    MTurkAdapter(initf)
  }

  def SurveyWithQuals[A <: MTurkAdapter, O](
                                      questions: List[Question],
                                      budget: BigDecimal = MagicNumbers.DefaultBudget,
                                      sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
                                      dont_reject: Boolean = true,
                                      dry_run: Boolean = false,
                                      initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                                      minimum_spawn_policy: MinimumSpawnPolicy = null,
                                      pay_all_on_failure: Boolean = true,
                                      question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                                      text: String,
                                      title: String = null,
                                      csv_output: String = null,
                                      wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                                      cohen_d_threshold: Double = 12,
                                      noise_percentage: Double = 0.2,
                                      words_candidates: ListMap[String, Array[String]] = ListMap(),
                                      functions: ListMap[String, (String, Map[String, String])] = ListMap(),
                                      qualifications: List[QualificationRequirement]
                                    )(implicit a: MTurkAdapter): FakeSurvey#O = {
    def initf[Q <: FakeSurvey](q2: Q): Unit = {
      val q = q2.asInstanceOf[MTFakeSurvey]
      // mandatory parameters
      q.text = text
      q.questions = questions

      // mandatory parameters with sane defaults
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      q.sample_size = sample_size
      q.wage = wage
      q.qualifications = qualifications

      // optional parameters
      //      if (image_alt_text != null) {
      //        q.image_alt_text = image_alt_text
      //      }
      //      if (image_url != null) {
      //        q.image_url = image_url
      //      }
      if (title != null) {
        q.title = title
      }
      //      if (mock_answers != null) {
      //        q.mock_answers = mock_answers
      //      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }

      if (csv_output != null) {
        q.csv_output = csv_output
      }

      q.d_threshold = cohen_d_threshold
      q.noise_percentage = noise_percentage
      q.words_candidates = words_candidates
      q.functions = functions
    }

    a.Survey(initf)
  }

  def choice(key: Symbol, text: String)(implicit mt: MTurkAdapter) : MTQuestionOption = {
    mt.Option(key, text)
  }

  def choice(key: Symbol, text: String, image_url: String)(implicit mt: MTurkAdapter) : MTQuestionOption = {
    mt.Option(key, text, image_url)
  }
  
  // Instead of writing a list, let the user supply a "big tuple"
  implicit def product2OptionList(p: Product) : List[MTQuestionOption] = p.productIterator.toList.asInstanceOf[List[MTQuestionOption]]
  implicit def tupSymbString2MTQuestionOption(opt: (Symbol, String))(implicit mt: MTurkAdapter) : MTQuestionOption = choice(opt._1, opt._2)
  implicit def tupStrURL2MTQuestionOption(opt: (String, String))(implicit mt: MTurkAdapter) : MTQuestionOption = choice(Symbol(opt._1), opt._1, opt._2)
  implicit def tupWithURL2MTQuestionOption(opt: (Symbol, String, String))(implicit mt: MTurkAdapter) : MTQuestionOption = choice(opt._1, opt._2, opt._3)
  implicit def str2MTQuestionOption(s: String)(implicit mt: MTurkAdapter) : MTQuestionOption = choice(Symbol(s), s)
}
