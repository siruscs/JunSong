package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.finance.domain.FinCompositeAccountingPool;
import com.junsong.finance.domain.FinCompositePeriodItem;
import com.junsong.finance.domain.FinCompositePoolDept;
import com.junsong.finance.domain.FinCompositePoolInvestor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 复合核算池概览 VO
 *
 * @author junsong
 */
public class CompositePoolOverviewVO {

    /** 池基础信息 */
    private FinCompositeAccountingPool pool;

    /** 参与店面列表 */
    private List<FinCompositePoolDept> depts;

    /** 共享投资人列表 */
    private List<FinCompositePoolInvestor> investors;

    /** 已纳入周期明细 */
    private List<FinCompositePeriodItem> periodItems;

    /** 复合核算汇总 */
    private CompositeAccountingSummaryVO summary;

    /** 回本进度(0-1) */
    private BigDecimal breakEvenProgress = BigDecimal.ZERO;

    /** 待处理异常周期数量(自动纳入失败) */
    private Integer pendingExceptionCount = 0;

    public FinCompositeAccountingPool getPool() { return pool; }
    public void setPool(FinCompositeAccountingPool pool) { this.pool = pool; }
    public List<FinCompositePoolDept> getDepts() { return depts; }
    public void setDepts(List<FinCompositePoolDept> depts) { this.depts = depts; }
    public List<FinCompositePoolInvestor> getInvestors() { return investors; }
    public void setInvestors(List<FinCompositePoolInvestor> investors) { this.investors = investors; }
    public List<FinCompositePeriodItem> getPeriodItems() { return periodItems; }
    public void setPeriodItems(List<FinCompositePeriodItem> periodItems) { this.periodItems = periodItems; }
    public CompositeAccountingSummaryVO getSummary() { return summary; }
    public void setSummary(CompositeAccountingSummaryVO summary) { this.summary = summary; }
    public BigDecimal getBreakEvenProgress() { return breakEvenProgress; }
    public void setBreakEvenProgress(BigDecimal breakEvenProgress) { this.breakEvenProgress = breakEvenProgress; }
    public Integer getPendingExceptionCount() { return pendingExceptionCount; }
    public void setPendingExceptionCount(Integer pendingExceptionCount) { this.pendingExceptionCount = pendingExceptionCount; }
}
