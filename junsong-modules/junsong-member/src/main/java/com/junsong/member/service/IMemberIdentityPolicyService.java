package com.junsong.member.service;

import com.junsong.member.domain.MemIdentityPolicy;

public interface IMemberIdentityPolicyService
{
    MemIdentityPolicy get(Long tenantId, Long deptId);
    String resolveMode(Long tenantId, Long deptId);
    boolean allowsAnonymous(Long tenantId, Long deptId);
    int save(MemIdentityPolicy policy);
}
