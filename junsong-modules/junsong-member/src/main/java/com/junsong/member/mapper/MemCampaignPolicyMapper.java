package com.junsong.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.member.domain.MemCampaignPolicy;
import com.junsong.member.domain.MemCampaignPolicyPackage;

public interface MemCampaignPolicyMapper
{
    MemCampaignPolicy selectPolicyById(MemCampaignPolicy query);
    List<MemCampaignPolicy> selectPolicyList(MemCampaignPolicy policy);
    int insertPolicy(MemCampaignPolicy policy);
    int insertPolicyPackage(MemCampaignPolicyPackage policyPackage);
    int updatePolicy(MemCampaignPolicy policy);
    int deletePolicyPackages(@Param("policyId") Long policyId, @Param("tenantId") Long tenantId,
                             @Param("deptId") Long deptId, @Param("updateBy") String updateBy);
    int updatePolicyStatus(@Param("policyId") Long policyId, @Param("tenantId") Long tenantId,
                           @Param("deptId") Long deptId, @Param("status") String status,
                           @Param("updateBy") String updateBy);
    int deletePolicy(@Param("policyId") Long policyId, @Param("tenantId") Long tenantId,
                     @Param("deptId") Long deptId, @Param("updateBy") String updateBy);
}
