package com.junsong.finance.domain;

import java.math.BigDecimal;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 期初库存批次行表 finance_stock_init_item。
 *
 * 每行对应一个商品的期初库存记录：
 * - quantity: 期初数量（DECIMAL 18,2）
 * - unit_cost: 单位成本（DECIMAL 18,6）
 * - amount: 金额 = quantity * unit_cost，scale 2 HALF_UP
 * - stock_ledger_id / cost_ledger_id: 过账生成的流水引用
 *
 * @author junsong
 */
public class FinStockInitItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "行ID", cellType = ColumnType.NUMERIC)
    private Long itemId;
    @Excel(name = "批次ID", cellType = ColumnType.NUMERIC)
    private Long batchId;
    private Long tenantId;
    @Excel(name = "门店ID", cellType = ColumnType.NUMERIC)
    private Long deptId;
    @Excel(name = "商品ID", cellType = ColumnType.NUMERIC)
    private Long productId;
    @Excel(name = "商品名称")
    private String productName;
    @Excel(name = "期初数量", cellType = ColumnType.NUMERIC)
    private BigDecimal quantity;
    @Excel(name = "单位成本")
    private BigDecimal unitCost;
    @Excel(name = "金额")
    private BigDecimal amount;
    private Long stockLedgerId;
    private Long costLedgerId;
    private Integer version;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getStockLedgerId() { return stockLedgerId; }
    public void setStockLedgerId(Long stockLedgerId) { this.stockLedgerId = stockLedgerId; }

    public Long getCostLedgerId() { return costLedgerId; }
    public void setCostLedgerId(Long costLedgerId) { this.costLedgerId = costLedgerId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
