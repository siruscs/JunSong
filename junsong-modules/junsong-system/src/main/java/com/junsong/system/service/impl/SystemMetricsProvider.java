package com.junsong.system.service.impl;

import com.junsong.system.domain.SysDashboardHealth;

public interface SystemMetricsProvider
{
    SysDashboardHealth.SystemMetrics getSystemMetrics();
}
