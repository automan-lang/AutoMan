package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.question._
import edu.umass.cs.automan.core.MagicNumbers
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.question.confidence._
import edu.umass.cs.automan.core.policy.aggregation._
import edu.umass.cs.automan.core.mock._
import edu.umass.cs.automan.core.question.Dimension


object DSL extends edu.umass.cs.automan.core.GDSL {
  type GQuestionOption = edu.umass.cs.automan.adapters.googleads.question.GQuestionOption

  def gads(production_account_id: Long) : GoogleAdsAdapter = {
    val initf = (g: GoogleAdsAdapter) => {
      // mandatory parameters
      g.production_account_id_=(1373958703)
    }
      GoogleAdsAdapter(initf)
  }

  def choice(key: Symbol, text: String)(implicit ga: GoogleAdsAdapter): GQuestionOption = {
    ga.Option(key, text)
  }

  def choice(key: Symbol, text: String, image_url: String)(implicit ga: GoogleAdsAdapter): GQuestionOption = {
    ga.Option(key, text, image_url)
  }

  // DSL for GQuestions
  def estimate( confidence_interval: ConfidenceInterval = UnconstrainedCI(),
                confidence: Double = MagicNumbers.DefaultConfidence,
                budget: BigDecimal = MagicNumbers.DefaultBudget,
                default_sample_size: Int = -1,
                dont_reject: Boolean = true,
                dry_run: Boolean = false,
                estimator: Seq[Double] => Double = null,
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                max_value: Double = Double.MaxValue,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                min_value: Double = Double.MinValue,
                mock_answers: Iterable[MockAnswer[Double]] = null,
                pay_all_on_failure: Boolean = true,
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                title: String = null, // form title
                wage: BigDecimal = MagicNumbers.USFederalMinimumWage, // unused, should automatically set CPC
                ad_title: String = null,
                ad_subtitle: String = null,
                ad_description: String = null,
                english: Boolean = false, // restrict ad to English-speaking users
                required: Boolean = true // require respondents to answer this question
                )
                (implicit gads: GoogleAdsAdapter) : EstimationOutcome = {
    def initf(q: GEstimationQuestion): Unit = {
      // mandatory parameters
      q.text = text

      // mandatory parameters with sane defaults
      q.confidence_interval = confidence_interval
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (default_sample_size != -1 && default_sample_size > 0) { q.default_sample_size = default_sample_size }
      if (estimator != null) { q.estimator = estimator }
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (max_value != Double.MaxValue) { q.max_value = max_value }
      if (min_value != Double.MinValue) { q.min_value = min_value }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    gads.EstimationQuestion(initf)
  }
  // WIP
  def multiestimate( dimensions: Array[Dimension],
                     confidence: Double = MagicNumbers.DefaultConfidence,
                     budget: BigDecimal = MagicNumbers.DefaultBudget,
                     default_sample_size: Int = -1,
                     dont_reject: Boolean = true,
                     dry_run: Boolean = false,
                     image_alt_text: String = null,
                     image_url: String = null,
                     initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                     minimum_spawn_policy: MinimumSpawnPolicy = null,
                     mock_answers: Iterable[MockAnswer[Array[Double]]] = null,
                     pay_all_on_failure: Boolean = true,
                     question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                     text: String,
                     title: String = null, // form title
                     wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                     ad_title: String = "",
                     ad_subtitle: String = "",
                     ad_description: String = "",
                     english: Boolean = false, // restrict ad to English-speaking users
                     required: Boolean = true // require respondents to answer this question
                     )
                     (implicit a: GoogleAdsAdapter) : MultiEstimationOutcome = {
    def initf(q: GMultiEstimationQuestion): Unit = {
      // mandatory parameters
      q.dimensions = dimensions
      q.text = text

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (default_sample_size != -1 && default_sample_size > 0) { q.default_sample_size = default_sample_size }
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.MultiEstimationQuestion(initf)
  }

  def freetext( allow_empty_pattern: Boolean = false,
                confidence: Double = MagicNumbers.DefaultConfidence,
                before_filter: String => String = (a: String) => a,
                budget: BigDecimal = MagicNumbers.DefaultBudget,
                dont_reject: Boolean = true,
                dry_run: Boolean = false,
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                mock_answers: Iterable[MockAnswer[String]] = null,
                pay_all_on_failure: Boolean = true,
                pattern: String, // still have to implement in apps script
                pattern_error_text: String = null,
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                title: String = null,
                wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                ad_title: String = "",
                ad_subtitle: String = "",
                ad_description: String = "",
                english: Boolean = false, // restrict ad to English-speaking users
                required: Boolean = true // require respondents to answer this question
                )
                (implicit a: GoogleAdsAdapter) : ScalarOutcome[String] = {
    def initf(q: GFreeTextQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.pattern = pattern

      // mandatory parameters with sane defaults
      q.allow_empty_pattern = allow_empty_pattern
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.FreeTextQuestion(initf)
  }

  def freetexts( allow_empty_pattern: Boolean = false,
                 sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
                 before_filter: String => String = (a: String) => a,
                 budget: BigDecimal = MagicNumbers.DefaultBudget,
                 dont_reject: Boolean = true,
                 dry_run: Boolean = false,
                 image_alt_text: String = null,
                 image_url: String = null,
                 initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                 minimum_spawn_policy: MinimumSpawnPolicy = null,
                 mock_answers: Iterable[MockAnswer[String]] = null,
                 pay_all_on_failure: Boolean = true,
                 pattern: String = null,
                 pattern_error_text: String = null,
                 question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                 text: String,
                 title: String = null,
                 wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                 ad_title: String = "",
                 ad_subtitle: String = "",
                 ad_description: String = "",
                 english: Boolean = false, // restrict ad to English-speaking users
                 required: Boolean = true // require respondents to answer this question
                 )
                 (implicit a: GoogleAdsAdapter) : VectorOutcome[String] = {
    def initf(q: GFreeTextVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text

      // mandatory parameters with sane defaults
      q.allow_empty_pattern = allow_empty_pattern
      q.sample_size = sample_size
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern != null) { q.pattern = pattern }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.FreeTextDistributionQuestion(initf)
  }

  def checkbox( confidence: Double = MagicNumbers.DefaultConfidence,
                budget: BigDecimal = MagicNumbers.DefaultBudget,
                dont_reject: Boolean = true,
                dry_run: Boolean = false,
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                mock_answers: Iterable[MockAnswer[Set[Symbol]]] = null,
                options: List[AnyRef],
                pay_all_on_failure: Boolean = true,
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                title: String = null,
                wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                ad_title: String = "",
                ad_subtitle: String = "",
                ad_description: String = "",
                english: Boolean = false, // restrict ad to English-speaking users
                other: Boolean = false,
                required: Boolean = true // require respondents to answer this question
                )
                (implicit a: GoogleAdsAdapter) : ScalarOutcome[Set[Symbol]] = {
    def initf(q: GCheckboxQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.other = other
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.CheckboxQuestion(initf)
  }

  def checkboxes( sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
                  budget: BigDecimal = MagicNumbers.DefaultBudget,
                  dont_reject: Boolean = true,
                  dry_run: Boolean = false,
                  image_alt_text: String = null,
                  image_url: String = null,
                  initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                  minimum_spawn_policy: MinimumSpawnPolicy = null,
                  mock_answers: Iterable[MockAnswer[Set[Symbol]]] = null,
                  options: List[AnyRef],
                  pay_all_on_failure: Boolean = true,
                  question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                  text: String,
                  title: String = null,
                  wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
                  ad_title: String = "",
                  ad_subtitle: String = "",
                  ad_description: String = "",
                  english: Boolean = false, // restrict ad to English-speaking users
                  other: Boolean = false,
                  required: Boolean = true // require respondents to answer this question
                  )
                  (implicit a: GoogleAdsAdapter) : VectorOutcome[Set[Symbol]] = {
    def initf(q: GCheckboxVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.other = other
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.CheckboxDistributionQuestion(initf)
  }

  def radio( confidence: Double = MagicNumbers.DefaultConfidence,
             budget: BigDecimal = MagicNumbers.DefaultBudget,
             dont_reject: Boolean = true,
             dry_run: Boolean = false,
             image_alt_text: String = null,
             image_url: String = null,
             initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
             minimum_spawn_policy: MinimumSpawnPolicy = null,
             mock_answers: Iterable[MockAnswer[Symbol]] = null,
             options: List[AnyRef],
             pay_all_on_failure: Boolean = true,
             question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
             text: String,
             title: String = null,
             wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
             ad_title: String = "",
             ad_subtitle: String = "",
             ad_description: String = "",
             english: Boolean = false, // restrict ad to English-speaking users
             other: Boolean = false,
             required: Boolean = true // require respondents to answer this question
             )
             (implicit a: GoogleAdsAdapter) : ScalarOutcome[Symbol] = {
    def initf(q: GRadioButtonQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.other = other
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.RadioButtonQuestion(initf)
  }

  def radios( sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
              budget: BigDecimal = MagicNumbers.DefaultBudget,
              dont_reject: Boolean = true,
              dry_run: Boolean = false,
              image_alt_text: String = null,
              image_url: String = null,
              initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
              minimum_spawn_policy: MinimumSpawnPolicy = null,
              mock_answers: Iterable[MockAnswer[Symbol]] = null,
              options: List[AnyRef],
              pay_all_on_failure: Boolean = true,
              question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
              text: String,
              title: String = null,
              wage: BigDecimal = MagicNumbers.USFederalMinimumWage,
              ad_title: String = "",
              ad_subtitle: String = "",
              ad_description: String = "",
              english: Boolean = false, // restrict ad to English-speaking users
              other: Boolean = false,
              required: Boolean = true // require respondents to answer this question
              )
              (implicit a: GoogleAdsAdapter) : VectorOutcome[Symbol] = {
    def initf(q: GRadioButtonVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier
      // google-specific parameters
      q.english = english
      q.other = other
      q.required = required
      q.wage = wage // CPC

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.RadioButtonDistributionQuestion(initf)
  }

  // Instead of writing a list, let the user supply a "big tuple"
  implicit def product2OptionList(p: Product) : List[GQuestionOption] = p.productIterator.toList.asInstanceOf[List[GQuestionOption]]
  implicit def tupSymbString2MTQuestionOption(opt: (Symbol, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2)
  implicit def tupStrURL2MTQuestionOption(opt: (String, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(opt._1), opt._1, opt._2)
  implicit def tupWithURL2MTQuestionOption(opt: (Symbol, String, String))(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2, opt._3)
  implicit def str2MTQuestionOption(s: String)(implicit mt: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(s), s)
}
