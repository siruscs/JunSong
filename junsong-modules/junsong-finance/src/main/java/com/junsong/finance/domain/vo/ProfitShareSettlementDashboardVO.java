package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProfitShareSettlementDashboardVO {
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private BigDecimal managerShare = BigDecimal.ZERO;
    private BigDecimal investorShare = BigDecimal.ZERO;
    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal managerProfitRate = BigDecimal.ZERO;
    private List<Map<String, Object>> deptSettlementRows;
    private List<ProfitShareExceptionVO> exceptions;

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    public BigDecimal getManagerShare() { return managerShare; }
    public void setManagerShare(BigDecimal managerShare) { this.managerShare = managerShare; }
    public BigDecimal getInvestorShare() { return investorShare; }
    public void setInvestorShare(BigDecimal investorShare) { this.investorShare = investorShare; }
    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getManagerProfitRate() { return managerProfitRate; }
    public void setManagerProfitRate(BigDecimal managerProfitRate) { this.managerProfitRate = managerProfitRate; }
    public List<Map<String, Object>> getDeptSettlementRows() { return deptSettlementRows; }
    public void setDeptSettlementRows(List<Map<String, Object>> deptSettlementRows) { this.deptSettlementRows = deptSettlementRows; }
    public List<ProfitShareExceptionVO> getExceptions() { return exceptions; }
    public void setExceptions(List<ProfitShareExceptionVO> exceptions) { this.exceptions = exceptions; }
}
