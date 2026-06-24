package com.junsong.finance.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

public class FinInvestor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "投资人ID", cellType = ColumnType.NUMERIC)
    private Long investorId;

    @Excel(name = "机构ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "机构ID不能为空")
    private Long deptId;

    @Excel(name = "投资人姓名")
    @NotBlank(message = "投资人姓名不能为空")
    private String investorName;

    @Excel(name = "联系电话")
    private String phone;

    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    private String delFlag;

    public Long getInvestorId() { return investorId; }
    public void setInvestorId(Long investorId) { this.investorId = investorId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    @Size(min = 0, max = 64, message = "投资人姓名长度不能超过64个字符")
    public String getInvestorName() { return investorName; }
    public void setInvestorName(String investorName) { this.investorName = investorName; }
    @Size(min = 0, max = 32, message = "联系电话长度不能超过32个字符")
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("investorId", getInvestorId())
            .append("deptId", getDeptId())
            .append("investorName", getInvestorName())
            .append("phone", getPhone())
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
