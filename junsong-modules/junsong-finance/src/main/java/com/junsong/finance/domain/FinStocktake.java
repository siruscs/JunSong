package com.junsong.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 库存盘点任务头表 finance_stocktake。
 *
 * 状态机：DRAFT → COUNTING → SUBMITTED → RECOUNTING → APPROVED → POSTED
 * 异常终态：CANCELLED（过账前取消）、REVERSED（过账后整单冲销）
 *
 * @author junsong
 */
public class FinStocktake extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long stocktakeId;
    private Long tenantId;
    private String takeNo;
    private Long deptId;
    private String scopeType;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date freezeTime;
    private Long counterUserId;
    private Long recountUserId;
    private String submittedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submittedTime;
    private String approvedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approvedTime;
    private String postedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date postedTime;
    private String reversedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reversedTime;
    private String reversalReason;
    private Integer version;

    public Long getStocktakeId() { return stocktakeId; }
    public void setStocktakeId(Long stocktakeId) { this.stocktakeId = stocktakeId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTakeNo() { return takeNo; }
    public void setTakeNo(String takeNo) { this.takeNo = takeNo; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getFreezeTime() { return freezeTime; }
    public void setFreezeTime(Date freezeTime) { this.freezeTime = freezeTime; }

    public Long getCounterUserId() { return counterUserId; }
    public void setCounterUserId(Long counterUserId) { this.counterUserId = counterUserId; }

    public Long getRecountUserId() { return recountUserId; }
    public void setRecountUserId(Long recountUserId) { this.recountUserId = recountUserId; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Date getSubmittedTime() { return submittedTime; }
    public void setSubmittedTime(Date submittedTime) { this.submittedTime = submittedTime; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Date getApprovedTime() { return approvedTime; }
    public void setApprovedTime(Date approvedTime) { this.approvedTime = approvedTime; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public Date getPostedTime() { return postedTime; }
    public void setPostedTime(Date postedTime) { this.postedTime = postedTime; }

    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }

    public Date getReversedTime() { return reversedTime; }
    public void setReversedTime(Date reversedTime) { this.reversedTime = reversedTime; }

    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
