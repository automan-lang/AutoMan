package edu.umass.cs.automan.core.memoizer;

import net.java.ao.Entity;

public interface FreeTextAnswerMemo extends AnswerMemo {
  public String getAnswerValue();
  public void setAnswerValue(String value);
}