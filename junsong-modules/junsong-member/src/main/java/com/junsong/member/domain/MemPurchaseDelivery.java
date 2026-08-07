package com.junsong.member.domain;

import java.math.BigDecimal;
import java.util.Date;

public class MemPurchaseDelivery
{
    private Long deliveryId;
    private Long purchaseId;
    private Long itemId;
    private Long tenantId;
    private Long deptId;
    private String deliveryNo;
    private BigDecimal saleDeliveryQuantity;
    private BigDecimal giftDeliveryQuantity;
    private BigDecimal totalDeliveryQuantity;
    private Date deliveryDate;
    private String receiverName;
    private Long operatorId;
    private String operatorName;
    private String status;
    private String idempotencyKey;
    private String remark;
    public Long getDeliveryId() { return deliveryId; } public void setDeliveryId(Long v) { deliveryId = v; }
    public Long getPurchaseId() { return purchaseId; } public void setPurchaseId(Long v) { purchaseId = v; }
    public Long getItemId() { return itemId; } public void setItemId(Long v) { itemId = v; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long v) { deptId = v; }
    public String getDeliveryNo() { return deliveryNo; } public void setDeliveryNo(String v) { deliveryNo = v; }
    public BigDecimal getSaleDeliveryQuantity() { return saleDeliveryQuantity; } public void setSaleDeliveryQuantity(BigDecimal v) { saleDeliveryQuantity = v; }
    public BigDecimal getGiftDeliveryQuantity() { return giftDeliveryQuantity; } public void setGiftDeliveryQuantity(BigDecimal v) { giftDeliveryQuantity = v; }
    public BigDecimal getTotalDeliveryQuantity() { return totalDeliveryQuantity; } public void setTotalDeliveryQuantity(BigDecimal v) { totalDeliveryQuantity = v; }
    public Date getDeliveryDate() { return deliveryDate; } public void setDeliveryDate(Date v) { deliveryDate = v; }
    public String getReceiverName() { return receiverName; } public void setReceiverName(String v) { receiverName = v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { operatorId = v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { operatorName = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
