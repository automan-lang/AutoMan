package org.automanlang.core

import org.automanlang.core.answer._
import org.automanlang.core.question._
import org.automanlang.core.question.confidence._
import org.automanlang.core.policy.aggregation._
import org.automanlang.core.mock._

trait DSL {
  val automan = org.automanlang.automan
  val LogConfig = org.automanlang.core.logging.LogConfig
  val Utilities = org.automanlang.core.util.Utilities

  // to simplify imports
  type Answer[T] = org.automanlang.core.answer.Answer[T]
  type Answers[T] = org.automanlang.core.answer.Answers[T]
  type AsymmetricCI = org.automanlang.core.question.confidence.AsymmetricCI
  type DistributionOutcome[T] = org.automanlang.core.answer.VectorOutcome[T]
  type Estimate = org.automanlang.core.answer.Estimate
  type IncompleteAnswers[T] = org.automanlang.core.answer.IncompleteAnswers[T]
  type LowConfidenceAnswer[T] = org.automanlang.core.answer.LowConfidenceAnswer[T]
  type LowConfidenceEstimate = org.automanlang.core.answer.LowConfidenceEstimate
  type OverBudgetAnswer[T] = org.automanlang.core.answer.OverBudgetAnswer[T]
  type OverBudgetAnswers[T] = org.automanlang.core.answer.OverBudgetAnswers[T]
  type OverBudgetEstimate = org.automanlang.core.answer.OverBudgetEstimate
  type ScalarOutcome[T] = org.automanlang.core.answer.ScalarOutcome[T]
  type Outcome[T] = org.automanlang.core.answer.Outcome[T]

  //type CBQuestion = org.automanlang.core.info.QuestionType.QuestionType

  // to simplify pattern matching
  val Answer = org.automanlang.core.answer.Answer
  val Answers = org.automanlang.core.answer.Answers
  val Estimate = org.automanlang.core.answer.Estimate
  val IncompleteAnswers = org.automanlang.core.answer.IncompleteAnswers
  val LowConfidenceAnswer = org.automanlang.core.answer.LowConfidenceAnswer
  val LowConfidenceEstimate = org.automanlang.core.answer.LowConfidenceEstimate
  val OverBudgetAnswer = org.automanlang.core.answer.OverBudgetAnswer
  val OverBudgetAnswers = org.automanlang.core.answer.OverBudgetAnswers
  val OverBudgetEstimate = org.automanlang.core.answer.OverBudgetEstimate
  val SymmetricCI = org.automanlang.core.question.confidence.SymmetricCI
  val UnconstrainedCI = org.automanlang.core.question.confidence.UnconstrainedCI

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
                                   (implicit a: A): EstimationOutcome = {
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
      if (default_sample_size != -1 && default_sample_size > 0) {
        q.default_sample_size = default_sample_size
      }
      if (estimator != null) {
        q.estimator = estimator
      }
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (max_value != Double.MaxValue) {
        q.max_value = max_value
      }
      if (min_value != Double.MinValue) {
        q.min_value = min_value
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                        (implicit a: A): MultiEstimationOutcome = {
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
      if (default_sample_size != -1 && default_sample_size > 0) {
        q.default_sample_size = default_sample_size
      }
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                   (implicit a: A): ScalarOutcome[String] = {
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
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (pattern_error_text != null) {
        q.pattern_error_text = pattern_error_text
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                    (implicit a: A): VectorOutcome[String] = {
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
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (pattern != null) {
        q.pattern = pattern
      }
      if (pattern_error_text != null) {
        q.pattern_error_text = pattern_error_text
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                      (implicit a: A): ScalarOutcome[Set[Symbol]] = {
    def initf[Q <: CheckboxQuestion](q: Q) = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]] // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier

      // optional parameters
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                        (implicit a: A): VectorOutcome[Set[Symbol]] = {
    def initf[Q <: CheckboxVectorQuestion](q: Q) = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]] // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier

      // optional parameters
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                   (implicit a: A): ScalarOutcome[Symbol] = {
    def initf[Q <: RadioButtonQuestion](q: Q) = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]] // yeah... ugly

      // mandatory parameters with sane defaults
      q.confidence = confidence
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier

      // optional parameters
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
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
                                    (implicit a: A): VectorOutcome[Symbol] = {
    def initf[Q <: RadioButtonVectorQuestion](q: Q) = {
      // mandatory parameters
      q.text = text
      q.options = options.asInstanceOf[List[q.QuestionOptionType]] // yeah... ugly

      // mandatory parameters with sane defaults
      q.sample_size = sample_size
      q.budget = budget
      q.dont_reject = dont_reject
      q.dry_run = dry_run
      q.initial_worker_timeout_in_s = initial_worker_timeout_in_s
      q.pay_all_on_failure = pay_all_on_failure
      q.question_timeout_multiplier = question_timeout_multiplier

      // optional parameters
      if (image_alt_text != null) {
        q.image_alt_text = image_alt_text
      }
      if (image_url != null) {
        q.image_url = image_url
      }
      if (title != null) {
        q.title = title
      }
      if (mock_answers != null) {
        q.mock_answers = mock_answers
      }
      if (minimum_spawn_policy != null) {
        q.minimum_spawn_policy = minimum_spawn_policy
      }
    }

    a.RadioButtonDistributionQuestion(initf)
  }
}
