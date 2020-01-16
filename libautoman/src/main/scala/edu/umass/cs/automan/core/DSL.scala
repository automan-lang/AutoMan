package edu.umass.cs.automan.core

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.question.confidence._
import edu.umass.cs.automan.core.policy.aggregation._
import edu.umass.cs.automan.core.mock._

trait DSL {
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

  // DSL constructs
  def estimate[A <: AutomanAdapter](
               confidence_interval: ConfidenceInterval = UnconstrainedCI(),
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
               title: String = null,
               wage: BigDecimal = MagicNumbers.USFederalMinimumWage
              )
              (implicit a: A) : EstimationOutcome = {
    def initf[Q <: EstimationQuestion](q: Q) = {
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
    }
    a.EstimationQuestion(initf)
  }

  def multiestimate[A <: AutomanAdapter](
                     dimensions: Array[Dimension],
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
                     title: String = null,
                     wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                   )
                   (implicit a: A) : MultiEstimationOutcome = {
    def initf[Q <: MultiEstimationQuestion](q: Q) = {
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

      // optional parameters
      if (default_sample_size != -1 && default_sample_size > 0) { q.default_sample_size = default_sample_size }
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.MultiEstimationQuestion(initf)
  }

  def freetext[A <: AutomanAdapter](
                        allow_empty_pattern: Boolean = false,
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
                        pattern: String,
                        pattern_error_text: String = null,
                        question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                        text: String,
                        title: String = null,
                        wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                       )
                       (implicit a: A) : ScalarOutcome[String] = {
    def initf[Q <: FreeTextQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.FreeTextQuestion(initf)
  }

  def freetexts[A <: AutomanAdapter](
                       allow_empty_pattern: Boolean = false,
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
                       wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                     )
                     (implicit a: A) : VectorOutcome[String] = {
    def initf[Q <: FreeTextVectorQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (pattern != null) { q.pattern = pattern }
      if (pattern_error_text != null) { q.pattern_error_text = pattern_error_text }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.FreeTextDistributionQuestion(initf)
  }

  def checkbox[A <: AutomanAdapter, O](
                       confidence: Double = MagicNumbers.DefaultConfidence,
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
                       wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                     )
                     (implicit a: A) : ScalarOutcome[Set[Symbol]] = {
    def initf[Q <: CheckboxQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.CheckboxQuestion(initf)
  }

  def checkboxes[A <: AutomanAdapter, O](
                      sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
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
                      wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                    )
                    (implicit a: A) : VectorOutcome[Set[Symbol]] = {
    def initf[Q <: CheckboxVectorQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.CheckboxDistributionQuestion(initf)
  }

  def radio[A <: AutomanAdapter, O](
                          confidence: Double = MagicNumbers.DefaultConfidence,
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
                          wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                        )
                        (implicit a: A) : ScalarOutcome[Symbol] = {
    def initf[Q <: RadioButtonQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.RadioButtonQuestion(initf)
  }

  def radios[A <: AutomanAdapter, O](
                           sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
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
                           wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                         )
                         (implicit a: A) : VectorOutcome[Symbol] = {
    def initf[Q <: RadioButtonVectorQuestion](q: Q) = {
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

      // optional parameters
      if (image_alt_text != null) { q.image_alt_text = image_alt_text }
      if (image_url != null) { q.image_url = image_url }
      if (title != null) { q.title = title }
      if (mock_answers != null ) { q.mock_answers = mock_answers }
      if (minimum_spawn_policy != null) { q.minimum_spawn_policy = minimum_spawn_policy }
    }
    a.RadioButtonDistributionQuestion(initf)
  }

  def survey[A <: AutomanAdapter, O](
                                      allow_empty_pattern: Boolean = false,
                                      confidence_interval: ConfidenceInterval = UnconstrainedCI(), // TODO: need every possible param?
                                      sample_size: Int = MagicNumbers.DefaultSampleSizeForDistrib,
                                      confidence: Double = MagicNumbers.DefaultConfidence,
                                      dimensions: Array[Dimension],
                                      default_sample_size: Int = -1,
                                      budget: BigDecimal = MagicNumbers.DefaultBudget,
                                      dont_reject: Boolean = true,
                                      dry_run: Boolean = false,
                                      estimator: Seq[Double] => Double = null,
                                      image_alt_text: String = null,
                                      image_url: String = null,
                                      initial_worker_timeout_in_s: Int = MagicNumbers.InitialWorkerTimeoutInS,
                                      max_value: Double = Double.MaxValue,
                                      minimum_spawn_policy: MinimumSpawnPolicy = null,
                                      min_value: Double = Double.MinValue,
                                      d_mock_answers: Iterable[MockAnswer[Double]] = null,
                                      darr_mock_answers: Iterable[MockAnswer[Array[Double]]] = null,
                                      str_mock_answers: Iterable[MockAnswer[String]] = null,
                                      sym_mock_answers: Iterable[MockAnswer[Symbol]] = null,
                                      symset_mock_answers: Iterable[MockAnswer[Set[Symbol]]] = null,
                                      options: List[AnyRef], // these def need to be in questions
                                      pattern: String,
                                      pattern_error_text: String = null,
                                      pay_all_on_failure: Boolean = true,
                                      question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier,
                                      text: String,
                                      title: String = null,
                                      wage: BigDecimal = MagicNumbers.USFederalMinimumWage
                                    )
                                    (implicit a: A) : SurveyOutcome = {
    def initf[Q <: Survey](rvq: Q) = {
      // mandatory parameters
      rvq.text = text
      // need survey-specific text, etc
      //q.question_list.foreach()
      for(qu: Question <- rvq.question_list){
        qu match {
          case e: EstimationQuestion => {
            // mandatory parameters
            e.text = text

            // mandatory parameters with sane defaults
            e.confidence_interval = confidence_interval
            e.confidence = confidence
            e.budget = budget
            e.dont_reject = dont_reject
            e.dry_run = dry_run
            e.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            e.pay_all_on_failure = pay_all_on_failure
            e.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (default_sample_size != -1 && default_sample_size > 0) { e.default_sample_size = default_sample_size }
            if (estimator != null) { e.estimator = estimator }
            if (image_alt_text != null) { e.image_alt_text = image_alt_text }
            if (image_url != null) { e.image_url = image_url }
            if (max_value != Double.MaxValue) { e.max_value = max_value }
            if (min_value != Double.MinValue) { e.min_value = min_value }
            if (title != null) { e.title = title }
            if (d_mock_answers != null ) { e.mock_answers = d_mock_answers }
            if (minimum_spawn_policy != null) { e.minimum_spawn_policy = minimum_spawn_policy }
          }
          case me: MultiEstimationQuestion => {
            // mandatory parameters
            me.dimensions = dimensions
            me.text = text

            // mandatory parameters with sane defaults
            me.confidence = confidence
            me.budget = budget
            me.dont_reject = dont_reject
            me.dry_run = dry_run
            me.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            me.pay_all_on_failure = pay_all_on_failure
            me.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (default_sample_size != -1 && default_sample_size > 0) { me.default_sample_size = default_sample_size }
            if (image_alt_text != null) { me.image_alt_text = image_alt_text }
            if (image_url != null) { me.image_url = image_url }
            if (title != null) { me.title = title }
            if (darr_mock_answers != null ) { me.mock_answers = darr_mock_answers }
            if (minimum_spawn_policy != null) { me.minimum_spawn_policy = minimum_spawn_policy }
          }
          case f: FreeTextQuestion => {
            // mandatory parameters
            f.text = text
            f.pattern = pattern

            // mandatory parameters with sane defaults
            f.allow_empty_pattern = allow_empty_pattern
            f.confidence = confidence
            f.budget = budget
            f.dont_reject = dont_reject
            f.dry_run = dry_run
            f.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            f.pay_all_on_failure = pay_all_on_failure
            f.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) {
              f.image_alt_text = image_alt_text
            }
            if (image_url != null) {
              f.image_url = image_url
            }
            if (pattern_error_text != null) {
              f.pattern_error_text = pattern_error_text
            }
            if (title != null) {
              f.title = title
            }
            if (str_mock_answers != null) {
              f.mock_answers = str_mock_answers
            }
            if (minimum_spawn_policy != null) {
              f.minimum_spawn_policy = minimum_spawn_policy
            }
          }
          case fv: FreeTextVectorQuestion => {
            // mandatory parameters
            fv.text = text

            // mandatory parameters with sane defaults
            fv.allow_empty_pattern = allow_empty_pattern
            fv.sample_size = sample_size
            fv.budget = budget
            fv.dont_reject = dont_reject
            fv.dry_run = dry_run
            fv.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            fv.pay_all_on_failure = pay_all_on_failure
            fv.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { fv.image_alt_text = image_alt_text }
            if (image_url != null) { fv.image_url = image_url }
            if (pattern != null) { fv.pattern = pattern }
            if (pattern_error_text != null) { fv.pattern_error_text = pattern_error_text }
            if (title != null) { fv.title = title }
            if (str_mock_answers != null ) { fv.mock_answers = str_mock_answers }
            if (minimum_spawn_policy != null) { fv.minimum_spawn_policy = minimum_spawn_policy }
          }
          case cbq: CheckboxQuestion => {
            // mandatory parameters
            cbq.text = text
            cbq.options = options.asInstanceOf[List[cbq.QuestionOptionType]]  // yeah... ugly

            // mandatory parameters with sane defaults
            cbq.confidence = confidence
            cbq.budget = budget
            cbq.dont_reject = dont_reject
            cbq.dry_run = dry_run
            cbq.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            cbq.pay_all_on_failure = pay_all_on_failure
            cbq.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { cbq.image_alt_text = image_alt_text }
            if (image_url != null) { cbq.image_url = image_url }
            if (title != null) { cbq.title = title }
            if (symset_mock_answers != null ) { cbq.mock_answers = symset_mock_answers }
            if (minimum_spawn_policy != null) { cbq.minimum_spawn_policy = minimum_spawn_policy }
          }
          case cbvq: CheckboxVectorQuestion => {
            // mandatory parameters
            cbvq.text = text
            cbvq.options = options.asInstanceOf[List[cbvq.QuestionOptionType]]  // yeah... ugly

            // mandatory parameters with sane defaults
            cbvq.sample_size = sample_size
            cbvq.budget = budget
            cbvq.dont_reject = dont_reject
            cbvq.dry_run = dry_run
            cbvq.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            cbvq.pay_all_on_failure = pay_all_on_failure
            cbvq.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { cbvq.image_alt_text = image_alt_text }
            if (image_url != null) { cbvq.image_url = image_url }
            if (title != null) { cbvq.title = title }
            if (symset_mock_answers != null ) { cbvq.mock_answers = symset_mock_answers }
            if (minimum_spawn_policy != null) { cbvq.minimum_spawn_policy = minimum_spawn_policy }
            cbvq.options = options.asInstanceOf[List[cbvq.QuestionOptionType]]  // yeah... ugly

            // mandatory parameters with sane defaults
            cbvq.sample_size = sample_size
            cbvq.budget = budget
            cbvq.dont_reject = dont_reject
            cbvq.dry_run = dry_run
            cbvq.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            cbvq.pay_all_on_failure = pay_all_on_failure
            cbvq.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { cbvq.image_alt_text = image_alt_text }
            if (image_url != null) { cbvq.image_url = image_url }
            if (title != null) { cbvq.title = title }
            if (symset_mock_answers != null ) { cbvq.mock_answers = symset_mock_answers }
            if (minimum_spawn_policy != null) { cbvq.minimum_spawn_policy = minimum_spawn_policy }
          }
          case rq: RadioButtonQuestion => {
            // mandatory parameters
            rq.text = text
            rq.options = options.asInstanceOf[List[rq.QuestionOptionType]]  // yeah... ugly

            // mandatory parameters with sane defaults
            rq.confidence = confidence
            rq.budget = budget
            rq.dont_reject = dont_reject
            rq.dry_run = dry_run
            rq.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            rq.pay_all_on_failure = pay_all_on_failure
            rq.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { rq.image_alt_text = image_alt_text }
            if (image_url != null) { rq.image_url = image_url }
            if (title != null) { rq.title = title }
            if (sym_mock_answers != null ) { rq.mock_answers = sym_mock_answers }
            if (minimum_spawn_policy != null) { rq.minimum_spawn_policy = minimum_spawn_policy }
          }
          case rvq: RadioButtonVectorQuestion => {
            // mandatory parameters
            rvq.text = text
            rvq.options = options.asInstanceOf[List[rvq.QuestionOptionType]]  // yeah... ugly

            // mandatory parameters with sane defaults
            rvq.sample_size = sample_size
            rvq.budget = budget
            rvq.dont_reject = dont_reject
            rvq.dry_run = dry_run
            rvq.initial_worker_timeout_in_s = initial_worker_timeout_in_s
            rvq.pay_all_on_failure = pay_all_on_failure
            rvq.question_timeout_multiplier = question_timeout_multiplier

            // optional parameters
            if (image_alt_text != null) { rvq.image_alt_text = image_alt_text }
            if (image_url != null) { rvq.image_url = image_url }
            if (title != null) { rvq.title = title }
            if (sym_mock_answers != null ) { rvq.mock_answers = sym_mock_answers }
            if (minimum_spawn_policy != null) { rvq.minimum_spawn_policy = minimum_spawn_policy }
          }
        }

      }

    }
    a.Survey(initf)
  }
}
