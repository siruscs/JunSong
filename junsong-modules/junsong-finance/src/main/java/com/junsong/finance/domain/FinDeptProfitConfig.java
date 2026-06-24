package com.junsong.finance.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

public class FinDeptProfitConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "配置ID", cellType = ColumnType.NUMERIC)
    private Long configId;

    @Excel(name = "机构ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "机构ID不能为空")
    private Long deptId;

    @Excel(name = "店长分润比例", cellType = ColumnType.NUMERIC)
    @NotNull(message = "店长分润比例不能为空")
    private BigDecimal managerProfitRate;

    @Excel(name = "自动生成投资人返款", readConverterExp = "0=否,1=是")
    private String autoCreateInvestorPayment;

    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    private String delFlag;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public BigDecimal getManagerProfitRate() { return managerProfitRate; }
    public void setManagerProfitRate(BigDecimal managerProfitRate) { this.managerProfitRate = managerProfitRate; }
    public String getAutoCreateInvestorPayment() { return autoCreateInvestorPayment; }
    public void setAutoCreateInvestorPayment(String autoCreateInvestorPayment) { this.autoCreateInvestorPayment = autoCreateInvestorPayment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("deptId", getDeptId())
            .append("managerProfitRate", getManagerProfitRate())
            .append("autoCreateInvestorPayment", getAutoCreateInvestorPayment())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
