package com.junsong.workflow.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 轻量级查询 sys_user_delegate（避免跨模块依赖）
 */
public interface WfSysUserDelegateMapper
{
    @Select("SELECT d.user_id, d.delegate_user_id, d.delegate_type, d.process_keys " +
            "FROM sys_user_delegate d " +
            "WHERE d.delegate_user_id = #{delegateUserId} " +
            "AND d.status = '0' " +
            "AND d.start_time <= NOW() " +
            "AND d.end_time >= NOW() " +
            "ORDER BY d.create_time DESC")
    List<WfDelegateRecord> selectActiveByDelegateUserId(@Param("delegateUserId") Long delegateUserId);

    class WfDelegateRecord
    {
        public Long userId;
        public Long delegateUserId;
        public String delegateType;
        public String processKeys;
    }
}
