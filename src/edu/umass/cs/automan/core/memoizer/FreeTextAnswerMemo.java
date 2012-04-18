package edu.umass.cs.automan.core.memoizer;

import net.java.ao.Entity;

public interface FreeTextAnswerMemo extends Entity {
  public String getMemoHash();
  public void setMemoHash(String memo_hash);
  public String getAnswerValue();
  public void setAnswerValue(String value);
  public String getCustomInfo();
  public void setCustomInfo(String i);
  public Boolean getPaidStatus();
  public void setPaidStatus(Boolean s);
  public String getWorkerId();
  public void setWorkerId(String w);
}