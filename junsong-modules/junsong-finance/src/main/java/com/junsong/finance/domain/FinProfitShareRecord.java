package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

public class FinProfitShareRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "分润ID", cellType = ColumnType.NUMERIC)
    private Long shareId;
    private Long deptId;
    private Long periodId;
    @Excel(name = "分润单号")
    private String shareNo;
    private BigDecimal netProfit;
    private BigDecimal managerProfitRate;
    private BigDecimal managerProfitAmount;
    private BigDecimal investorProfitAmount;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shareTime;
    private String delFlag;
    private List<FinProfitShareDetail> details;

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public String getShareNo() { return shareNo; }
    public void setShareNo(String shareNo) { this.shareNo = shareNo; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getManagerProfitRate() { return managerProfitRate; }
    public void setManagerProfitRate(BigDecimal managerProfitRate) { this.managerProfitRate = managerProfitRate; }
    public BigDecimal getManagerProfitAmount() { return managerProfitAmount; }
    public void setManagerProfitAmount(BigDecimal managerProfitAmount) { this.managerProfitAmount = managerProfitAmount; }
    public BigDecimal getInvestorProfitAmount() { return investorProfitAmount; }
    public void setInvestorProfitAmount(BigDecimal investorProfitAmount) { this.investorProfitAmount = investorProfitAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getShareTime() { return shareTime; }
    public void setShareTime(Date shareTime) { this.shareTime = shareTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<FinProfitShareDetail> getDetails() { return details; }
    public void setDetails(List<FinProfitShareDetail> details) { this.details = details; }
}
