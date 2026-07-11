package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日经营复盘看板 VO。
 * R8-A: 聚合当日销售额、实收现金、费用、净现金流、待办数、关注项 Top3、经营建议。
 */
public class DailyReviewBoardVO {

    /** 复盘日期 yyyy-MM-dd */
    private String reviewDate;

    /** 门店ID */
    private Long deptId;

    /** 门店名称 */
    private String deptName;

    /** 销售额（按 sale_date） */
    private BigDecimal salesAmount = BigDecimal.ZERO;

    /** 实收现金（按 payment_date） */
    private BigDecimal cashInAmount = BigDecimal.ZERO;

    /** 费用支出（按 expense_date） */
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    /** 净现金流 = cashInAmount - expenseAmount */
    private BigDecimal netCashflowAmount = BigDecimal.ZERO;

    /** 待处理任务数 */
    private Integer pendingTaskCount = 0;

    /** 高优先级任务数 */
    private Integer highPriorityTaskCount = 0;

    /** 关注项 Top 3 */
    private List<DailyReviewItemVO> focusItems = new ArrayList<>();

    /** 经营建议 */
    private List<String> suggestions = new ArrayList<>();

    // ===== getters / setters =====

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public BigDecimal getSalesAmount() { return salesAmount; }
    public void setSalesAmount(BigDecimal salesAmount) { this.salesAmount = salesAmount == null ? BigDecimal.ZERO : salesAmount; }

    public BigDecimal getCashInAmount() { return cashInAmount; }
    public void setCashInAmount(BigDecimal cashInAmount) { this.cashInAmount = cashInAmount == null ? BigDecimal.ZERO : cashInAmount; }

    public BigDecimal getExpenseAmount() { return expenseAmount; }
    public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount == null ? BigDecimal.ZERO : expenseAmount; }

    public BigDecimal getNetCashflowAmount() { return netCashflowAmount; }
    public void setNetCashflowAmount(BigDecimal netCashflowAmount) { this.netCashflowAmount = netCashflowAmount == null ? BigDecimal.ZERO : netCashflowAmount; }

    public Integer getPendingTaskCount() { return pendingTaskCount; }
    public void setPendingTaskCount(Integer pendingTaskCount) { this.pendingTaskCount = pendingTaskCount == null ? 0 : pendingTaskCount; }

    public Integer getHighPriorityTaskCount() { return highPriorityTaskCount; }
    public void setHighPriorityTaskCount(Integer highPriorityTaskCount) { this.highPriorityTaskCount = highPriorityTaskCount == null ? 0 : highPriorityTaskCount; }

    public List<DailyReviewItemVO> getFocusItems() { return focusItems; }
    public void setFocusItems(List<DailyReviewItemVO> focusItems) { this.focusItems = focusItems == null ? new ArrayList<>() : focusItems; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions == null ? new ArrayList<>() : suggestions; }
}
