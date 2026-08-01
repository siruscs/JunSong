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

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long memberId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    private String memberName;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 销售数量 */
    @Excel(name = "销售数量", cellType = ColumnType.NUMERIC)
    @NotNull(message = "销售数量不能为空")
    private BigDecimal saleQuantity;

    /** 赠品数量 */
    @Excel(name = "赠品数量", cellType = ColumnType.NUMERIC)
    private BigDecimal giftQuantity;

    /** 总数量(销售+赠品) */
    @Excel(name = "总数量", cellType = ColumnType.NUMERIC)
    private BigDecimal totalQuantity;

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
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "销售日期", width = 30, dateFormat = "yyyy-MM-dd")
    @NotNull(message = "销售日期不能为空")
    private Date saleDate;

    /** 状态（0待缴款 1部分缴款 2已缴清） */
    @Excel(name = "状态", readConverterExp = "0=待缴款,1=部分缴款,2=已缴清")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 幂等键（租户内唯一） */
    private String idempotencyKey;

    /** 缴款记录列表 */
    private List<FinSalePayment> payments;

    /** 核算周期编号（历史欠款查询用，来自关联表） */
    private String periodNo;

    /** 核算周期状态（0进行中 1已回本待结转 2已结转，来自关联表） */
    private String periodStatus;

    /** 最近缴款时间（历史欠款查询用，来自缴款子查询） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date latestPaymentDate;

    /** 剩余应收金额（销售金额-已缴金额，前端展示用） */
    public BigDecimal getUnpaidAmount()
    {
        BigDecimal sale = saleAmount != null ? saleAmount : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        return sale.subtract(paid);
    }

    public String getPeriodNo()
    {
        return periodNo;
    }

    public void setPeriodNo(String periodNo)
    {
        this.periodNo = periodNo;
    }

    public String getPeriodStatus()
    {
        return periodStatus;
    }

    public void setPeriodStatus(String periodStatus)
    {
        this.periodStatus = periodStatus;
    }

    public Date getLatestPaymentDate()
    {
        return latestPaymentDate;
    }

    public void setLatestPaymentDate(Date latestPaymentDate)
    {
        this.latestPaymentDate = latestPaymentDate;
    }

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

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    @Size(min = 0, max = 64, message = "会员编号长度不能超过64个字符")
    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    @Size(min = 0, max = 100, message = "会员姓名长度不能超过100个字符")
    public String getMemberName()
    {
        return memberName;
    }

    public void setMemberName(String memberName)
    {
        this.memberName = memberName;
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

    public BigDecimal getSaleQuantity()
    {
        return saleQuantity;
    }

    public void setSaleQuantity(BigDecimal saleQuantity)
    {
        this.saleQuantity = saleQuantity;
    }

    public BigDecimal getGiftQuantity()
    {
        return giftQuantity;
    }

    public void setGiftQuantity(BigDecimal giftQuantity)
    {
        this.giftQuantity = giftQuantity;
    }

    public BigDecimal getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity)
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

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
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
            .append("memberId", getMemberId())
            .append("memberNo", getMemberNo())
            .append("memberName", getMemberName())
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
            .append("idempotencyKey", getIdempotencyKey())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
