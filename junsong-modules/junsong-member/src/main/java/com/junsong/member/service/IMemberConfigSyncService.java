package com.junsong.member.service;

import java.util.Map;
import com.junsong.member.domain.vo.ConfigSyncPreviewRequest;
import com.junsong.member.domain.vo.ConfigSyncExecuteRequest;

public interface IMemberConfigSyncService
{
    Map<String, Object> preview(ConfigSyncPreviewRequest request, Long tenantId, Long sourceDeptId,
                                Long userId, String operator);
    Map<String, Object> getBatch(Long tenantId, Long batchId);
    Map<String, Object> execute(ConfigSyncExecuteRequest request, Long tenantId, Long userId, String operator);
}
