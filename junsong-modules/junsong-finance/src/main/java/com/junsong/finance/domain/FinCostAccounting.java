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
 * 成本核算对象 fin_cost_accounting
 * 
 * @author junsong
 */
public class FinCostAccounting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成本核算ID */
    @Excel(name = "成本核算ID", cellType = ColumnType.NUMERIC)
    private Long accountingId;

    /** 部门ID */
    private Long deptId;

    /** 核算单号 */
    @Excel(name = "核算单号")
    private String accountingNo;

    /** 核算开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "开始日期不能为空")
    private Date startDate;

    /** 核算结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "结束日期不能为空")
    private Date endDate;

    /** 总花销费用 */
    @Excel(name = "总花销费用", cellType = ColumnType.NUMERIC)
    private BigDecimal totalExpense;

    /** 总进货金额 */
    @Excel(name = "总进货金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalPurchase;

    /** 总销售金额 */
    @Excel(name = "总销售金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalSale;

    /** 总缴款金额 */
    @Excel(name = "总缴款金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalPayment;

    /** 投资来源金额 */
    @Excel(name = "投资来源金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalInvest;

    /** 当前借支金额 */
    @Excel(name = "当前借支金额", cellType = ColumnType.NUMERIC)
    private BigDecimal currentAdvance;

    /** 净利(总缴款-已核销费用-进货款-借支未核销) */
    @Excel(name = "净利", cellType = ColumnType.NUMERIC)
    private BigDecimal returnSituation;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getAccountingId()
    {
        return accountingId;
    }

    public void setAccountingId(Long accountingId)
    {
        this.accountingId = accountingId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    @Size(min = 0, max = 64, message = "核算单号长度不能超过64个字符")
    public String getAccountingNo()
    {
        return accountingNo;
    }

    public void setAccountingNo(String accountingNo)
    {
        this.accountingNo = accountingNo;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public BigDecimal getTotalExpense()
    {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense)
    {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getTotalPurchase()
    {
        return totalPurchase;
    }

    public void setTotalPurchase(BigDecimal totalPurchase)
    {
        this.totalPurchase = totalPurchase;
    }

    public BigDecimal getTotalSale()
    {
        return totalSale;
    }

    public void setTotalSale(BigDecimal totalSale)
    {
        this.totalSale = totalSale;
    }

    public BigDecimal getTotalPayment()
    {
        return totalPayment;
    }

    public void setTotalPayment(BigDecimal totalPayment)
    {
        this.totalPayment = totalPayment;
    }

    public BigDecimal getTotalInvest()
    {
        return totalInvest;
    }

    public void setTotalInvest(BigDecimal totalInvest)
    {
        this.totalInvest = totalInvest;
    }

    public BigDecimal getCurrentAdvance()
    {
        return currentAdvance;
    }

    public void setCurrentAdvance(BigDecimal currentAdvance)
    {
        this.currentAdvance = currentAdvance;
    }

    public BigDecimal getReturnSituation()
    {
        return returnSituation;
    }

    public void setReturnSituation(BigDecimal returnSituation)
    {
        this.returnSituation = returnSituation;
    }

    // 为前端兼容添加的别名方法
    public BigDecimal getTotalExpenseAmount()
    {
        return totalExpense;
    }

    public BigDecimal getTotalPurchaseAmount()
    {
        return totalPurchase;
    }

    public BigDecimal getTotalSaleAmount()
    {
        return totalSale;
    }

    public BigDecimal getTotalPaymentAmount()
    {
        return totalPayment;
    }

    public BigDecimal getTotalInvestAmount()
    {
        return totalInvest;
    }

    public BigDecimal getCurrentAdvanceAmount()
    {
        return currentAdvance;
    }

    public BigDecimal getProfit()
    {
        return returnSituation;
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
            .append("accountingId", getAccountingId())
            .append("deptId", getDeptId())
            .append("accountingNo", getAccountingNo())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("totalExpense", getTotalExpense())
            .append("totalPurchase", getTotalPurchase())
            .append("totalSale", getTotalSale())
            .append("totalPayment", getTotalPayment())
            .append("returnSituation", getReturnSituation())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
