package edu.umass.cs.automan.core.memoizer;

import net.java.ao.Entity;

import java.util.Date;

public interface AnswerMemo extends Entity {
    public String getMemoHash();
    public void setMemoHash(String memo_hash);
    public String getCustomInfo();
    public void setCustomInfo(String i);
    public Boolean getPaidStatus();
    public void setPaidStatus(Boolean s);
    public String getWorkerId();
    public void setWorkerId(String w);
    public Boolean getIsForDistribution();
    public void setIsForDistribution(Boolean b);
    public Date getCompletedAt();
    public void setCompletedAt(Date d);
}