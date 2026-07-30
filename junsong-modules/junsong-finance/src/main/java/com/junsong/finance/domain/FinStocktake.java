package com.junsong.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
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

    @Excel(name = "盘点ID", cellType = ColumnType.NUMERIC)
    private Long stocktakeId;
    private Long tenantId;
    @Excel(name = "盘点单号")
    private String takeNo;
    @Excel(name = "门店ID", cellType = ColumnType.NUMERIC)
    private Long deptId;
    @Excel(name = "盘点范围")
    private String scopeType;
    @Excel(name = "状态")
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "冻结时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date freezeTime;
    @Excel(name = "盘点人ID", cellType = ColumnType.NUMERIC)
    private Long counterUserId;
    @Excel(name = "复盘人ID", cellType = ColumnType.NUMERIC)
    private Long recountUserId;
    @Excel(name = "提交人")
    private String submittedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提交时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date submittedTime;
    @Excel(name = "审批人")
    private String approvedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approvedTime;
    @Excel(name = "过账人")
    private String postedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "过账时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date postedTime;
    @Excel(name = "冲销人")
    private String reversedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "冲销时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date reversedTime;
    @Excel(name = "冲销原因")
    private String reversalReason;
    private String reverseIdempotencyKey;
    /** 工作流实例ID（Flowable processInstanceId） */
    private String processInstanceId;
    /** 流程定义Key（如 stocktake_apply） */
    private String processDefinitionKey;
    /** 业务Key（盘点单号 takeNo） */
    private String businessKey;
    /** 当前流程节点（由工作流同步回写） */
    private String currentNode;
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

    public String getReverseIdempotencyKey() { return reverseIdempotencyKey; }
    public void setReverseIdempotencyKey(String reverseIdempotencyKey) { this.reverseIdempotencyKey = reverseIdempotencyKey; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
