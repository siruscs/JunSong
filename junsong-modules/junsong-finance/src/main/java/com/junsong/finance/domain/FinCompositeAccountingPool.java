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
 * 复合核算池主表 fin_composite_accounting_pool
 *
 * @author junsong
 */
public class FinCompositeAccountingPool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "复合核算池ID", cellType = ColumnType.NUMERIC)
    private Long poolId;

    @Excel(name = "复合核算池编号")
    private String poolNo;

    @Excel(name = "复合核算池名称")
    private String poolName;

    private Long tenantId;

    @Excel(name = "共享投资人总出资", cellType = ColumnType.NUMERIC)
    private BigDecimal totalInvestAmount;

    @Excel(name = "累计回本金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalReturnAmount;

    @Excel(name = "回本缺口", cellType = ColumnType.NUMERIC)
    private BigDecimal breakEvenGap;

    @Excel(name = "超额收益", cellType = ColumnType.NUMERIC)
    private BigDecimal overReturnAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "达到回本时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date breakEvenTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "财务确认回本时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date confirmedTime;

    private String confirmedBy;

    @Excel(name = "状态", readConverterExp = "0=进行中,1=已达回本,2=已确认回本,3=已关闭")
    private String status;

    private String delFlag;

    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    public String getPoolNo() { return poolNo; }
    public void setPoolNo(String poolNo) { this.poolNo = poolNo; }
    public String getPoolName() { return poolName; }
    public void setPoolName(String poolName) { this.poolName = poolName; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public BigDecimal getTotalInvestAmount() { return totalInvestAmount; }
    public void setTotalInvestAmount(BigDecimal totalInvestAmount) { this.totalInvestAmount = totalInvestAmount; }
    public BigDecimal getTotalReturnAmount() { return totalReturnAmount; }
    public void setTotalReturnAmount(BigDecimal totalReturnAmount) { this.totalReturnAmount = totalReturnAmount; }
    public BigDecimal getBreakEvenGap() { return breakEvenGap; }
    public void setBreakEvenGap(BigDecimal breakEvenGap) { this.breakEvenGap = breakEvenGap; }
    public BigDecimal getOverReturnAmount() { return overReturnAmount; }
    public void setOverReturnAmount(BigDecimal overReturnAmount) { this.overReturnAmount = overReturnAmount; }
    public Date getBreakEvenTime() { return breakEvenTime; }
    public void setBreakEvenTime(Date breakEvenTime) { this.breakEvenTime = breakEvenTime; }
    public Date getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(Date confirmedTime) { this.confirmedTime = confirmedTime; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("poolId", getPoolId())
            .append("poolNo", getPoolNo())
            .append("poolName", getPoolName())
            .append("totalInvestAmount", getTotalInvestAmount())
            .append("totalReturnAmount", getTotalReturnAmount())
            .append("status", getStatus())
            .append("remark", getRemark())
            .toString();
    }
}
