package com.junsong.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.system.api.domain.SysUserMpBinding;
import com.junsong.system.mapper.SysUserMpBindingMapper;
import com.junsong.system.service.ISysUserMpBindingService;

/**
 * 小程序微信账号绑定关系 服务实现
 */
@Service
public class SysUserMpBindingServiceImpl implements ISysUserMpBindingService
{
    @Autowired
    private SysUserMpBindingMapper sysUserMpBindingMapper;

    @Override
    public SysUserMpBinding selectActiveByAppOpenidForLogin(String appId, String openid)
    {
        return sysUserMpBindingMapper.selectActiveByAppOpenidForLogin(appId, openid);
    }

    @Override
    public List<SysUserMpBinding> selectByUserId(Long tenantId, Long userId)
    {
        return sysUserMpBindingMapper.selectByUserId(tenantId, userId);
    }

    @Override
    public int insertBinding(SysUserMpBinding binding)
    {
        return sysUserMpBindingMapper.insert(binding);
    }

    @Override
    public int revokeBinding(SysUserMpBinding binding)
    {
        return sysUserMpBindingMapper.revoke(binding);
    }

    @Override
    public int reactivateBinding(SysUserMpBinding binding)
    {
        return sysUserMpBindingMapper.reactivate(binding);
    }

    @Override
    public int updateLastLoginTime(Long tenantId, Long bindingId)
    {
        return sysUserMpBindingMapper.updateLastLoginTime(tenantId, bindingId);
    }
}
