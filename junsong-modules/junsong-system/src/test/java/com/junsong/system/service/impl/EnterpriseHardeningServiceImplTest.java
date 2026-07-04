package com.junsong.system.service.impl;

import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.vo.EnterpriseHardeningDashboardVO;
import com.junsong.system.mapper.SysDataArchiveRunMapper;
import com.junsong.system.mapper.SysOperationAlertEventMapper;
import com.junsong.system.mapper.SysOperationAuditSnapshotMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R25企业级硬化看板服务测试。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class EnterpriseHardeningServiceImplTest
{
    @Test
    void dashboardAggregatesAuditArchiveAndAlertCounts()
    {
        RecordingAuditMapper auditMapper = new RecordingAuditMapper();
        auditMapper.highRiskCount = 3;
        RecordingArchiveRunMapper archiveRunMapper = new RecordingArchiveRunMapper();
        archiveRunMapper.latestCandidate = 128L;
        RecordingAlertEventMapper alertEventMapper = new RecordingAlertEventMapper();
        alertEventMapper.openCritical = 2;

        EnterpriseHardeningServiceImpl service = new EnterpriseHardeningServiceImpl(
                auditMapper, archiveRunMapper, alertEventMapper);

        EnterpriseHardeningDashboardVO vo = service.getDashboard();

        assertEquals(3, vo.getHighRiskAuditCount());
        assertEquals(128L, vo.getArchiveCandidateCount());
        assertEquals(2, vo.getOpenCriticalAlertCount());
    }

    static class RecordingAuditMapper implements SysOperationAuditSnapshotMapper
    {
        int highRiskCount = 0;

        @Override
        public int insertAuditSnapshot(com.junsong.system.domain.SysOperationAuditSnapshot snapshot) { return 1; }

        @Override
        public java.util.List<com.junsong.system.domain.SysOperationAuditSnapshot> selectAuditSnapshots(
                com.junsong.system.domain.vo.AuditSnapshotQueryParams params) { return Collections.emptyList(); }

        @Override
        public int countHighRiskSince(Date since) { return highRiskCount; }
    }

    static class RecordingArchiveRunMapper implements SysDataArchiveRunMapper
    {
        Long latestCandidate = 0L;

        @Override
        public int insertArchiveRun(com.junsong.system.domain.SysDataArchiveRun run) { return 1; }

        @Override
        public Long sumLatestCandidateCount() { return latestCandidate; }

        @Override
        public Long countArchiveCandidates(String tableName, Date cutoffTime) { return 0L; }
    }

    static class RecordingAlertEventMapper implements SysOperationAlertEventMapper
    {
        int openCritical = 0;

        @Override
        public int insertAlertEvent(com.junsong.system.domain.SysOperationAlertEvent event) { return 1; }

        @Override
        public com.junsong.system.domain.SysOperationAlertEvent selectOpenByDedupKey(String dedupKey) { return null; }

        @Override
        public int incrementHitCount(Long eventId) { return 1; }

        @Override
        public int updateEventStatus(Long eventId, String status) { return 1; }

        @Override
        public java.util.List<com.junsong.system.domain.SysOperationAlertEvent> selectAlertEvents(
                com.junsong.system.domain.vo.AlertEventQueryParams params) { return Collections.emptyList(); }

        @Override
        public int countOpenCritical() { return openCritical; }
    }
}
