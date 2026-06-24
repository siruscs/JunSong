package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class FinProfitShareDetail
{
    private Long detailId;
    private Long shareId;
    private Long deptId;
    private Long periodId;
    private String receiverType;
    private Long receiverId;
    private String receiverName;
    private BigDecimal investAmount;
    private BigDecimal investRatio;
    private BigDecimal shareAmount;
    private Long paymentId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String receiverType) { this.receiverType = receiverType; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public BigDecimal getInvestAmount() { return investAmount; }
    public void setInvestAmount(BigDecimal investAmount) { this.investAmount = investAmount; }
    public BigDecimal getInvestRatio() { return investRatio; }
    public void setInvestRatio(BigDecimal investRatio) { this.investRatio = investRatio; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
