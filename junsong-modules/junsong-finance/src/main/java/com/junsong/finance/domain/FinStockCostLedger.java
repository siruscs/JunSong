package com.junsong.finance.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 库存成本流水（每一笔成本变动的不可追溯流水）。
 * 关联原库存流水 fin_stock_ledger.ledger_id，固化出库瞬间的成本。
 *
 * @author junsong
 */
public class FinStockCostLedger extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long costLedgerId;
    private Long tenantId;
    private Long deptId;
    private Long productId;
    /** 来源类型: PURCHASE / SALE / ADJUST */
    private String sourceType;
    /** 关联的库存流水ID */
    private Long sourceLedgerId;
    /** 成本变动类型: COST_IN / COST_OUT / COST_REVERSE_IN / COST_REVERSE_OUT / COST_ADJUST */
    private String costChangeType;
    /** 变动数量（正增负减） */
    private BigDecimal quantity;
    /** 单位成本（6位小数） */
    private BigDecimal unitCost;
    /** 金额（2位小数） */
    private BigDecimal amount;
    /** 会计期间ID */
    private Long periodId;
    /** 调整原因（仅 COST_ADJUST 必填） */
    private String adjustReason;
    /** 操作者 */
    private String operator;
    private String delFlag;

    public Long getCostLedgerId() { return costLedgerId; }
    public void setCostLedgerId(Long costLedgerId) { this.costLedgerId = costLedgerId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceLedgerId() { return sourceLedgerId; }
    public void setSourceLedgerId(Long sourceLedgerId) { this.sourceLedgerId = sourceLedgerId; }

    public String getCostChangeType() { return costChangeType; }
    public void setCostChangeType(String costChangeType) { this.costChangeType = costChangeType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public String getAdjustReason() { return adjustReason; }
    public void setAdjustReason(String adjustReason) { this.adjustReason = adjustReason; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("costLedgerId", getCostLedgerId())
            .append("tenantId", getTenantId())
            .append("deptId", getDeptId())
            .append("productId", getProductId())
            .append("sourceType", getSourceType())
            .append("sourceLedgerId", getSourceLedgerId())
            .append("costChangeType", getCostChangeType())
            .append("quantity", getQuantity())
            .append("unitCost", getUnitCost())
            .append("amount", getAmount())
            .append("periodId", getPeriodId())
            .append("adjustReason", getAdjustReason())
            .append("operator", getOperator())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
