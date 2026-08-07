package com.junsong.member.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.idempotency.IdempotencyRetryPolicy;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.vo.ConfigSyncPreviewRequest;
import com.junsong.member.domain.vo.ConfigSyncExecuteRequest;
import com.junsong.member.service.IMemberConfigSyncService;

@RestController
@RequestMapping("/config-sync")
public class MemConfigSyncController
{
    private final IMemberConfigSyncService configSyncService;

    public MemConfigSyncController(IMemberConfigSyncService configSyncService)
    {
        this.configSyncService = configSyncService;
    }

    @RequiresPermissions("member:configSync:query")
    @Log(title = "跨机构配置同步预览", businessType = BusinessType.OTHER)
    @Idempotent(scene = "member:configSync:preview")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody ConfigSyncPreviewRequest request)
    {
        Long tenantId = TenantContext.getTenantId();
        Long deptId = SecurityUtils.getDeptId();
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> result = configSyncService.preview(request, tenantId, deptId, userId,
                SecurityUtils.getUsername());
        return AjaxResult.success(result);
    }

    @RequiresPermissions("member:configSync:query")
    @GetMapping("/{batchId}")
    public AjaxResult detail(@PathVariable Long batchId)
    {
        return AjaxResult.success(configSyncService.getBatch(TenantContext.getTenantId(), batchId));
    }

    @RequiresPermissions("member:configSync:query")
    @Log(title = "跨机构配置同步执行", businessType = BusinessType.UPDATE)
    @Idempotent(scene = "member:configSync:execute", retryPolicy = IdempotencyRetryPolicy.ALLOW_SAME_KEY)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody ConfigSyncExecuteRequest request)
    {
        return AjaxResult.success(configSyncService.execute(request, TenantContext.getTenantId(),
                SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }
}
