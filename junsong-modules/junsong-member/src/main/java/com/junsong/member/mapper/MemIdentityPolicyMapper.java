package com.junsong.member.mapper;

import com.junsong.member.domain.MemIdentityPolicy;

public interface MemIdentityPolicyMapper
{
    MemIdentityPolicy selectByTenantAndDept(Long tenantId, Long deptId);
    int insert(MemIdentityPolicy policy);
    int update(MemIdentityPolicy policy);
}
