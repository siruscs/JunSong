package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存报表 VO
 */
public class StockReportVO {
    /** 总库存 */
    private Integer totalStock = 0;
    /** 库存金额 */
    private BigDecimal totalValue = BigDecimal.ZERO;
    /** 低库存商品数 */
    private Integer lowStockCount = 0;
    /** 门店统计 */
    private List<Map<String, Object>> deptStats;
    /** 分类统计 */
    private List<Map<String, Object>> categoryStats;

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public Integer getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(Integer lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public List<Map<String, Object>> getDeptStats() {
        return deptStats;
    }

    public void setDeptStats(List<Map<String, Object>> deptStats) {
        this.deptStats = deptStats;
    }

    public List<Map<String, Object>> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<Map<String, Object>> categoryStats) {
        this.categoryStats = categoryStats;
    }
}