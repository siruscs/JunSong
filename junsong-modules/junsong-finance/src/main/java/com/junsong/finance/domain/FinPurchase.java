package com.junsong.finance.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 进货单对象 fin_purchase
 * 
 * @author junsong
 */
public class FinPurchase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 进货单ID */
    @Excel(name = "进货单ID", cellType = ColumnType.NUMERIC)
    private Long purchaseId;


    /** 进货单号 */
    @Excel(name = "进货单号")
    private String purchaseNo;

    /** 供应商ID */
    @Excel(name = "供应商ID", cellType = ColumnType.NUMERIC)
    private Long supplierId;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String supplierName;

    /** 进货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "进货日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date purchaseDate;

    /** 总金额 */
    @Excel(name = "总金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalAmount;

    /** 已付金额 */
    @Excel(name = "已付金额", cellType = ColumnType.NUMERIC)
    private BigDecimal paidAmount;

    /** 付款方式 */
    @Excel(name = "付款方式")
    private String paymentMethod;

    /** 总数量（含赠品） */
    @Excel(name = "总数量", cellType = ColumnType.NUMERIC)
    private Integer totalQuantity;

    /** 状态（0草稿 1已确认 2已完成） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已确认,2=已完成")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 收货人姓名 */
    @Excel(name = "收货人姓名")
    private String receiverName;

    /** 收货人电话 */
    @Excel(name = "收货人电话")
    private String receiverPhone;

    /** 收货人地址 */
    @Excel(name = "收货人地址")
    private String receiverAddress;

    /** 进货单明细 */
    private List<FinPurchaseDetail> details;

    public Long getPurchaseId()
    {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId)
    {
        this.purchaseId = purchaseId;
    }


    @Size(min = 0, max = 64, message = "进货单号长度不能超过64个字符")
    public String getPurchaseNo()
    {
        return purchaseNo;
    }

    public void setPurchaseNo(String purchaseNo)
    {
        this.purchaseNo = purchaseNo;
    }

    @NotNull(message = "供应商ID不能为空")
    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    @NotNull(message = "进货日期不能为空")
    public Date getPurchaseDate()
    {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate)
    {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount()
    {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount)
    {
        this.paidAmount = paidAmount;
    }

    @Size(min = 0, max = 32, message = "付款方式长度不能超过32个字符")
    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    public Integer getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity)
    {
        this.totalQuantity = totalQuantity;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId(Long periodId)
    {
        this.periodId = periodId;
    }

    public String getReceiverName()
    {
        return receiverName;
    }

    public void setReceiverName(String receiverName)
    {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone()
    {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone)
    {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress()
    {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress)
    {
        this.receiverAddress = receiverAddress;
    }

    public List<FinPurchaseDetail> getDetails()
    {
        return details;
    }

    public void setDetails(List<FinPurchaseDetail> details)
    {
        this.details = details;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("purchaseId", getPurchaseId())
            .append("purchaseNo", getPurchaseNo())
            .append("supplierId", getSupplierId())
            .append("supplierName", getSupplierName())
            .append("purchaseDate", getPurchaseDate())
            .append("totalAmount", getTotalAmount())
            .append("paidAmount", getPaidAmount())
            .append("paymentMethod", getPaymentMethod())
            .append("totalQuantity", getTotalQuantity())
            .append("status", getStatus())
            .append("receiverName", getReceiverName())
            .append("receiverPhone", getReceiverPhone())
            .append("receiverAddress", getReceiverAddress())
            .append("delFlag", getDelFlag())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
