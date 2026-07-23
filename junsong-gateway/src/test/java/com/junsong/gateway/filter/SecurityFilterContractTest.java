package com.junsong.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecurityFilterContractTest {
    @Test
    void businessRateLimitUsesThreeDimensionsAndRetryHint() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/junsong/gateway/filter/BusinessRateLimitFilter.java"));
        assertTrue(source.contains("gateway:business-rate:"));
        assertTrue(source.contains("ip:"));
        assertTrue(source.contains("user:"));
        assertTrue(source.contains("path:"));
        assertTrue(source.contains("Retry-After"));
    }

    @Test
    void antiCrawlerDoesNotUseRefererAsAuthorization() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/junsong/gateway/filter/AntiCrawlerFilter.java"));
        assertTrue(source.contains("python-requests"));
        assertTrue(source.contains("octoparse"));
        assertTrue(source.contains("/auth/"));
        assertTrue(!source.contains("getFirst(\"Referer\")"));
    }

    @Test
    void auditFilterDoesNotReadTokenOrRequestBody() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/junsong/gateway/filter/SecurityAuditFilter.java"));
        assertTrue(source.contains("security_audit"));
        assertTrue(source.contains("DETAILS_USER_ID"));
        assertTrue(!source.contains("Authorization"));
        assertTrue(!source.contains("getBody()"));
    }
}
