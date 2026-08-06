package com.junsong.member.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.MemIdentityPolicy;
import com.junsong.member.service.IMemberIdentityPolicyService;

@RestController
@RequestMapping("/identity/policy")
public class MemIdentityPolicyController extends BaseController
{
    private final IMemberIdentityPolicyService service;

    public MemIdentityPolicyController(IMemberIdentityPolicyService service)
    {
        this.service = service;
    }

    @RequiresPermissions("member:identityPolicy:query")
    @GetMapping
    public AjaxResult get()
    {
        return AjaxResult.success(service.get(TenantContext.getTenantId(), SecurityUtils.getDeptId()));
    }

    @RequiresPermissions("member:identityPolicy:edit")
    @PostMapping
    public AjaxResult save(@RequestBody MemIdentityPolicy policy)
    {
        policy.setTenantId(TenantContext.getTenantId());
        policy.setDeptId(SecurityUtils.getDeptId());
        return toAjax(service.save(policy));
    }
}
