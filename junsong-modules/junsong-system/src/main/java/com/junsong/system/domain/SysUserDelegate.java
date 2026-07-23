package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 用户委托代理表 sys_user_delegate
 */
public class SysUserDelegate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 委托ID */
    private Long id;

    /** 委托人用户ID */
    private Long userId;

    /** 委托人用户名（列表展示） */
    private String userName;

    /** 代理人用户ID */
    private Long delegateUserId;

    /** 代理人用户名（列表展示） */
    private String delegateUserName;

    /** 委托类型（all=全部, workflow=工作流, system=系统） */
    private String delegateType;

    /** 指定流程标识（逗号分隔，type=workflow时生效） */
    private String processKeys;

    /** 委托开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 委托结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public Long getDelegateUserId()
    {
        return delegateUserId;
    }

    public void setDelegateUserId(Long delegateUserId)
    {
        this.delegateUserId = delegateUserId;
    }

    public String getDelegateUserName()
    {
        return delegateUserName;
    }

    public void setDelegateUserName(String delegateUserName)
    {
        this.delegateUserName = delegateUserName;
    }

    public String getDelegateType()
    {
        return delegateType;
    }

    public void setDelegateType(String delegateType)
    {
        this.delegateType = delegateType;
    }

    public String getProcessKeys()
    {
        return processKeys;
    }

    public void setProcessKeys(String processKeys)
    {
        this.processKeys = processKeys;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
