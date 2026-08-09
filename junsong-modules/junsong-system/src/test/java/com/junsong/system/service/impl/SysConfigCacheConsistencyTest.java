package com.junsong.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 参数表缓存一致性 契约测试
 *
 * <p>覆盖：BUG1 参数表 sys_config 缓存经常"丢失/过期"，导致即使 DB 设为 FALSE，
 * Gateway 侧读到 null 后回退为"默认要验证码"。
 *
 * <p>期望行为（源码契约断言）：
 * <ol>
 *   <li>resetConfigCache 不得先 {@code clearConfigCache}（全量 SCAN 删除）再
 *       {@code loadingConfigCache}（重新写回）。这两步之间存在空窗口，
 *       任何并发读都会拿到 null → 错误退回到默认行为。</li>
 *   <li>应改成"覆盖式"或"写完再 prune"的模式，例如：
 *       {@code loadingConfigCache()} 先覆盖写所有已知 key，
 *       再删除 DB 里不存在的旧 key（或采用双版本前缀 + CAS，但这是小步修复）。</li>
 *   <li>{@code selectConfigByKey} 必须始终在 Redis 读 null 时，查库并回写缓存，
 *       保证下一次读无需再穿透。</li>
 * </ol>
 */
class SysConfigCacheConsistencyTest
{
    /** 测试默认在 junsong-system 模块 cwd 下运行，使用相对路径。 */
    private static final Path SYS_CONFIG_IMPL = Path
            .of("src/main/java/com/junsong/system/service/impl/SysConfigServiceImpl.java");

    private static String readSource() throws Exception
    {
        if (Files.exists(SYS_CONFIG_IMPL))
        {
            return Files.readString(SYS_CONFIG_IMPL);
        }
        // 兼容在仓库根目录执行的场景（例如 mvn -pl 失败时）
        return Files.readString(Path.of("junsong-modules", "junsong-system").resolve(SYS_CONFIG_IMPL));
    }

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
    void resetConfigCacheMustNotClearThenLoadWindow() throws Exception
    {
        String source = readSource();
        String resetBody = extractMethodBody(source, "resetConfigCache");
        // 契约：resetConfigCache 方法体里不得同时出现 clearConfigCache + loadingConfigCache，
        // 尤其不能是"先清再写"顺序。
        int clearIdx = resetBody.indexOf("clearConfigCache()");
        int loadIdx = resetBody.indexOf("loadingConfigCache()");
        // 要么移除 clearConfigCache（覆盖式写），要么先 load 后 prune，
        // 但不能先清后写。
        assertFalse(clearIdx != -1 && loadIdx != -1 && clearIdx < loadIdx,
                "BUG1: resetConfigCache 不得先 clearConfigCache 再 loadingConfigCache，"
                        + "中间空窗口会导致所有 sys_config 读穿透到 null；"
                        + "请改为覆盖式：先 loadingConfigCache 覆盖写入，再用 SCAN 清理 DB 不存在的 key");
    }

    @Test
    void selectConfigByKeyMustWriteBackToCacheAfterDbFallback() throws Exception
    {
        String source = readSource();
        String method = extractMethodBody(source, "selectConfigByKey");
        assertTrue(method.contains("configMapper.selectConfig(config)"),
                "selectConfigByKey 必须包含 DB fallback 查询");
        assertTrue(method.contains("setCacheObject") && method.contains("getCacheKey(configKey)"),
                "selectConfigByKey 在 DB 回查成功后必须 setCacheObject(getCacheKey(configKey), value) 回写缓存，"
                        + "避免下一次继续穿透");
    }
}
