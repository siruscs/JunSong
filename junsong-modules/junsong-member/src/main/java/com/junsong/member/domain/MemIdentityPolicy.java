package com.junsong.member.domain;

import com.junsong.common.core.web.domain.BaseEntity;

/** 门店顾客身份识别策略。 */
public class MemIdentityPolicy extends BaseEntity
{
    private Long policyId;
    private Long tenantId;
    private Long deptId;
    private String identityMode;
    private Boolean allowAnonymous;
    private Boolean allowMemberWithoutPhone;
    private String status;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long v) { policyId = v; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long v) { tenantId = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { deptId = v; }
    public String getIdentityMode() { return identityMode; }
    public void setIdentityMode(String v) { identityMode = v; }
    public Boolean getAllowAnonymous() { return allowAnonymous; }
    public void setAllowAnonymous(Boolean v) { allowAnonymous = v; }
    public Boolean getAllowMemberWithoutPhone() { return allowMemberWithoutPhone; }
    public void setAllowMemberWithoutPhone(Boolean v) { allowMemberWithoutPhone = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
}
