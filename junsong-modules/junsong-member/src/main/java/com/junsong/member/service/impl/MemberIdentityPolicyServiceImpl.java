package com.junsong.member.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemIdentityPolicy;
import com.junsong.member.mapper.MemIdentityPolicyMapper;
import com.junsong.member.service.IMemberIdentityPolicyService;
import com.junsong.member.service.MemberIdentityPolicyValidator;

@Service
public class MemberIdentityPolicyServiceImpl implements IMemberIdentityPolicyService
{
    private final MemIdentityPolicyMapper mapper;

    public MemberIdentityPolicyServiceImpl(MemIdentityPolicyMapper mapper)
    {
        this.mapper = mapper;
    }

    @Override
    public MemIdentityPolicy get(Long tenantId, Long deptId)
    {
        return mapper.selectByTenantAndDept(tenantId, deptId);
    }

    @Override
    public String resolveMode(Long tenantId, Long deptId)
    {
        MemIdentityPolicy policy = get(tenantId, deptId);
        return policy == null || policy.getIdentityMode() == null ? "MANUAL" : policy.getIdentityMode();
    }

    @Override
    public boolean allowsAnonymous(Long tenantId, Long deptId)
    {
        MemIdentityPolicy policy = get(tenantId, deptId);
        return policy == null || !Boolean.FALSE.equals(policy.getAllowAnonymous());
    }

    @Override
    @Transactional
    public int save(MemIdentityPolicy policy)
    {
        if (policy == null || policy.getTenantId() == null || policy.getDeptId() == null)
            throw new IllegalArgumentException("tenant and department are required");
        MemberIdentityPolicyValidator.validate(policy.getIdentityMode(), policy.getAllowAnonymous());
        MemIdentityPolicy existing = get(policy.getTenantId(), policy.getDeptId());
        if (existing == null) return mapper.insert(policy);
        policy.setPolicyId(existing.getPolicyId());
        return mapper.update(policy);
    }
}
