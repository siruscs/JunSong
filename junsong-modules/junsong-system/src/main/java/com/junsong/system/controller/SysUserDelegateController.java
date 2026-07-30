package com.junsong.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysUserDelegate;
import com.junsong.system.service.ISysUserDelegateService;
import com.junsong.common.security.annotation.RequiresPermissions;

/**
 * 用户委托代理 控制器
 */
@RestController
@RequestMapping("/delegate")
public class SysUserDelegateController extends BaseController
{
    @Autowired
    private ISysUserDelegateService delegateService;

    /**
     * 查询委托代理列表
     */
    @RequiresPermissions("system:delegate:list")
    @GetMapping("/list")
    public TableDataInfo list(SysUserDelegate delegate)
    {
        startPage();
        List<SysUserDelegate> list = delegateService.selectList(delegate);
        return getDataTable(list);
    }

    /**
     * 获取当前用户的委托规则
     */
    @RequiresPermissions("system:delegate:query")
    @GetMapping("/my")
    public AjaxResult myDelegates()
    {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(delegateService.selectActiveByUserId(userId));
    }

    /**
     * 获取当前用户作为代理人的委托规则
     */
    @RequiresPermissions("system:delegate:query")
    @GetMapping("/agent")
    public AjaxResult myAgentTasks()
    {
        Long userId = SecurityUtils.getUserId();
        return AjaxResult.success(delegateService.selectActiveByDelegateUserId(userId));
    }

    /**
     * 新增委托代理
     */
    @RequiresPermissions("system:delegate:add")
    @Idempotent(scene = "system:user-delegate:add")
    @PostMapping
    public AjaxResult add(@RequestBody SysUserDelegate delegate)
    {
        delegate.setUserId(SecurityUtils.getUserId());
        return toAjax(delegateService.insert(delegate));
    }

    /**
     * 修改委托代理
     */
    @RequiresPermissions("system:delegate:edit")
    @Idempotent(scene = "system:user-delegate:edit")
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserDelegate delegate)
    {
        return toAjax(delegateService.update(delegate));
    }

    /**
     * 删除委托代理
     */
    @RequiresPermissions("system:delegate:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(delegateService.deleteByIds(ids));
    }
}
