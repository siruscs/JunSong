package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 期初库存明细行输入。
 *
 * 业务规则：
 * - productId：商品ID（必填）
 * - quantity：期初数量（必填，> 0）
 * - unitCost：单位成本（必填，>= 0）
 *
 * @author junsong
 */
public class StockInitItemInput {

    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitCost;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
