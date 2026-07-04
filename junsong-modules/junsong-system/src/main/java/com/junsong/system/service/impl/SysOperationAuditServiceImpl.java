package com.junsong.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.junsong.common.core.utils.SensitiveDataMasker;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysOperationAuditSnapshot;
import com.junsong.system.mapper.SysOperationAuditSnapshotMapper;
import com.junsong.system.service.ISysOperationAuditService;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;

/**
 * R25操作审计 服务实现
 */
@Service
public class SysOperationAuditServiceImpl implements ISysOperationAuditService
{
    private final SysOperationAuditSnapshotMapper auditSnapshotMapper;

    public SysOperationAuditServiceImpl(SysOperationAuditSnapshotMapper auditSnapshotMapper)
    {
        this.auditSnapshotMapper = auditSnapshotMapper;
    }

    @Override
    public void recordSnapshot(String bizType, String bizId, String operation, String riskLevel, Object before, Object after)
    {
        String beforeJson = toMaskedJson(before);
        String afterJson = toMaskedJson(after);

        String diffSummary = buildDiffSummary(beforeJson, afterJson);

        SysOperationAuditSnapshot snapshot = new SysOperationAuditSnapshot();
        snapshot.setBizType(bizType);
        snapshot.setBizId(bizId);
        snapshot.setOperation(operation);
        snapshot.setRiskLevel(riskLevel);
        snapshot.setBeforeSnapshot(beforeJson);
        snapshot.setAfterSnapshot(afterJson);
        snapshot.setDiffSummary(diffSummary);
        snapshot.setOperatorName(SecurityUtils.getUsername());
        snapshot.setCreateTime(new Date());
        auditSnapshotMapper.insertAuditSnapshot(snapshot);
    }

    @Override
    public List<SysOperationAuditSnapshot> listSnapshots(AuditSnapshotQueryParams params)
    {
        return auditSnapshotMapper.selectAuditSnapshots(params);
    }

    private String toMaskedJson(Object obj)
    {
        if (obj == null)
        {
            return "";
        }
        String json;
        if (obj instanceof String)
        {
            json = (String) obj;
        }
        else
        {
            json = JSON.toJSONString(obj);
        }
        return SensitiveDataMasker.maskSensitive(json);
    }

    private String buildDiffSummary(String beforeJson, String afterJson)
    {
        int beforeLen = beforeJson == null ? 0 : beforeJson.length();
        int afterLen = afterJson == null ? 0 : afterJson.length();
        StringBuilder sb = new StringBuilder();
        sb.append(beforeLen).append(" -> ").append(afterLen);
        try
        {
            com.alibaba.fastjson2.JSONObject afterObj = JSON.parseObject(afterJson);
            if (afterObj != null && !afterObj.isEmpty())
            {
                sb.append("; fields: ").append(String.join(",", afterObj.keySet()));
            }
        }
        catch (Exception ignored)
        {
        }
        return sb.toString();
    }
}
