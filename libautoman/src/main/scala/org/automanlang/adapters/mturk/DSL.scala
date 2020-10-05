package org.automanlang.adapters.mturk

import scala.language.implicitConversions
import org.automanlang.core.answer._
import org.automanlang.adapters.mturk.mock.MockSetup
import org.automanlang.adapters.mturk.question.MTQuestionOption
import org.automanlang.core.AutomanAdapter
import org.automanlang.core.logging.LogConfig.LogConfig
import org.automanlang.core.logging.{LogLevel, LogLevelInfo}
import org.automanlang.core.policy.aggregation.MinimumSpawnPolicy

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
