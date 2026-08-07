package com.junsong.member.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

/** 核算周期内某个商品的会员政策。 */
public class MemCampaignPolicy extends BaseEntity
{
    private Long policyId;
    private Long tenantId;
    private Long deptId;
    private Long periodId;
    private Long productId;
    private String policyNo;
    private String policyName;
    private Integer version;
    private String customerScope;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date effectiveStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date effectiveEnd;
    private String status;
    private String delFlag;
    private List<MemCampaignPolicyPackage> packages;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long value) { policyId = value; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long value) { deptId = value; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long value) { periodId = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String value) { policyNo = value; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String value) { policyName = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public String getCustomerScope() { return customerScope; }
    public void setCustomerScope(String value) { customerScope = value; }
    public Date getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(Date value) { effectiveStart = value; }
    public Date getEffectiveEnd() { return effectiveEnd; }
    public void setEffectiveEnd(Date value) { effectiveEnd = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String value) { delFlag = value; }
    public List<MemCampaignPolicyPackage> getPackages() { return packages; }
    public void setPackages(List<MemCampaignPolicyPackage> value) { packages = value; }
}
