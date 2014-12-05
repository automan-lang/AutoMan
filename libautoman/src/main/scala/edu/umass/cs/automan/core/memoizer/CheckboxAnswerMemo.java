package edu.umass.cs.automan.core.memoizer;

import net.java.ao.Entity;

public interface CheckboxAnswerMemo extends AnswerMemo {
  public String getAnswerValues();
  public void setAnswerValues(String values);
}