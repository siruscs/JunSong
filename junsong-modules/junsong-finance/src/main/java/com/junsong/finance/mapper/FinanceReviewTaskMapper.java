package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceReviewTask;
import org.apache.ibatis.annotations.Param;

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
}
