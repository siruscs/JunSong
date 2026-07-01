package com.junsong.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 租户表 sys_tenant
 *
 * @author junsong
 */
public class SysTenant extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    @Excel(name = "租户编号", cellType = ColumnType.NUMERIC)
    private Long tenantId;

    /** 租户名称 */
    @Excel(name = "租户名称")
    private String tenantName;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contactName;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 域名 */
    @Excel(name = "域名")
    private String domain;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 用户数量 */
    @Excel(name = "用户数量")
    private Integer accountCount;

    /** 管理员用户名（创建租户时使用） */
    private String adminUserName;

    /** 管理员密码（创建租户时使用） */
    private String adminPassword;

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    @NotBlank(message = "租户名称不能为空")
    @Size(min = 0, max = 30, message = "租户名称长度不能超过30个字符")
    public String getTenantName()
    {
        return tenantName;
    }

    public void setTenantName(String tenantName)
    {
        this.tenantName = tenantName;
    }

    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    @Size(min = 0, max = 20, message = "联系电话长度不能超过20个字符")
    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    @Size(min = 0, max = 128, message = "域名长度不能超过128个字符")
    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Integer getAccountCount()
    {
        return accountCount;
    }

    public void setAccountCount(Integer accountCount)
    {
        this.accountCount = accountCount;
    }

    public String getAdminUserName()
    {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName)
    {
        this.adminUserName = adminUserName;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("tenantId", getTenantId())
            .append("tenantName", getTenantName())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("domain", getDomain())
            .append("status", getStatus())
            .append("expireTime", getExpireTime())
            .append("accountCount", getAccountCount())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
