package com.junsong.system.mapper;

import java.util.Date;
import java.util.List;
import com.junsong.system.domain.SysActionCenterTouchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysActionCenterTouchLogMapper {
    int insertLog(SysActionCenterTouchLog log);
    SysActionCenterTouchLog selectLatestByActionId(@Param("actionId") String actionId);
    int countByDigestSince(@Param("requestDigest") String requestDigest, @Param("since") Date since);
    int countByTargetSince(@Param("channel") String channel, @Param("targetRef") String targetRef, @Param("since") Date since);
    List<SysActionCenterTouchLog> selectRecentByActionId(@Param("actionId") String actionId, @Param("limit") int limit);
}
