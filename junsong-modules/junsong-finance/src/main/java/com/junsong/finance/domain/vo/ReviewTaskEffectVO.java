package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 复盘任务动作效果评估 VO
 */
public class ReviewTaskEffectVO {
    private Long taskId;
    private Long deptId;
    private String taskTitle;
    private String problemType;
    private Integer windowDays;
    private BigDecimal beforeSales = BigDecimal.ZERO;
    private BigDecimal afterSales = BigDecimal.ZERO;
    private BigDecimal beforeExpense = BigDecimal.ZERO;
    private BigDecimal afterExpense = BigDecimal.ZERO;
    private BigDecimal beforeProfit = BigDecimal.ZERO;
    private BigDecimal afterProfit = BigDecimal.ZERO;
    private BigDecimal salesChangeRate = BigDecimal.ZERO;
    private BigDecimal expenseChangeRate = BigDecimal.ZERO;
    private BigDecimal profitChangeRate = BigDecimal.ZERO;
    private Integer beforeHealthScore;
    private Integer afterHealthScore;
    private Integer beforeSimilarOpenCount;
    private Integer afterSimilarOpenCount;
    private Integer effectScore = 0;
    private String effectLevel;
    private List<String> evidence = new ArrayList<>();

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public BigDecimal getBeforeSales() { return beforeSales; }
    public void setBeforeSales(BigDecimal beforeSales) { this.beforeSales = beforeSales; }
    public BigDecimal getAfterSales() { return afterSales; }
    public void setAfterSales(BigDecimal afterSales) { this.afterSales = afterSales; }
    public BigDecimal getBeforeExpense() { return beforeExpense; }
    public void setBeforeExpense(BigDecimal beforeExpense) { this.beforeExpense = beforeExpense; }
    public BigDecimal getAfterExpense() { return afterExpense; }
    public void setAfterExpense(BigDecimal afterExpense) { this.afterExpense = afterExpense; }
    public BigDecimal getBeforeProfit() { return beforeProfit; }
    public void setBeforeProfit(BigDecimal beforeProfit) { this.beforeProfit = beforeProfit; }
    public BigDecimal getAfterProfit() { return afterProfit; }
    public void setAfterProfit(BigDecimal afterProfit) { this.afterProfit = afterProfit; }
    public BigDecimal getSalesChangeRate() { return salesChangeRate; }
    public void setSalesChangeRate(BigDecimal salesChangeRate) { this.salesChangeRate = salesChangeRate; }
    public BigDecimal getExpenseChangeRate() { return expenseChangeRate; }
    public void setExpenseChangeRate(BigDecimal expenseChangeRate) { this.expenseChangeRate = expenseChangeRate; }
    public BigDecimal getProfitChangeRate() { return profitChangeRate; }
    public void setProfitChangeRate(BigDecimal profitChangeRate) { this.profitChangeRate = profitChangeRate; }
    public Integer getBeforeHealthScore() { return beforeHealthScore; }
    public void setBeforeHealthScore(Integer beforeHealthScore) { this.beforeHealthScore = beforeHealthScore; }
    public Integer getAfterHealthScore() { return afterHealthScore; }
    public void setAfterHealthScore(Integer afterHealthScore) { this.afterHealthScore = afterHealthScore; }
    public Integer getBeforeSimilarOpenCount() { return beforeSimilarOpenCount; }
    public void setBeforeSimilarOpenCount(Integer beforeSimilarOpenCount) { this.beforeSimilarOpenCount = beforeSimilarOpenCount; }
    public Integer getAfterSimilarOpenCount() { return afterSimilarOpenCount; }
    public void setAfterSimilarOpenCount(Integer afterSimilarOpenCount) { this.afterSimilarOpenCount = afterSimilarOpenCount; }
    public Integer getEffectScore() { return effectScore; }
    public void setEffectScore(Integer effectScore) { this.effectScore = effectScore; }
    public String getEffectLevel() { return effectLevel; }
    public void setEffectLevel(String effectLevel) { this.effectLevel = effectLevel; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
