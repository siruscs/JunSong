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
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.domain.SysUserDept;
import com.junsong.system.service.ISysUserDeptService;

/**
 * 用户与部门关联
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/userDept")
public class SysUserDeptController extends BaseController
{
    @Autowired
    private ISysUserDeptService userDeptService;

    /**
     * 获取用户与部门关联列表
     */
    @RequiresPermissions("system:userDept:list")
    @GetMapping("/list")
    public AjaxResult list(SysUserDept sysUserDept)
    {
        List<SysUserDept> list = userDeptService.selectSysUserDeptList(sysUserDept);
        return success(list);
    }

    /**
     * 根据用户ID获取关联的部门列表
     */
    @RequiresPermissions("system:userDept:query")
    @GetMapping("/user/{userId}")
    public AjaxResult getUserDeptList(@PathVariable Long userId)
    {
        List<SysUserDept> list = userDeptService.selectUserDeptByUserId(userId);
        return success(list);
    }

    /**
     * 根据用户ID和部门ID获取详细信息
     */
    @RequiresPermissions("system:userDept:query")
    @GetMapping(value = "/{userDeptId}")
    public AjaxResult getInfo(@PathVariable Long userDeptId)
    {
        return success(userDeptService.selectSysUserDeptByUserDeptId(userDeptId));
    }

    /**
     * 新增用户与部门关联
     */
    @RequiresPermissions("system:userDept:add")
    @Log(title = "用户与部门关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUserDept sysUserDept)
    {
        return toAjax(userDeptService.insertSysUserDept(sysUserDept));
    }

    /**
     * 修改用户与部门关联
     */
    @RequiresPermissions("system:userDept:edit")
    @Log(title = "用户与部门关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUserDept sysUserDept)
    {
        return toAjax(userDeptService.updateSysUserDept(sysUserDept));
    }

    /**
     * 删除用户与部门关联
     */
    @RequiresPermissions("system:userDept:remove")
    @Log(title = "用户与部门关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userDeptIds}")
    public AjaxResult remove(@PathVariable Long[] userDeptIds)
    {
        return toAjax(userDeptService.deleteSysUserDeptByUserDeptIds(userDeptIds));
    }

    /**
     * 用户入职部门
     */
    @RequiresPermissions("system:userDept:edit")
    @Log(title = "用户入职", businessType = BusinessType.UPDATE)
    @PostMapping("/hire")
    public AjaxResult hire(@RequestBody SysUserDept sysUserDept)
    {
        return toAjax(userDeptService.hireUserToDept(sysUserDept.getUserId(), sysUserDept.getDeptId()));
    }

    /**
     * 用户离职部门
     */
    @RequiresPermissions("system:userDept:edit")
    @Log(title = "用户离职", businessType = BusinessType.UPDATE)
    @PostMapping("/leave")
    public AjaxResult leave(@RequestBody SysUserDept sysUserDept)
    {
        return toAjax(userDeptService.leaveUserFromDept(sysUserDept.getUserId(), sysUserDept.getDeptId()));
    }

    /**
     * 批量新增用户部门关联
     */
    @RequiresPermissions("system:userDept:add")
    @Log(title = "用户与部门关联", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody List<SysUserDept> userDeptList)
    {
        return toAjax(userDeptService.batchUserDept(userDeptList));
    }

    @RequiresPermissions("system:userDept:list")
    @GetMapping("/staff/{deptId}")
    public AjaxResult staffList(@PathVariable Long deptId)
    {
        return success(userDeptService.selectStaffByDeptId(deptId));
    }
}
