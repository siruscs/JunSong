package com.junsong.finance.domain;

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
 * 销售记录对象 fin_sale_record
 * 
 * @author junsong
 */
public class FinSaleRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 销售记录ID */
    @Excel(name = "销售记录ID", cellType = ColumnType.NUMERIC)
    private Long saleId;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 销售单号 */
    @Excel(name = "销售单号")
    private String saleNo;

    /** 商品ID */
    @Excel(name = "商品ID", cellType = ColumnType.NUMERIC)
    private Long productId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 销售数量 */
    @Excel(name = "销售数量", cellType = ColumnType.NUMERIC)
    @NotNull(message = "销售数量不能为空")
    private Integer saleQuantity;

    /** 赠品数量 */
    @Excel(name = "赠品数量", cellType = ColumnType.NUMERIC)
    private Integer giftQuantity;

    /** 总数量(销售+赠品) */
    @Excel(name = "总数量", cellType = ColumnType.NUMERIC)
    private Integer totalQuantity;

    /** 销售金额 */
    @Excel(name = "销售金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "销售金额不能为空")
    private BigDecimal saleAmount;

    /** 单价 */
    @Excel(name = "单价", cellType = ColumnType.NUMERIC)
    private BigDecimal unitPrice;

    /** 已缴金额 */
    @Excel(name = "已缴金额", cellType = ColumnType.NUMERIC)
    private BigDecimal paidAmount;

    /** 销售日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "销售日期", width = 30, dateFormat = "yyyy-MM-dd")
    @NotNull(message = "销售日期不能为空")
    private Date saleDate;

    /** 状态（0待缴款 1部分缴款 2已缴清） */
    @Excel(name = "状态", readConverterExp = "0=待缴款,1=部分缴款,2=已缴清")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 缴款记录列表 */
    private List<FinSalePayment> payments;

    public Long getSaleId()
    {
        return saleId;
    }

    public void setSaleId(Long saleId)
    {
        this.saleId = saleId;
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

    @Size(min = 0, max = 64, message = "销售单号长度不能超过64个字符")
    public String getSaleNo()
    {
        return saleNo;
    }

    public void setSaleNo(String saleNo)
    {
        this.saleNo = saleNo;
    }

    @NotNull(message = "商品ID不能为空")
    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    @Size(min = 0, max = 128, message = "商品名称长度不能超过128个字符")
    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public Integer getSaleQuantity()
    {
        return saleQuantity;
    }

    public void setSaleQuantity(Integer saleQuantity)
    {
        this.saleQuantity = saleQuantity;
    }

    public Integer getGiftQuantity()
    {
        return giftQuantity;
    }

    public void setGiftQuantity(Integer giftQuantity)
    {
        this.giftQuantity = giftQuantity;
    }

    public Integer getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity)
    {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getSaleAmount()
    {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount)
    {
        this.saleAmount = saleAmount;
    }

    public BigDecimal getUnitPrice()
    {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice)
    {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getPaidAmount()
    {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount)
    {
        this.paidAmount = paidAmount;
    }

    public Date getSaleDate()
    {
        return saleDate;
    }

    public void setSaleDate(Date saleDate)
    {
        this.saleDate = saleDate;
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

    public List<FinSalePayment> getPayments()
    {
        return payments;
    }

    public void setPayments(List<FinSalePayment> payments)
    {
        this.payments = payments;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("saleId", getSaleId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("saleNo", getSaleNo())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("saleQuantity", getSaleQuantity())
            .append("giftQuantity", getGiftQuantity())
            .append("totalQuantity", getTotalQuantity())
            .append("saleAmount", getSaleAmount())
            .append("unitPrice", getUnitPrice())
            .append("paidAmount", getPaidAmount())
            .append("saleDate", getSaleDate())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
