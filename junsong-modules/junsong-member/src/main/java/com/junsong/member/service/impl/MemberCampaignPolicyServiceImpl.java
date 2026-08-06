package com.junsong.member.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.domain.MemCampaignPolicy;
import com.junsong.member.domain.MemCampaignPolicyPackage;
import com.junsong.member.mapper.MemCampaignPolicyMapper;
import com.junsong.member.service.IMemberCampaignPolicyService;
import com.junsong.member.service.MemberCampaignPolicyValidator;

@Service
public class MemberCampaignPolicyServiceImpl implements IMemberCampaignPolicyService
{
    private final MemCampaignPolicyMapper policyMapper;
    private final JdbcTemplate jdbcTemplate;

    public MemberCampaignPolicyServiceImpl(MemCampaignPolicyMapper policyMapper, JdbcTemplate jdbcTemplate)
    {
        this.policyMapper = policyMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MemCampaignPolicy selectPolicyById(MemCampaignPolicy query)
    {
        return policyMapper.selectPolicyById(query);
    }

    @Override
    public List<MemCampaignPolicy> selectPolicyList(MemCampaignPolicy policy)
    {
        return policyMapper.selectPolicyList(policy);
    }

    @Override
    @Transactional
    public int createPolicy(MemCampaignPolicy policy)
    {
        validate(policy);
        List<Map<String, Object>> periods = jdbcTemplate.queryForList(
                "select start_time, end_time from fin_accounting_period where period_id = ? and tenant_id = ? and dept_id = ? and del_flag = '0' limit 1",
                policy.getPeriodId(), policy.getTenantId(), policy.getDeptId());
        if (periods.isEmpty()) throw new IllegalArgumentException("核算周期不存在或不属于当前机构");
        policy.setEffectiveStart(normalizeDate(periods.get(0).get("start_time")));
        policy.setEffectiveEnd(normalizeDate(periods.get(0).get("end_time")));
        if (policy.getVersion() == null) policy.setVersion(1);
        if (policy.getCustomerScope() == null) policy.setCustomerScope("ALL");
        if (policy.getStatus() == null) policy.setStatus("0");
        int rows = policyMapper.insertPolicy(policy);
        if (rows != 1) return rows;
        for (MemCampaignPolicyPackage item : policy.getPackages())
        {
            item.setPolicyId(policy.getPolicyId());
            item.setTenantId(policy.getTenantId());
            item.setDeptId(policy.getDeptId());
            if (item.getGiftQuantity() == null) item.setGiftQuantity(BigDecimal.ZERO);
            item.setTotalQuantity(item.getPurchaseQuantity().add(item.getGiftQuantity()));
            policyMapper.insertPolicyPackage(item);
        }
        return rows;
    }

    @Override
    @Transactional
    public int updatePolicy(MemCampaignPolicy policy, String operator)
    {
        validate(policy);
        List<Map<String, Object>> periods = jdbcTemplate.queryForList(
                "select start_time, end_time from fin_accounting_period where period_id = ? and tenant_id = ? and dept_id = ? and del_flag = '0' limit 1",
                policy.getPeriodId(), policy.getTenantId(), policy.getDeptId());
        if (periods.isEmpty()) throw new IllegalArgumentException("核算周期不存在或不属于当前机构");
        policy.setEffectiveStart(normalizeDate(periods.get(0).get("start_time")));
        policy.setEffectiveEnd(normalizeDate(periods.get(0).get("end_time")));
        policy.setUpdateBy(operator);
        int rows = policyMapper.updatePolicy(policy);
        if (rows != 1) return rows;
        policyMapper.deletePolicyPackages(policy.getPolicyId(), policy.getTenantId(), policy.getDeptId(), operator);
        for (MemCampaignPolicyPackage item : policy.getPackages())
        {
            item.setPolicyId(policy.getPolicyId());
            item.setTenantId(policy.getTenantId());
            item.setDeptId(policy.getDeptId());
            if (item.getGiftQuantity() == null) item.setGiftQuantity(BigDecimal.ZERO);
            item.setTotalQuantity(item.getPurchaseQuantity().add(item.getGiftQuantity()));
            item.setCreateBy(operator);
            policyMapper.insertPolicyPackage(item);
        }
        return rows;
    }

    @Override
    public int changeStatus(Long policyId, Long tenantId, Long deptId, String status, String operator)
    {
        if (!List.of("0", "1", "2", "3").contains(status))
        {
            throw new IllegalArgumentException("invalid policy status");
        }
        return policyMapper.updatePolicyStatus(policyId, tenantId, deptId, status, operator);
    }

    private void validate(MemCampaignPolicy policy)
    {
        if (policy == null || policy.getTenantId() == null || policy.getDeptId() == null
                || policy.getPeriodId() == null || policy.getProductId() == null)
        {
            throw new IllegalArgumentException("policy tenant, department, period and product are required");
        }
        if (policy.getPackages() == null || policy.getPackages().isEmpty())
        {
            throw new IllegalArgumentException("policy packages are required");
        }
        MemberCampaignPolicyValidator.validatePackageRules(policy.getPackages().stream()
                .map(item -> new com.junsong.member.service.MemberCampaignPackageCalculator.PackageRule(
                        String.valueOf(item.getPackageId() == null ? item.getPackageName() : item.getPackageId()),
                        item.getPurchaseQuantity().intValueExact(),
                        item.getGiftQuantity() == null ? 0 : item.getGiftQuantity().intValueExact()))
                .toList());
    }

    static Date normalizeDate(Object value)
    {
        if (value == null) return null;
        if (value instanceof Date date) return date;
        if (value instanceof LocalDateTime dateTime)
        {
            return Date.from(dateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
        }
        if (value instanceof LocalDate date)
        {
            return Date.from(date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        }
        throw new IllegalArgumentException("不支持的核算周期时间类型: " + value.getClass().getName());
    }
}
