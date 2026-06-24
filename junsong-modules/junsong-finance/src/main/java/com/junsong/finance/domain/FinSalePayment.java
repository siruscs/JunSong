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

/**
 * 缴款记录对象 fin_sale_payment
 * 
 * @author junsong
 */
public class FinSalePayment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缴款记录ID */
    @Excel(name = "缴款记录ID", cellType = ColumnType.NUMERIC)
    private Long paymentId;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 销售记录ID */
    @Excel(name = "销售记录ID", cellType = ColumnType.NUMERIC)
    private Long saleId;

    /** 缴款单号 */
    @Excel(name = "缴款单号")
    private String paymentNo;

    /** 缴款金额 */
    @Excel(name = "缴款金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "缴款金额不能为空")
    private BigDecimal paymentAmount;

    /** 付款方式 */
    @Excel(name = "付款方式")
    private String paymentMethod;

    /** 缴款日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "缴款日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "缴款日期不能为空")
    private Date paymentDate;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getPaymentId()
    {
        return paymentId;
    }

    public void setPaymentId(Long paymentId)
    {
        this.paymentId = paymentId;
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

    @NotNull(message = "销售记录ID不能为空")
    public Long getSaleId()
    {
        return saleId;
    }

    public void setSaleId(Long saleId)
    {
        this.saleId = saleId;
    }

    @Size(min = 0, max = 64, message = "缴款单号长度不能超过64个字符")
    public String getPaymentNo()
    {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo)
    {
        this.paymentNo = paymentNo;
    }

    public BigDecimal getPaymentAmount()
    {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount)
    {
        this.paymentAmount = paymentAmount;
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

    public Date getPaymentDate()
    {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate)
    {
        this.paymentDate = paymentDate;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("paymentId", getPaymentId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("saleId", getSaleId())
            .append("paymentNo", getPaymentNo())
            .append("paymentAmount", getPaymentAmount())
            .append("paymentMethod", getPaymentMethod())
            .append("paymentDate", getPaymentDate())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
