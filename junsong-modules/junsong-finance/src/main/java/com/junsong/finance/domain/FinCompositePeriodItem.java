package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 复合核算周期纳入明细表 fin_composite_period_item
 *
 * @author junsong
 */
public class FinCompositePeriodItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long itemId;

    @Excel(name = "复合核算池ID", cellType = ColumnType.NUMERIC)
    private Long poolId;

    @Excel(name = "店面ID", cellType = ColumnType.NUMERIC)
    private Long deptId;

    @Excel(name = "店面名称")
    private String deptName;

    @Excel(name = "周期ID", cellType = ColumnType.NUMERIC)
    private Long periodId;

    @Excel(name = "周期编号")
    private String periodNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "周期开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date periodStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "周期结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date periodEndTime;

    @Excel(name = "周期净利", cellType = ColumnType.NUMERIC)
    private BigDecimal netProfit;

    @Excel(name = "店长分润金额", cellType = ColumnType.NUMERIC)
    private BigDecimal managerProfitAmount;

    @Excel(name = "纳入复合核算金额", cellType = ColumnType.NUMERIC)
    private BigDecimal investorProfitAmount;

    @Excel(name = "纳入方式", readConverterExp = "0=自动,1=手动")
    private String includedMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "纳入时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date includedTime;

    private String includedBy;

    @Excel(name = "状态", readConverterExp = "0=有效,1=撤销")
    private String status;

    private String delFlag;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public String getPeriodNo() { return periodNo; }
    public void setPeriodNo(String periodNo) { this.periodNo = periodNo; }
    public Date getPeriodStartTime() { return periodStartTime; }
    public void setPeriodStartTime(Date periodStartTime) { this.periodStartTime = periodStartTime; }
    public Date getPeriodEndTime() { return periodEndTime; }
    public void setPeriodEndTime(Date periodEndTime) { this.periodEndTime = periodEndTime; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getManagerProfitAmount() { return managerProfitAmount; }
    public void setManagerProfitAmount(BigDecimal managerProfitAmount) { this.managerProfitAmount = managerProfitAmount; }
    public BigDecimal getInvestorProfitAmount() { return investorProfitAmount; }
    public void setInvestorProfitAmount(BigDecimal investorProfitAmount) { this.investorProfitAmount = investorProfitAmount; }
    public String getIncludedMode() { return includedMode; }
    public void setIncludedMode(String includedMode) { this.includedMode = includedMode; }
    public Date getIncludedTime() { return includedTime; }
    public void setIncludedTime(Date includedTime) { this.includedTime = includedTime; }
    public String getIncludedBy() { return includedBy; }
    public void setIncludedBy(String includedBy) { this.includedBy = includedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("poolId", getPoolId())
            .append("deptId", getDeptId())
            .append("deptName", getDeptName())
            .append("periodId", getPeriodId())
            .append("periodNo", getPeriodNo())
            .append("investorProfitAmount", getInvestorProfitAmount())
            .append("includedMode", getIncludedMode())
            .toString();
    }
}
