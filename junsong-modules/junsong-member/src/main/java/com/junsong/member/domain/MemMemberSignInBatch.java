package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员签到补录批次对象 mem_member_sign_in_batch
 *
 * @author junsong
 */
public class MemMemberSignInBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    @Excel(name = "批次ID", cellType = ColumnType.NUMERIC)
    private Long batchId;

    /** 租户ID */
    private Long tenantId;

    /** 部门ID */
    private Long deptId;

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long memberId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    private String memberName;

    /** 目标月份（yyyy-MM） */
    @Excel(name = "目标月份")
    private String targetMonth;

    /** 补录模式（SELECT_DATES / COUNT_ONLY） */
    @Excel(name = "补录模式", readConverterExp = "SELECT_DATES=选择日期,COUNT_ONLY=按次数补录")
    private String fillMode;

    /** 请求补录次数 */
    @Excel(name = "请求次数", cellType = ColumnType.NUMERIC)
    private Integer requestedCount;

    /** 实际补录次数 */
    @Excel(name = "实际次数", cellType = ColumnType.NUMERIC)
    private Integer actualCount;

    /** 选中的日期（逗号分隔） */
    private String selectedDates;

    /** 奖励等级编码快照 */
    @Excel(name = "奖励等级编码")
    private String rewardLevelCode;

    /** 单次签到积分 */
    @Excel(name = "单次积分", cellType = ColumnType.NUMERIC)
    private BigDecimal pointsPerSign;

    /** 单次签到成长值 */
    @Excel(name = "单次成长值", cellType = ColumnType.NUMERIC)
    private Long growthPerSign;

    /** 总积分 */
    @Excel(name = "总积分", cellType = ColumnType.NUMERIC)
    private BigDecimal totalPoints;

    /** 总成长值 */
    @Excel(name = "总成长值", cellType = ColumnType.NUMERIC)
    private Long totalGrowth;

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    public String getMemberName()
    {
        return memberName;
    }

    public void setMemberName(String memberName)
    {
        this.memberName = memberName;
    }

    public String getTargetMonth()
    {
        return targetMonth;
    }

    public void setTargetMonth(String targetMonth)
    {
        this.targetMonth = targetMonth;
    }

    public String getFillMode()
    {
        return fillMode;
    }

    public void setFillMode(String fillMode)
    {
        this.fillMode = fillMode;
    }

    public Integer getRequestedCount()
    {
        return requestedCount;
    }

    public void setRequestedCount(Integer requestedCount)
    {
        this.requestedCount = requestedCount;
    }

    public Integer getActualCount()
    {
        return actualCount;
    }

    public void setActualCount(Integer actualCount)
    {
        this.actualCount = actualCount;
    }

    public String getSelectedDates()
    {
        return selectedDates;
    }

    public void setSelectedDates(String selectedDates)
    {
        this.selectedDates = selectedDates;
    }

    public String getRewardLevelCode()
    {
        return rewardLevelCode;
    }

    public void setRewardLevelCode(String rewardLevelCode)
    {
        this.rewardLevelCode = rewardLevelCode;
    }

    public BigDecimal getPointsPerSign()
    {
        return pointsPerSign;
    }

    public void setPointsPerSign(BigDecimal pointsPerSign)
    {
        this.pointsPerSign = pointsPerSign;
    }

    public Long getGrowthPerSign()
    {
        return growthPerSign;
    }

    public void setGrowthPerSign(Long growthPerSign)
    {
        this.growthPerSign = growthPerSign;
    }

    public BigDecimal getTotalPoints()
    {
        return totalPoints;
    }

    public void setTotalPoints(BigDecimal totalPoints)
    {
        this.totalPoints = totalPoints;
    }

    public Long getTotalGrowth()
    {
        return totalGrowth;
    }

    public void setTotalGrowth(Long totalGrowth)
    {
        this.totalGrowth = totalGrowth;
    }
}
