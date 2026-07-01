package com.junsong.system.mapper;

import java.util.List;
import java.util.Map;
import com.junsong.system.domain.SysTenant;

/**
 * 租户信息 数据层
 *
 * @author junsong
 */
public interface SysTenantMapper
{
    /**
     * 查询租户数据集合
     *
     * @param tenant 租户信息
     * @return 租户数据集合
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
     * 新增租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    public int insertTenant(SysTenant tenant);

    /**
     * 修改租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    public int updateTenant(SysTenant tenant);

    /**
     * 删除租户信息
     *
     * @param tenantId 租户ID
     * @return 结果
     */
    public int deleteTenantById(Long tenantId);

    /**
     * 校验租户名称
     *
     * @param tenantName 租户名称
     * @return 结果
     */
    public SysTenant checkTenantNameUnique(String tenantName);

    /**
     * 初始化租户数据：创建根部门
     */
    public int initTenantDept(Map<String, Object> params);

    /**
     * 初始化租户数据：创建管理员角色并关联所有菜单
     */
    public int initTenantAdminRole(Map<String, Object> params);

    /**
     * 初始化租户数据：创建普通角色
     */
    public int initTenantCommonRole(Map<String, Object> params);

    /**
     * 初始化租户数据：创建管理员用户
     */
    public int initTenantAdminUser(Map<String, Object> params);

    /**
     * 初始化租户数据：关联用户角色
     */
    public int initTenantUserRole(Map<String, Object> params);

    /**
     * 初始化租户数据：批量关联角色与所有菜单
     */
    public int initTenantRoleMenu(Map<String, Object> params);

    /**
     * 初始化租户数据：复制默认配置到新租户
     */
    public int initTenantConfig(Map<String, Object> params);
}
