package com.junsong.system.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysHealthRuleConfig;
import com.junsong.system.domain.SysOperationAuditSnapshot;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;
import com.junsong.system.mapper.SysHealthRuleConfigMapper;
import com.junsong.system.service.ISysOperationAuditService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自检规则配置服务测试。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysHealthRuleConfigServiceImplTest
{
    private FakeHealthRuleConfigMapper mapper;
    private NoOpAuditService auditService;
    private SysHealthRuleConfigServiceImpl service;

    @BeforeEach
    void setUp()
    {
        mapper = new FakeHealthRuleConfigMapper();
        auditService = new NoOpAuditService();
        service = new SysHealthRuleConfigServiceImpl(mapper, auditService);
    }

    @Test
    void invalidCompareOpIsRejected()
    {
        SysHealthRuleConfig config = new SysHealthRuleConfig();
        config.setRuleId(1L);
        config.setCompareOp("INVALID");
        config.setThresholdValue(BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class, () -> service.updateHealthRule(config));
    }

    @Test
    void invalidSeverityIsRejected()
    {
        SysHealthRuleConfig config = new SysHealthRuleConfig();
        config.setRuleId(1L);
        config.setSeverity("CRITICAL");
        config.setThresholdValue(BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class, () -> service.updateHealthRule(config));
    }

    @Test
    void thresholdValueNullIsRejected()
    {
        // R10-FIX-D: thresholdValue 必填，即使 ruleId != null（完整编辑语义）
        SysHealthRuleConfig existing = buildRule(1L, "FIN_REVIEW_CLOSE_HOURS", "72", "GT", "HIGH", "1");
        mapper.rules.add(existing);

        SysHealthRuleConfig update = new SysHealthRuleConfig();
        update.setRuleId(1L);
        update.setThresholdValue(null);

        assertThrows(IllegalArgumentException.class, () -> service.updateHealthRule(update),
            "thresholdValue 为空时必须抛出 IllegalArgumentException");
    }

    @Test
    void ruleCodeCannotBeChanged()
    {
        SysHealthRuleConfig existing = buildRule(1L, "FIN_REVIEW_CLOSE_HOURS", "72", "GT", "HIGH", "1");
        mapper.rules.add(existing);

        SysHealthRuleConfig update = new SysHealthRuleConfig();
        update.setRuleId(1L);
        update.setRuleCode("DIFFERENT_CODE");
        update.setThresholdValue(BigDecimal.valueOf(48));

        assertThrows(IllegalArgumentException.class, () -> service.updateHealthRule(update));
    }

    @Test
    void toggleEnabledUpdatesOnlyEnabledFlag()
    {
        SysHealthRuleConfig existing = buildRule(1L, "FIN_REVIEW_CLOSE_HOURS", "72", "GT", "HIGH", "1");
        mapper.rules.add(existing);

        int rows = service.toggleEnabled(1L, "0");

        assertEquals(1, rows);
        assertEquals(1, mapper.updated.size());
        assertEquals("0", mapper.updated.get(0).getEnabled());
        assertEquals(1L, mapper.updated.get(0).getRuleId());
    }

    @Test
    void toggleEnabledRejectsInvalidValue()
    {
        assertThrows(IllegalArgumentException.class, () -> service.toggleEnabled(1L, "2"));
    }

    @Test
    void getThresholdReturnsConfiguredValue()
    {
        SysHealthRuleConfig rule = buildRule(1L, "FIN_REVIEW_CLOSE_HOURS", "72", "GT", "HIGH", "1");
        mapper.rules.add(rule);

        BigDecimal result = service.getThreshold("FIN_REVIEW_CLOSE_HOURS", BigDecimal.valueOf(99));

        assertEquals(0, new BigDecimal("72").compareTo(result));
    }

    @Test
    void getThresholdReturnsDefaultWhenDisabled()
    {
        SysHealthRuleConfig rule = buildRule(1L, "FIN_REVIEW_CLOSE_HOURS", "72", "GT", "HIGH", "0");
        mapper.rules.add(rule);

        BigDecimal result = service.getThreshold("FIN_REVIEW_CLOSE_HOURS", BigDecimal.valueOf(99));

        assertEquals(0, BigDecimal.valueOf(99).compareTo(result));
    }

    @Test
    void getThresholdReturnsDefaultWhenMissing()
    {
        BigDecimal result = service.getThreshold("NONEXISTENT", BigDecimal.valueOf(42));
        assertEquals(0, BigDecimal.valueOf(42).compareTo(result));
    }

    @Test
    void isEnabledReturnsFalseWhenDisabled()
    {
        SysHealthRuleConfig rule = buildRule(1L, "TEST_RULE", "10", "GT", "LOW", "0");
        mapper.rules.add(rule);

        assertFalse(service.isEnabled("TEST_RULE", true));
    }

    private SysHealthRuleConfig buildRule(Long id, String code, String threshold, String op, String severity, String enabled)
    {
        SysHealthRuleConfig r = new SysHealthRuleConfig();
        r.setRuleId(id);
        r.setRuleCode(code);
        r.setThresholdValue(new BigDecimal(threshold));
        r.setCompareOp(op);
        r.setSeverity(severity);
        r.setEnabled(enabled);
        return r;
    }

    static class FakeHealthRuleConfigMapper implements SysHealthRuleConfigMapper
    {
        List<SysHealthRuleConfig> rules = new ArrayList<>();
        List<SysHealthRuleConfig> updated = new ArrayList<>();

        @Override
        public List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig query) { return rules; }

        @Override
        public SysHealthRuleConfig selectById(Long ruleId)
        {
            return rules.stream().filter(r -> r.getRuleId().equals(ruleId)).findFirst().orElse(null);
        }

        @Override
        public SysHealthRuleConfig selectByCode(String ruleCode)
        {
            return rules.stream().filter(r -> r.getRuleCode().equals(ruleCode)).findFirst().orElse(null);
        }

        @Override
        public int updateHealthRule(SysHealthRuleConfig config)
        {
            updated.add(config);
            return 1;
        }
    }

    /**
     * R25 审计服务 no-op fake：构造签名变化后需要注入，但不参与断言。
     */
    static class NoOpAuditService implements ISysOperationAuditService
    {
        @Override
        public void recordSnapshot(String bizType, String bizId, String operation, String riskLevel, Object before, Object after)
        {
            // no-op
        }

        @Override
        public List<SysOperationAuditSnapshot> listSnapshots(AuditSnapshotQueryParams params)
        {
            return Collections.emptyList();
        }
    }
}
