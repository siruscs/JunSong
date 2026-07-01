package com.junsong.system.service;

import java.util.List;
import com.junsong.system.domain.SysTenant;

/**
 * 租户信息 服务层
 *
 * @author junsong
 */
public interface ISysTenantService
{
    /**
     * 查询租户信息集合
     *
     * @param tenant 租户信息
     * @return 租户列表
     */
    public List<SysTenant> selectTenantList(SysTenant tenant);

    /**
     * 通过租户ID查询租户信息
     *
     * @param tenantId 租户ID
     * @return 租户对象信息
     */
    public SysTenant selectTenantByTenantId(Long tenantId);

    /**
     * 校验租户名称是否唯一
     *
     * @param tenant 租户信息
     * @return 结果
     */
    public boolean checkTenantNameUnique(SysTenant tenant);

    /**
     * 删除租户信息
     *
     * @param tenantId 租户ID
     * @return 结果
     */
    public int deleteTenantById(Long tenantId);

    /**
     * 新增保存租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    public int insertTenant(SysTenant tenant);

    /**
     * 修改保存租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    public int updateTenant(SysTenant tenant);

    /**
     * 创建租户并初始化基础数据（部门、角色、管理员用户）
     * @param tenant 租户信息
     * @param adminUserName 管理员用户名
     * @param adminPassword 管理员密码（明文，内部加密）
     * @return 创建的租户ID
     */
    public Long createTenantWithInit(SysTenant tenant, String adminUserName, String adminPassword);
}
