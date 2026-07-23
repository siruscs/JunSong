package com.junsong.system.service;

import java.util.List;
import com.junsong.system.api.domain.SysUserMpBinding;

/**
 * 小程序微信账号绑定关系 服务层
 */
public interface ISysUserMpBindingService
{
    /**
     * 仅按 (appId, openid) 全局查询 ACTIVE 绑定（仅限微信快捷登录流程）
     */
    SysUserMpBinding selectActiveByAppOpenidForLogin(String appId, String openid);

    /**
     * 按 (tenantId, userId) 查询绑定列表
     */
    List<SysUserMpBinding> selectByUserId(Long tenantId, Long userId);

    /**
     * 新增绑定关系
     */
    int insertBinding(SysUserMpBinding binding);

    /**
     * 撤销绑定
     */
    int revokeBinding(SysUserMpBinding binding);

    /**
     * 重新激活已撤销的绑定关系（REVOKED → ACTIVE）
     */
    int reactivateBinding(SysUserMpBinding binding);

    /**
     * 更新最近登录时间
     */
    int updateLastLoginTime(Long tenantId, Long bindingId);
}
