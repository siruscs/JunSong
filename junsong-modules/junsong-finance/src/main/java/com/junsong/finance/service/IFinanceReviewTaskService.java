package com.junsong.finance.service;

import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.ReportQueryParams;

import java.util.List;
import java.util.Map;

/**
 * 财务复盘任务Service接口
 *
 * @author junsong
 */
public interface IFinanceReviewTaskService {

    /**
     * 查询复盘任务列表
     *
     * @param params 查询参数
     * @return 复盘任务集合
     */
    List<FinanceReviewTask> listTasks(Map<String, Object> params);

    /**
     * 从诊断结果生成复盘任务
     *
     * @param deptIds 门店ID列表
     * @param params  报表查询参数（时间范围等）
     * @return 新增任务数量
     */
    int generateFromDiagnosis(List<Long> deptIds, ReportQueryParams params);

    /**
     * 标记任务为处理中
     *
     * @param taskId      任务ID
     * @param handlerId   处理人ID
     * @param handlerName 处理人姓名
     */
    void markInProgress(Long taskId, Long handlerId, String handlerName);

    /**
     * 标记任务为已完成
     *
     * @param taskId      任务ID
     * @param handlerId   处理人ID
     * @param handlerName 处理人姓名
     * @param handlerNote 处理备注（必填）
     */
    void markDone(Long taskId, Long handlerId, String handlerName, String handlerNote);

    /**
     * 标记任务为已忽略
     *
     * @param taskId       任务ID
     * @param handlerId    处理人ID
     * @param handlerName  处理人姓名
     * @param ignoreReason 忽略原因（必填）
     */
    void markIgnored(Long taskId, Long handlerId, String handlerName, String ignoreReason);
}
