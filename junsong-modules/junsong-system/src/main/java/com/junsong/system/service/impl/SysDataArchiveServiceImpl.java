package com.junsong.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysDataArchiveRun;
import com.junsong.system.domain.SysDataRetentionPolicy;
import com.junsong.system.mapper.SysDataArchiveRunMapper;
import com.junsong.system.mapper.SysDataRetentionPolicyMapper;
import com.junsong.system.service.ISysDataArchiveService;
import com.junsong.system.domain.vo.ArchivePreviewVO;

/**
 * R25数据归档 服务实现
 */
@Service
public class SysDataArchiveServiceImpl implements ISysDataArchiveService
{
    private final SysDataRetentionPolicyMapper retentionPolicyMapper;
    private final SysDataArchiveRunMapper archiveRunMapper;

    public SysDataArchiveServiceImpl(SysDataRetentionPolicyMapper retentionPolicyMapper,
            SysDataArchiveRunMapper archiveRunMapper)
    {
        this.retentionPolicyMapper = retentionPolicyMapper;
        this.archiveRunMapper = archiveRunMapper;
    }

    @Override
    public ArchivePreviewVO previewArchive(String tableName)
    {
        SysDataRetentionPolicy policy = retentionPolicyMapper.selectByTableName(tableName);
        ArchivePreviewVO vo = new ArchivePreviewVO();
        vo.setTableName(tableName);
        vo.setDryRun("1");
        if (policy == null)
        {
            vo.setArchiveMode("SUMMARY_ONLY");
            vo.setCandidateCount(0L);
            vo.setCutoffTime(new Date());
            return vo;
        }
        vo.setArchiveMode(policy.getArchiveMode());
        Date cutoffTime = computeCutoffTime(policy.getRetentionDays());
        vo.setCutoffTime(cutoffTime);
        Long candidateCount = archiveRunMapper.countArchiveCandidates(tableName, cutoffTime);
        vo.setCandidateCount(candidateCount == null ? 0L : candidateCount);
        return vo;
    }

    @Override
    public SysDataArchiveRun runArchive(String tableName, boolean dryRun)
    {
        SysDataRetentionPolicy policy = retentionPolicyMapper.selectByTableName(tableName);
        SysDataArchiveRun run = new SysDataArchiveRun();
        run.setTableName(tableName);
        run.setDryRun(dryRun ? "1" : "0");
        if (policy != null)
        {
            run.setPolicyId(policy.getPolicyId());
            run.setCutoffTime(computeCutoffTime(policy.getRetentionDays()));
        }
        else
        {
            run.setCutoffTime(new Date());
        }
        Date cutoffTime = run.getCutoffTime();
        Long candidateCount = archiveRunMapper.countArchiveCandidates(tableName, cutoffTime);
        run.setCandidateCount(candidateCount == null ? 0L : candidateCount);
        // R25 不真正删除/归档行，仅记录执行情况
        run.setArchivedCount(0L);
        run.setStatus("SUCCESS");
        run.setErrorMessage("");
        run.setCreateTime(new Date());
        archiveRunMapper.insertArchiveRun(run);
        return run;
    }

    @Override
    public List<SysDataRetentionPolicy> listPolicies()
    {
        return retentionPolicyMapper.selectAllEnabledPolicies();
    }

    private Date computeCutoffTime(Integer retentionDays)
    {
        if (retentionDays == null || retentionDays <= 0)
        {
            return new Date();
        }
        long offsetMs = (long) retentionDays * 24L * 60L * 60L * 1000L;
        return new Date(System.currentTimeMillis() - offsetMs);
    }
}
