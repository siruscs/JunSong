package com.junsong.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.system.domain.SysUserDelegate;
import com.junsong.system.mapper.SysUserDelegateMapper;
import com.junsong.system.service.ISysUserDelegateService;

/**
 * 用户委托代理 服务层实现
 */
@Service
public class SysUserDelegateServiceImpl implements ISysUserDelegateService
{
    @Autowired
    private SysUserDelegateMapper delegateMapper;

    @Override
    public List<SysUserDelegate> selectList(SysUserDelegate delegate)
    {
        return delegateMapper.selectList(delegate);
    }

    @Override
    public SysUserDelegate selectById(Long id)
    {
        return delegateMapper.selectById(id);
    }

    @Override
    public int insert(SysUserDelegate delegate)
    {
        return delegateMapper.insert(delegate);
    }

    @Override
    public int update(SysUserDelegate delegate)
    {
        return delegateMapper.update(delegate);
    }

    @Override
    public int deleteById(Long id)
    {
        return delegateMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Long[] ids)
    {
        return delegateMapper.deleteByIds(ids);
    }

    @Override
    public List<SysUserDelegate> selectActiveByUserId(Long userId)
    {
        return delegateMapper.selectActiveByUserId(userId);
    }

    @Override
    public List<SysUserDelegate> selectActiveByDelegateUserId(Long delegateUserId)
    {
        return delegateMapper.selectActiveByDelegateUserId(delegateUserId);
    }

    @Override
    public Long getDelegateUserId(Long userId, String processKey)
    {
        List<SysUserDelegate> list = delegateMapper.selectActiveByUserId(userId);
        for (SysUserDelegate d : list)
        {
            if ("all".equals(d.getDelegateType()))
            {
                return d.getDelegateUserId();
            }
            if ("workflow".equals(d.getDelegateType()) && d.getProcessKeys() != null)
            {
                for (String key : d.getProcessKeys().split(","))
                {
                    if (key.trim().equals(processKey))
                    {
                        return d.getDelegateUserId();
                    }
                }
            }
        }
        return null;
    }
}
