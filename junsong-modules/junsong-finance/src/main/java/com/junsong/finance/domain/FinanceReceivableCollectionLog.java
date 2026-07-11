package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 应收催收跟进日志对象 finance_receivable_collection_log。
 */
public class FinanceReceivableCollectionLog {

    private Long logId;
    private Long collectionId;
    private String oldStatus;
    private String newStatus;
    private String followNote;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date promisedPayDate;

    private BigDecimal promisedAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextFollowTime;

    private Long operatorId;
    private String operatorName;
    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getFollowNote() { return followNote; }
    public void setFollowNote(String followNote) { this.followNote = followNote; }
    public Date getPromisedPayDate() { return promisedPayDate; }
    public void setPromisedPayDate(Date promisedPayDate) { this.promisedPayDate = promisedPayDate; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public void setPromisedAmount(BigDecimal promisedAmount) { this.promisedAmount = promisedAmount; }
    public Date getNextFollowTime() { return nextFollowTime; }
    public void setNextFollowTime(Date nextFollowTime) { this.nextFollowTime = nextFollowTime; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
