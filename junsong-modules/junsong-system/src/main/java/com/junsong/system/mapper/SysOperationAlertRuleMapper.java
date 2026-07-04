package com.junsong.system.mapper;

import com.junsong.system.domain.SysOperationAlertRule;

/**
 * R25操作告警规则 数据层
 */
public interface SysOperationAlertRuleMapper
{
    /**
     * 按规则键查询告警规则
     */
    SysOperationAlertRule selectByRuleKey(String ruleKey);
}
