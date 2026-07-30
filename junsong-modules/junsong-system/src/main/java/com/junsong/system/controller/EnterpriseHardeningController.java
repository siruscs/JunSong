package com.junsong.system.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;
import com.junsong.system.domain.vo.AlertEventQueryParams;
import com.junsong.system.service.IEnterpriseHardeningService;
import com.junsong.system.service.ISysDataArchiveService;
import com.junsong.system.service.ISysOperationAlertService;
import com.junsong.system.service.ISysOperationAuditService;

/**
 * R25企业级硬化 控制器
 */
@RestController
@RequestMapping("/hardening")
public class EnterpriseHardeningController extends BaseController
{
    private final IEnterpriseHardeningService enterpriseHardeningService;
    private final ISysOperationAuditService auditService;
    private final ISysDataArchiveService archiveService;
    private final ISysOperationAlertService alertService;

    public EnterpriseHardeningController(IEnterpriseHardeningService enterpriseHardeningService,
            ISysOperationAuditService auditService,
            ISysDataArchiveService archiveService,
            ISysOperationAlertService alertService)
    {
        this.enterpriseHardeningService = enterpriseHardeningService;
        this.auditService = auditService;
        this.archiveService = archiveService;
        this.alertService = alertService;
    }

    /**
     * 获取硬化看板数据
     */
    @RequiresPermissions("system:hardening:view")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return AjaxResult.success(enterpriseHardeningService.getDashboard());
    }

    /**
     * 查询审计快照列表
     */
    @RequiresPermissions("system:hardening:audit")
    @PostMapping("/audits")
    public AjaxResult audits(@RequestBody AuditSnapshotQueryParams params)
    {
        return AjaxResult.success(auditService.listSnapshots(params));
    }

    /**
     * 预览归档候选数据量
     */
    @RequiresPermissions("system:hardening:archive")
    @PostMapping("/archive/preview")
    public AjaxResult archivePreview(@RequestBody Map<String, String> body)
    {
        return AjaxResult.success(archiveService.previewArchive(body.get("tableName")));
    }

    /**
     * 执行归档
     */
    @RequiresPermissions("system:hardening:archive")
    @Idempotent(scene = "system:hardening:archive-run", highRisk = true)
    @PostMapping("/archive/run")
    public AjaxResult archiveRun(@RequestBody Map<String, Object> body)
    {
        String tableName = (String) body.get("tableName");
        boolean dryRun = body.get("dryRun") == null || Boolean.TRUE.equals(body.get("dryRun"));
        return AjaxResult.success(archiveService.runArchive(tableName, dryRun));
    }

    /**
     * 查询告警事件列表
     */
    @RequiresPermissions("system:hardening:alert")
    @PostMapping("/alerts")
    public AjaxResult alerts(@RequestBody AlertEventQueryParams params)
    {
        return AjaxResult.success(alertService.listEvents(params));
    }

    /**
     * 确认告警事件
     */
    @RequiresPermissions("system:hardening:alert")
    @Idempotent(scene = "system:hardening:alert-ack")
    @PostMapping("/alerts/{eventId}/ack")
    public AjaxResult ackAlert(@PathVariable Long eventId)
    {
        return AjaxResult.success(alertService.ackEvent(eventId));
    }

    /**
     * 解决告警事件
     */
    @RequiresPermissions("system:hardening:alert")
    @Idempotent(scene = "system:hardening:alert-resolve")
    @PostMapping("/alerts/{eventId}/resolve")
    public AjaxResult resolveAlert(@PathVariable Long eventId)
    {
        return AjaxResult.success(alertService.resolveEvent(eventId));
    }
}
