package com.junsong.system.controller;

import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.service.ISysDataQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据质量看板控制器。
 * R20: 只读展示数据质量问题，不做自动修复。
 */
@RestController
@RequestMapping("/data-quality")
public class SysDataQualityController {

    private final ISysDataQualityService sysDataQualityService;

    public SysDataQualityController(ISysDataQualityService sysDataQualityService) {
        this.sysDataQualityService = sysDataQualityService;
    }

    @RequiresPermissions("system:data-quality:view")
    @GetMapping("/dashboard")
    public AjaxResult dashboard() {
        return AjaxResult.success(sysDataQualityService.getDashboard());
    }
}
