package com.junsong.system.domain;

import java.util.Date;

/**
 * 用户邀请码对象 sys_invite_code
 *
 * @author junsong
 */
public class SysInviteCode
{
    /** 主键 */
    private Long id;

    /** 邀请码 */
    private String code;

    /** 邀请人用户ID */
    private Long inviterUserId;

    /** 邀请人用户名 */
    private String inviterName;

    /** 状态（0未使用 1已使用 2已失效） */
    private String status;

    /** 被邀请人用户ID */
    private Long inviteeUserId;

    /** 创建时间 */
    private Date createTime;

    /** 使用时间 */
    private Date useTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public Long getInviterUserId()
    {
        return inviterUserId;
    }

    public void setInviterUserId(Long inviterUserId)
    {
        this.inviterUserId = inviterUserId;
    }

    public String getInviterName()
    {
        return inviterName;
    }

    public void setInviterName(String inviterName)
    {
        this.inviterName = inviterName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getInviteeUserId()
    {
        return inviteeUserId;
    }

    public void setInviteeUserId(Long inviteeUserId)
    {
        this.inviteeUserId = inviteeUserId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUseTime()
    {
        return useTime;
    }

    public void setUseTime(Date useTime)
    {
        this.useTime = useTime;
    }
}
