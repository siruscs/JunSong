package com.junsong.member.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.annotation.Logical;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemCampaignPolicy;
import com.junsong.member.service.IMemberCampaignPolicyService;

/** 会员周期商品政策配置接口。 */
@RestController
@RequestMapping("/campaign/policy")
public class MemCampaignPolicyController
{
    private final IMemberCampaignPolicyService policyService;

    public MemCampaignPolicyController(IMemberCampaignPolicyService policyService)
    {
        this.policyService = policyService;
    }

    @RequiresPermissions(value = {"member:campaignPolicy:list", "member:purchase:add"}, logical = Logical.OR)
    @GetMapping("/list")
    public AjaxResult list(MemCampaignPolicy policy)
    {
        policy.setTenantId(TenantContext.getTenantId());
        policy.setDeptId(SecurityUtils.getDeptId());
        List<MemCampaignPolicy> policies = policyService.selectPolicyList(policy);
        return AjaxResult.success(policies);
    }

    @RequiresPermissions(value = {"member:campaignPolicy:query", "member:purchase:add"}, logical = Logical.OR)
    @GetMapping("/{policyId}")
    public AjaxResult detail(@PathVariable Long policyId)
    {
        MemCampaignPolicy query = new MemCampaignPolicy();
        query.setPolicyId(policyId);
        query.setTenantId(TenantContext.getTenantId());
        query.setDeptId(SecurityUtils.getDeptId());
        return AjaxResult.success(policyService.selectPolicyById(query));
    }

    @RequiresPermissions("member:campaignPolicy:add")
    @Log(title = "会员商品政策", businessType = BusinessType.INSERT)
    @Idempotent(scene = "member:campaign-policy:create")
    @PostMapping
    public AjaxResult create(@RequestBody MemCampaignPolicy policy)
    {
        policy.setTenantId(TenantContext.getTenantId());
        policy.setDeptId(SecurityUtils.getDeptId());
        policy.setCreateBy(SecurityUtils.getUsername());
        return policyService.createPolicy(policy) == 1
                ? AjaxResult.success(policy)
                : AjaxResult.error("政策创建失败");
    }

    @RequiresPermissions("member:campaignPolicy:edit")
    @Log(title = "会员商品政策", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:campaign-policy:update")
    @PutMapping("/{policyId}")
    public AjaxResult update(@PathVariable Long policyId, @RequestBody MemCampaignPolicy policy)
    {
        policy.setPolicyId(policyId);
        policy.setTenantId(TenantContext.getTenantId());
        policy.setDeptId(SecurityUtils.getDeptId());
        return policyService.updatePolicy(policy, SecurityUtils.getUsername()) == 1
                ? AjaxResult.success(policy) : AjaxResult.error("政策更新失败");
    }

    @RequiresPermissions("member:campaignPolicy:edit")
    @Log(title = "会员商品政策", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:campaign-policy:status")
    @PutMapping("/{policyId}/status")
    public AjaxResult changeStatus(@PathVariable Long policyId, @RequestParam String status)
    {
        int rows = policyService.changeStatus(policyId, TenantContext.getTenantId(), SecurityUtils.getDeptId(),
                status, SecurityUtils.getUsername());
        return rows == 1 ? AjaxResult.success() : AjaxResult.error("政策状态更新失败");
    }
}
