package com.junsong.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.junsong.common.core.web.domain.BaseEntity;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;

/** 会员域购买单，不等同于财务销售单。 */
public class MemPurchaseOrder extends BaseEntity
{
    private Long purchaseId;
    private Long tenantId;
    private Long deptId;
    private Long periodId;
    private Date purchaseDate;
    @Excel(name = "购买单号") private String purchaseNo;
    @Excel(name = "顾客类型", readConverterExp = "MEMBER=会员,WALK_IN=散客,CUSTOMER=非会员") private String customerType;
    private Long memberId;
    private Long customerId;
    @Excel(name = "顾客") private String customerName;
    private String customerPhone;
    private String identityMode;
    private Boolean identityConfirmed;
    @Excel(name = "应收金额", cellType = ColumnType.NUMERIC) private BigDecimal totalAmount;
    @Excel(name = "已收金额", cellType = ColumnType.NUMERIC) private BigDecimal paidAmount;
    @Excel(name = "欠款金额", cellType = ColumnType.NUMERIC) private BigDecimal receivableAmount;
    @Excel(name = "付款状态", readConverterExp = "0=未付款,1=部分付款,2=已付清,3=已退款") private String paymentStatus;
    @Excel(name = "领取状态", readConverterExp = "0=未领取,1=部分领取,2=已全部领取") private String deliveryStatus;
    private String orderStatus;
    private String idempotencyKey;
    private String delFlag;
    /** 列表查询的购买单创建日期范围，结束日期按自然日包含。 */
    private String beginTime;
    private String endTime;
    private List<MemPurchaseItem> items;
    private List<MemPurchasePayment> payments;
    private List<MemPurchaseDelivery> deliveries;
    @Excel(name = "购买数量", cellType = ColumnType.NUMERIC) private BigDecimal purchaseQuantity;
    @Excel(name = "赠送数量", cellType = ColumnType.NUMERIC) private BigDecimal giftQuantity;

    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long v) { purchaseId = v; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { deptId = v; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long v) { periodId = v; }
    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date v) { purchaseDate = v; }
    public String getPurchaseNo() { return purchaseNo; }
    public void setPurchaseNo(String v) { purchaseNo = v; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String v) { customerType = v; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long v) { memberId = v; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long v) { customerId = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { customerPhone = v; }
    public String getIdentityMode() { return identityMode; }
    public void setIdentityMode(String v) { identityMode = v; }
    public Boolean getIdentityConfirmed() { return identityConfirmed; }
    public void setIdentityConfirmed(Boolean v) { identityConfirmed = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { totalAmount = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { paidAmount = v; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal v) { receivableAmount = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { paymentStatus = v; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String v) { deliveryStatus = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v) { orderStatus = v; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { idempotencyKey = v; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String v) { delFlag = v; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String v) { beginTime = v; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String v) { endTime = v; }
    public List<MemPurchaseItem> getItems() { return items; }
    public void setItems(List<MemPurchaseItem> v) { items = v; }
    public List<MemPurchasePayment> getPayments() { return payments; }
    public void setPayments(List<MemPurchasePayment> v) { payments = v; }
    public List<MemPurchaseDelivery> getDeliveries() { return deliveries; }
    public void setDeliveries(List<MemPurchaseDelivery> v) { deliveries = v; }
    public BigDecimal getPurchaseQuantity() { return purchaseQuantity; }
    public void setPurchaseQuantity(BigDecimal v) { purchaseQuantity = v; }
    public BigDecimal getGiftQuantity() { return giftQuantity; }
    public void setGiftQuantity(BigDecimal v) { giftQuantity = v; }
}
