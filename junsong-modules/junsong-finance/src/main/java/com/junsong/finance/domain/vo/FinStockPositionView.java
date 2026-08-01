package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 库存当前结存视图：用于快照生成时读取 fin_stock_position 的轻量投影。
 *
 * @author junsong
 */
public class FinStockPositionView {

    private Long tenantId;
    private Long deptId;
    private Long productId;
    private BigDecimal quantity;
    private String productName;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
