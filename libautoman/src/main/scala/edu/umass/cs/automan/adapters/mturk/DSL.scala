package edu.umass.cs.automan.adapters.mturk

import scala.language.implicitConversions
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.adapters.mturk.question.{MTQuestionOption, MTSurvey}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.logging.{LogLevel, LogLevelInfo}
import edu.umass.cs.automan.core.policy.aggregation.MinimumSpawnPolicy
import edu.umass.cs.automan.core.question.Survey

object DSL extends edu.umass.cs.automan.core.DSL {
  type MTQuestionOption = edu.umass.cs.automan.adapters.mturk.question.MTQuestionOption
  
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

  override def survey[A <: AutomanAdapter, O](budget: BigDecimal,
                                              dont_reject: Boolean,
                                              dry_run: Boolean,
                                              image_alt_text: String,
                                              image_url: String,
                                              initial_worker_timeout_in_s: Int,
                                              minimum_spawn_policy: MinimumSpawnPolicy,
                                              pay_all_on_failure: Boolean,
                                              questions: List[AutomanAdapter => DSL.Outcome[_]],
                                              sample_size: Int,
                                              survey_timeout_multiplier: Double,
                                              text: String,
                                              title: String,
                                              wage: BigDecimal)(implicit a: A): SurveyOutcome = {
  def initf[S <: Survey](s: Survey) = {
              // mandatory parameters
              s.text = text
              s.question_list = questions.map(f => f(new MTurkNoOpAdapter)) // reading from question list, so it's getting evaluated

              // mandatory parameters with sane defaults
              s.budget = budget
              s.dont_reject = dont_reject
              s.dry_run = dry_run
              s.initial_worker_timeout_in_s = initial_worker_timeout_in_s
              s.pay_all_on_failure = pay_all_on_failure
              s.question_timeout_multiplier = survey_timeout_multiplier

              // optional parameters
              if (image_alt_text != null) { s.image_alt_text = image_alt_text }
              if (image_url != null) { s.image_url = image_url }
              if (title != null) { s.title = title }
              if (minimum_spawn_policy != null) { s.minimum_spawn_policy = minimum_spawn_policy }

            }
            a.Survey(initf)
  }

  override def surveyGrammar[A <: AutomanAdapter, O](budget: BigDecimal,
                                              dont_reject: Boolean,
                                              dry_run: Boolean,
                                              image_alt_text: String,
                                              image_url: String,
                                              initial_worker_timeout_in_s: Int,
                                              minimum_spawn_policy: MinimumSpawnPolicy,
                                              pay_all_on_failure: Boolean,
                                              questions: List[AutomanAdapter => DSL.Outcome[_]],
                                              sample_size: Int,
                                              survey_timeout_multiplier: Double,
                                              text: String,
                                              title: String,
                                              wage: BigDecimal)(implicit a: A): SurveyOutcome = {
    def initf[S <: Survey](s: Survey) = {
      // mandatory parameters
      s.text = text
      s.question_list = questions.map(f => f(new MTurkNoOpAdapter)) // reading from question list, so it's getting evaluated

      // mandatory parameters with sane defaults
      s.budget = budget
      s.dont_reject = dont_reject
      s.dry_run = dry_run
      s.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      s.pay_all_on_failure = pay_all_on_failure
      s.question_timeout_multiplier = survey_timeout_multiplier

      // optional parameters
      if (image_alt_text != null) { s.image_alt_text = image_alt_text }
      if (image_url != null) { s.image_url = image_url }
      if (title != null) { s.title = title }
      if (minimum_spawn_policy != null) { s.minimum_spawn_policy = minimum_spawn_policy }

    }
    a.Survey(initf)
  }
}
