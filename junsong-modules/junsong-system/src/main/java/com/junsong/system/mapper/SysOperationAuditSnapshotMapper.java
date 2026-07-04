package com.junsong.system.mapper;

import java.util.Date;
import java.util.List;
import com.junsong.system.domain.SysOperationAuditSnapshot;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;

/**
 * R25操作审计快照 数据层
 */
public interface SysOperationAuditSnapshotMapper
{
    /**
     * 新增审计快照
     */
    int insertAuditSnapshot(SysOperationAuditSnapshot snapshot);

    /**
     * 查询审计快照列表
     */
    List<SysOperationAuditSnapshot> selectAuditSnapshots(AuditSnapshotQueryParams params);

    /**
     * 统计指定时间后的高风险审计数
     */
    int countHighRiskSince(Date since);
}
