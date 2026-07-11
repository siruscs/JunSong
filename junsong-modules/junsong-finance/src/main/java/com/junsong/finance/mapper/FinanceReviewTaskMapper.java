package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceReviewTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 财务复盘任务Mapper接口
 *
 * @author junsong
 */
public interface FinanceReviewTaskMapper {

    /**
     * 查询复盘任务列表（支持 status / deptIds 过滤）
     *
     * @param params 查询参数，可包含 status、deptIds、title 等
     * @return 复盘任务集合
     */
    List<FinanceReviewTask> selectReviewTaskList(Map<String, Object> params);

    /**
     * 根据任务ID查询复盘任务
     *
     * @param taskId 任务ID
     * @return 复盘任务
     */
    FinanceReviewTask selectByTaskId(Long taskId);

    /**
     * 根据去重Key查询复盘任务（用于幂等判断）
     *
     * @param alertId  去重标识
     * @param taskDate 任务日期（yyyy-MM-dd）
     * @return 复盘任务，不存在返回 null
     */
    FinanceReviewTask selectByAlertId(@Param("alertId") String alertId, @Param("taskDate") String taskDate);

    /**
     * 新增复盘任务
     *
     * @param task 复盘任务
     * @return 影响行数
     */
    int insertReviewTask(FinanceReviewTask task);

    /**
     * 更新复盘任务
     *
     * @param task 复盘任务
     * @return 影响行数
     */
    int updateReviewTask(FinanceReviewTask task);

    /**
     * 按状态统计复盘任务数量
     *
     * @param status  状态
     * @param deptIds 门店ID列表
     * @return 数量
     */
    int countByStatus(@Param("status") String status, @Param("deptIds") List<Long> deptIds);

    /**
     * 查询指定门店、时间窗口内的销售总额和费用总额
     */
    Map<String, Object> selectTaskEffectAmountWindow(@Param("deptId") Long deptId,
                                                     @Param("startTime") Date startTime,
                                                     @Param("endTime") Date endTime);

    /**
     * 统计指定门店、问题类型、时间窗口内的未完成同类任务数
     */
    int countSimilarOpenTasks(@Param("deptId") Long deptId,
                              @Param("problemType") String problemType,
                              @Param("startTime") Date startTime,
                              @Param("endTime") Date endTime);

    /**
     * 查询最近归档的已完成任务（用于效果汇总）
     *
     * @param deptIds   门店ID列表
     * @param sinceDate 最早归档时间
     * @param limit     最大返回数量
     * @return 已完成任务列表
     */
    List<FinanceReviewTask> selectRecentDoneTasks(@Param("deptIds") List<Long> deptIds,
                                                   @Param("sinceDate") Date sinceDate,
                                                   @Param("limit") int limit);

    /**
     * 查询可重开的候选任务（归档超过指定天数的已完成任务）
     *
     * @param deptIds    门店ID列表
     * @param cutoffDate 归档时间截止日
     * @param limit      最大返回数量
     * @return 重开候选任务列表
     */
    List<FinanceReviewTask> selectReopenCandidates(@Param("deptIds") List<Long> deptIds,
                                                    @Param("cutoffDate") Date cutoffDate,
                                                    @Param("limit") int limit);
}
