package com.junsong.open.controller;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.open.domain.OpenApiLog;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenApiLogService;
import com.junsong.open.service.IOpenAppSecretService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenInternalControllerTest
{
    private OpenInternalController controller;
    private FakeOpenAppSecretService secretService;
    private FakeOpenApiLogService logService;

    private static final String CONFIGURED_SECRET = "test-inner-secret-2026";

    @BeforeEach
    void setUp() throws Exception
    {
        secretService = new FakeOpenAppSecretService();
        logService = new FakeOpenApiLogService();
        controller = new OpenInternalController();
        setField(controller, "openAppSecretService", secretService);
        setField(controller, "openApiLogService", logService);
        setField(controller, "innerToken", CONFIGURED_SECRET);
    }

    @Test
    @DisplayName("缺少 X-Inner-Token 应返回 401 且不查询 Secret")
    void missingInnerToken_shouldReturn401()
    {
        AjaxResult result = controller.getByAppKey("test-key", null);

        assertEquals(401, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 0);
    }

    @Test
    @DisplayName("错误的 X-Inner-Token 应返回 401 且不查询 Secret")
    void wrongInnerToken_shouldReturn401()
    {
        AjaxResult result = controller.getByAppKey("test-key", "wrong-secret");

        assertEquals(401, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 0);
    }

    @Test
    @DisplayName("open.internal.secret 为空时应 fail closed 返回 500")
    void emptyConfiguredSecret_shouldFailClosed() throws Exception
    {
        setField(controller, "innerToken", "");

        AjaxResult result = controller.getByAppKey("test-key", null);

        assertEquals(500, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 0);
    }

    @Test
    @DisplayName("open.internal.secret 为空时即使请求带 token 也应 fail closed")
    void emptyConfiguredSecret_withToken_shouldFailClosed() throws Exception
    {
        setField(controller, "innerToken", "");

        AjaxResult result = controller.getByAppKey("test-key", "some-token");

        assertEquals(500, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 0);
    }

    @Test
    @DisplayName("正确 X-Inner-Token 且 AppKey 有效应返回 200 和 appSecret")
    void correctInnerToken_validAppKey_shouldReturn200()
    {
        OpenAppSecret secret = new OpenAppSecret();
        secret.setAppKey("test-key");
        secret.setAppSecret("the-actual-secret");
        secret.setAppId(1L);
        secret.setTenantId(100L);
        secret.setKeyType("production");
        secret.setStatus("0");
        secret.setDailyQuota(1000);
        secretService.secrets.put("test-key", secret);

        AjaxResult result = controller.getByAppKey("test-key", CONFIGURED_SECRET);

        assertEquals(200, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 1);
    }

    @Test
    @DisplayName("正确 X-Inner-Token 但 AppKey 不存在应返回 error")
    void correctInnerToken_appKeyNotFound_shouldReturnError()
    {
        secretService.secrets.clear();

        AjaxResult result = controller.getByAppKey("missing-key", CONFIGURED_SECRET);

        assertEquals(500, result.get("code"));
        assertTrue(secretService.selectByAppKeyCallCount == 1);
    }

    @Test
    @DisplayName("logAccess 缺少 X-Inner-Token 应返回 401 且不写日志")
    void logAccess_missingToken_shouldReturn401()
    {
        AjaxResult result = controller.logAccess(new OpenApiLog(), null);

        assertEquals(401, result.get("code"));
        assertTrue(logService.insertCallCount == 0);
    }

    @Test
    @DisplayName("logAccess 错误 X-Inner-Token 应返回 401 且不写日志")
    void logAccess_wrongToken_shouldReturn401()
    {
        AjaxResult result = controller.logAccess(new OpenApiLog(), "wrong-secret");

        assertEquals(401, result.get("code"));
        assertTrue(logService.insertCallCount == 0);
    }

    @Test
    @DisplayName("logAccess 正确 X-Inner-Token 应返回 200 且写日志")
    void logAccess_correctToken_shouldReturn200()
    {
        OpenApiLog logEntry = new OpenApiLog();

        AjaxResult result = controller.logAccess(logEntry, CONFIGURED_SECRET);

        assertEquals(200, result.get("code"));
        assertTrue(logService.insertCallCount == 1);
        assertTrue(logService.logs.contains(logEntry));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    static class FakeOpenAppSecretService implements IOpenAppSecretService
    {
        final Map<String, OpenAppSecret> secrets = new ConcurrentHashMap<>();
        int selectByAppKeyCallCount = 0;

        @Override
        public List<OpenAppSecret> selectOpenAppSecretList(OpenAppSecret openAppSecret)
        {
            return new ArrayList<>(secrets.values());
        }

        @Override
        public List<OpenAppSecret> selectKeysByAppId(Long appId)
        {
            return new ArrayList<>();
        }

        @Override
        public OpenAppSecret selectByAppKey(String appKey)
        {
            selectByAppKeyCallCount++;
            return secrets.get(appKey);
        }

        @Override
        public void generateTestKey(com.junsong.open.domain.OpenApp app)
        {
        }

        @Override
        public void generateProductionKey(com.junsong.open.domain.OpenApp app)
        {
        }

        @Override
        public int changeStatus(OpenAppSecret openAppSecret)
        {
            return 0;
        }

        @Override
        public String generateAppKey()
        {
            return "";
        }

        @Override
        public String generateAppSecret()
        {
            return "";
        }
    }

    static class FakeOpenApiLogService implements IOpenApiLogService
    {
        final List<OpenApiLog> logs = new ArrayList<>();
        int insertCallCount = 0;

        @Override
        public List<OpenApiLog> selectOpenApiLogList(OpenApiLog openApiLog)
        {
            return logs;
        }

        @Override
        public int insertOpenApiLog(OpenApiLog openApiLog)
        {
            insertCallCount++;
            logs.add(openApiLog);
            return 1;
        }
    }
}
