package com.junsong.system.service.impl;

import com.junsong.system.domain.SysDashboardHealth;
import com.junsong.system.domain.SysServiceHealth;
import com.junsong.system.service.ISysDashboardHealthService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDashboardHealthServiceImpl implements ISysDashboardHealthService
{
    private final SystemMetricsProvider systemMetricsProvider;
    private final ServiceHealthProbe serviceHealthProbe;

    public SysDashboardHealthServiceImpl(SystemMetricsProvider systemMetricsProvider, ServiceHealthProbe serviceHealthProbe)
    {
        this.systemMetricsProvider = systemMetricsProvider;
        this.serviceHealthProbe = serviceHealthProbe;
    }

    @Override
    public SysDashboardHealth getDashboardHealth()
    {
        SysDashboardHealth.SystemMetrics metrics = systemMetricsProvider.getSystemMetrics();
        List<SysServiceHealth> services = serviceHealthProbe.probeServices();
        int upCount = (int) services.stream().filter(item -> "UP".equals(item.getStatus())).count();
        int downCount = services.size() - upCount;
        int resourceScore = 100 - (int) Math.round((metrics.getCpuUsage() + metrics.getMemoryUsage() + metrics.getDiskUsage()) / 3.0);
        int serviceScore = services.isEmpty() ? 100 : (int) Math.round(upCount * 100.0 / services.size());
        int overallScore = Math.max(0, Math.min(100, (int) Math.round(resourceScore * 0.6 + serviceScore * 0.4)));

        SysDashboardHealth health = new SysDashboardHealth();
        health.setCpuUsage(metrics.getCpuUsage());
        health.setMemoryUsage(metrics.getMemoryUsage());
        health.setDiskUsage(metrics.getDiskUsage());
        health.setAvailableProcessors(metrics.getAvailableProcessors());
        health.setTotalMemoryGb(metrics.getTotalMemoryGb());
        health.setHostName(metrics.getHostName());
        health.setOsName(metrics.getOsName());
        health.setServices(services);
        health.setServiceCount(services.size());
        health.setUpServiceCount(upCount);
        health.setDownServiceCount(downCount);
        health.setOverallScore(overallScore);
        health.setLevel(resolveLevel(overallScore, downCount));
        health.setUpdateTime(System.currentTimeMillis());
        return health;
    }

    private String resolveLevel(int overallScore, int downCount)
    {
        if (downCount > 0 || overallScore < 70)
        {
            return "WARN";
        }
        if (overallScore < 85)
        {
            return "GOOD";
        }
        return "EXCELLENT";
    }
}
