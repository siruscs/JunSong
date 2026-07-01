package com.junsong.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.UserConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysTenant;
import com.junsong.system.mapper.SysTenantMapper;
import com.junsong.system.service.ISysTenantService;

/**
 * 租户信息 服务层处理
 *
 * @author junsong
 */
@Service
public class SysTenantServiceImpl implements ISysTenantService
{
    @Autowired
    private SysTenantMapper tenantMapper;

    /**
     * 查询租户信息集合
     *
     * @param tenant 租户信息
     * @return 租户信息集合
     */
    @Override
    public List<SysTenant> selectTenantList(SysTenant tenant)
    {
        return tenantMapper.selectTenantList(tenant);
    }

    /**
     * 通过租户ID查询租户信息
     *
     * @param tenantId 租户ID
     * @return 租户对象信息
     */
    @Override
    public SysTenant selectTenantByTenantId(Long tenantId)
    {
        return tenantMapper.selectTenantByTenantId(tenantId);
    }

    /**
     * 校验租户名称是否唯一
     *
     * @param tenant 租户信息
     * @return 结果
     */
    @Override
    public boolean checkTenantNameUnique(SysTenant tenant)
    {
        Long tenantId = StringUtils.isNull(tenant.getTenantId()) ? -1L : tenant.getTenantId();
        SysTenant info = tenantMapper.checkTenantNameUnique(tenant.getTenantName());
        if (StringUtils.isNotNull(info) && info.getTenantId().longValue() != tenantId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 删除租户信息
     *
     * @param tenantId 租户ID
     * @return 结果
     */
    @Override
    public int deleteTenantById(Long tenantId)
    {
        return tenantMapper.deleteTenantById(tenantId);
    }

    /**
     * 新增保存租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    @Override
    public int insertTenant(SysTenant tenant)
    {
        return tenantMapper.insertTenant(tenant);
    }

    /**
     * 修改保存租户信息
     *
     * @param tenant 租户信息
     * @return 结果
     */
    @Override
    public int updateTenant(SysTenant tenant)
    {
        return tenantMapper.updateTenant(tenant);
    }

    /**
     * 创建租户并初始化基础数据（部门、角色、管理员用户）
     *
     * @param tenant 租户信息
     * @param adminUserName 管理员用户名
     * @param adminPassword 管理员密码（明文，内部加密）
     * @return 创建的租户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTenantWithInit(SysTenant tenant, String adminUserName, String adminPassword)
    {
        // 1. 插入租户记录（sys_tenant 是共享表，跳过租户拦截器）
        TenantContext.setIgnore(true);
        try
        {
            tenantMapper.insertTenant(tenant);
        }
        finally
        {
            TenantContext.setIgnore(false);
        }
        Long tenantId = tenant.getTenantId();

        // 2. 初始化部门
        Map<String, Object> deptParams = new HashMap<>();
        deptParams.put("tenantId", tenantId);
        deptParams.put("tenantName", tenant.getTenantName());
        tenantMapper.initTenantDept(deptParams);
        Long deptId = ((Number) deptParams.get("deptId")).longValue();

        // 3. 初始化管理员角色
        Map<String, Object> roleParams = new HashMap<>();
        roleParams.put("tenantId", tenantId);
        roleParams.put("roleName", "管理员");
        tenantMapper.initTenantAdminRole(roleParams);
        Long adminRoleId = ((Number) roleParams.get("roleId")).longValue();

        // 3.5 关联管理员角色与所有菜单
        Map<String, Object> rmParams = new HashMap<>();
        rmParams.put("tenantId", tenantId);
        rmParams.put("roleId", adminRoleId);
        tenantMapper.initTenantRoleMenu(rmParams);

        // 4. 初始化普通角色
        Map<String, Object> commonRoleParams = new HashMap<>();
        commonRoleParams.put("tenantId", tenantId);
        commonRoleParams.put("roleName", "普通角色");
        tenantMapper.initTenantCommonRole(commonRoleParams);

        // 5. 初始化管理员用户
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("tenantId", tenantId);
        userParams.put("deptId", deptId);
        userParams.put("userName", adminUserName);
        userParams.put("password", SecurityUtils.encryptPassword(adminPassword));
        userParams.put("nickName", tenant.getContactName() != null ? tenant.getContactName() : adminUserName);
        tenantMapper.initTenantAdminUser(userParams);
        Long userId = ((Number) userParams.get("userId")).longValue();

        // 6. 关联用户与管理员角色
        Map<String, Object> urParams = new HashMap<>();
        urParams.put("tenantId", tenantId);
        urParams.put("userId", userId);
        urParams.put("roleId", adminRoleId);
        tenantMapper.initTenantUserRole(urParams);

        // 7. 复制默认配置（验证码开关、皮肤等）
        Map<String, Object> configParams = new HashMap<>();
        configParams.put("tenantId", tenantId);
        tenantMapper.initTenantConfig(configParams);

        return tenantId;
    }
}
