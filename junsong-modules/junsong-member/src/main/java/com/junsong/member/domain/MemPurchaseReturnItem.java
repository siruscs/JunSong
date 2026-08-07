package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.web.domain.BaseEntity;

public class MemPurchaseReturnItem extends BaseEntity
{
    private Long returnItemId, returnId, purchaseId, itemId, tenantId, deptId, productId;
    private String productNameSnapshot;
    private BigDecimal returnSaleQuantity, returnGiftQuantity, returnTotalQuantity, refundUnitPrice, refundAmount;
    public Long getReturnItemId(){return returnItemId;} public void setReturnItemId(Long v){returnItemId=v;}
    public Long getReturnId(){return returnId;} public void setReturnId(Long v){returnId=v;}
    public Long getPurchaseId(){return purchaseId;} public void setPurchaseId(Long v){purchaseId=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getDeptId(){return deptId;} public void setDeptId(Long v){deptId=v;}
    public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;}
    public String getProductNameSnapshot(){return productNameSnapshot;} public void setProductNameSnapshot(String v){productNameSnapshot=v;}
    public BigDecimal getReturnSaleQuantity(){return returnSaleQuantity;} public void setReturnSaleQuantity(BigDecimal v){returnSaleQuantity=v;}
    public BigDecimal getReturnGiftQuantity(){return returnGiftQuantity;} public void setReturnGiftQuantity(BigDecimal v){returnGiftQuantity=v;}
    public BigDecimal getReturnTotalQuantity(){return returnTotalQuantity;} public void setReturnTotalQuantity(BigDecimal v){returnTotalQuantity=v;}
    public BigDecimal getRefundUnitPrice(){return refundUnitPrice;} public void setRefundUnitPrice(BigDecimal v){refundUnitPrice=v;}
    public BigDecimal getRefundAmount(){return refundAmount;} public void setRefundAmount(BigDecimal v){refundAmount=v;}
}
