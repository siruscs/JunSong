package com.junsong.common.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文
 * 基于 TransmittableThreadLocal 存储当前请求的租户ID
 * 使用 TTL 保证异步/线程池场景下租户上下文正确透传
 *
 * @author junsong
 */
public class TenantContext
{
    private static final ThreadLocal<Long> TENANT_ID = new TransmittableThreadLocal<>();

    private static final ThreadLocal<Boolean> IGNORE = new TransmittableThreadLocal<>();

    /**
     * 默认租户ID（单租户模式）
     */
    public static final Long DEFAULT_TENANT_ID = 1L;

    /**
     * 获取当前租户ID
     */
    public static Long getTenantId()
    {
        Long tenantId = TENANT_ID.get();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    /**
     * 设置当前租户ID
     */
    public static void setTenantId(Long tenantId)
    {
        TENANT_ID.set(tenantId);
    }

    /**
     * 是否忽略租户隔离（用于系统级查询，如租户表自身）
     */
    public static boolean isIgnore()
    {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /**
     * 设置是否忽略租户隔离
     */
    public static void setIgnore(boolean ignore)
    {
        IGNORE.set(ignore);
    }

    /**
     * 清除上下文
     */
    public static void clear()
    {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
