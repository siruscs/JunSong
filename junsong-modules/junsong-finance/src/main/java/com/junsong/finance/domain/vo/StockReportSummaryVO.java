package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 经营库存报表汇总指标。
 *
 * @author junsong
 */
public class StockReportSummaryVO {

    /** 期初库存数量 */
    private BigDecimal openingQuantity;

    /** 区间采购净入库数量 */
    private BigDecimal purchaseNetInQuantity;

    /** 区间销售净出库数量 */
    private BigDecimal saleNetOutQuantity;

    /** 其他调整净数量（第一期恒为0） */
    private BigDecimal otherAdjustmentNetQuantity;

    /** 期末库存数量 */
    private BigDecimal closingQuantity;

    /** 负库存商品数 */
    private int negativeStockCount;

    /** 低库存商品数 */
    private int lowStockCount;

    /** 零库存商品数 */
    private int zeroStockCount;

    /** 滞销商品数 */
    private int staleStockCount;

    /** 快照或流水对账异常商品数 */
    private int anomalyCount;

    public BigDecimal getOpeningQuantity() {
        return openingQuantity;
    }

    public void setOpeningQuantity(BigDecimal openingQuantity) {
        this.openingQuantity = openingQuantity;
    }

    public BigDecimal getPurchaseNetInQuantity() {
        return purchaseNetInQuantity;
    }

    public void setPurchaseNetInQuantity(BigDecimal purchaseNetInQuantity) {
        this.purchaseNetInQuantity = purchaseNetInQuantity;
    }

    public BigDecimal getSaleNetOutQuantity() {
        return saleNetOutQuantity;
    }

    public void setSaleNetOutQuantity(BigDecimal saleNetOutQuantity) {
        this.saleNetOutQuantity = saleNetOutQuantity;
    }

    public BigDecimal getOtherAdjustmentNetQuantity() {
        return otherAdjustmentNetQuantity;
    }

    public void setOtherAdjustmentNetQuantity(BigDecimal otherAdjustmentNetQuantity) {
        this.otherAdjustmentNetQuantity = otherAdjustmentNetQuantity;
    }

    public BigDecimal getClosingQuantity() {
        return closingQuantity;
    }

    public void setClosingQuantity(BigDecimal closingQuantity) {
        this.closingQuantity = closingQuantity;
    }

    public int getNegativeStockCount() {
        return negativeStockCount;
    }

    public void setNegativeStockCount(int negativeStockCount) {
        this.negativeStockCount = negativeStockCount;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getZeroStockCount() {
        return zeroStockCount;
    }

    public void setZeroStockCount(int zeroStockCount) {
        this.zeroStockCount = zeroStockCount;
    }

    public int getStaleStockCount() {
        return staleStockCount;
    }

    public void setStaleStockCount(int staleStockCount) {
        this.staleStockCount = staleStockCount;
    }

    public int getAnomalyCount() {
        return anomalyCount;
    }

    public void setAnomalyCount(int anomalyCount) {
        this.anomalyCount = anomalyCount;
    }
}
