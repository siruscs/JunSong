package com.junsong.system.mapper;

import com.junsong.system.domain.SysActionCenterTouchThrottle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysActionCenterTouchThrottleMapper {
    SysActionCenterTouchThrottle selectByKey(@Param("throttleKey") String throttleKey);
    int insertThrottle(SysActionCenterTouchThrottle throttle);
    int updateThrottle(SysActionCenterTouchThrottle throttle);
}
