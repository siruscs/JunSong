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
 * 借支记录对象 fin_advance
 * 
 * @author junsong
 */
public class FinAdvance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 借支记录ID */
    @Excel(name = "借支记录ID", cellType = ColumnType.NUMERIC)
    private Long advanceId;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 借支单号 */
    @Excel(name = "借支单号")
    private String advanceNo;

    /** 借支日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "借支日期", width = 30, dateFormat = "yyyy-MM-dd")
    @NotNull(message = "借支日期不能为空")
    private Date advanceDate;

    /** 借支金额 */
    @Excel(name = "借支金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "借支金额不能为空")
    private BigDecimal advanceAmount;

    /** 借款人 */
    @Excel(name = "借款人")
    private String borrower;

    /** 借支用途 */
    @Excel(name = "借支用途")
    private String purpose;

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

    public Long getAdvanceId()
    {
        return advanceId;
    }

    public void setAdvanceId(Long advanceId)
    {
        this.advanceId = advanceId;
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

    @Size(min = 0, max = 64, message = "借支单号长度不能超过64个字符")
    public String getAdvanceNo()
    {
        return advanceNo;
    }

    public void setAdvanceNo(String advanceNo)
    {
        this.advanceNo = advanceNo;
    }

    public Date getAdvanceDate()
    {
        return advanceDate;
    }

    public void setAdvanceDate(Date advanceDate)
    {
        this.advanceDate = advanceDate;
    }

    public BigDecimal getAdvanceAmount()
    {
        return advanceAmount;
    }

    public void setAdvanceAmount(BigDecimal advanceAmount)
    {
        this.advanceAmount = advanceAmount;
    }

    @Size(min = 0, max = 64, message = "借款人长度不能超过64个字符")
    public String getBorrower()
    {
        return borrower;
    }

    public void setBorrower(String borrower)
    {
        this.borrower = borrower;
    }

    @Size(min = 0, max = 256, message = "借支用途长度不能超过256个字符")
    public String getPurpose()
    {
        return purpose;
    }

    public void setPurpose(String purpose)
    {
        this.purpose = purpose;
    }

    public String getAdvancePurpose()
    {
        return purpose;
    }

    public void setAdvancePurpose(String advancePurpose)
    {
        this.purpose = advancePurpose;
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
            .append("advanceId", getAdvanceId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("advanceNo", getAdvanceNo())
            .append("advanceDate", getAdvanceDate())
            .append("advanceAmount", getAdvanceAmount())
            .append("borrower", getBorrower())
            .append("purpose", getPurpose())
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
