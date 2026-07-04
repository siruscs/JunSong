package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysOperationAuditSnapshot;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;

/**
 * R25操作审计 服务层
 */
public interface ISysOperationAuditService
{
    /**
     * 记录操作快照
     */
    void recordSnapshot(String bizType, String bizId, String operation, String riskLevel, Object before, Object after);

    /**
     * 查询审计快照列表
     */
    List<SysOperationAuditSnapshot> listSnapshots(AuditSnapshotQueryParams params);
}
