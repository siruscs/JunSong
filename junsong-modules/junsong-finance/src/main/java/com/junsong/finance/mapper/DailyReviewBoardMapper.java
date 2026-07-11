package com.junsong.finance.mapper;

import com.junsong.finance.domain.vo.DailyReviewBoardVO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 每日经营复盘 Mapper
 * R8-A: 基于 fin_sale_record / fin_sale_payment / fin_expense / finance_review_task 真实表查询。
 *
 * @author junsong
 */
public interface DailyReviewBoardMapper {

    /**
     * 查询某日销售额（按 sale_date）
     */
    java.math.BigDecimal selectSalesAmount(@Param("deptIds") List<Long> deptIds,
                                           @Param("startDate") Date startDate,
                                           @Param("endDate") Date endDate);

    /**
     * 查询某日实收现金（按 payment_date）
     */
    java.math.BigDecimal selectCashInAmount(@Param("deptIds") List<Long> deptIds,
                                            @Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    /**
     * 查询某日费用支出（按 expense_date）
     */
    java.math.BigDecimal selectExpenseAmount(@Param("deptIds") List<Long> deptIds,
                                             @Param("startDate") Date startDate,
                                             @Param("endDate") Date endDate);

    /**
     * 查询待处理任务数（PENDING/IN_PROGRESS）
     */
    int selectPendingTaskCount(@Param("deptIds") List<Long> deptIds);

    /**
     * 查询高优先级任务数（severity=HIGH 且 pending）
     */
    int selectHighPriorityTaskCount(@Param("deptIds") List<Long> deptIds);

    /**
     * 查询本周已完成任务数（status=DONE 且 update_time 在指定范围内）
     * R8-F: 用于周复盘已完成任务统计
     */
    int selectCompletedTaskCount(@Param("deptIds") List<Long> deptIds,
                                  @Param("startDate") Date startDate,
                                  @Param("endDate") Date endDate);

    /**
     * 查询高优先级待办任务列表（最多 limit 条），返回原始字段 Map
     */
    List<java.util.Map<String, Object>> selectHighPriorityTasks(@Param("deptIds") List<Long> deptIds,
                                                               @Param("limit") int limit);

    /**
     * 查询门店名称
     */
    String selectDeptName(@Param("deptId") Long deptId);

    /**
     * 检查表是否存在
     */
    int checkTableExists(@Param("tableName") String tableName);

    /**
     * R10-F: 查询本周已完成任务的 handler_note（最多5条非空备注）
     */
    List<String> selectDoneTaskNotes(@Param("deptIds") List<Long> deptIds,
                                      @Param("startDate") Date startDate,
                                      @Param("endDate") Date endDate);

    /**
     * R10-F: 查询未解决的高风险任务标题
     */
    List<String> selectUnresolvedHighRiskTasks(@Param("deptIds") List<Long> deptIds);
}
