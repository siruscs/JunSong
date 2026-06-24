package com.junsong.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import java.util.Date;

/**
 * 秒杀领取明细对象 mem_seckill_claim_record
 */
public class MemSeckillClaimRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 领取ID */
    @Excel(name = "领取ID", cellType = ColumnType.NUMERIC)
    private Long claimId;

    /** 部门ID */
    private Long deptId;

    /** 秒杀记录ID */
    @Excel(name = "秒杀记录ID", cellType = ColumnType.NUMERIC)
    private Long recordId;

    /** 秒杀活动ID */
    @Excel(name = "秒杀活动ID", cellType = ColumnType.NUMERIC)
    private Long seckillId;

    /** 会员ID */
    private Long memberId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    private String memberName;

    /** 本次领取份额 */
    @Excel(name = "领取份额", cellType = ColumnType.NUMERIC)
    private Integer claimShares;

    /** 领取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "领取时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date claimTime;

    /** 领取人 */
    @Excel(name = "领取人")
    private String claimBy;

    /** 删除标志 */
    private String delFlag;

    /** 秒杀活动名称（非数据库字段） */
    private String seckillName;

    public Long getClaimId()
    {
        return claimId;
    }

    public void setClaimId(Long claimId)
    {
        this.claimId = claimId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getSeckillId()
    {
        return seckillId;
    }

    public void setSeckillId(Long seckillId)
    {
        this.seckillId = seckillId;
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

    public Integer getClaimShares()
    {
        return claimShares;
    }

    public void setClaimShares(Integer claimShares)
    {
        this.claimShares = claimShares;
    }

    public Date getClaimTime()
    {
        return claimTime;
    }

    public void setClaimTime(Date claimTime)
    {
        this.claimTime = claimTime;
    }

    public String getClaimBy()
    {
        return claimBy;
    }

    public void setClaimBy(String claimBy)
    {
        this.claimBy = claimBy;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getSeckillName()
    {
        return seckillName;
    }

    public void setSeckillName(String seckillName)
    {
        this.seckillName = seckillName;
    }
}
