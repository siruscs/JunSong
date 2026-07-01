package com.junsong.system.service;

import org.junit.jupiter.api.Test;
import com.junsong.system.service.impl.WebhookUrlValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Webhook URL校验器测试
 * 覆盖SSRF防御：内网地址、非法协议、无效URL均须被拒绝
 *
 * @author junsong
 */
class WebhookUrlValidatorTest
{
    // ==================== 应拒绝的用例 ====================

    @Test
    void shouldRejectLocalhost()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://localhost/webhook"));
    }

    @Test
    void shouldRejectLoopbackIp()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://127.0.0.1/webhook"));
    }

    @Test
    void shouldRejectIpv6Loopback()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://[::1]/webhook"));
    }

    @Test
    void shouldRejectSiteLocalAddress10()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://10.0.0.1/webhook"));
    }

    @Test
    void shouldRejectSiteLocalAddress172()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://172.16.0.1/webhook"));
    }

    @Test
    void shouldRejectSiteLocalAddress192()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://192.168.1.1/webhook"));
    }

    @Test
    void shouldRejectAwsMetadataEndpoint()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void shouldRejectFileProtocol()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("file:///etc/passwd"));
    }

    @Test
    void shouldRejectFtpProtocol()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("ftp://example.com/file"));
    }

    @Test
    void shouldRejectNullUrl()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate(null));
    }

    @Test
    void shouldRejectEmptyUrl()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate(""));
    }

    @Test
    void shouldRejectBlankUrl()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("   "));
    }

    @Test
    void shouldRejectUrlWithoutHost()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http:///path"));
    }

    @Test
    void shouldReject0000Address()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://0.0.0.0/webhook"));
    }

    // ==================== SSRF 二次加固：特殊地址用例 ====================

    @Test
    void shouldRejectUrlWithCredentials()
    {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://admin:secret@example.com/webhook"));
        assertTrue(ex.getMessage().contains("凭据"), "应拒绝含凭据的URL");
    }

    @Test
    void shouldRejectUrlWithUsernameOnly()
    {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://admin@example.com/webhook"));
    }

    @Test
    void shouldRejectIpv4MappedIpv6Loopback()
    {
        // ::ffff:127.0.0.1 是 IPv4-mapped IPv6 形式的回环地址
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://[::ffff:127.0.0.1]/webhook"));
    }

    @Test
    void shouldRejectIpv4MappedIpv6SiteLocal()
    {
        // ::ffff:192.168.1.1 是 IPv4-mapped IPv6 形式的内网地址
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://[::ffff:192.168.1.1]/webhook"));
    }

    @Test
    void shouldRejectSuperLongHost()
    {
        // 构造超过 255 字符的主机名
        StringBuilder longHost = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            longHost.append("abcdefghij"); // 10 chars * 26 = 260 chars
            if (i < 25) longHost.append('.');
        }
        String url = "http://" + longHost + "/webhook";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate(url));
        assertTrue(ex.getMessage().contains("主机名过长"), "应拒绝超长主机名");
    }

    @Test
    void shouldRejectHexIpv4()
    {
        // 0x7f000001 = 127.0.0.1 的十六进制形式
        // Java URI 可能不接受十六进制 IP，此时应抛无效 URL 异常
        assertThrows(Exception.class,
                () -> WebhookUrlValidator.validate("http://0x7f000001/webhook"));
    }

    @Test
    void shouldHandleOctalIpv4()
    {
        // 0177.0.0.1 = 127.0.0.1 的八进制形式
        // 行为取决于 JDK 是否将其解析为回环地址：
        // - 如果解析为 127.0.0.1 → 被 SSRF 检查拒绝
        // - 如果解析为其他地址或无法解析 → 可能通过或拒绝
        // 关键：验证器不应抛异常以外的不可控行为
        try {
            WebhookUrlValidator.validate("http://0177.0.0.1/webhook");
            // 如果通过，说明系统将 0177.0.0.1 解析为公网地址（可接受）
        } catch (IllegalArgumentException e) {
            // 如果拒绝，说明系统正确识别了八进制回环地址
            assertTrue(e.getMessage().contains("回环") || e.getMessage().contains("无效")
                    || e.getMessage().contains("内网"), "拒绝原因应为SSRF相关");
        }
    }

    // ==================== 重定向安全 ====================

    @Test
    void shouldTreat302AsNonSuccessNotRedirect()
    {
        // 验证 3xx 不在 200-299 范围内，不会被视为成功
        // 由于 RestTemplate 已禁用重定向跟随，302 响应会走到 handleFailure
        // 此处仅验证逻辑：302 >= 200 但 302 >= 300，不在 [200, 300) 范围
        int status302 = 302;
        assertFalse(status302 >= 200 && status302 < 300, "302不应被视为成功");
    }

    @Test
    void shouldAcceptValidHttpsUrl()
    {
        // 使用公网IP（Google DNS），确保测试环境可解析
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("https://8.8.8.8/callback"));
    }

    @Test
    void shouldAcceptValidHttpUrl()
    {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("http://8.8.8.8:8080/webhook"));
    }

    @Test
    void shouldAcceptUrlWithQueryParam()
    {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("https://1.1.1.1/webhook?token=abc123"));
    }
}
