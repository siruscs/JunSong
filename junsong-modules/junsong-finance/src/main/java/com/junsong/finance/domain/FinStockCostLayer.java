package com.junsong.finance.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 库存成本层（移动加权平均成本当前值）。
 * 按 tenant + dept + product 唯一，记录当前平均单位成本、库存数量和库存金额。
 * 与 fin_stock_position 的 quantity 对账，但不覆盖第一期库存流水。
 *
 * @author junsong
 */
public class FinStockCostLayer extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long costLayerId;
    private Long tenantId;
    private Long deptId;
    private Long productId;
    /** 移动加权平均单位成本（6位小数） */
    private BigDecimal avgUnitCost;
    /** 成本层记录的库存数量（与 position 对账） */
    private Integer stockQuantity;
    /** 库存金额（2位小数） */
    private BigDecimal stockAmount;
    /** 乐观锁版本号 */
    private Integer version;

    public Long getCostLayerId() { return costLayerId; }
    public void setCostLayerId(Long costLayerId) { this.costLayerId = costLayerId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getAvgUnitCost() { return avgUnitCost; }
    public void setAvgUnitCost(BigDecimal avgUnitCost) { this.avgUnitCost = avgUnitCost; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getStockAmount() { return stockAmount; }
    public void setStockAmount(BigDecimal stockAmount) { this.stockAmount = stockAmount; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("costLayerId", getCostLayerId())
            .append("tenantId", getTenantId())
            .append("deptId", getDeptId())
            .append("productId", getProductId())
            .append("avgUnitCost", getAvgUnitCost())
            .append("stockQuantity", getStockQuantity())
            .append("stockAmount", getStockAmount())
            .append("version", getVersion())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
