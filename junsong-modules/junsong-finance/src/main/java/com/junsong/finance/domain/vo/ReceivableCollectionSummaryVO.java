package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class ReceivableCollectionSummaryVO {
    private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;
    private Long pendingCount = 0L;
    private Long promisedCount = 0L;
    private Long overduePromiseCount = 0L;
    private Long paidCount = 0L;
    private Long age0To7Count = 0L;
    private Long age8To14Count = 0L;
    private Long age15To30Count = 0L;
    private Long age30PlusCount = 0L;
    private BigDecimal promisedAmount = BigDecimal.ZERO;
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    public BigDecimal getTotalUnpaidAmount() { return totalUnpaidAmount; }
    public void setTotalUnpaidAmount(BigDecimal totalUnpaidAmount) { this.totalUnpaidAmount = totalUnpaidAmount == null ? BigDecimal.ZERO : totalUnpaidAmount; }
    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount == null ? 0L : pendingCount; }
    public Long getPromisedCount() { return promisedCount; }
    public void setPromisedCount(Long promisedCount) { this.promisedCount = promisedCount == null ? 0L : promisedCount; }
    public Long getOverduePromiseCount() { return overduePromiseCount; }
    public void setOverduePromiseCount(Long overduePromiseCount) { this.overduePromiseCount = overduePromiseCount == null ? 0L : overduePromiseCount; }
    public Long getPaidCount() { return paidCount; }
    public void setPaidCount(Long paidCount) { this.paidCount = paidCount == null ? 0L : paidCount; }
    public Long getAge0To7Count() { return age0To7Count; }
    public void setAge0To7Count(Long age0To7Count) { this.age0To7Count = age0To7Count == null ? 0L : age0To7Count; }
    public Long getAge8To14Count() { return age8To14Count; }
    public void setAge8To14Count(Long age8To14Count) { this.age8To14Count = age8To14Count == null ? 0L : age8To14Count; }
    public Long getAge15To30Count() { return age15To30Count; }
    public void setAge15To30Count(Long age15To30Count) { this.age15To30Count = age15To30Count == null ? 0L : age15To30Count; }
    public Long getAge30PlusCount() { return age30PlusCount; }
    public void setAge30PlusCount(Long age30PlusCount) { this.age30PlusCount = age30PlusCount == null ? 0L : age30PlusCount; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public void setPromisedAmount(BigDecimal promisedAmount) { this.promisedAmount = promisedAmount == null ? BigDecimal.ZERO : promisedAmount; }
    public BigDecimal getRecoveredAmount() { return recoveredAmount; }
    public void setRecoveredAmount(BigDecimal recoveredAmount) { this.recoveredAmount = recoveredAmount == null ? BigDecimal.ZERO : recoveredAmount; }
}
