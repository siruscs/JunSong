package com.junsong.system.controller;

import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.system.service.ISysDashboardHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class SysDashboardController
{
    @Autowired
    private ISysDashboardHealthService dashboardHealthService;

    @GetMapping("/health")
    public AjaxResult health()
    {
        return AjaxResult.success(dashboardHealthService.getDashboardHealth());
    }
}
