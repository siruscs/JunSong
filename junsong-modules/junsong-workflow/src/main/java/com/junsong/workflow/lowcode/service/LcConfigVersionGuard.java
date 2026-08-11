package com.junsong.workflow.lowcode.service;

/**
 * 配置草稿来源版本保护规则。旧客户端未携带来源版本时保持兼容，
 * 新客户端携带来源版本后禁止覆盖更新的已发布配置。
 */
public final class LcConfigVersionGuard
{
    private LcConfigVersionGuard() { }

    public static void requireCompatible(Integer sourceVersion, Integer currentPublishedVersion)
    {
        if (sourceVersion != null && !sourceVersion.equals(currentPublishedVersion))
        {
            throw new IllegalStateException("配置草稿基于旧版本，请重新读取最新配置后再保存");
        }
    }
}
