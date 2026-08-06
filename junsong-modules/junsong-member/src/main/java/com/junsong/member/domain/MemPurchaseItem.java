package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.web.domain.BaseEntity;

/** 会员域购买明细，保存商品政策和套餐结果快照。 */
public class MemPurchaseItem extends BaseEntity
{
    private Long itemId;
    private Long purchaseId;
    private Long tenantId;
    private Long deptId;
    private Long productId;
    private String productNameSnapshot;
    private Long policyId;
    private Integer policyVersion;
    private Long packageId;
    private String packageNameSnapshot;
    private BigDecimal purchaseQuantity;
    private BigDecimal giftQuantity;
    private BigDecimal totalQuantity;
    private BigDecimal deliveredQuantity;
    private BigDecimal deliveredSaleQuantity;
    private BigDecimal deliveredGiftQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal unitPrice;
    private BigDecimal itemAmount;
    private String policySnapshot;
    private String delFlag;

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { itemId = v; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long v) { purchaseId = v; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { deptId = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { productId = v; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String v) { productNameSnapshot = v; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long v) { policyId = v; }
    public Integer getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(Integer v) { policyVersion = v; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long v) { packageId = v; }
    public String getPackageNameSnapshot() { return packageNameSnapshot; }
    public void setPackageNameSnapshot(String v) { packageNameSnapshot = v; }
    public BigDecimal getPurchaseQuantity() { return purchaseQuantity; }
    public void setPurchaseQuantity(BigDecimal v) { purchaseQuantity = v; }
    public BigDecimal getGiftQuantity() { return giftQuantity; }
    public void setGiftQuantity(BigDecimal v) { giftQuantity = v; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { totalQuantity = v; }
    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(BigDecimal v) { deliveredQuantity = v; }
    public BigDecimal getDeliveredSaleQuantity() { return deliveredSaleQuantity; }
    public void setDeliveredSaleQuantity(BigDecimal v) { deliveredSaleQuantity = v; }
    public BigDecimal getDeliveredGiftQuantity() { return deliveredGiftQuantity; }
    public void setDeliveredGiftQuantity(BigDecimal v) { deliveredGiftQuantity = v; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal v) { remainingQuantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { unitPrice = v; }
    public BigDecimal getItemAmount() { return itemAmount; }
    public void setItemAmount(BigDecimal v) { itemAmount = v; }
    public String getPolicySnapshot() { return policySnapshot; }
    public void setPolicySnapshot(String v) { policySnapshot = v; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String v) { delFlag = v; }
}
