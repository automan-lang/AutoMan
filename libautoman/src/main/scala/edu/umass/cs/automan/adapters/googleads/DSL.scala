package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.question._
import edu.umass.cs.automan.adapters.googleads.util.KeywordList._
import edu.umass.cs.automan.core.MagicNumbers
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.question.confidence._
import edu.umass.cs.automan.core.policy.aggregation._
import edu.umass.cs.automan.core.question.Dimension

object DSL {
  val automan = edu.umass.cs.automan.automan
  val LogConfig = edu.umass.cs.automan.core.logging.LogConfig
  val Utilities = edu.umass.cs.automan.core.util.Utilities

  // to simplify imports
  type Answer[T] = edu.umass.cs.automan.core.answer.Answer[T]
  type Answers[T] = edu.umass.cs.automan.core.answer.Answers[T]
  type AsymmetricCI = edu.umass.cs.automan.core.question.confidence.AsymmetricCI
  type DistributionOutcome[T] = edu.umass.cs.automan.core.answer.VectorOutcome[T]
  type Estimate = edu.umass.cs.automan.core.answer.Estimate
  type IncompleteAnswers[T] = edu.umass.cs.automan.core.answer.IncompleteAnswers[T]
  type LowConfidenceAnswer[T] = edu.umass.cs.automan.core.answer.LowConfidenceAnswer[T]
  type LowConfidenceEstimate = edu.umass.cs.automan.core.answer.LowConfidenceEstimate
  type OverBudgetAnswer[T] = edu.umass.cs.automan.core.answer.OverBudgetAnswer[T]
  type OverBudgetAnswers[T] = edu.umass.cs.automan.core.answer.OverBudgetAnswers[T]
  type OverBudgetEstimate = edu.umass.cs.automan.core.answer.OverBudgetEstimate
  type ScalarOutcome[T] = edu.umass.cs.automan.core.answer.ScalarOutcome[T]
  type Outcome[T] = edu.umass.cs.automan.core.answer.Outcome[T]

  // to simplify pattern matching
  val Answer = edu.umass.cs.automan.core.answer.Answer
  val Answers = edu.umass.cs.automan.core.answer.Answers
  val Estimate = edu.umass.cs.automan.core.answer.Estimate
  val IncompleteAnswers = edu.umass.cs.automan.core.answer.IncompleteAnswers
  val LowConfidenceAnswer = edu.umass.cs.automan.core.answer.LowConfidenceAnswer
  val LowConfidenceEstimate = edu.umass.cs.automan.core.answer.LowConfidenceEstimate
  val OverBudgetAnswer = edu.umass.cs.automan.core.answer.OverBudgetAnswer
  val OverBudgetAnswers = edu.umass.cs.automan.core.answer.OverBudgetAnswers
  val OverBudgetEstimate = edu.umass.cs.automan.core.answer.OverBudgetEstimate
  val SymmetricCI = edu.umass.cs.automan.core.question.confidence.SymmetricCI
  val UnconstrainedCI = edu.umass.cs.automan.core.question.confidence.UnconstrainedCI

  type GQuestionOption = edu.umass.cs.automan.adapters.googleads.question.GQuestionOption

  def gads(production_account_id: Long, dry_run: Boolean = false) : GoogleAdsAdapter = {
    val initf = (g: GoogleAdsAdapter) => {
      // mandatory parameters
      g.production_account_id_=(1373958703)
      g.test = dry_run
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
                estimator: Seq[Double] => Double = null,
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                max_value: Double = Double.MaxValue,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                min_value: Double = Double.MinValue,
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                form_title: String = null,
                ad_title: String = null,
                ad_subtitle: String = null,
                ad_description: String = null,
                ad_keywords: Set[String] = keywords(),
                english: Boolean = false, // restrict ad to English-speaking users
                us: Boolean = false,      // restrict ad to the US
                male: Boolean = false,    // restrict ad to male users
                female: Boolean = false,  // restrict ad to female users
                required: Boolean = true, // require respondents to answer this question
                cpc: BigDecimal = MagicNumbers.DefaultCPC
                )
                (implicit gads: GoogleAdsAdapter) : EstimationOutcome = {
    def initf(q: GEstimationQuestion): Unit = {
      // mandatory parameters
      q.text = text

      // mandatory parameters with sane defaults
      q.confidence_interval = confidence_interval
      q.confidence = confidence
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.required = required
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s //calculate wage from cpc ($/task)

      // optional parameters
      if (default_sample_size != -1 && default_sample_size > 0) { q.default_sample_size = default_sample_size }
      if (estimator != null) { q.estimator = estimator }
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (max_value != Double.MaxValue) { q.max_value = max_value }
      if (min_value != Double.MinValue) { q.min_value = min_value }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
      if (cpc != null) {q.cpc = cpc}
    }
    gads.EstimationQuestion(initf)
  }

  def multiestimate( dimensions: Array[Dimension],
                     confidence: Double = MagicNumbers.DefaultConfidence,
                     budget: BigDecimal = MagicNumbers.DefaultBudget,
                     default_sample_size: Int = -1,
                     image_alt_text: String = null,
                     image_url: String = null,
                     initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                     minimum_spawn_policy: MinimumSpawnPolicy = null,
                     question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                     text: String,
                     form_title: String = null,
                     cpc: BigDecimal = MagicNumbers.DefaultCPC,
                     ad_title: String = null,
                     ad_subtitle: String = null,
                     ad_description: String = null,
                     ad_keywords: Set[String] = keywords(),
                     english: Boolean = false,
                     us: Boolean = false,
                     male: Boolean = false,
                     female: Boolean = false,
                     required: Boolean = true
                     )
                     (implicit a: GoogleAdsAdapter) : MultiEstimationOutcome = {
    def initf(q: GMultiEstimationQuestion): Unit = {
      // mandatory parameters
      q.dimensions = dimensions
      q.text = text

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.required = required
      q.wage = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s //calculate wage from cpc ($/task)

      // optional parameters
      if (default_sample_size != -1 && default_sample_size > 0) { q.default_sample_size = default_sample_size }
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (form_title != null) { q.title = form_title }
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
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                pattern: String, // still have to implement in apps script
                pattern_error_text: String = null,
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                form_title: String = null,
                cpc: BigDecimal = MagicNumbers.DefaultCPC,
                ad_title: String = null,
                ad_subtitle: String = null,
                ad_description: String = null,
                ad_keywords: Set[String] = keywords(),
                english: Boolean = false,
                us: Boolean = false,
                male: Boolean = false,
                female: Boolean = false,
                required: Boolean = true
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
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.required = required
      q.wage = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s // calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (form_title != null) { q.title = form_title }
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
                 image_alt_text: String = null,
                 image_url: String = null,
                 initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                 minimum_spawn_policy: MinimumSpawnPolicy = null,
                 pattern: String = null,
                 pattern_error_text: String = null,
                 question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                 text: String,
                 form_title: String = null,
                 cpc: BigDecimal = MagicNumbers.DefaultCPC,
                 ad_title: String = null,
                 ad_subtitle: String = null,
                 ad_description: String = null,
                 ad_keywords: Set[String] = keywords(),
                 english: Boolean = false,
                 us: Boolean = false,
                 male: Boolean = false,
                 female: Boolean = false,
                 required: Boolean = true
                 )
                 (implicit a: GoogleAdsAdapter) : VectorOutcome[String] = {
    def initf(q: GFreeTextVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text

      // mandatory parameters with sane defaults
      q.allow_empty_pattern = allow_empty_pattern
      q.sample_size = sample_size
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.required = required
      q.cpc = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s // calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern != null) { q.pattern = pattern }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.FreeTextDistributionQuestion(initf)
  }

  def checkbox( confidence: Double = MagicNumbers.DefaultConfidence,
                budget: BigDecimal = MagicNumbers.DefaultBudget,
                image_alt_text: String = null,
                image_url: String = null,
                initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                minimum_spawn_policy: MinimumSpawnPolicy = null,
                options: List[AnyRef],
                question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                text: String,
                form_title: String = null,
                cpc: BigDecimal = MagicNumbers.DefaultCPC,
                ad_title: String = null,
                ad_subtitle: String = null,
                ad_description: String = null,
                ad_keywords: Set[String] = keywords(),
                english: Boolean = false,
                us: Boolean = false,
                male: Boolean = false,
                female: Boolean = false,
                other: Boolean = false,
                required: Boolean = true
                )
                (implicit a: GoogleAdsAdapter) : ScalarOutcome[Set[Symbol]] = {
    def initf(q: GCheckboxQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.other = other
      q.required = required
      q.cpc = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s //calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.CheckboxQuestion(initf)
  }

  def checkboxes( sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
                  budget: BigDecimal = MagicNumbers.DefaultBudget,
                  image_alt_text: String = null,
                  image_url: String = null,
                  initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                  minimum_spawn_policy: MinimumSpawnPolicy = null,
                  options: List[AnyRef],
                  question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                  text: String,
                  form_title: String = null,
                  cpc: BigDecimal = MagicNumbers.DefaultCPC,
                  ad_title: String = null,
                  ad_subtitle: String = null,
                  ad_description: String = null,
                  ad_keywords: Set[String] = keywords(),
                  english: Boolean = false,
                  us: Boolean = false,
                  male: Boolean = false,
                  female: Boolean = false,
                  other: Boolean = false,
                  required: Boolean = true
                  )
                  (implicit a: GoogleAdsAdapter) : VectorOutcome[Set[Symbol]] = {
    def initf(q: GCheckboxVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.other = other
      q.required = required
      q.cpc = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s // calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.CheckboxDistributionQuestion(initf)
  }

  def radio( confidence: Double = MagicNumbers.DefaultConfidence,
             budget: BigDecimal = MagicNumbers.DefaultBudget,
             image_alt_text: String = null,
             image_url: String = null,
             initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
             minimum_spawn_policy: MinimumSpawnPolicy = null,
             options: List[AnyRef],
             question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
             text: String,
             form_title: String = null,
             ad_title: String = null,
             ad_subtitle: String = null,
             ad_description: String = null,
             ad_keywords: Set[String] = keywords(),
             english: Boolean = false,
             us: Boolean = false,
             male: Boolean = false,
             female: Boolean = false,
             other: Boolean = false,
             required: Boolean = true,
             cpc: BigDecimal = MagicNumbers.DefaultCPC
             )
             (implicit a: GoogleAdsAdapter) : ScalarOutcome[Symbol] = {
    def initf(q: GRadioButtonQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.other = other
      q.required = required
      q.cpc = cpc
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s // calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.RadioButtonQuestion(initf)
  }

  def radios( sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
              budget: BigDecimal = MagicNumbers.DefaultBudget,
              image_alt_text: String = null,
              image_url: String = null,
              initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
              minimum_spawn_policy: MinimumSpawnPolicy = null,
              options: List[AnyRef],
              question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
              text: String,
              form_title: String = null,
              cpc: BigDecimal = MagicNumbers.DefaultCPC,
              ad_title: String = null,
              ad_subtitle: String = null,
              ad_description: String = null,
              ad_keywords: Set[String] = keywords(),
              english: Boolean = false,
              us: Boolean = false,
              male: Boolean = false,
              female: Boolean = false,
              other: Boolean = false,
              required: Boolean = true
              )
              (implicit a: GoogleAdsAdapter) : VectorOutcome[Symbol] = {
    def initf(q: GRadioButtonVectorQuestion): Unit = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]]  // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.question_timeout_multiplier = question_timeout_multiplier
      q.pay_all_on_failure = true
      // google-specific parameters
      q.english_only = english
      q.us_only = us
      q.male_only = male
      q.female_only = female
      q.other = other
      q.required = required
      q.cpc = cpc // CPC
      q.ad_keywords = ad_keywords
      q.wage = cpc * 3600/initial_worker_timeout_in_s // calculate wage from cpc ($/task)

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (form_title != null) { q.title = form_title }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
      if (ad_title != null) { q.ad_title = ad_title }
      if (ad_subtitle != null) { q.ad_subtitle = ad_subtitle }
      if (ad_description != null) { q.ad_description = ad_description }
    }
    a.RadioButtonDistributionQuestion(initf)
  }

  // Instead of writing a list, let the user supply a "big tuple"
  implicit def product2OptionList(p: Product) : List[GQuestionOption] = p.productIterator.toList.asInstanceOf[List[GQuestionOption]]
  implicit def tupSymbString2MTQuestionOption(opt: (Symbol, String))(implicit ga: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2)
  implicit def tupStrURL2MTQuestionOption(opt: (String, String))(implicit ga: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(opt._1), opt._1, opt._2)
  implicit def tupWithURL2MTQuestionOption(opt: (Symbol, String, String))(implicit ga: GoogleAdsAdapter) : GQuestionOption = choice(opt._1, opt._2, opt._3)
  implicit def str2MTQuestionOption(s: String)(implicit ga: GoogleAdsAdapter) : GQuestionOption = choice(Symbol(s), s)
}
