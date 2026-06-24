package com.junsong.system.service.impl;

import com.junsong.system.domain.SysServiceHealth;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class ActuatorServiceHealthProbe implements ServiceHealthProbe
{
    private final RestTemplate restTemplate = new RestTemplate();
    private final List<ServiceEndpoint> endpoints = List.of(
        new ServiceEndpoint("网关服务", "junsong-gateway", "http://junsong-gateway:8080/actuator/health"),
        new ServiceEndpoint("认证服务", "junsong-auth", "http://junsong-auth:9200/actuator/health"),
        new ServiceEndpoint("系统服务", "junsong-system", "http://junsong-modules-system:9201/actuator/health"),
        new ServiceEndpoint("代码生成", "junsong-gen", "http://junsong-modules-gen:9202/actuator/health"),
        new ServiceEndpoint("定时任务", "junsong-job", "http://junsong-modules-job:9203/actuator/health"),
        new ServiceEndpoint("文件服务", "junsong-file", "http://junsong-modules-file:9300/actuator/health"),
        new ServiceEndpoint("会员服务", "junsong-member", "http://junsong-modules-member:9206/actuator/health"),
        new ServiceEndpoint("财务服务", "junsong-finance", "http://junsong-modules-finance:9205/actuator/health"),
        new ServiceEndpoint("监控服务", "junsong-monitor", "http://junsong-visual-monitor:9100/actuator/health")
    );

    @Override
    public List<SysServiceHealth> probeServices()
    {
        List<SysServiceHealth> services = new ArrayList<>();
        for (ServiceEndpoint endpoint : endpoints)
        {
            services.add(probe(endpoint));
        }
        return services;
    }

    private SysServiceHealth probe(ServiceEndpoint endpoint)
    {
        long start = System.currentTimeMillis();
        try
        {
            String response = restTemplate.getForObject(endpoint.url, String.class);
            long responseTime = System.currentTimeMillis() - start;
            if (response != null && response.contains("\"status\":\"UP\""))
            {
                return SysServiceHealth.up(endpoint.name, endpoint.code, endpoint.url, responseTime);
            }
            return SysServiceHealth.down(endpoint.name, endpoint.code, endpoint.url, "健康检查未返回UP");
        }
        catch (Exception e)
        {
            return SysServiceHealth.down(endpoint.name, endpoint.code, endpoint.url, e.getClass().getSimpleName());
        }
    }

    private static class ServiceEndpoint
    {
        private final String name;
        private final String code;
        private final String url;

        private ServiceEndpoint(String name, String code, String url)
        {
            this.name = name;
            this.code = code;
            this.url = url;
        }
    }
}
