package com.junsong.finance.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 复盘质量数据层
 */
public interface ReviewQualityMapper {

    int countTasks(@Param("deptIds") List<Long> deptIds,
                   @Param("startDate") Date startDate,
                   @Param("endDate") Date endDate);

    int countDoneTasks(@Param("deptIds") List<Long> deptIds,
                       @Param("startDate") Date startDate,
                       @Param("endDate") Date endDate);

    int countOverdueTasks(@Param("deptIds") List<Long> deptIds,
                          @Param("startDate") Date startDate,
                          @Param("endDate") Date endDate);

    Double avgFirstResponseHours(@Param("deptIds") List<Long> deptIds,
                                 @Param("startDate") Date startDate,
                                 @Param("endDate") Date endDate);

    Double avgCloseHours(@Param("deptIds") List<Long> deptIds,
                         @Param("startDate") Date startDate,
                         @Param("endDate") Date endDate);

    int countNoNoteDoneTasks(@Param("deptIds") List<Long> deptIds,
                             @Param("startDate") Date startDate,
                             @Param("endDate") Date endDate);
}
