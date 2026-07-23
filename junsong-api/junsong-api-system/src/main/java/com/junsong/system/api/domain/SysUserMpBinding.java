package com.junsong.system.api.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 小程序微信账号绑定关系表 sys_user_mp_binding
 *
 * <p>用于记录微信身份（appId + openid）与系统账号（tenantId + userId）的绑定关系。
 * 一个 (appId, openid) 全局只能绑定一个系统账号；解绑使用 REVOKED 状态保留审计链，不物理删除。</p>
 *
 * <p>安全注意：openid/unionid 属于敏感身份标识，日志输出时必须脱敏，
 * 不得在 toString 或日志中输出明文。</p>
 */
public class SysUserMpBinding extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 绑定关系ID */
    private Long bindingId;

    /** 租户ID（所有查询必须显式带此字段） */
    private Long tenantId;

    /** 绑定的系统用户ID */
    private Long userId;

    /** 微信小程序 AppID */
    private String appId;

    /** 微信 openid（同一 AppID 下唯一） */
    private String openid;

    /** 微信 unionid（可空） */
    private String unionid;

    /** 绑定状态 ACTIVE/REVOKED */
    private String status;

    /** 首次绑定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date boundTime;

    /** 最近一次微信快捷登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 绑定操作人 */
    private String boundBy;

    /** 撤销时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date revokedTime;

    /** 撤销操作人 */
    private String revokedBy;

    /** 撤销原因 */
    private String revokeReason;

    public Long getBindingId()
    {
        return bindingId;
    }

    public void setBindingId(Long bindingId)
    {
        this.bindingId = bindingId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    /**
     * 获取 openid（敏感字段，避免日志输出明文）
     */
    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    /**
     * 获取 unionid（敏感字段，避免日志输出明文）
     */
    public String getUnionid()
    {
        return unionid;
    }

    public void setUnionid(String unionid)
    {
        this.unionid = unionid;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getBoundTime()
    {
        return boundTime;
    }

    public void setBoundTime(Date boundTime)
    {
        this.boundTime = boundTime;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public String getBoundBy()
    {
        return boundBy;
    }

    public void setBoundBy(String boundBy)
    {
        this.boundBy = boundBy;
    }

    public Date getRevokedTime()
    {
        return revokedTime;
    }

    public void setRevokedTime(Date revokedTime)
    {
        this.revokedTime = revokedTime;
    }

    public String getRevokedBy()
    {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy)
    {
        this.revokedBy = revokedBy;
    }

    public String getRevokeReason()
    {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason)
    {
        this.revokeReason = revokeReason;
    }

    /**
     * 脱敏后的 openid（仅保留前 4 位 + ***），用于审计日志输出。
     */
    @JsonIgnore
    public String getMaskedOpenid()
    {
        if (openid == null || openid.length() <= 4)
        {
            return "***";
        }
        return openid.substring(0, 4) + "***";
    }
}
