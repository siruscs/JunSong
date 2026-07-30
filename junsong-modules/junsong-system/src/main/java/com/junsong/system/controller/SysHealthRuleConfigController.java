package com.junsong.system.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.SysHealthRuleConfig;
import com.junsong.system.service.ISysHealthRuleConfigService;

/**
 * 自检规则配置 Controller
 */
@RestController
@RequestMapping("/health-rule")
public class SysHealthRuleConfigController extends BaseController {

    private final ISysHealthRuleConfigService service;

    public SysHealthRuleConfigController(ISysHealthRuleConfigService service) {
        this.service = service;
    }

    @RequiresPermissions("system:healthRule:list")
    @GetMapping("/list")
    public TableDataInfo list(SysHealthRuleConfig query) {
        startPage();
        List<SysHealthRuleConfig> list = service.selectHealthRuleList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("system:healthRule:query")
    @GetMapping("/{ruleId}")
    public AjaxResult getInfo(@PathVariable Long ruleId) {
        return AjaxResult.success(service.selectById(ruleId));
    }

    @RequiresPermissions("system:healthRule:edit")
    @Idempotent(scene = "system:health-rule:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody SysHealthRuleConfig config) {
        return toAjax(service.updateHealthRule(config));
    }

    @RequiresPermissions("system:healthRule:edit")
    @Idempotent(scene = "system:health-rule:toggle")
    @PutMapping("/{ruleId}/toggle")
    public AjaxResult toggle(@PathVariable Long ruleId, @RequestBody Map<String, String> body) {
        String enabled = body.get("enabled");
        return toAjax(service.toggleEnabled(ruleId, enabled));
    }
}
