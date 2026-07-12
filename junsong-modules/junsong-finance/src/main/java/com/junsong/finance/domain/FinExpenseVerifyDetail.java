package com.junsong.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 费用核销费用明细快照。 */
public class FinExpenseVerifyDetail implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long detailId;
    private Long batchId;
    private Long expenseId;
    private Long tenantId;
    private Long deptId;
    private BigDecimal expenseAmount;
    private String originalStatus;
    private Long originalAdvanceId;
    private Long periodId;
    private Date createTime;
    /** 展示用：费用单号（JOIN fin_expense 获取，不持久化到明细表）。 */
    private String expenseNo;
    /** 展示用：费用内容。 */
    private String expenseContent;
    /** 展示用：费用日期。 */
    private Date expenseDate;
    /** 展示用：费用类型。 */
    private String expenseType;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }
    public String getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(String originalStatus) { this.originalStatus = originalStatus; }
    public Long getOriginalAdvanceId() { return originalAdvanceId; }
    public void setOriginalAdvanceId(Long originalAdvanceId) { this.originalAdvanceId = originalAdvanceId; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getExpenseNo() { return expenseNo; }
    public void setExpenseNo(String expenseNo) { this.expenseNo = expenseNo; }
    public String getExpenseContent() { return expenseContent; }
    public void setExpenseContent(String expenseContent) { this.expenseContent = expenseContent; }
    public Date getExpenseDate() { return expenseDate; }
    public void setExpenseDate(Date expenseDate) { this.expenseDate = expenseDate; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
}
