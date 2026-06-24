package com.junsong.system.service.impl;

import com.junsong.system.domain.SysDashboardHealth;
import com.junsong.system.domain.SysServiceHealth;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysDashboardHealthServiceImplTest
{
    @Test
    void calculatesOverallHealthFromResourceUsageAndServiceAvailability()
    {
        TestSystemMetricsProvider metricsProvider = new TestSystemMetricsProvider(40.0, 50.0, 60.0, 8, 16);
        TestServiceHealthProbe serviceHealthProbe = new TestServiceHealthProbe(Arrays.asList(
            SysServiceHealth.up("网关服务", "junsong-gateway", "http://127.0.0.1:8081/actuator/health", 31L),
            SysServiceHealth.down("认证服务", "junsong-auth", "http://127.0.0.1:9200/actuator/health", "Connection refused")
        ));
        SysDashboardHealthServiceImpl service = new SysDashboardHealthServiceImpl(metricsProvider, serviceHealthProbe);

        SysDashboardHealth health = service.getDashboardHealth();

        assertEquals(40.0, health.getCpuUsage());
        assertEquals(50.0, health.getMemoryUsage());
        assertEquals(60.0, health.getDiskUsage());
        assertEquals(8, health.getAvailableProcessors());
        assertEquals(16, health.getTotalMemoryGb());
        assertEquals(2, health.getServices().size());
        assertEquals(1, health.getUpServiceCount());
        assertEquals(1, health.getDownServiceCount());
        assertEquals(50, health.getOverallScore());
        assertEquals("WARN", health.getLevel());
        assertTrue(health.getUpdateTime() > 0);
    }

    private static class TestSystemMetricsProvider implements SystemMetricsProvider
    {
        private final double cpuUsage;
        private final double memoryUsage;
        private final double diskUsage;
        private final int availableProcessors;
        private final long totalMemoryGb;

        private TestSystemMetricsProvider(double cpuUsage, double memoryUsage, double diskUsage, int availableProcessors, long totalMemoryGb)
        {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.availableProcessors = availableProcessors;
            this.totalMemoryGb = totalMemoryGb;
        }

        @Override
        public SysDashboardHealth.SystemMetrics getSystemMetrics()
        {
            SysDashboardHealth.SystemMetrics metrics = new SysDashboardHealth.SystemMetrics();
            metrics.setCpuUsage(cpuUsage);
            metrics.setMemoryUsage(memoryUsage);
            metrics.setDiskUsage(diskUsage);
            metrics.setAvailableProcessors(availableProcessors);
            metrics.setTotalMemoryGb(totalMemoryGb);
            metrics.setHostName("test-host");
            metrics.setOsName("test-os");
            return metrics;
        }
    }

    private static class TestServiceHealthProbe implements ServiceHealthProbe
    {
        private final List<SysServiceHealth> services;

        private TestServiceHealthProbe(List<SysServiceHealth> services)
        {
            this.services = services;
        }

        @Override
        public List<SysServiceHealth> probeServices()
        {
            assertNotNull(services);
            assertFalse(services.isEmpty());
            return services;
        }
    }
}
