package com.junsong.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.junsong.common.core.web.domain.BaseEntity;

public class MemPurchaseReturn extends BaseEntity
{
    private Long returnId, tenantId, deptId, purchaseId, originalPeriodId, returnPeriodId, memberId;
    private String returnNo, customerType, customerName, customerPhone, status, reason, remark, idempotencyKey, delFlag;
    private Date returnDate;
    private BigDecimal refundAmount, refundedAmount;
    private Long version;
    private List<MemPurchaseReturnItem> items;
    public Long getReturnId(){return returnId;} public void setReturnId(Long v){returnId=v;}
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;}
    public Long getPurchaseId(){return purchaseId;} public void setPurchaseId(Long v){purchaseId=v;}
    public Long getOriginalPeriodId(){return originalPeriodId;} public void setOriginalPeriodId(Long v){originalPeriodId=v;}
    public Long getReturnPeriodId(){return returnPeriodId;} public void setReturnPeriodId(Long v){returnPeriodId=v;}
    public Long getMemberId(){return memberId;} public void setMemberId(Long v){memberId=v;}
    public String getReturnNo(){return returnNo;} public void setReturnNo(String v){returnNo=v;}
    public String getCustomerType(){return customerType;} public void setCustomerType(String v){customerType=v;}
    public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;}
    public String getCustomerPhone(){return customerPhone;} public void setCustomerPhone(String v){customerPhone=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public String getDelFlag(){return delFlag;} public void setDelFlag(String v){delFlag=v;}
    public Date getReturnDate(){return returnDate;} public void setReturnDate(Date v){returnDate=v;}
    public BigDecimal getRefundAmount(){return refundAmount;} public void setRefundAmount(BigDecimal v){refundAmount=v;}
    public BigDecimal getRefundedAmount(){return refundedAmount;} public void setRefundedAmount(BigDecimal v){refundedAmount=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
    public List<MemPurchaseReturnItem> getItems(){return items;} public void setItems(List<MemPurchaseReturnItem> v){items=v;}
}
