package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

public class ReceivableCollectionUpdateParams {
    private String collectionStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date promisedPayDate;

    private BigDecimal promisedAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextFollowTime;

    private String followNote;

    public String getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(String collectionStatus) { this.collectionStatus = collectionStatus; }
    public Date getPromisedPayDate() { return promisedPayDate; }
    public void setPromisedPayDate(Date promisedPayDate) { this.promisedPayDate = promisedPayDate; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public void setPromisedAmount(BigDecimal promisedAmount) { this.promisedAmount = promisedAmount; }
    public Date getNextFollowTime() { return nextFollowTime; }
    public void setNextFollowTime(Date nextFollowTime) { this.nextFollowTime = nextFollowTime; }
    public String getFollowNote() { return followNote; }
    public void setFollowNote(String followNote) { this.followNote = followNote; }
}
