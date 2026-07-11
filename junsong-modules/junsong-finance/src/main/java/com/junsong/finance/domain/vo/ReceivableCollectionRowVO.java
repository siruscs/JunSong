package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

public class ReceivableCollectionRowVO {
    private Long collectionId;
    private Long saleId;
    private String saleNo;
    private Long deptId;
    private String deptName;
    private Long memberId;
    private String customerName;
    private BigDecimal saleAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
    private Integer ageDays = 0;
    private String ageBucket;
    private String collectionStatus;
    private String priorityLevel;
    private Long ownerId;
    private String ownerName;
    private Date promisedPayDate;
    private BigDecimal promisedAmount = BigDecimal.ZERO;
    private Date nextFollowTime;
    private Date lastFollowTime;
    private Integer followCount = 0;

    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public String getSaleNo() { return saleNo; }
    public void setSaleNo(String saleNo) { this.saleNo = saleNo; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getSaleAmount() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount = saleAmount == null ? BigDecimal.ZERO : saleAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount == null ? BigDecimal.ZERO : unpaidAmount; }
    public Integer getAgeDays() { return ageDays; }
    public void setAgeDays(Integer ageDays) { this.ageDays = ageDays == null ? 0 : ageDays; }
    public String getAgeBucket() { return ageBucket; }
    public void setAgeBucket(String ageBucket) { this.ageBucket = ageBucket; }
    public String getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(String collectionStatus) { this.collectionStatus = collectionStatus; }
    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Date getPromisedPayDate() { return promisedPayDate; }
    public void setPromisedPayDate(Date promisedPayDate) { this.promisedPayDate = promisedPayDate; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public void setPromisedAmount(BigDecimal promisedAmount) { this.promisedAmount = promisedAmount == null ? BigDecimal.ZERO : promisedAmount; }
    public Date getNextFollowTime() { return nextFollowTime; }
    public void setNextFollowTime(Date nextFollowTime) { this.nextFollowTime = nextFollowTime; }
    public Date getLastFollowTime() { return lastFollowTime; }
    public void setLastFollowTime(Date lastFollowTime) { this.lastFollowTime = lastFollowTime; }
    public Integer getFollowCount() { return followCount; }
    public void setFollowCount(Integer followCount) { this.followCount = followCount == null ? 0 : followCount; }
}
