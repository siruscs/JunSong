package com.junsong.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员签到记录对象 mem_member_sign_in
 *
 * @author junsong
 */
public class MemMemberSignIn extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 签到ID */
    @Excel(name = "签到ID", cellType = ColumnType.NUMERIC)
    private Long signId;

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

    /** 签到日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "签到日期", width = 20, dateFormat = "yyyy-MM-dd")
    private Date signDate;

    /** 连续签到天数 */
    @Excel(name = "连续签到天数", cellType = ColumnType.NUMERIC)
    private Integer continuousDays;

    /** 获得积分 */
    @Excel(name = "获得积分", cellType = ColumnType.NUMERIC)
    private BigDecimal pointsEarned;

    /** 获得成长值 */
    @Excel(name = "获得成长值", cellType = ColumnType.NUMERIC)
    private Long growthEarned;

    /** 批次ID（批量补录签到时关联的批次） */
    private Long batchId;

    /** 签到类型（REALTIME 实时签到 / BACKFILL 批量补录） */
    @Excel(name = "签到类型", readConverterExp = "REALTIME=实时签到,BACKFILL=批量补录")
    private String signType = "REALTIME";

    /** 奖励等级快照（签到时会员当前等级编码） */
    @Excel(name = "奖励等级编码")
    private String rewardLevelCode;

    public Long getSignId()
    {
        return signId;
    }

    public void setSignId(Long signId)
    {
        this.signId = signId;
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

    public Date getSignDate()
    {
        return signDate;
    }

    public void setSignDate(Date signDate)
    {
        this.signDate = signDate;
    }

    public Integer getContinuousDays()
    {
        return continuousDays;
    }

    public void setContinuousDays(Integer continuousDays)
    {
        this.continuousDays = continuousDays;
    }

    public BigDecimal getPointsEarned()
    {
        return pointsEarned;
    }

    public void setPointsEarned(BigDecimal pointsEarned)
    {
        this.pointsEarned = pointsEarned;
    }

    public Long getGrowthEarned()
    {
        return growthEarned;
    }

    public void setGrowthEarned(Long growthEarned)
    {
        this.growthEarned = growthEarned;
    }

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public String getSignType()
    {
        return signType;
    }

    public void setSignType(String signType)
    {
        this.signType = signType;
    }

    public String getRewardLevelCode()
    {
        return rewardLevelCode;
    }

    public void setRewardLevelCode(String rewardLevelCode)
    {
        this.rewardLevelCode = rewardLevelCode;
    }
}
