package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CashflowPressureVO {
    private Integer pressureScore = 0;
    private String pressureLevel = "LOW";
    private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;
    private BigDecimal overduePromiseAmount = BigDecimal.ZERO;
    private BigDecimal age30PlusAmount = BigDecimal.ZERO;
    private BigDecimal recentCashInAmount = BigDecimal.ZERO;
    private BigDecimal recentExpenseAmount = BigDecimal.ZERO;
    private List<String> reasons = new ArrayList<>();

    public Integer getPressureScore() { return pressureScore; }
    public void setPressureScore(Integer pressureScore) { this.pressureScore = pressureScore; }
    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }
    public BigDecimal getTotalUnpaidAmount() { return totalUnpaidAmount; }
    public void setTotalUnpaidAmount(BigDecimal totalUnpaidAmount) { this.totalUnpaidAmount = totalUnpaidAmount == null ? BigDecimal.ZERO : totalUnpaidAmount; }
    public BigDecimal getOverduePromiseAmount() { return overduePromiseAmount; }
    public void setOverduePromiseAmount(BigDecimal overduePromiseAmount) { this.overduePromiseAmount = overduePromiseAmount == null ? BigDecimal.ZERO : overduePromiseAmount; }
    public BigDecimal getAge30PlusAmount() { return age30PlusAmount; }
    public void setAge30PlusAmount(BigDecimal age30PlusAmount) { this.age30PlusAmount = age30PlusAmount == null ? BigDecimal.ZERO : age30PlusAmount; }
    public BigDecimal getRecentCashInAmount() { return recentCashInAmount; }
    public void setRecentCashInAmount(BigDecimal recentCashInAmount) { this.recentCashInAmount = recentCashInAmount == null ? BigDecimal.ZERO : recentCashInAmount; }
    public BigDecimal getRecentExpenseAmount() { return recentExpenseAmount; }
    public void setRecentExpenseAmount(BigDecimal recentExpenseAmount) { this.recentExpenseAmount = recentExpenseAmount == null ? BigDecimal.ZERO : recentExpenseAmount; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons == null ? new ArrayList<>() : reasons; }
}
