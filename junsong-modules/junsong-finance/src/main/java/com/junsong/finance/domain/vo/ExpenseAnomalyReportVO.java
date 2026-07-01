package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ExpenseAnomalyReportVO {
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private List<Map<String, Object>> categoryBreakdown;
    private List<Map<String, Object>> storeExpenseRank;
    private List<ExpenseAnomalyRowVO> categorySpikes;
    private List<ExpenseAnomalyRowVO> storeSpikes;
    private List<ExpenseAnomalyRowVO> unverifiedList;
    private List<ExpenseAnomalyRowVO> ocrAnomalies;

    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }
    public List<Map<String, Object>> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<Map<String, Object>> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    public List<Map<String, Object>> getStoreExpenseRank() { return storeExpenseRank; }
    public void setStoreExpenseRank(List<Map<String, Object>> storeExpenseRank) { this.storeExpenseRank = storeExpenseRank; }
    public List<ExpenseAnomalyRowVO> getCategorySpikes() { return categorySpikes; }
    public void setCategorySpikes(List<ExpenseAnomalyRowVO> categorySpikes) { this.categorySpikes = categorySpikes; }
    public List<ExpenseAnomalyRowVO> getStoreSpikes() { return storeSpikes; }
    public void setStoreSpikes(List<ExpenseAnomalyRowVO> storeSpikes) { this.storeSpikes = storeSpikes; }
    public List<ExpenseAnomalyRowVO> getUnverifiedList() { return unverifiedList; }
    public void setUnverifiedList(List<ExpenseAnomalyRowVO> unverifiedList) { this.unverifiedList = unverifiedList; }
    public List<ExpenseAnomalyRowVO> getOcrAnomalies() { return ocrAnomalies; }
    public void setOcrAnomalies(List<ExpenseAnomalyRowVO> ocrAnomalies) { this.ocrAnomalies = ocrAnomalies; }
}
