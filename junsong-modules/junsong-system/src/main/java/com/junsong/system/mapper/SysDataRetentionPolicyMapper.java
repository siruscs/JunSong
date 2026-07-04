package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysDataRetentionPolicy;

/**
 * R25数据留存策略 数据层
 */
public interface SysDataRetentionPolicyMapper
{
    /**
     * 查询全部启用的留存策略
     */
    List<SysDataRetentionPolicy> selectAllEnabledPolicies();

    /**
     * 按表名查询留存策略
     */
    SysDataRetentionPolicy selectByTableName(String tableName);
}
