package com.junsong.system.service.impl;

import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.domain.SysDataRetentionPolicy;
import com.junsong.system.domain.vo.ArchivePreviewVO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * R25数据归档服务测试。
 */
class SysDataArchiveServiceImplTest
{
    @Test
    void previewArchiveComputesCutoffAndDoesNotArchiveRows()
    {
        FakePolicyMapper policyMapper = new FakePolicyMapper();
        policyMapper.policy = new SysDataRetentionPolicy();
        policyMapper.policy.setTableName("sys_oper_log");
        policyMapper.policy.setRetentionDays(180);
        policyMapper.policy.setArchiveMode("SUMMARY_ONLY");
        policyMapper.policy.setEnabled("1");

        FakeArchiveRunMapper archiveRunMapper = new FakeArchiveRunMapper();
        archiveRunMapper.candidateCount = 42L;

        SysDataArchiveServiceImpl service = new SysDataArchiveServiceImpl(policyMapper, archiveRunMapper);

        ArchivePreviewVO vo = service.previewArchive("sys_oper_log");

        assertEquals("sys_oper_log", vo.getTableName());
        assertEquals("SUMMARY_ONLY", vo.getArchiveMode());
        assertEquals("1", vo.getDryRun());
        assertEquals(42L, vo.getCandidateCount());
        assertNotNull(vo.getCutoffTime());
        // R25 不真正归档/删除行：archiveRunMapper 不应被调用插入
        assertEquals(0, archiveRunMapper.insertCount);
    }

    @Test
    void previewArchiveRejectsDisabledArchiveMode()
    {
        FakePolicyMapper policyMapper = new FakePolicyMapper();
        policyMapper.policy = new SysDataRetentionPolicy();
        policyMapper.policy.setTableName("open_webhook_subscription");
        policyMapper.policy.setRetentionDays(365);
        policyMapper.policy.setArchiveMode("DISABLED");
        policyMapper.policy.setEnabled("1");

        FakeArchiveRunMapper archiveRunMapper = new FakeArchiveRunMapper();
        SysDataArchiveServiceImpl service = new SysDataArchiveServiceImpl(policyMapper, archiveRunMapper);

        assertThrows(ServiceException.class, () -> service.previewArchive("open_webhook_subscription"));
        assertEquals(0, archiveRunMapper.insertCount);
    }

    @Test
    void previewArchiveRejectsNonWhitelistTableForSqlInjection()
    {
        FakePolicyMapper policyMapper = new FakePolicyMapper();
        FakeArchiveRunMapper archiveRunMapper = new FakeArchiveRunMapper();
        SysDataArchiveServiceImpl service = new SysDataArchiveServiceImpl(policyMapper, archiveRunMapper);

        assertThrows(ServiceException.class, () -> service.previewArchive("sys_user; DROP TABLE--"));
        assertEquals(0, archiveRunMapper.insertCount);
    }

    static class FakePolicyMapper implements com.junsong.system.mapper.SysDataRetentionPolicyMapper
    {
        SysDataRetentionPolicy policy;

        @Override
        public java.util.List<SysDataRetentionPolicy> selectAllEnabledPolicies()
        {
            return policy == null ? Collections.emptyList() : Collections.singletonList(policy);
        }

        @Override
        public SysDataRetentionPolicy selectByTableName(String tableName)
        {
            return policy;
        }
    }

    static class FakeArchiveRunMapper implements com.junsong.system.mapper.SysDataArchiveRunMapper
    {
        Long candidateCount = 0L;
        int insertCount = 0;

        @Override
        public int insertArchiveRun(com.junsong.system.domain.SysDataArchiveRun run)
        {
            insertCount++;
            return 1;
        }

        @Override
        public Long sumLatestCandidateCount()
        {
            return candidateCount;
        }

        @Override
        public Long countArchiveCandidates(String tableName, Date cutoffTime)
        {
            return candidateCount;
        }
    }
}
