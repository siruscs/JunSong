package com.junsong.system.service.impl;

import java.util.Date;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.vo.EnterpriseHardeningDashboardVO;
import com.junsong.system.mapper.SysOperationAlertEventMapper;
import com.junsong.system.mapper.SysOperationAuditSnapshotMapper;
import com.junsong.system.mapper.SysDataArchiveRunMapper;
import com.junsong.system.service.IEnterpriseHardeningService;

/**
 * R25企业级硬化看板 服务实现
 */
@Service
public class EnterpriseHardeningServiceImpl implements IEnterpriseHardeningService
{
    private final SysOperationAuditSnapshotMapper auditSnapshotMapper;
    private final SysDataArchiveRunMapper archiveRunMapper;
    private final SysOperationAlertEventMapper alertEventMapper;

    public EnterpriseHardeningServiceImpl(SysOperationAuditSnapshotMapper auditSnapshotMapper,
            SysDataArchiveRunMapper archiveRunMapper,
            SysOperationAlertEventMapper alertEventMapper)
    {
        this.auditSnapshotMapper = auditSnapshotMapper;
        this.archiveRunMapper = archiveRunMapper;
        this.alertEventMapper = alertEventMapper;
    }

    @Override
    public EnterpriseHardeningDashboardVO getDashboard()
    {
        EnterpriseHardeningDashboardVO vo = new EnterpriseHardeningDashboardVO();
        Date since = new Date(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L);
        vo.setHighRiskAuditCount(auditSnapshotMapper.countHighRiskSince(since));
        Long archiveCandidate = archiveRunMapper.sumLatestCandidateCount();
        vo.setArchiveCandidateCount(archiveCandidate == null ? 0L : archiveCandidate);
        vo.setOpenCriticalAlertCount(alertEventMapper.countOpenCritical());
        vo.setBasis("近7天高风险审计 + 近7天最大归档候选 + OPEN/CRITICAL告警");
        return vo;
    }
}
