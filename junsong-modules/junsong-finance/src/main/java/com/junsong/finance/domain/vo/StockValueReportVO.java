package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存价值报表（第二期财务计价）。
 *
 * <p>恒等式：{@code openingAmount + inboundAmount - saleCost + adjustmentAmount = closingAmount}；
 * 毛利：{@code saleRevenue - saleCost - adjustmentAmount = grossProfit}。</p>
 *
 * <p>{@code costReady} 为 true 时表示租户已初始化成本层，前端方可展示金额和毛利；
 * 为 false 时金额字段全部为零且 items 为空，禁止用零值伪装未完成成本。</p>
 *
 * <p>期间控制：{@code periodStatus} 反馈当前会计期间状态（{@code ACTIVE}/{@code LOCKED}/{@code CARRIED_FORWARD}）。
 * LOCKED 或 CARRIED_FORWARD 期间拒绝回写历史成本流水；差异只能在当前 ACTIVE 期间生成有原因的调整流水。</p>
 *
 * @author junsong
 */
public class StockValueReportVO {

    /** 成本层是否已就绪：true 时金额和毛利有效，false 时禁止展示金额 */
    private boolean costReady;

    /** 当前会计期间状态：ACTIVE(0)/LOCKED(1)/CARRIED_FORWARD(2) */
    private String periodStatus;

    /** 期初库存金额 */
    private BigDecimal openingAmount;

    /** 区间采购净入库金额 = COST_IN - COST_REVERSE_OUT（赠品入库金额为0，不影响金额但摊薄成本） */
    private BigDecimal inboundAmount;

    /** 区间销售成本 = COST_OUT - COST_REVERSE_IN（销售出库固化成本，销售冲销按原成本回补） */
    private BigDecimal saleCost;

    /** 成本调整金额（COST_ADJUST，需有原因和操作者） */
    private BigDecimal adjustmentAmount;

    /** 期末库存金额 = 期初 + 入库 - 销售成本 + 调整 */
    private BigDecimal closingAmount;

    /** 区间销售收入（不含赠品收入） */
    private BigDecimal saleRevenue;

    /** 毛利 = 销售收入 - 销售成本 */
    private BigDecimal grossProfit;

    /** 毛利率 = 毛利 / 销售收入 * 100 */
    private BigDecimal grossProfitRate;

    /** 单商品价值明细 */
    private List<StockValueReportItemVO> items;

    public boolean isCostReady() {
        return costReady;
    }

    public void setCostReady(boolean costReady) {
        this.costReady = costReady;
    }

    public String getPeriodStatus() {
        return periodStatus;
    }

    public void setPeriodStatus(String periodStatus) {
        this.periodStatus = periodStatus;
    }

    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount) {
        this.openingAmount = openingAmount;
    }

    public BigDecimal getInboundAmount() {
        return inboundAmount;
    }

    public void setInboundAmount(BigDecimal inboundAmount) {
        this.inboundAmount = inboundAmount;
    }

    public BigDecimal getSaleCost() {
        return saleCost;
    }

    public void setSaleCost(BigDecimal saleCost) {
        this.saleCost = saleCost;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public BigDecimal getClosingAmount() {
        return closingAmount;
    }

    public void setClosingAmount(BigDecimal closingAmount) {
        this.closingAmount = closingAmount;
    }

    public BigDecimal getSaleRevenue() {
        return saleRevenue;
    }

    public void setSaleRevenue(BigDecimal saleRevenue) {
        this.saleRevenue = saleRevenue;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public BigDecimal getGrossProfitRate() {
        return grossProfitRate;
    }

    public void setGrossProfitRate(BigDecimal grossProfitRate) {
        this.grossProfitRate = grossProfitRate;
    }

    public List<StockValueReportItemVO> getItems() {
        return items;
    }

    public void setItems(List<StockValueReportItemVO> items) {
        this.items = items;
    }
}
