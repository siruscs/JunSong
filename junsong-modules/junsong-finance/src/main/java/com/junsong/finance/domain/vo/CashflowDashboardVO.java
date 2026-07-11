package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 轻量现金流看板 VO.
 * R7-D: 只做现金流入/流出/净额/待结算，不做预算。
 */
public class CashflowDashboardVO {

    /** 现金流入（销售缴款） */
    private BigDecimal cashInAmount = BigDecimal.ZERO;

    /** 现金流出（已核销费用 + 投资人返款） */
    private BigDecimal cashOutAmount = BigDecimal.ZERO;

    /** 净现金流 = cashInAmount - cashOutAmount */
    private BigDecimal netCashflowAmount = BigDecimal.ZERO;

    /** 待核销费用金额 */
    private BigDecimal pendingExpenseAmount = BigDecimal.ZERO;

    /** 待核销借支金额 */
    private BigDecimal pendingAdvanceAmount = BigDecimal.ZERO;

    /** 待分润金额 */
    private BigDecimal pendingProfitShareAmount = BigDecimal.ZERO;

    /** 待核销费用数量 */
    private Integer pendingExpenseCount = 0;

    /** 待核销借支数量 */
    private Integer pendingAdvanceCount = 0;

    /** 待分润数量 */
    private Integer pendingProfitShareCount = 0;

    /** 日趋势行 */
    private List<CashflowTrendRowVO> trendRows;

    /** 待结算明细 */
    private List<CashflowPendingItemVO> pendingItems;

    // ── getters / setters ──

    public BigDecimal getCashInAmount() {
        return cashInAmount;
    }

    public void setCashInAmount(BigDecimal cashInAmount) {
        this.cashInAmount = cashInAmount;
    }

    public BigDecimal getCashOutAmount() {
        return cashOutAmount;
    }

    public void setCashOutAmount(BigDecimal cashOutAmount) {
        this.cashOutAmount = cashOutAmount;
    }

    public BigDecimal getNetCashflowAmount() {
        return netCashflowAmount;
    }

    public void setNetCashflowAmount(BigDecimal netCashflowAmount) {
        this.netCashflowAmount = netCashflowAmount;
    }

    public BigDecimal getPendingExpenseAmount() {
        return pendingExpenseAmount;
    }

    public void setPendingExpenseAmount(BigDecimal pendingExpenseAmount) {
        this.pendingExpenseAmount = pendingExpenseAmount;
    }

    public BigDecimal getPendingAdvanceAmount() {
        return pendingAdvanceAmount;
    }

    public void setPendingAdvanceAmount(BigDecimal pendingAdvanceAmount) {
        this.pendingAdvanceAmount = pendingAdvanceAmount;
    }

    public BigDecimal getPendingProfitShareAmount() {
        return pendingProfitShareAmount;
    }

    public void setPendingProfitShareAmount(BigDecimal pendingProfitShareAmount) {
        this.pendingProfitShareAmount = pendingProfitShareAmount;
    }

    public Integer getPendingExpenseCount() {
        return pendingExpenseCount;
    }

    public void setPendingExpenseCount(Integer pendingExpenseCount) {
        this.pendingExpenseCount = pendingExpenseCount;
    }

    public Integer getPendingAdvanceCount() {
        return pendingAdvanceCount;
    }

    public void setPendingAdvanceCount(Integer pendingAdvanceCount) {
        this.pendingAdvanceCount = pendingAdvanceCount;
    }

    public Integer getPendingProfitShareCount() {
        return pendingProfitShareCount;
    }

    public void setPendingProfitShareCount(Integer pendingProfitShareCount) {
        this.pendingProfitShareCount = pendingProfitShareCount;
    }

    public List<CashflowTrendRowVO> getTrendRows() {
        return trendRows;
    }

    public void setTrendRows(List<CashflowTrendRowVO> trendRows) {
        this.trendRows = trendRows;
    }

    public List<CashflowPendingItemVO> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<CashflowPendingItemVO> pendingItems) {
        this.pendingItems = pendingItems;
    }

    // ── 内部静态类 ──

    /**
     * 现金流日趋势行
     */
    public static class CashflowTrendRowVO {
        /** 日期 yyyy-MM-dd */
        private String date;
        /** 当日现金流入 */
        private BigDecimal cashIn = BigDecimal.ZERO;
        /** 当日现金流出 */
        private BigDecimal cashOut = BigDecimal.ZERO;
        /** 当日净额 */
        private BigDecimal net = BigDecimal.ZERO;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getCashIn() {
            return cashIn;
        }

        public void setCashIn(BigDecimal cashIn) {
            this.cashIn = cashIn;
        }

        public BigDecimal getCashOut() {
            return cashOut;
        }

        public void setCashOut(BigDecimal cashOut) {
            this.cashOut = cashOut;
        }

        public BigDecimal getNet() {
            return net;
        }

        public void setNet(BigDecimal net) {
            this.net = net;
        }
    }

    /**
     * 待结算明细项
     */
    public static class CashflowPendingItemVO {
        /** 类型: EXPENSE / ADVANCE / PROFIT_SHARE */
        private String type;
        /** 业务ID */
        private String bizId;
        /** 金额 */
        private BigDecimal amount = BigDecimal.ZERO;
        /** 门店名称 */
        private String deptName;
        /** 创建时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date createDate;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getBizId() {
            return bizId;
        }

        public void setBizId(String bizId) {
            this.bizId = bizId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Date getCreateDate() {
            return createDate;
        }

        public void setCreateDate(Date createDate) {
            this.createDate = createDate;
        }
    }
}
