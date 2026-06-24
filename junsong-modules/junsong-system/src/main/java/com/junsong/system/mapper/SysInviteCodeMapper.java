package com.junsong.system.mapper;

import com.junsong.system.domain.SysInviteCode;
import org.apache.ibatis.annotations.Param;

/**
 * 邀请码 数据层
 *
 * @author junsong
 */
public interface SysInviteCodeMapper
{
    /**
     * 根据邀请码查询（未使用）
     */
    SysInviteCode selectByCode(@Param("code") String code);

    /**
     * 新增邀请码
     */
    int insertInviteCode(SysInviteCode inviteCode);

    /**
     * 更新邀请码状态（标记已使用）
     */
    int updateInviteCodeUsed(@Param("code") String code, @Param("inviteeUserId") Long inviteeUserId);
}
