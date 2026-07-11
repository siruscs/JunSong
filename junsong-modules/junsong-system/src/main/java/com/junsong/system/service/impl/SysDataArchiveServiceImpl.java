package com.junsong.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
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
    /** 归档候选表白名单：仅允许已知的留存策略表，与 Mapper XML choose 分支保持一致，防止 SQL 注入 */
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "sys_oper_log",
            "sys_logininfor",
            "sys_notification",
            "sys_action_center_touch_log",
            "sys_operation_schedule_log",
            "finance_prediction_sample",
            "finance_what_if_simulation",
            "open_api_log",
            "open_webhook_subscription"
    );

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
        SysDataRetentionPolicy policy = validateArchiveRequest(tableName);
        ArchivePreviewVO vo = new ArchivePreviewVO();
        vo.setTableName(tableName);
        vo.setDryRun("1");
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
        SysDataRetentionPolicy policy = validateArchiveRequest(tableName);
        SysDataArchiveRun run = new SysDataArchiveRun();
        run.setTableName(tableName);
        run.setDryRun(dryRun ? "1" : "0");
        run.setPolicyId(policy.getPolicyId());
        run.setCutoffTime(computeCutoffTime(policy.getRetentionDays()));
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

    /**
     * 校验归档请求：表名白名单 + 策略存在 + 策略启用 + 归档模式非 DISABLED
     */
    private SysDataRetentionPolicy validateArchiveRequest(String tableName)
    {
        if (tableName == null || tableName.isEmpty())
        {
            throw new ServiceException("表名不能为空");
        }
        if (!ALLOWED_TABLES.contains(tableName))
        {
            throw new ServiceException("不支持的归档表: " + tableName);
        }
        SysDataRetentionPolicy policy = retentionPolicyMapper.selectByTableName(tableName);
        if (policy == null)
        {
            throw new ServiceException("未找到表 " + tableName + " 的留存策略");
        }
        if (!"1".equals(policy.getEnabled()))
        {
            throw new ServiceException("表 " + tableName + " 的留存策略已禁用");
        }
        if ("DISABLED".equals(policy.getArchiveMode()))
        {
            throw new ServiceException("表 " + tableName + " 的归档模式为 DISABLED，不允许归档");
        }
        return policy;
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
