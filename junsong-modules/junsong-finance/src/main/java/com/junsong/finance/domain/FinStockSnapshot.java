package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 库存快照实体：对应 fin_stock_snapshot 表。
 *
 * 字段说明（与 DDL 实际字段名一致）：
 * - quantity：期末库存数量（closing），来源 fin_stock_position.quantity
 * - opening_quantity：期初库存数量 = closing - in + out
 * - in_quantity：当日入库数量（正向流水合计）
 * - out_quantity：当日出库数量（反向流水绝对值合计）
 *
 * 唯一键：snapshot_date + dept_id + product_id（uk_stock_snapshot_date_dept_product）
 *
 * @author junsong
 */
public class FinStockSnapshot extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long snapshotId;
    private LocalDate snapshotDate;
    private Long deptId;
    private String deptName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer openingQuantity;
    private Integer inQuantity;
    private Integer outQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalValue;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getOpeningQuantity() { return openingQuantity; }
    public void setOpeningQuantity(Integer openingQuantity) { this.openingQuantity = openingQuantity; }

    public Integer getInQuantity() { return inQuantity; }
    public void setInQuantity(Integer inQuantity) { this.inQuantity = inQuantity; }

    public Integer getOutQuantity() { return outQuantity; }
    public void setOutQuantity(Integer outQuantity) { this.outQuantity = outQuantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("snapshotId", getSnapshotId())
            .append("snapshotDate", getSnapshotDate())
            .append("deptId", getDeptId())
            .append("deptName", getDeptName())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("quantity", getQuantity())
            .append("openingQuantity", getOpeningQuantity())
            .append("inQuantity", getInQuantity())
            .append("outQuantity", getOutQuantity())
            .append("unitCost", getUnitCost())
            .append("totalValue", getTotalValue())
            .append("createTime", getCreateTime())
            .toString();
    }
}
