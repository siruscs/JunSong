package com.junsong.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;

@Mapper
public interface MemMpDashboardMapper {

    long queryCount(@Param("deptId") Long deptId, @Param("metric") String metric);

    BigDecimal queryDecimal(@Param("deptId") Long deptId, @Param("metric") String metric);

    BigDecimal queryDecimalWithDate(@Param("deptId") Long deptId, @Param("date") String date, @Param("metric") String metric);
}
