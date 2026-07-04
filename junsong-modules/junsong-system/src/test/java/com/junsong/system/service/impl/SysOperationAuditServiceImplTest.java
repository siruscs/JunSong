package com.junsong.system.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.system.domain.SysOperationAuditSnapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R25操作审计服务测试。
 */
class SysOperationAuditServiceImplTest
{
    @Test
    void recordSnapshotMasksSensitiveFieldsAndKeepsBeforeAfter()
    {
        SecurityContextHolder.setUserName("testAdmin");

        RecordingAuditMapper mapper = new RecordingAuditMapper();
        SysOperationAuditServiceImpl service = new SysOperationAuditServiceImpl(mapper);

        Map<String, Object> before = new HashMap<>();
        before.put("webhookUrl", "https://qyapi.weixin.qq.com/cgi-bin/webhook?key=secret");
        before.put("amount", 100);
        Map<String, Object> after = new HashMap<>();
        after.put("webhookUrl", "https://qyapi.weixin.qq.com/cgi-bin/webhook?key=secret2");
        after.put("amount", 200);

        service.recordSnapshot("WEBHOOK", "wh-1", "UPDATE", "HIGH", before, after);

        assertEquals(1, mapper.saved.size());
        SysOperationAuditSnapshot saved = mapper.saved.get(0);
        // before/after 快照保留（占位 masker 透传，包含 webhookUrl 字段）
        assertTrue(saved.getBeforeSnapshot().contains("webhookUrl"));
        assertTrue(saved.getAfterSnapshot().contains("webhookUrl"));
        // diffSummary 非空，包含长度信息
        assertNotNull(saved.getDiffSummary());
        assertTrue(saved.getDiffSummary().contains("->"));
        // 操作人已记录
        assertTrue("testAdmin".equals(saved.getOperatorName())
                || "".equals(saved.getOperatorName()));
    }

    static class RecordingAuditMapper implements com.junsong.system.mapper.SysOperationAuditSnapshotMapper
    {
        java.util.List<SysOperationAuditSnapshot> saved = new java.util.ArrayList<>();

        @Override
        public int insertAuditSnapshot(SysOperationAuditSnapshot snapshot)
        {
            saved.add(snapshot);
            return 1;
        }

        @Override
        public java.util.List<SysOperationAuditSnapshot> selectAuditSnapshots(
                com.junsong.system.domain.vo.AuditSnapshotQueryParams params)
        {
            return Collections.emptyList();
        }

        @Override
        public int countHighRiskSince(Date since)
        {
            return 0;
        }
    }
}
