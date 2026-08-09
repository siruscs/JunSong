package com.junsong.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 登录验证码 + 参数表缓存 契约测试
 *
 * <p>覆盖两个生产 BUG 的行为期望：
 * <ol>
 *   <li>参数表 sys.account.captchaEnabled 缓存丢失 (null) 时，
 *       ValidateCodeFilter 必须走"查库兜底"，不能直接视为需要验证码。</li>
 *   <li>图形验证码一次校验成功后，必须保留一段复用窗口期（同 uuid + 同 code），
 *       以支持"多部门用户首次登录 → 弹出部门选择 → 再次带 deptId 调用 /auth/login"
 *       的二次登录不会被"验证码已失效"卡死形成死循环。</li>
 *   <li>窗口期内复用必须强制同 code 值；不同 code 仍应拒绝，防止暴力破解。</li>
 *   <li>删除 key 的操作必须发生在"比对成功之后"；不能先删再比，
 *       否则用户打错一次验证码立刻就把 key 干掉，连再输正确的机会都没有。</li>
 * </ol>
 *
 * <p>实现方式：采用"源码契约 + 关键调用顺序"断言，避免引入 MockRedis 后
 * 整个 Spring Gateway 上下文启动成本；以最小粒度对修复点做回归保护。
 */
class ValidateCodeFlowContractTest
{
    private static final Path VALIDATE_CODE_FILTER = Path
            .of("src/main/java/com/junsong/gateway/filter/ValidateCodeFilter.java");
    private static final Path VALIDATE_CODE_SERVICE_IMPL = Path
            .of("src/main/java/com/junsong/gateway/service/impl/ValidateCodeServiceImpl.java");

    /** 从给定源码中抽取指定方法名的方法体（粗略的大括号对对齐）。 */
    private static String extractMethodBody(String source, String methodName)
    {
        String anchor = " " + methodName + "(";
        int idx = source.indexOf(anchor);
        if (idx == -1)
        {
            idx = source.indexOf(methodName + "(");
        }
        if (idx == -1)
        {
            return "";
        }
        int open = source.indexOf('{', idx);
        if (open == -1)
        {
            return "";
        }
        int depth = 0;
        int i = open;
        for (; i < source.length(); i++)
        {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) break;
        }
        return source.substring(open, Math.min(i + 1, source.length()));
    }

    @Test
    void filterMustHaveRemoteFallbackWhenCaptchaEnabledCacheIsNull() throws Exception
    {
        String filter = Files.readString(VALIDATE_CODE_FILTER);
        // 契约：当 Redis 里 sys_config:sys.account.captchaEnabled 读到 null 时，
        // 必须调用与 createCaptcha 里等价的"远程兜底查库"，不能直接跳过。
        // 以关键特征调用 + 配置键做断言。
        assertTrue(filter.contains("sys.account.captchaEnabled"),
                "Filter 必须读取 sys.account.captchaEnabled 参数");
        assertTrue(
                filter.contains("isCaptchaDisabledFromRemote")
                        || filter.contains("captchaEnabledFallback")
                        || filter.contains("configKey/sys.account.captchaEnabled"),
                "BUG1: Redis 缓存丢失时 Filter 必须走查库兜底，不能直接走 captchaProperties 默认值；"
                        + "请引入与 createCaptcha 等价的 isCaptchaDisabledFromRemote / captchaEnabledFallback 调用");
    }

    @Test
    void createCaptchaAndFilterMustUseSameCacheFallbackLogic() throws Exception
    {
        String service = Files.readString(VALIDATE_CODE_SERVICE_IMPL);
        String filter = Files.readString(VALIDATE_CODE_FILTER);
        // 契约：createCaptcha 里已经有"Redis null → 远程 HTTP 查 system 模块"的兜底，
        // Filter 必须使用同一条规则；要么共用 helper，要么两处都包含兜底并回写缓存。
        assertTrue(service.contains("isCaptchaDisabledFromRemote"),
                "createCaptcha 必须保留原有的查库兜底");
        assertTrue(
                filter.contains("isCaptchaDisabledFromRemote")
                        || filter.contains("captchaEnabledFallback")
                        || filter.contains("captchaEnabledRemoteFallback"),
                "Filter 必须与 createCaptcha 使用一致的兜底逻辑（优先抽取共享方法）");
    }

    @Test
    void checkCaptchaMustAllowOneReuseWithinShortTtlForMultiDeptLogin() throws Exception
    {
        String service = Files.readString(VALIDATE_CODE_SERVICE_IMPL);
        // 契约：验证成功后不得立刻 deleteObject；应改为"保留一段复用窗口（如 5 分钟）
        // 仅允许相同 code 再通过一次"的一次性复用语义。
        // 典型实现：deleteObject 改成 setCacheObject(verifyKey, code, 5, MINUTES)
        // 或增加 CAPTCHA_CONSUMED_PREFIX + MINUTE_REUSE_WINDOW。
        assertFalse(service.matches("(?s)checkCaptcha[\\s\\S]*?redisService\\.deleteObject\\(verifyKey\\)"),
                "BUG2: checkCaptcha 不得在一次校验成功后立即删除 verifyKey，"
                        + "否则多部门用户二次带 deptId 登录命中 key 已删 → 死循环；"
                        + "请改成 5 分钟窗口内允许同 code 再通过一次的复用语义");
        assertTrue(
                service.contains("REUSE_WINDOW_MINUTES")
                        || service.contains("CAPTCHA_REUSE_TTL")
                        || service.contains("TimeUnit.MINUTES, 5")
                        || service.contains("setCacheObject(verifyKey, code,"),
                "应引入一次性复用窗口期（建议 5 分钟），setCacheObject(verifyKey, code, 5, MINUTES)");
    }

    @Test
    void checkCaptchaReuseWindowMustRejectDifferentCode() throws Exception
    {
        String service = Files.readString(VALIDATE_CODE_SERVICE_IMPL);
        String checkBlock = extractMethodBody(service, "checkCaptcha");
        // 契约：窗口内必须依旧做 code 值匹配，不能变成"只要 uuid 存在就过"，
        // 否则等同于把验证码降级为一次性 uuid，失去防刷意义。
        assertTrue(
                checkBlock.contains("!code.equalsIgnoreCase(captcha)"),
                "checkCaptcha 必须始终比较 code 大小写不敏感匹配，否则窗口内会引入暴力破解风险");
    }

    @Test
    void checkCaptchaDeletionOrReuseMustHappenAfterCodeCompareSuccess() throws Exception
    {
        String service = Files.readString(VALIDATE_CODE_SERVICE_IMPL);
        // 契约：任何对 verifyKey 的 delete / setCacheObject 操作必须发生在
        // code.equalsIgnoreCase(captcha) 之后；避免"先删再比对，失败直接 key 丢失"。
        String checkBlock = extractMethodBody(service, "checkCaptcha");
        int compareIdx = checkBlock.indexOf("equalsIgnoreCase(captcha)");
        int deleteIdx = checkBlock.indexOf("deleteObject(verifyKey)");
        int overwriteIdx = checkBlock.indexOf("setCacheObject(verifyKey, code,");
        int anySideEffect = Math.min(
                deleteIdx == -1 ? Integer.MAX_VALUE : deleteIdx,
                overwriteIdx == -1 ? Integer.MAX_VALUE : overwriteIdx);
        assertTrue(compareIdx >= 0, "checkCaptcha 内必须包含 equalsIgnoreCase(captcha) 比对");
        assertTrue(anySideEffect != Integer.MAX_VALUE,
                "checkCaptcha 内必须包含副作用：删除 key 或设置复用窗口");
        assertTrue(compareIdx < anySideEffect,
                "副作用（删除 / 重置 TTL）必须在 equalsIgnoreCase 比较成功之后执行，"
                        + "顺序反了会导致'第一次输错就连正确的也校验不到'");
    }
}
