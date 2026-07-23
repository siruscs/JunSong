package com.junsong.system.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActuatorServiceHealthProbeTest
{
    @Test
    void includesAllProductionServices()
        throws Exception
    {
        ActuatorServiceHealthProbe probe = new ActuatorServiceHealthProbe();
        Set<String> serviceCodes = endpointCodes(probe);

        assertEquals(Set.of(
            "junsong-gateway",
            "junsong-auth",
            "junsong-system",
            "junsong-gen",
            "junsong-job",
            "junsong-finance",
            "junsong-member",
            "junsong-workflow",
            "junsong-file",
            "junsong-open",
            "junsong-monitor"
        ), serviceCodes);
    }

    @Test
    void workflowAndOpenUseTheirProductionContainerActuatorUrls()
        throws Exception
    {
        ActuatorServiceHealthProbe probe = new ActuatorServiceHealthProbe();
        List<?> endpoints = endpoints(probe);

        assertTrue(endpoints.stream().anyMatch(endpoint -> hasEndpoint(endpoint,
            "junsong-workflow",
            "http://junsong-modules-workflow:9207/actuator/health")));
        assertTrue(endpoints.stream().anyMatch(endpoint -> hasEndpoint(endpoint,
            "junsong-open",
            "http://junsong-modules-open:9208/actuator/health")));
    }

    private static Set<String> endpointCodes(ActuatorServiceHealthProbe probe)
        throws Exception
    {
        List<?> endpoints = endpoints(probe);
        Set<String> codes = new java.util.HashSet<>();
        for (Object endpoint : endpoints)
        {
            codes.add(stringField(endpoint, "code"));
        }
        return codes;
    }

    private static List<?> endpoints(ActuatorServiceHealthProbe probe)
        throws Exception
    {
        Field endpoints = ActuatorServiceHealthProbe.class.getDeclaredField("endpoints");
        endpoints.setAccessible(true);
        return (List<?>) endpoints.get(probe);
    }

    private static boolean hasEndpoint(Object endpoint, String code, String url)
    {
        try
        {
            return code.equals(stringField(endpoint, "code")) && url.equals(stringField(endpoint, "url"));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static String stringField(Object target, String name)
        throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }
}
