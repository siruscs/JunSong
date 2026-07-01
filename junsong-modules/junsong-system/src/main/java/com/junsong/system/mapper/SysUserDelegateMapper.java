package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.domain.SysUserDelegate;

/**
 * 用户委托代理表 数据层
 */
public interface SysUserDelegateMapper
{
    /**
     * 查询委托代理信息
     */
    public SysUserDelegate selectById(Long id);

    /**
     * 查询委托代理列表
     */
    public List<SysUserDelegate> selectList(SysUserDelegate delegate);

    /**
     * 查询某人当前有效的委托规则
     */
    public List<SysUserDelegate> selectActiveByUserId(Long userId);

    /**
     * 查询某人当前作为代理人的委托规则
     */
    public List<SysUserDelegate> selectActiveByDelegateUserId(Long delegateUserId);

    /**
     * 新增委托代理
     */
    public int insert(SysUserDelegate delegate);

    /**
     * 修改委托代理
     */
    public int update(SysUserDelegate delegate);

    /**
     * 删除委托代理
     */
    public int deleteById(Long id);

    /**
     * 批量删除委托代理
     */
    public int deleteByIds(Long[] ids);
}
