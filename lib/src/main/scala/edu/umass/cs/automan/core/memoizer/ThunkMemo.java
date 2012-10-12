package edu.umass.cs.automan.core.memoizer;

import net.java.ao.Entity;

import java.util.Date;

public interface ThunkMemo extends Entity {
    public Date getCreationTime();
    public void setCreationTime(Date d);
    public Date getCompletionTime();
    public void setCompletionTime(Date d);
    public Integer getCostInCents();
    public void setCostInCents(Integer c);
    public String getWorkerId();
    public void setWorkerId(String id);
    public String getQuestionId();
    public void setQuestionId(String id);
    public String getComputationId();
    public void setComputationId(String id);
    public Date getExpirationDate();
    public void setExpirationDate(Date d);
    public MemoState getState();
    public void setState(MemoState s);
}
