package com.junsong.system.service.impl;

import com.junsong.system.domain.SysDashboardHealth;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Arrays;

@Component
public class JdkSystemMetricsProvider implements SystemMetricsProvider
{
    @Override
    public SysDashboardHealth.SystemMetrics getSystemMetrics()
    {
        SysDashboardHealth.SystemMetrics metrics = new SysDashboardHealth.SystemMetrics();
        com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long maxJvmMemory = runtime.maxMemory();
        long usedJvmMemory = runtime.totalMemory() - runtime.freeMemory();
        File[] roots = File.listRoots();
        long totalDisk = Arrays.stream(roots).mapToLong(File::getTotalSpace).sum();
        long freeDisk = Arrays.stream(roots).mapToLong(File::getFreeSpace).sum();

        metrics.setCpuUsage(roundPercent(osBean.getCpuLoad() * 100));
        metrics.setMemoryUsage(roundPercent(percent(totalMemory - freeMemory, totalMemory)));
        metrics.setDiskUsage(roundPercent(percent(totalDisk - freeDisk, totalDisk)));
        metrics.setAvailableProcessors(osBean.getAvailableProcessors());
        metrics.setTotalMemoryGb(toGb(totalMemory > 0 ? totalMemory : maxJvmMemory));
        metrics.setHostName(resolveHostName());
        metrics.setOsName(osBean.getName() + " " + osBean.getVersion());
        if (metrics.getMemoryUsage() <= 0 && maxJvmMemory > 0)
        {
            metrics.setMemoryUsage(roundPercent(percent(usedJvmMemory, maxJvmMemory)));
        }
        return metrics;
    }

    private double percent(long used, long total)
    {
        if (total <= 0)
        {
            return 0.0;
        }
        return used * 100.0 / total;
    }

    private double roundPercent(double value)
    {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0)
        {
            return 0.0;
        }
        return Math.round(Math.min(value, 100.0) * 10.0) / 10.0;
    }

    private long toGb(long bytes)
    {
        if (bytes <= 0)
        {
            return 0L;
        }
        return Math.max(1L, Math.round(bytes / 1024.0 / 1024.0 / 1024.0));
    }

    private String resolveHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }
}
