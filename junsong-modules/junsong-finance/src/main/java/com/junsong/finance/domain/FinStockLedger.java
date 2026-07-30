package com.junsong.finance.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;

public class FinStockLedger extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long ledgerId;
    private Long tenantId;
    private Long deptId;
    private Long productId;
    private String productName;
    private String changeType;
    private Integer changeQuantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private BigDecimal unitCost;
    private String referenceType;
    private Long referenceId;
    private String referenceNo;
    /** 幂等键（租户内唯一）：由 Service 层填充，格式 reference_type + ':' + reference_id。
     *  DB 唯一索引 uk_stock_ledger_idempotency_key (tenant_id, idempotency_key) 兜底，
     *  即使 AOP 幂等失效也能阻止重复写入流水。 */
    private String idempotencyKey;
    private String delFlag;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public Integer getChangeQuantity() { return changeQuantity; }
    public void setChangeQuantity(Integer changeQuantity) { this.changeQuantity = changeQuantity; }

    public Integer getBeforeQuantity() { return beforeQuantity; }
    public void setBeforeQuantity(Integer beforeQuantity) { this.beforeQuantity = beforeQuantity; }

    public Integer getAfterQuantity() { return afterQuantity; }
    public void setAfterQuantity(Integer afterQuantity) { this.afterQuantity = afterQuantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ledgerId", getLedgerId())
            .append("tenantId", getTenantId())
            .append("deptId", getDeptId())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("changeType", getChangeType())
            .append("changeQuantity", getChangeQuantity())
            .append("beforeQuantity", getBeforeQuantity())
            .append("afterQuantity", getAfterQuantity())
            .append("unitCost", getUnitCost())
            .append("referenceType", getReferenceType())
            .append("referenceId", getReferenceId())
            .append("referenceNo", getReferenceNo())
            .append("idempotencyKey", getIdempotencyKey())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}