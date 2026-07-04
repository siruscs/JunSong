package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysHealthRuleConfig;

/**
 * 自检规则配置 数据层
 */
public interface SysHealthRuleConfigMapper {

    List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig query);

    SysHealthRuleConfig selectById(Long ruleId);

    SysHealthRuleConfig selectByCode(String ruleCode);

    int updateHealthRule(SysHealthRuleConfig config);
}
