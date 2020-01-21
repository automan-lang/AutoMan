package edu.umass.cs.automan.adapters.mturk

import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.question.{MTCheckboxQuestion, MTCheckboxVectorQuestion, MTEstimationQuestion, MTFreeTextQuestion, MTFreeTextVectorQuestion, MTMultiEstimationQuestion, MTQuestionOption, MTRadioButtonQuestion, MTRadioButtonVectorQuestion, MTSurvey}
import edu.umass.cs.automan.core.NoOpAdapter
import edu.umass.cs.automan.core.answer.{EstimationOutcome, MultiEstimationOutcome, NoAnswer, NoAnswers, NoEstimate, NoMultiEstimate, NoSurveyAnswers, ScalarOutcome, SurveyOutcome, VectorOutcome}
import edu.umass.cs.automan.core.question.{QuestionOption, Survey}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MTurkNoOpAdapter extends NoOpAdapter {
  override type CBQ     = MTCheckboxQuestion
  override type CBDQ    = MTCheckboxVectorQuestion
  override type MEQ     = MTMultiEstimationQuestion
  override type EQ      = MTEstimationQuestion
  override type FTQ     = MTFreeTextQuestion
  override type FTDQ    = MTFreeTextVectorQuestion
  override type RBQ     = MTRadioButtonQuestion
  override type RBDQ    = MTRadioButtonVectorQuestion
  override type MemoDB  = MTMemo
  override type S       = MTSurvey

  override def Option(id: Symbol, text: String): QuestionOption = new MTQuestionOption(id, text, "")

  protected def CBQFactory()  = new MTCheckboxQuestion
  protected def CBDQFactory() = new MTCheckboxVectorQuestion
  protected def MEQFactory()  = new MTMultiEstimationQuestion(true)
  protected def EQFactory()   = new MTEstimationQuestion
  protected def FTQFactory()  = new MTFreeTextQuestion
  protected def FTDQFactory() = new MTFreeTextVectorQuestion
  protected def RBQFactory()  = new MTRadioButtonQuestion
  protected def RBDQFactory() = new MTRadioButtonVectorQuestion
  protected def SFactory()    = new MTSurvey

  override protected def MemoDBFactory(): MTMemo = ???

  override def CheckboxQuestion(init: MTCheckboxQuestion => Unit): ScalarOutcome[Set[Symbol]] = {
    val q = CBQFactory()
    new ScalarOutcome[Set[Symbol]](q, Future{new NoAnswer[Set[Symbol]](q)})
  }

  override def CheckboxDistributionQuestion(init: MTCheckboxVectorQuestion => Unit): VectorOutcome[Set[Symbol]] = {
    val q = CBDQFactory()
    new VectorOutcome[Set[Symbol]](q, Future{new NoAnswers[Set[Symbol]](q)})
  }

  override def MultiEstimationQuestion(init: MTMultiEstimationQuestion => Unit): MultiEstimationOutcome = {
    val q = MEQFactory()
    new MultiEstimationOutcome(q, Future{new NoMultiEstimate(q)})
  }

  override def EstimationQuestion(init: MTEstimationQuestion => Unit): EstimationOutcome = {
    val q = EQFactory()
    new EstimationOutcome(q, Future{new NoEstimate(q)})
  }

  override def FreeTextQuestion(init: MTFreeTextQuestion => Unit): ScalarOutcome[String] = {
    val q = CBQFactory()
    new ScalarOutcome[String](q, Future{new NoAnswer[String](q)})
  }

  override def FreeTextDistributionQuestion(init: MTFreeTextVectorQuestion => Unit): VectorOutcome[String] = {
    val q = CBDQFactory()
    new VectorOutcome[String](q, Future{new NoAnswers[String](q)})
  }

  override def RadioButtonQuestion(init: MTRadioButtonQuestion => Unit): ScalarOutcome[Symbol] = {
    val q = CBQFactory()
    new ScalarOutcome[Symbol](q, Future{new NoAnswer[Symbol](q)})
  }

  override def RadioButtonDistributionQuestion(init: MTRadioButtonVectorQuestion => Unit): VectorOutcome[Symbol] = {
    val q = CBDQFactory()
    new VectorOutcome[Symbol](q, Future{new NoAnswers[Symbol](q)})
  }

  override def Survey(init: Survey => Unit): SurveyOutcome = {
    val q = SFactory()
    new SurveyOutcome(q, Future{new NoSurveyAnswers(q)})
  }

}