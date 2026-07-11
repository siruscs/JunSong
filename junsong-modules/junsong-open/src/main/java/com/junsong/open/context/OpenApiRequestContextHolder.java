package com.junsong.open.context;

/**
 * 开放平台请求上下文线程持有器
 *
 * @author junsong
 */
public final class OpenApiRequestContextHolder
{
    private static final ThreadLocal<OpenApiRequestContext> HOLDER = new ThreadLocal<>();

    private OpenApiRequestContextHolder() {}

    public static void set(OpenApiRequestContext context) { HOLDER.set(context); }
    public static OpenApiRequestContext get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
