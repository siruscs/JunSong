package com.junsong.finance.domain.vo;

import java.util.Date;

/**
 * 经营库存报表单商品行。
 *
 * @author junsong
 */
public class StockReportItemVO {

    private Long tenantId;
    private Long deptId;
    private String deptName;
    private Long productId;
    private String productCode;
    private String productName;
    private String unit;

    /** 最低库存 */
    private Integer minStock;

    /** 期初数量 */
    private Integer openingQuantity;

    /** 采购净入库 */
    private Integer purchaseNetInQuantity;

    /** 销售净出库 */
    private Integer saleNetOutQuantity;

    /** 期末数量 */
    private Integer closingQuantity;

    /** 最近入库时间 */
    private Date lastInboundTime;

    /** 最近出库时间 */
    private Date lastOutboundTime;

    /** 无出库天数 */
    private Integer daysWithoutSale;

    /** 库存状态：NORMAL/LOW_STOCK/ZERO_STOCK/NEGATIVE_STOCK/STALE */
    private String stockStatus;

    /** 对账状态：OK/ANOMALY */
    private String reconciliationStatus;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getOpeningQuantity() {
        return openingQuantity;
    }

    public void setOpeningQuantity(Integer openingQuantity) {
        this.openingQuantity = openingQuantity;
    }

    public Integer getPurchaseNetInQuantity() {
        return purchaseNetInQuantity;
    }

    public void setPurchaseNetInQuantity(Integer purchaseNetInQuantity) {
        this.purchaseNetInQuantity = purchaseNetInQuantity;
    }

    public Integer getSaleNetOutQuantity() {
        return saleNetOutQuantity;
    }

    public void setSaleNetOutQuantity(Integer saleNetOutQuantity) {
        this.saleNetOutQuantity = saleNetOutQuantity;
    }

    public Integer getClosingQuantity() {
        return closingQuantity;
    }

    public void setClosingQuantity(Integer closingQuantity) {
        this.closingQuantity = closingQuantity;
    }

    public Date getLastInboundTime() {
        return lastInboundTime;
    }

    public void setLastInboundTime(Date lastInboundTime) {
        this.lastInboundTime = lastInboundTime;
    }

    public Date getLastOutboundTime() {
        return lastOutboundTime;
    }

    public void setLastOutboundTime(Date lastOutboundTime) {
        this.lastOutboundTime = lastOutboundTime;
    }

    public Integer getDaysWithoutSale() {
        return daysWithoutSale;
    }

    public void setDaysWithoutSale(Integer daysWithoutSale) {
        this.daysWithoutSale = daysWithoutSale;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }
}
