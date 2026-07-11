package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 读取 sys_health_rule_config 配置阈值（finance 模块本地直读同库）。
 * 表不存在或规则缺失时返回默认值，不抛异常。
 */
@Component
public class HealthRuleConfigReader {
    private static final Logger log = LoggerFactory.getLogger(HealthRuleConfigReader.class);
    private final JdbcTemplate jdbcTemplate;

    public HealthRuleConfigReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal getThreshold(String ruleCode, BigDecimal defaultValue) {
        try {
            BigDecimal val = jdbcTemplate.queryForObject(
                "SELECT threshold_value FROM sys_health_rule_config WHERE rule_code = ? AND enabled = '1'",
                BigDecimal.class, ruleCode);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.warn("读取规则阈值失败 ruleCode={}, 使用默认值 {}", ruleCode, defaultValue);
            return defaultValue;
        }
    }

    public String getSeverity(String ruleCode, String defaultValue) {
        try {
            String val = jdbcTemplate.queryForObject(
                "SELECT severity FROM sys_health_rule_config WHERE rule_code = ? AND enabled = '1'",
                String.class, ruleCode);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.warn("读取规则严重级别失败 ruleCode={}", ruleCode);
            return defaultValue;
        }
    }

    public String getSuggestion(String ruleCode, String defaultValue) {
        try {
            String val = jdbcTemplate.queryForObject(
                "SELECT suggestion FROM sys_health_rule_config WHERE rule_code = ? AND enabled = '1'",
                String.class, ruleCode);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.warn("读取规则建议失败 ruleCode={}", ruleCode);
            return defaultValue;
        }
    }

    public boolean isEnabled(String ruleCode, boolean defaultValue) {
        try {
            String val = jdbcTemplate.queryForObject(
                "SELECT enabled FROM sys_health_rule_config WHERE rule_code = ?",
                String.class, ruleCode);
            return val != null ? "1".equals(val) : defaultValue;
        } catch (Exception e) {
            log.warn("读取规则启停状态失败 ruleCode={}", ruleCode);
            return defaultValue;
        }
    }
}
