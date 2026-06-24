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
 * 费用记录对象 fin_expense
 * 
 * @author junsong
 */
public class FinExpense extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 费用记录ID */
    @Excel(name = "费用记录ID", cellType = ColumnType.NUMERIC)
    private Long expenseId;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 费用单号 */
    @Excel(name = "费用单号")
    private String expenseNo;

    /** 费用日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "费用日期", width = 30, dateFormat = "yyyy-MM-dd")
    @NotNull(message = "费用日期不能为空")
    private Date expenseDate;

    /** 费用类别(预支 开支 收入) */
    @Excel(name = "费用类别", combo = {"预支", "开支", "收入"})
    @NotNull(message = "费用类别不能为空")
    private String expenseType;

    /** 花销内容 */
    @Excel(name = "花销内容")
    private String expenseContent;

    /** 付款方式(直接付款 预支资金 自行垫付 收入) */
    @Excel(name = "付款方式", combo = {"现金", "微信", "支付宝", "银行卡"})
    @NotNull(message = "付款方式不能为空")
    private String paymentMethod;

    /** 费用金额 */
    @Excel(name = "费用金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "费用金额不能为空")
    private BigDecimal expenseAmount;

    /** 关联借支ID */
    @Excel(name = "关联借支ID", cellType = ColumnType.NUMERIC)
    private Long advanceId;

    /** 关联借支单号 */
    private String advanceNo;

    /** 状态(0未核销 1已核销) */
    @Excel(name = "状态", readConverterExp = "0=未核销,1=已核销")
    private String status;

    /** 核销人 */
    @Excel(name = "核销人")
    private String verifyBy;

    /** 核销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "核销时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getExpenseId()
    {
        return expenseId;
    }

    public void setExpenseId(Long expenseId)
    {
        this.expenseId = expenseId;
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

    @Size(min = 0, max = 64, message = "费用单号长度不能超过64个字符")
    public String getExpenseNo()
    {
        return expenseNo;
    }

    public void setExpenseNo(String expenseNo)
    {
        this.expenseNo = expenseNo;
    }

    public Date getExpenseDate()
    {
        return expenseDate;
    }

    public void setExpenseDate(Date expenseDate)
    {
        this.expenseDate = expenseDate;
    }

    @Size(min = 0, max = 32, message = "费用类别长度不能超过32个字符")
    public String getExpenseType()
    {
        return expenseType;
    }

    public void setExpenseType(String expenseType)
    {
        this.expenseType = expenseType;
    }

    public String getExpenseContent()
    {
        return expenseContent;
    }

    public void setExpenseContent(String expenseContent)
    {
        this.expenseContent = expenseContent;
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

    public BigDecimal getExpenseAmount()
    {
        return expenseAmount;
    }

    public void setExpenseAmount(BigDecimal expenseAmount)
    {
        this.expenseAmount = expenseAmount;
    }

    public Long getAdvanceId()
    {
        return advanceId;
    }

    public void setAdvanceId(Long advanceId)
    {
        this.advanceId = advanceId;
    }

    public String getAdvanceNo()
    {
        return advanceNo;
    }

    public void setAdvanceNo(String advanceNo)
    {
        this.advanceNo = advanceNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getVerifyBy()
    {
        return verifyBy;
    }

    public void setVerifyBy(String verifyBy)
    {
        this.verifyBy = verifyBy;
    }

    public Date getVerifyTime()
    {
        return verifyTime;
    }

    public void setVerifyTime(Date verifyTime)
    {
        this.verifyTime = verifyTime;
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
            .append("expenseId", getExpenseId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("expenseNo", getExpenseNo())
            .append("expenseDate", getExpenseDate())
            .append("expenseType", getExpenseType())
            .append("expenseContent", getExpenseContent())
            .append("paymentMethod", getPaymentMethod())
            .append("expenseAmount", getExpenseAmount())
            .append("advanceId", getAdvanceId())
            .append("status", getStatus())
            .append("verifyBy", getVerifyBy())
            .append("verifyTime", getVerifyTime())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
