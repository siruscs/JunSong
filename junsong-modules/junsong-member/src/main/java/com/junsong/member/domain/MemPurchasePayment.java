package com.junsong.member.domain;

import java.math.BigDecimal;
import java.util.Date;

public class MemPurchasePayment
{
    private Long paymentId;
    private Long purchaseId;
    private Long tenantId;
    private Long deptId;
    private String paymentNo;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private Date paymentDate;
    private Long operatorId;
    private String operatorName;
    private String status;
    private String idempotencyKey;
    private String remark;
    public Long getPaymentId() { return paymentId; } public void setPaymentId(Long v) { paymentId = v; }
    public Long getPurchaseId() { return purchaseId; } public void setPurchaseId(Long v) { purchaseId = v; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long v) { deptId = v; }
    public String getPaymentNo() { return paymentNo; } public void setPaymentNo(String v) { paymentNo = v; }
    public BigDecimal getPaymentAmount() { return paymentAmount; } public void setPaymentAmount(BigDecimal v) { paymentAmount = v; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { paymentMethod = v; }
    public Date getPaymentDate() { return paymentDate; } public void setPaymentDate(Date v) { paymentDate = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { operatorName = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
