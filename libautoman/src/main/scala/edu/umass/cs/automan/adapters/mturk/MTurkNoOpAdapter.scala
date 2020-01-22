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

  override def CheckboxQuestion(init: MTCheckboxQuestion => Unit) = noschedule(CBQFactory(), init)
  override def CheckboxDistributionQuestion(init: MTCheckboxVectorQuestion => Unit) = noschedule(CBDQFactory(), init)
  override def MultiEstimationQuestion(init: MTMultiEstimationQuestion => Unit) = noschedule(MEQFactory(), init)
  override def EstimationQuestion(init: MTEstimationQuestion => Unit)= noschedule(EQFactory(), init)
  override def FreeTextQuestion(init: MTFreeTextQuestion => Unit) = noschedule(FTQFactory(), init)
  override def FreeTextDistributionQuestion(init: MTFreeTextVectorQuestion => Unit) = noschedule(FTDQFactory(), init)
  override def RadioButtonQuestion(init: MTRadioButtonQuestion => Unit) = noschedule(RBQFactory(), init)
  override def RadioButtonDistributionQuestion(init: MTRadioButtonVectorQuestion => Unit) = noschedule(RBDQFactory(), init)
  override def Survey(init: Survey => Unit) = noschedule(SFactory(), init)
}