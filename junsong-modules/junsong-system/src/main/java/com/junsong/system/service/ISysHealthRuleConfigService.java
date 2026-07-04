package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysHealthRuleConfig;

/**
 * 自检规则配置 服务层
 */
public interface ISysHealthRuleConfigService {

    List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig query);

    SysHealthRuleConfig selectById(Long ruleId);

    int updateHealthRule(SysHealthRuleConfig config);

    int toggleEnabled(Long ruleId, String enabled);
}
