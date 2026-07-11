package com.junsong.finance.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 复合池共享投资人表 fin_composite_pool_investor
 *
 * @author junsong
 */
public class FinCompositePoolInvestor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "复合核算池ID", cellType = ColumnType.NUMERIC)
    private Long poolId;

    @Excel(name = "投资人ID", cellType = ColumnType.NUMERIC)
    private Long investorId;

    @Excel(name = "投资人姓名")
    private String investorName;

    @Excel(name = "共享出资款", cellType = ColumnType.NUMERIC)
    private BigDecimal investAmount;

    @Excel(name = "出资占比", cellType = ColumnType.NUMERIC)
    private BigDecimal investRatio;

    @Excel(name = "已分摊回本金额", cellType = ColumnType.NUMERIC)
    private BigDecimal returnedAmount;

    @Excel(name = "状态", readConverterExp = "0=有效,1=停用")
    private String status;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    public Long getInvestorId() { return investorId; }
    public void setInvestorId(Long investorId) { this.investorId = investorId; }
    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }
    public BigDecimal getInvestAmount() { return investAmount; }
    public void setInvestAmount(BigDecimal investAmount) { this.investAmount = investAmount; }
    public BigDecimal getInvestRatio() { return investRatio; }
    public void setInvestRatio(BigDecimal investRatio) { this.investRatio = investRatio; }
    public BigDecimal getReturnedAmount() { return returnedAmount; }
    public void setReturnedAmount(BigDecimal returnedAmount) { this.returnedAmount = returnedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("poolId", getPoolId())
            .append("investorId", getInvestorId())
            .append("investorName", getInvestorName())
            .append("investAmount", getInvestAmount())
            .append("investRatio", getInvestRatio())
            .append("returnedAmount", getReturnedAmount())
            .toString();
    }
}
