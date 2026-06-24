package com.junsong.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分规则表对象 mem_points_rule
 *
 * @author junsong
 */
public class MemPointsRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    @Excel(name = "规则ID", cellType = ColumnType.NUMERIC)
    private Long ruleId;

    /** 规则名称 */
    @Excel(name = "规则名称")
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过128个字符")
    private String ruleName;

    /** 规则代码 */
    @Excel(name = "规则代码")
    @NotBlank(message = "规则代码不能为空")
    @Size(max = 64, message = "规则代码长度不能超过64个字符")
    private String ruleCode;

    /** 计算方式(进一法/四舍五入/舍零取整) */
    @Excel(name = "计算方式", readConverterExp = "进一法=进一法,四舍五入=四舍五入,舍零取整=舍零取整")
    @NotBlank(message = "计算方式不能为空")
    @Size(max = 32, message = "计算方式长度不能超过32个字符")
    private String ruleType;

    /** 每元积分 */
    @Excel(name = "每元积分", cellType = ColumnType.NUMERIC)
    @NotNull(message = "每元积分不能为空")
    private BigDecimal pointsPerYuan;

    /** 积分有效期(天) */
    @Excel(name = "积分有效期(天)", cellType = ColumnType.NUMERIC)
    private Integer validityDays;

    /** 状态(0启用/1禁用) */
    @Excel(name = "状态", readConverterExp = "0=启用,1=禁用")
    private String status;

    /** 删除标志 */
    private String delFlag;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public void setRuleName(String ruleName)
    {
        this.ruleName = ruleName;
    }

    public String getRuleCode()
    {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode)
    {
        this.ruleCode = ruleCode;
    }

    public String getRuleType()
    {
        return ruleType;
    }

    public void setRuleType(String ruleType)
    {
        this.ruleType = ruleType;
    }

    public BigDecimal getPointsPerYuan()
    {
        return pointsPerYuan;
    }

    public void setPointsPerYuan(BigDecimal pointsPerYuan)
    {
        this.pointsPerYuan = pointsPerYuan;
    }

    public Integer getValidityDays()
    {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays)
    {
        this.validityDays = validityDays;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
        return "MemPointsRule{" +
            "ruleId=" + ruleId +
            ", ruleName='" + ruleName + '\'' +
            ", ruleCode='" + ruleCode + '\'' +
            ", ruleType='" + ruleType + '\'' +
            ", pointsPerYuan=" + pointsPerYuan +
            ", validityDays=" + validityDays +
            ", status='" + status + '\'' +
            '}';
    }
}
