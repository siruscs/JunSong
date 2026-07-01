package com.junsong.workflow.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 轻量级查询 sys_user（避免跨模块依赖）
 */
public interface WfSysUserMapper
{
    @Select("SELECT user_id FROM sys_user WHERE user_name = #{userName} AND del_flag = '0' LIMIT 1")
    Long selectUserIdByUserName(@Param("userName") String userName);

    @Select("SELECT user_name FROM sys_user WHERE user_id = #{userId} AND del_flag = '0' LIMIT 1")
    String selectUserNameByUserId(@Param("userId") Long userId);

    @Select("SELECT su.user_id FROM sys_user su " +
            "INNER JOIN sys_user_role sur ON su.user_id = sur.user_id " +
            "INNER JOIN sys_role sr ON sur.role_id = sr.role_id " +
            "WHERE sr.role_key = #{roleKey} AND sr.status = '0' AND su.del_flag = '0'")
    List<Long> selectUserIdsByRoleKey(@Param("roleKey") String roleKey);
}
