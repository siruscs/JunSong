package com.junsong.member.domain;

import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员成长值变动记录对象 mem_growth_record
 *
 * @author junsong
 */
public class MemGrowthRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    private Long recordId;

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

    /** 来源: SALE/SIGN_IN/MANUAL/DECAY */
    @Excel(name = "来源类型", readConverterExp = "SALE=消费,SIGN_IN=签到,MANUAL=手动调整,DECAY=衰减")
    private String sourceType;

    /** 来源业务ID */
    private Long sourceId;

    /** 幂等键 */
    private String dedupKey;

    /** 成长值变动 */
    @Excel(name = "成长值变动", cellType = ColumnType.NUMERIC)
    private Long growthChange;

    /** 变动后成长值 */
    @Excel(name = "变动后成长值", cellType = ColumnType.NUMERIC)
    private Long balance;

    /** 变动前等级 */
    @Excel(name = "变动前等级")
    private String beforeLevel;

    /** 变动后等级 */
    @Excel(name = "变动后等级")
    private String afterLevel;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
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

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getDedupKey()
    {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey)
    {
        this.dedupKey = dedupKey;
    }

    public Long getGrowthChange()
    {
        return growthChange;
    }

    public void setGrowthChange(Long growthChange)
    {
        this.growthChange = growthChange;
    }

    public Long getBalance()
    {
        return balance;
    }

    public void setBalance(Long balance)
    {
        this.balance = balance;
    }

    public String getBeforeLevel()
    {
        return beforeLevel;
    }

    public void setBeforeLevel(String beforeLevel)
    {
        this.beforeLevel = beforeLevel;
    }

    public String getAfterLevel()
    {
        return afterLevel;
    }

    public void setAfterLevel(String afterLevel)
    {
        this.afterLevel = afterLevel;
    }

    @Override
    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
