package com.junsong.finance.domain.vo;

/**
 * 经营库存报表汇总指标。
 *
 * @author junsong
 */
public class StockReportSummaryVO {

    /** 期初库存数量 */
    private int openingQuantity;

    /** 区间采购净入库数量 */
    private int purchaseNetInQuantity;

    /** 区间销售净出库数量 */
    private int saleNetOutQuantity;

    /** 其他调整净数量（第一期恒为0） */
    private int otherAdjustmentNetQuantity;

    /** 期末库存数量 */
    private int closingQuantity;

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

    public int getOpeningQuantity() {
        return openingQuantity;
    }

    public void setOpeningQuantity(int openingQuantity) {
        this.openingQuantity = openingQuantity;
    }

    public int getPurchaseNetInQuantity() {
        return purchaseNetInQuantity;
    }

    public void setPurchaseNetInQuantity(int purchaseNetInQuantity) {
        this.purchaseNetInQuantity = purchaseNetInQuantity;
    }

    public int getSaleNetOutQuantity() {
        return saleNetOutQuantity;
    }

    public void setSaleNetOutQuantity(int saleNetOutQuantity) {
        this.saleNetOutQuantity = saleNetOutQuantity;
    }

    public int getOtherAdjustmentNetQuantity() {
        return otherAdjustmentNetQuantity;
    }

    public void setOtherAdjustmentNetQuantity(int otherAdjustmentNetQuantity) {
        this.otherAdjustmentNetQuantity = otherAdjustmentNetQuantity;
    }

    public int getClosingQuantity() {
        return closingQuantity;
    }

    public void setClosingQuantity(int closingQuantity) {
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
