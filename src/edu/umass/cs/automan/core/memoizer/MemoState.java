package edu.umass.cs.automan.core.memoizer;

public enum MemoState {
  RUNNING, // task has been sent to crowdsourcing backend but no answer retrieved
  RETRIEVED, // answer has been retrieved
  ACCEPTED, // answer has been paid for
  REJECTED, // answer is incorrect (and was not paid for)
  TIMEOUT, // thunk timed out (reschedule)
  CANCELLED
}
