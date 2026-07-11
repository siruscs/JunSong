package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 复合核算试算结果 VO(不落库)
 *
 * @author junsong
 */
public class CompositeTrialResultVO {

    /** 本次纳入金额合计 */
    private BigDecimal currentIncludeAmount = BigDecimal.ZERO;

    /** 试算后累计回本金额 */
    private BigDecimal totalReturnAmount = BigDecimal.ZERO;

    /** 试算后回本缺口 */
    private BigDecimal breakEvenGap = BigDecimal.ZERO;

    /** 试算后超额收益 */
    private BigDecimal overReturnAmount = BigDecimal.ZERO;

    /** 共享投资人总出资 */
    private BigDecimal totalInvestAmount = BigDecimal.ZERO;

    /** 是否达到整体回本 */
    private Boolean breakEvenReached = false;

    /** 试算后回本进度(0-1) */
    private BigDecimal breakEvenProgress = BigDecimal.ZERO;

    /** 投资人分摊结果 */
    private List<InvestorAllocationRow> investorAllocations;

    public BigDecimal getCurrentIncludeAmount() { return currentIncludeAmount; }
    public void setCurrentIncludeAmount(BigDecimal currentIncludeAmount) { this.currentIncludeAmount = currentIncludeAmount; }
    public BigDecimal getTotalReturnAmount() { return totalReturnAmount; }
    public void setTotalReturnAmount(BigDecimal totalReturnAmount) { this.totalReturnAmount = totalReturnAmount; }
    public BigDecimal getBreakEvenGap() { return breakEvenGap; }
    public void setBreakEvenGap(BigDecimal breakEvenGap) { this.breakEvenGap = breakEvenGap; }
    public BigDecimal getOverReturnAmount() { return overReturnAmount; }
    public void setOverReturnAmount(BigDecimal overReturnAmount) { this.overReturnAmount = overReturnAmount; }
    public BigDecimal getTotalInvestAmount() { return totalInvestAmount; }
    public void setTotalInvestAmount(BigDecimal totalInvestAmount) { this.totalInvestAmount = totalInvestAmount; }
    public Boolean getBreakEvenReached() { return breakEvenReached; }
    public void setBreakEvenReached(Boolean breakEvenReached) { this.breakEvenReached = breakEvenReached; }
    public BigDecimal getBreakEvenProgress() { return breakEvenProgress; }
    public void setBreakEvenProgress(BigDecimal breakEvenProgress) { this.breakEvenProgress = breakEvenProgress; }
    public List<InvestorAllocationRow> getInvestorAllocations() { return investorAllocations; }
    public void setInvestorAllocations(List<InvestorAllocationRow> investorAllocations) { this.investorAllocations = investorAllocations; }

    /**
     * 投资人分摊行
     */
    public static class InvestorAllocationRow {
        private Long investorId;
        private String investorName;
        private BigDecimal investAmount;
        private BigDecimal investRatio;
        /** 本次分摊金额 */
        private BigDecimal currentAllocation;
        /** 累计已回本金额(含本次) */
        private BigDecimal totalReturned;

        public Long getInvestorId() { return investorId; }
        public void setInvestorId(Long investorId) { this.investorId = investorId; }
        public String getInvestorName() { return investorName; }
        public void setInvestorName(String investorName) { this.investorName = investorName; }
        public BigDecimal getInvestAmount() { return investAmount; }
        public void setInvestAmount(BigDecimal investAmount) { this.investAmount = investAmount; }
        public BigDecimal getInvestRatio() { return investRatio; }
        public void setInvestRatio(BigDecimal investRatio) { this.investRatio = investRatio; }
        public BigDecimal getCurrentAllocation() { return currentAllocation; }
        public void setCurrentAllocation(BigDecimal currentAllocation) { this.currentAllocation = currentAllocation; }
        public BigDecimal getTotalReturned() { return totalReturned; }
        public void setTotalReturned(BigDecimal totalReturned) { this.totalReturned = totalReturned; }
    }
}
