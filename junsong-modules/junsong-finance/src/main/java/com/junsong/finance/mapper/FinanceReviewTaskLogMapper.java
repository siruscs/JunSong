package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceReviewTaskLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 复盘任务处理轨迹Mapper接口
 *
 * @author junsong
 */
public interface FinanceReviewTaskLogMapper {

    /**
     * 新增复盘任务处理轨迹
     *
     * @param log 处理轨迹
     * @return 影响行数
     */
    int insertFinanceReviewTaskLog(FinanceReviewTaskLog log);

    /**
     * 根据任务ID查询处理轨迹
     *
     * @param taskId 复盘任务ID
     * @return 处理轨迹集合，按操作时间升序
     */
    List<FinanceReviewTaskLog> selectLogsByTaskId(@Param("taskId") Long taskId);
}
