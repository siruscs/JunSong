package com.junsong.system.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysHealthRuleConfig;
import com.junsong.system.mapper.SysHealthRuleConfigMapper;
import com.junsong.system.service.ISysHealthRuleConfigService;
import com.junsong.system.service.ISysOperationAuditService;

/**
 * 自检规则配置 服务实现
 */
@Service
public class SysHealthRuleConfigServiceImpl implements ISysHealthRuleConfigService {
    private static final Logger log = LoggerFactory.getLogger(SysHealthRuleConfigServiceImpl.class);
    private static final Set<String> VALID_COMPARE_OPS = Set.of("GT", "GTE", "LT", "LTE", "EQ");
    private static final Set<String> VALID_SEVERITIES = Set.of("HIGH", "MEDIUM", "LOW");

    private final SysHealthRuleConfigMapper mapper;
    private final ISysOperationAuditService auditService;

    public SysHealthRuleConfigServiceImpl(SysHealthRuleConfigMapper mapper, ISysOperationAuditService auditService) {
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Override
    public List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig query) {
        return mapper.selectHealthRuleList(query);
    }

    @Override
    public SysHealthRuleConfig selectById(Long ruleId) {
        return mapper.selectById(ruleId);
    }

    @Override
    public int updateHealthRule(SysHealthRuleConfig config) {
        validate(config);
        // ruleCode 不可变更
        SysHealthRuleConfig existing = mapper.selectById(config.getRuleId());
        if (existing == null) {
            throw new IllegalArgumentException("规则不存在: " + config.getRuleId());
        }
        if (config.getRuleCode() != null && !config.getRuleCode().equals(existing.getRuleCode())) {
            throw new IllegalArgumentException("规则编码不可修改");
        }
        int rows = mapper.updateHealthRule(config);
        // R25 审计：记录更新前后快照
        try {
            SysHealthRuleConfig after = mapper.selectById(config.getRuleId());
            auditService.recordSnapshot("HEALTH_RULE", String.valueOf(config.getRuleId()), "UPDATE", "HIGH", existing, after);
        } catch (Exception e) {
            log.warn("R25 审计记录失败 HEALTH_RULE UPDATE ruleId={}: {}", config.getRuleId(), e.getMessage());
        }
        return rows;
    }

    @Override
    public int toggleEnabled(Long ruleId, String enabled) {
        if (!"1".equals(enabled) && !"0".equals(enabled)) {
            throw new IllegalArgumentException("enabled 只能为 1 或 0");
        }
        SysHealthRuleConfig before = mapper.selectById(ruleId);
        SysHealthRuleConfig update = new SysHealthRuleConfig();
        update.setRuleId(ruleId);
        update.setEnabled(enabled);
        int rows = mapper.updateHealthRule(update);
        // R25 审计：记录启停前后快照
        try {
            SysHealthRuleConfig after = mapper.selectById(ruleId);
            auditService.recordSnapshot("HEALTH_RULE", String.valueOf(ruleId), "TOGGLE", "MEDIUM", before, after);
        } catch (Exception e) {
            log.warn("R25 审计记录失败 HEALTH_RULE TOGGLE ruleId={}: {}", ruleId, e.getMessage());
        }
        return rows;
    }

    static void validate(SysHealthRuleConfig config) {
        if (config.getCompareOp() != null && !VALID_COMPARE_OPS.contains(config.getCompareOp())) {
            throw new IllegalArgumentException("无效比较符: " + config.getCompareOp());
        }
        if (config.getSeverity() != null && !VALID_SEVERITIES.contains(config.getSeverity())) {
            throw new IllegalArgumentException("无效严重级别: " + config.getSeverity());
        }
        // R10-FIX-D: thresholdValue 必填。前端为完整编辑（非局部更新），
        // 阈值为空会导致评分逻辑无法工作，因此强制非空。
        if (config.getThresholdValue() == null) {
            throw new IllegalArgumentException("阈值不能为空");
        }
    }

    /** 供其他模块读取配置阈值（同库直读），失败返回 defaultValue */
    public BigDecimal getThreshold(String ruleCode, BigDecimal defaultValue) {
        try {
            SysHealthRuleConfig rule = mapper.selectByCode(ruleCode);
            if (rule != null && "1".equals(rule.getEnabled())) {
                return rule.getThresholdValue();
            }
        } catch (Exception e) {
            log.warn("读取规则配置失败 ruleCode={}, 使用默认值", ruleCode, e);
        }
        return defaultValue;
    }

    public String getSeverity(String ruleCode, String defaultValue) {
        try {
            SysHealthRuleConfig rule = mapper.selectByCode(ruleCode);
            if (rule != null && "1".equals(rule.getEnabled())) {
                return rule.getSeverity();
            }
        } catch (Exception e) {
            log.warn("读取规则配置失败 ruleCode={}", ruleCode, e);
        }
        return defaultValue;
    }

    public String getSuggestion(String ruleCode, String defaultValue) {
        try {
            SysHealthRuleConfig rule = mapper.selectByCode(ruleCode);
            if (rule != null && "1".equals(rule.getEnabled())) {
                return rule.getSuggestion();
            }
        } catch (Exception e) {
            log.warn("读取规则配置失败 ruleCode={}", ruleCode, e);
        }
        return defaultValue;
    }

    public boolean isEnabled(String ruleCode, boolean defaultValue) {
        try {
            SysHealthRuleConfig rule = mapper.selectByCode(ruleCode);
            if (rule != null) {
                return "1".equals(rule.getEnabled());
            }
        } catch (Exception e) {
            log.warn("读取规则配置失败 ruleCode={}", ruleCode, e);
        }
        return defaultValue;
    }
}
