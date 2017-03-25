package edu.umass.cs.automan.adapters.mturk

import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.logging.{LogLevel, LogLevelInfo}

object DSL extends edu.umass.cs.automan.core.DSL {
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
}
