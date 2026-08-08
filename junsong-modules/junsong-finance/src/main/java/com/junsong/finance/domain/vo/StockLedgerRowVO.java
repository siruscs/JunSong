package com.junsong.finance.domain.vo;

import java.util.Date;

/**
 * 库存流水明细行（单商品下钻）。
 *
 * @author junsong
 */
public class StockLedgerRowVO {

    private Long ledgerId;
    private Long tenantId;
    private Long deptId;
    private Long productId;
    private String productName;

    /** 变动类型：PURCHASE_IN/PURCHASE_REVERSE/SALE_OUT/SALE_REVERSE */
    private String changeType;

    /** 变动数量 */
    private java.math.BigDecimal changeQuantity;

    /** 变动前数量 */
    private java.math.BigDecimal beforeQuantity;

    /** 变动后数量 */
    private java.math.BigDecimal afterQuantity;

    /** 单位成本（计价） */
    private java.math.BigDecimal unitCost;

    /** 关联单据类型 */
    private String referenceType;

    /** 关联单据ID */
    private Long referenceId;

    /** 关联单据号 */
    private String referenceNo;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }

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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public java.math.BigDecimal getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(java.math.BigDecimal changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public java.math.BigDecimal getBeforeQuantity() {
        return beforeQuantity;
    }

    public void setBeforeQuantity(java.math.BigDecimal beforeQuantity) {
        this.beforeQuantity = beforeQuantity;
    }

    public java.math.BigDecimal getAfterQuantity() {
        return afterQuantity;
    }

    public void setAfterQuantity(java.math.BigDecimal afterQuantity) {
        this.afterQuantity = afterQuantity;
    }

    public java.math.BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(java.math.BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
