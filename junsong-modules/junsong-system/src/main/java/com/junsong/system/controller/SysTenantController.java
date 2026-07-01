package com.junsong.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysTenant;
import com.junsong.system.service.ISysTenantService;

/**
 * 租户信息操作处理
 *
 * @author junsong
 */
@RestController
@RequestMapping("/tenant")
public class SysTenantController extends BaseController
{
    @Autowired
    private ISysTenantService tenantService;

    /**
     * 获取租户列表
     */
    @RequiresPermissions("system:tenant:list")
    @GetMapping("/list")
    public TableDataInfo list(SysTenant tenant)
    {
        startPage();
        List<SysTenant> list = tenantService.selectTenantList(tenant);
        return getDataTable(list);
    }

    /**
     * 根据租户编号获取详细信息
     */
    @RequiresPermissions("system:tenant:query")
    @GetMapping(value = "/{tenantId}")
    public AjaxResult getInfo(@PathVariable Long tenantId)
    {
        return success(tenantService.selectTenantByTenantId(tenantId));
    }

    /**
     * 新增租户
     */
    @RequiresPermissions("system:tenant:add")
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysTenant tenant)
    {
        if (!tenantService.checkTenantNameUnique(tenant))
        {
            return error("新增租户'" + tenant.getTenantName() + "'失败，租户名称已存在");
        }
        if (StringUtils.isEmpty(tenant.getAdminUserName()))
        {
            return error("管理员账号不能为空");
        }
        if (StringUtils.isEmpty(tenant.getAdminPassword()))
        {
            return error("管理员密码不能为空");
        }
        Long tenantId = tenantService.createTenantWithInit(tenant, tenant.getAdminUserName(), tenant.getAdminPassword());
        return success(tenantId);
    }

    /**
     * 修改租户
     */
    @RequiresPermissions("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysTenant tenant)
    {
        if (!tenantService.checkTenantNameUnique(tenant))
        {
            return error("修改租户'" + tenant.getTenantName() + "'失败，租户名称已存在");
        }
        tenant.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(tenantService.updateTenant(tenant));
    }

    /**
     * 删除租户
     */
    @RequiresPermissions("system:tenant:remove")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tenantId}")
    public AjaxResult remove(@PathVariable Long tenantId)
    {
        return toAjax(tenantService.deleteTenantById(tenantId));
    }

    /**
     * 修改租户状态
     */
    @RequiresPermissions("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysTenant tenant)
    {
        tenant.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(tenantService.updateTenant(tenant));
    }
}
