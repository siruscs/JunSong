package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemCampaignPolicy;

public interface IMemberCampaignPolicyService
{
    MemCampaignPolicy selectPolicyById(MemCampaignPolicy query);
    List<MemCampaignPolicy> selectPolicyList(MemCampaignPolicy policy);
    int createPolicy(MemCampaignPolicy policy);
    int updatePolicy(MemCampaignPolicy policy, String operator);
    int changeStatus(Long policyId, Long tenantId, Long deptId, String status, String operator);
    int deletePolicy(Long policyId, Long tenantId, Long deptId, String operator);
}
