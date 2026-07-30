package com.junsong.common.core.idempotency;

/**
 * 幂等结果存储工具。
 *
 * 业务方在 REFERENCE 模式下使用此工具设置业务资源引用。
 * 典型用法：在 Service 中调用 store.record("fin_sale_record", saleId)。
 *
 * @author junsong
 */
public class IdempotencyResultStore {

    private static final ThreadLocal<ResourceRef> CURRENT = new ThreadLocal<>();
    /** 当前线程的幂等键（由 AOP 切面设置，业务层读取用于填充业务表 idempotency_key 列实现 DB 兜底） */
    private static final ThreadLocal<String> CURRENT_KEY = new ThreadLocal<>();

    /**
     * 记录业务资源引用。
     * 必须在 @Idempotent 注解的方法执行体内调用。
     */
    public static void record(String resourceType, Long resourceId) {
        CURRENT.set(new ResourceRef(resourceType, resourceId != null ? String.valueOf(resourceId) : null, null));
    }

    /**
     * 记录业务资源引用（字符串ID）。
     */
    public static void record(String resourceType, String resourceId) {
        CURRENT.set(new ResourceRef(resourceType, resourceId, null));
    }

    /**
     * 记录业务资源引用 + 结果摘要。
     */
    public static void record(String resourceType, Long resourceId, String resultSummary) {
        CURRENT.set(new ResourceRef(resourceType, resourceId != null ? String.valueOf(resourceId) : null, resultSummary));
    }

    /**
     * 获取并清除当前线程的资源引用（由 AOP 切面调用）。
     */
    public static ResourceRef getAndClear() {
        ResourceRef ref = CURRENT.get();
        CURRENT.remove();
        return ref;
    }

    /**
     * 设置当前线程的幂等键（由 AOP 切面调用）。
     * 业务层可通过 currentKey() 读取，用于填充业务表 idempotency_key 列实现 DB 唯一索引兜底。
     */
    public static void currentKey(String key) {
        CURRENT_KEY.set(key);
    }

    /**
     * 获取当前线程的幂等键（业务层调用）。
     * 必须在 @Idempotent 注解的方法执行体内调用，否则返回 null。
     */
    public static String currentKey() {
        return CURRENT_KEY.get();
    }

    /**
     * 清除当前线程的幂等键（由 AOP 切面在 finally 中调用）。
     */
    public static void clearKey() {
        CURRENT_KEY.remove();
    }

    public static class ResourceRef {
        private final String resourceType;
        private final String resourceId;
        private final String resultSummary;

        public ResourceRef(String resourceType, String resourceId, String resultSummary) {
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.resultSummary = resultSummary;
        }

        public String getResourceType() { return resourceType; }
        public String getResourceId() { return resourceId; }
        public String getResultSummary() { return resultSummary; }
    }
}
