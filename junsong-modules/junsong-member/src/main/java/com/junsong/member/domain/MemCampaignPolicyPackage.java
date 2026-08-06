package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.web.domain.BaseEntity;

/** 某个商品政策下的固定购买套餐。 */
public class MemCampaignPolicyPackage extends BaseEntity
{
    private Long packageId;
    private Long policyId;
    private Long tenantId;
    private Long deptId;
    private String packageName;
    private BigDecimal purchaseQuantity;
    private BigDecimal giftQuantity;
    private BigDecimal totalQuantity;
    private BigDecimal packagePrice;
    private Integer sortNo;
    private String delFlag;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long value) { packageId = value; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long value) { policyId = value; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long value) { deptId = value; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String value) { packageName = value; }
    public BigDecimal getPurchaseQuantity() { return purchaseQuantity; }
    public void setPurchaseQuantity(BigDecimal value) { purchaseQuantity = value; }
    public BigDecimal getGiftQuantity() { return giftQuantity; }
    public void setGiftQuantity(BigDecimal value) { giftQuantity = value; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal value) { totalQuantity = value; }
    public BigDecimal getPackagePrice() { return packagePrice; }
    public void setPackagePrice(BigDecimal value) { packagePrice = value; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer value) { sortNo = value; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String value) { delFlag = value; }
}
