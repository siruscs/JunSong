package com.junsong.open.domain;

import java.util.Date;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * ISV（独立软件供应商）注册对象 open_isv
 *
 * @author junsong
 */
public class OpenIsv extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "ISV名称")
    private String isvName;

    @Excel(name = "联系人")
    private String contactName;

    @Excel(name = "联系电话")
    private String contactPhone;

    @Excel(name = "联系邮箱")
    private String contactEmail;

    @Excel(name = "公司名称")
    private String companyName;

    private String businessLicense;

    @Excel(name = "网站地址")
    private String websiteUrl;

    /** 接入类型：merchant(商户) / service_provider(服务商) / internal(内部) */
    @Excel(name = "接入类型")
    private String accessType;

    /** 状态：PENDING / APPROVED / REJECTED */
    @Excel(name = "审核状态")
    private String status;

    private String rejectReason;

    private Long tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIsvName() { return isvName; }
    public void setIsvName(String isvName) { this.isvName = isvName; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
