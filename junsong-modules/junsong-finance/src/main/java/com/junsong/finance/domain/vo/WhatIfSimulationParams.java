package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * what-if 模拟输入参数 VO。
 * 所有字段允许为空（不模拟时取基线 0）。
 */
public class WhatIfSimulationParams {

    private Long deptId;
    private Integer windowDays = 7;

    /** 预计回款变化金额（>0 表示回款增加，<0 表示回款减少） */
    private BigDecimal expectedCollectionDelta = BigDecimal.ZERO;

    /** 预计费用变化金额（>0 表示费用增加，<0 表示费用减少） */
    private BigDecimal expectedExpenseDelta = BigDecimal.ZERO;

    /** 催收完成数（>0 表示额外完成催收） */
    private Integer completedCollectionActions = 0;

    /** 会员动作完成数（>0 表示额外完成会员动作） */
    private Integer completedMemberActions = 0;

    /** 库存补货变化量（>0 表示补货，<0 表示减库存） */
    private BigDecimal stockReplenishmentDelta = BigDecimal.ZERO;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays == null ? 7 : windowDays; }
    public BigDecimal getExpectedCollectionDelta() { return expectedCollectionDelta; }
    public void setExpectedCollectionDelta(BigDecimal expectedCollectionDelta) { this.expectedCollectionDelta = expectedCollectionDelta == null ? BigDecimal.ZERO : expectedCollectionDelta; }
    public BigDecimal getExpectedExpenseDelta() { return expectedExpenseDelta; }
    public void setExpectedExpenseDelta(BigDecimal expectedExpenseDelta) { this.expectedExpenseDelta = expectedExpenseDelta == null ? BigDecimal.ZERO : expectedExpenseDelta; }
    public Integer getCompletedCollectionActions() { return completedCollectionActions; }
    public void setCompletedCollectionActions(Integer completedCollectionActions) { this.completedCollectionActions = completedCollectionActions == null ? 0 : completedCollectionActions; }
    public Integer getCompletedMemberActions() { return completedMemberActions; }
    public void setCompletedMemberActions(Integer completedMemberActions) { this.completedMemberActions = completedMemberActions == null ? 0 : completedMemberActions; }
    public BigDecimal getStockReplenishmentDelta() { return stockReplenishmentDelta; }
    public void setStockReplenishmentDelta(BigDecimal stockReplenishmentDelta) { this.stockReplenishmentDelta = stockReplenishmentDelta == null ? BigDecimal.ZERO : stockReplenishmentDelta; }
}
