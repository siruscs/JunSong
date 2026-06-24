package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FinInvestRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "投资记录ID", cellType = ColumnType.NUMERIC)
    private Long investId;
    private Long deptId;
    private Long periodId;
    @NotNull(message = "投资人ID不能为空")
    private Long investorId;
    @Excel(name = "投资人姓名")
    private String investorName;
    @Excel(name = "投资金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "投资金额不能为空")
    private BigDecimal investAmount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "投资时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "投资时间不能为空")
    private Date investTime;
    private String delFlag;

    public Long getInvestId() { return investId; }
    public void setInvestId(Long investId) { this.investId = investId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public Long getInvestorId() { return investorId; }
    public void setInvestorId(Long investorId) { this.investorId = investorId; }
    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }
    public BigDecimal getInvestAmount() { return investAmount; }
    public void setInvestAmount(BigDecimal investAmount) { this.investAmount = investAmount; }
    public Date getInvestTime() { return investTime; }
    public void setInvestTime(Date investTime) { this.investTime = investTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("investId", getInvestId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("investorId", getInvestorId())
            .append("investorName", getInvestorName())
            .append("investAmount", getInvestAmount())
            .append("investTime", getInvestTime())
            .append("remark", getRemark())
            .toString();
    }
}
