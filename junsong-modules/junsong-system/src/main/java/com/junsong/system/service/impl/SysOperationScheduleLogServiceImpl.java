package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.junsong.system.domain.SysOperationScheduleLog;
import com.junsong.system.domain.vo.OperationScheduleDashboardVO;
import com.junsong.system.domain.vo.OperationScheduleLogVO;
import com.junsong.system.mapper.SysOperationScheduleLogMapper;
import com.junsong.system.service.ISysOperationScheduleLogService;
import org.springframework.stereotype.Service;

/**
 * R21 运维调度日志服务实现。
 * 负责调度执行日志的全生命周期管理：创建、更新、聚合。
 */
@Service
public class SysOperationScheduleLogServiceImpl implements ISysOperationScheduleLogService {

    private final SysOperationScheduleLogMapper mapper;

    public SysOperationScheduleLogServiceImpl(SysOperationScheduleLogMapper mapper) {
        this.mapper = mapper;
    }

    // ==================== 生命周期方法 ====================

    @Override
    public OperationScheduleLogVO start(String jobCode, String jobName, String triggerType) {
        SysOperationScheduleLog log = new SysOperationScheduleLog();
        log.setJobCode(jobCode);
        log.setJobName(jobName);
        log.setTriggerType(triggerType);
        log.setStatus("RUNNING");
        log.setStartedAt(new Date());
        log.setAffectedRows(0);
        mapper.insertLog(log);
        return toVO(log);
    }

    @Override
    public void finishSuccess(Long logId, int affectedRows, String resultSummary) {
        SysOperationScheduleLog log = mapper.selectById(logId);
        log.setStatus("SUCCESS");
        log.setAffectedRows(affectedRows);
        log.setResultSummary(resultSummary);
        finalizeLog(log);
    }

    @Override
    public void finishSkipped(Long logId, String resultSummary) {
        SysOperationScheduleLog log = mapper.selectById(logId);
        log.setStatus("SKIPPED");
        log.setResultSummary(resultSummary);
        finalizeLog(log);
    }

    @Override
    public void finishPartial(Long logId, int affectedRows, String resultSummary, String errorMessage) {
        SysOperationScheduleLog log = mapper.selectById(logId);
        log.setStatus("PARTIAL");
        log.setAffectedRows(affectedRows);
        log.setResultSummary(resultSummary);
        log.setErrorMessage(errorMessage);
        finalizeLog(log);
    }

    @Override
    public void finishFailed(Long logId, Throwable throwable) {
        SysOperationScheduleLog log = mapper.selectById(logId);
        log.setStatus("FAILED");
        // 必须完整记录异常类名和消息，不能吞掉异常文本
        log.setErrorMessage(throwable.getClass().getName() + ": " + throwable.getMessage());
        finalizeLog(log);
    }

    // ==================== 查询方法 ====================

    @Override
    public OperationScheduleDashboardVO getDashboard() {
        OperationScheduleDashboardVO dashboard = new OperationScheduleDashboardVO();

        // latestPerJobCode → 每个任务编码的最近一次执行记录
        List<SysOperationScheduleLog> latestPerJob = mapper.selectLatestPerJobCode();
        dashboard.setRecentLogs(toVOList(latestPerJob));

        // countFailedInLast24h → 过去 24 小时内失败次数
        dashboard.setFailureCount24h(mapper.countFailedInLast24h());

        // recentFailures → 最近的失败记录列表
        List<SysOperationScheduleLog> failures = mapper.selectRecentFailures();
        dashboard.setRecentFailures(toVOList(failures));

        return dashboard;
    }

    @Override
    public List<OperationScheduleLogVO> listRecent(String jobCode, int limit) {
        List<SysOperationScheduleLog> logs = mapper.selectRecent(jobCode, limit);
        return toVOList(logs);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 统一收尾：计算耗时并持久化更新。
     */
    private void finalizeLog(SysOperationScheduleLog log) {
        Date now = new Date();
        log.setFinishedAt(now);
        log.setDurationMs(now.getTime() - log.getStartedAt().getTime());
        mapper.updateLog(log);
    }

    private static final java.text.SimpleDateFormat DATE_FMT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 领域对象 → VO 转换。
     */
    private OperationScheduleLogVO toVO(SysOperationScheduleLog log) {
        OperationScheduleLogVO vo = new OperationScheduleLogVO();
        vo.setLogId(log.getLogId());
        vo.setJobCode(log.getJobCode());
        vo.setJobName(log.getJobName());
        vo.setTriggerType(log.getTriggerType());
        vo.setStatus(log.getStatus());
        vo.setStartedAt(log.getStartedAt() != null ? DATE_FMT.format(log.getStartedAt()) : null);
        vo.setFinishedAt(log.getFinishedAt() != null ? DATE_FMT.format(log.getFinishedAt()) : null);
        vo.setDurationMs(log.getDurationMs());
        vo.setAffectedRows(log.getAffectedRows());
        vo.setResultSummary(log.getResultSummary());
        vo.setErrorMessage(log.getErrorMessage());
        return vo;
    }

    /**
     * 领域对象列表 → VO 列表转换。
     */
    private List<OperationScheduleLogVO> toVOList(List<SysOperationScheduleLog> logs) {
        return logs.stream().map(this::toVO).collect(Collectors.toList());
    }
}
