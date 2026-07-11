package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 每周经营复盘看板 VO。
 * R8-F: 周复盘摘要，不做复杂 BI。
 * 包含本周数据、上周对比、环比变化率、完成任务数、周总结和下周重点。
 *
 * @author junsong
 */
public class WeeklyReviewBoardVO {

    /** 周开始日期 yyyy-MM-dd（周一） */
    private String weekStart;

    /** 周结束日期 yyyy-MM-dd（周日） */
    private String weekEnd;

    /** 门店ID */
    private Long deptId;

    /** 门店名称 */
    private String deptName;

    // ===== 本周数据 =====

    /** 本周销售额（按 sale_date） */
    private BigDecimal salesAmount = BigDecimal.ZERO;

    /** 本周费用支出（按 expense_date） */
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    /** 本周实收现金（按 payment_date） */
    private BigDecimal cashInAmount = BigDecimal.ZERO;

    /** 本周净现金流 = cashInAmount - expenseAmount */
    private BigDecimal netCashflowAmount = BigDecimal.ZERO;

    // ===== 上周数据 =====

    /** 上周销售额 */
    private BigDecimal previousWeekSalesAmount = BigDecimal.ZERO;

    /** 上周费用支出 */
    private BigDecimal previousWeekExpenseAmount = BigDecimal.ZERO;

    /** 上周净现金流 */
    private BigDecimal previousWeekNetCashflowAmount = BigDecimal.ZERO;

    // ===== 环比变化率 =====

    /** 销售环比变化率（%） */
    private BigDecimal salesChangeRate = BigDecimal.ZERO;

    /** 费用环比变化率（%） */
    private BigDecimal expenseChangeRate = BigDecimal.ZERO;

    /** 净现金流环比变化率（%） */
    private BigDecimal cashflowChangeRate = BigDecimal.ZERO;

    // ===== 任务 =====

    /** 本周完成任务数（status=DONE 且 update_time 在本周） */
    private Integer completedTaskCount = 0;

    /** 待处理任务数 */
    private Integer pendingTaskCount = 0;

    // ===== 总结 =====

    /** 周总结 */
    private String weeklySummary;

    /** 下周重点 */
    private String nextWeekFocus;

    // ===== getters / setters =====

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getWeekEnd() { return weekEnd; }
    public void setWeekEnd(String weekEnd) { this.weekEnd = weekEnd; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public BigDecimal getSalesAmount() { return salesAmount; }
    public void setSalesAmount(BigDecimal salesAmount) { this.salesAmount = salesAmount == null ? BigDecimal.ZERO : salesAmount; }

    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount == null ? BigDecimal.ZERO : expenseAmount; }

    public BigDecimal getCashInAmount() { return cashInAmount; }
    public void setCashInAmount(BigDecimal cashInAmount) { this.cashInAmount = cashInAmount == null ? BigDecimal.ZERO : cashInAmount; }

    public BigDecimal getNetCashflowAmount() { return netCashflowAmount; }
    public void setNetCashflowAmount(BigDecimal netCashflowAmount) { this.netCashflowAmount = netCashflowAmount == null ? BigDecimal.ZERO : netCashflowAmount; }

    public BigDecimal getPreviousWeekSalesAmount() { return previousWeekSalesAmount; }
    public void setPreviousWeekSalesAmount(BigDecimal previousWeekSalesAmount) { this.previousWeekSalesAmount = previousWeekSalesAmount == null ? BigDecimal.ZERO : previousWeekSalesAmount; }

    public BigDecimal getPreviousWeekExpenseAmount() { return previousWeekExpenseAmount; }
    public void setPreviousWeekExpenseAmount(BigDecimal previousWeekExpenseAmount) { this.previousWeekExpenseAmount = previousWeekExpenseAmount == null ? BigDecimal.ZERO : previousWeekExpenseAmount; }

    public BigDecimal getPreviousWeekNetCashflowAmount() { return previousWeekNetCashflowAmount; }
    public void setPreviousWeekNetCashflowAmount(BigDecimal previousWeekNetCashflowAmount) { this.previousWeekNetCashflowAmount = previousWeekNetCashflowAmount == null ? BigDecimal.ZERO : previousWeekNetCashflowAmount; }

    public BigDecimal getSalesChangeRate() { return salesChangeRate; }
    public void setSalesChangeRate(BigDecimal salesChangeRate) { this.salesChangeRate = salesChangeRate == null ? BigDecimal.ZERO : salesChangeRate; }

    public BigDecimal getExpenseChangeRate() { return expenseChangeRate; }
    public void setExpenseChangeRate(BigDecimal expenseChangeRate) { this.expenseChangeRate = expenseChangeRate == null ? BigDecimal.ZERO : expenseChangeRate; }

    public BigDecimal getCashflowChangeRate() { return cashflowChangeRate; }
    public void setCashflowChangeRate(BigDecimal cashflowChangeRate) { this.cashflowChangeRate = cashflowChangeRate == null ? BigDecimal.ZERO : cashflowChangeRate; }

    public Integer getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(Integer completedTaskCount) { this.completedTaskCount = completedTaskCount == null ? 0 : completedTaskCount; }

    public Integer getPendingTaskCount() { return pendingTaskCount; }
    public void setPendingTaskCount(Integer pendingTaskCount) { this.pendingTaskCount = pendingTaskCount == null ? 0 : pendingTaskCount; }

    public String getWeeklySummary() { return weeklySummary; }
    public void setWeeklySummary(String weeklySummary) { this.weeklySummary = weeklySummary; }

    public String getNextWeekFocus() { return nextWeekFocus; }
    public void setNextWeekFocus(String nextWeekFocus) { this.nextWeekFocus = nextWeekFocus; }
}
