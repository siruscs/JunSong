package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 库存价值报表单商品行（第二期财务计价）。
 *
 * <p>每个商品行展示期末库存数量、移动加权平均单位成本、期末库存金额、
 * 区间入库金额、区间销售成本、区间销售收入和毛利。</p>
 *
 * @author junsong
 */
public class StockValueReportItemVO {

    private Long tenantId;
    private Long deptId;
    private String deptName;
    private Long productId;
    private String productCode;
    private String productName;

    /** 期末库存数量 */
    private BigDecimal closingQuantity;

    /** 移动加权平均单位成本（6位小数） */
    private BigDecimal avgUnitCost;

    /** 期末库存金额 */
    private BigDecimal closingAmount;

    /** 区间采购净入库金额 */
    private BigDecimal inboundAmount;

    /** 区间销售成本 */
    private BigDecimal saleCost;

    /** 区间销售收入（不含赠品） */
    private BigDecimal saleRevenue;

    /** 毛利 = 销售收入 - 销售成本 */
    private BigDecimal grossProfit;

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

    public BigDecimal getClosingQuantity() {
        return closingQuantity;
    }

    public void setClosingQuantity(BigDecimal closingQuantity) {
        this.closingQuantity = closingQuantity;
    }

    public BigDecimal getAvgUnitCost() {
        return avgUnitCost;
    }

    public void setAvgUnitCost(BigDecimal avgUnitCost) {
        this.avgUnitCost = avgUnitCost;
    }

    public BigDecimal getClosingAmount() {
        return closingAmount;
    }

    public void setClosingAmount(BigDecimal closingAmount) {
        this.closingAmount = closingAmount;
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
}
