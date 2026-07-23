package com.junsong.finance.service;

import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.FinanceReviewTaskLog;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.domain.vo.ReviewTaskEffectSummaryVO;
import com.junsong.finance.domain.vo.ReviewTaskEffectVO;

import java.math.BigDecimal;
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
    FinanceReviewTask getTask(Long taskId);

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

    /**
     * 查询复盘任务处理轨迹
     *
     * @param taskId 任务ID
     * @return 处理轨迹集合，按操作时间升序
     */
    List<FinanceReviewTaskLog> getTaskLogs(Long taskId);

    /**
     * 重开已完成或已忽略的任务
     *
     * @param taskId 任务ID
     * @param reason 重开原因（必填）
     * @return 影响行数
     */
    int reopenTask(Long taskId, String reason);

    /**
     * 评估已完成任务的动作效果
     *
     * @param taskId     任务ID
     * @param windowDays 评估窗口天数（默认7）
     * @return 效果评估VO
     */
    ReviewTaskEffectVO evaluateTaskEffect(Long taskId, Integer windowDays);

    /**
     * 从会员动作生成复盘任务
     *
     * @param req 请求参数（deptId, actionType, problemType, title, reason, impactAmount, sourceId）
     * @return 创建或已存在的复盘任务
     */
    FinanceReviewTask createFromMemberAction(Map<String, Object> req);

    /**
     * 汇总已完成任务的动作效果评估
     *
     * @param deptIds    门店ID列表（可为 null 表示全部）
     * @param windowDays 评估窗口天数（默认7）
     * @return 效果汇总VO
     */
    ReviewTaskEffectSummaryVO summarizeEffect(List<Long> deptIds, Integer windowDays);

    /**
     * 从逾期应收生成催收复盘任务
     *
     * @param deptId          门店ID
     * @param minAgeDays      最小账龄天数（默认14）
     * @param minUnpaidAmount 最小未缴金额（默认500）
     * @return 新增任务数量
     */
    int generateReceivableCollectionTasks(Long deptId, Integer minAgeDays, BigDecimal minUnpaidAmount);
}
