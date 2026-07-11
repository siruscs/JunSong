package com.junsong.finance.domain.vo;

/**
 * 库存当前结存视图：用于快照生成时读取 fin_stock_position 的轻量投影。
 *
 * @author junsong
 */
public class FinStockPositionView {

    private Long deptId;
    private Long productId;
    private Integer quantity;
    private String productName;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
