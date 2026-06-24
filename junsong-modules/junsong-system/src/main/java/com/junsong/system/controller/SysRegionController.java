package com.junsong.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.junsong.system.domain.SysRegion;
import com.junsong.system.service.ISysRegionService;

@RestController
@RequestMapping("/region")
public class SysRegionController extends BaseController
{
    @Autowired
    private ISysRegionService regionService;

    @GetMapping("/tree")
    public AjaxResult tree()
    {
        return success(regionService.selectRegionTree());
    }

    @RequiresPermissions("system:region:list")
    @GetMapping("/children/{parentCode}")
    public AjaxResult children(@PathVariable String parentCode)
    {
        return success(regionService.selectChildrenByParentCode(parentCode));
    }

    @RequiresPermissions("system:region:query")
    @GetMapping(value = "/{code}")
    public AjaxResult getInfo(@PathVariable String code)
    {
        return success(regionService.selectRegionByCode(code));
    }

    @RequiresPermissions("system:region:add")
    @Log(title = "地址维护", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysRegion region)
    {
        return toAjax(regionService.insertRegion(region));
    }

    @RequiresPermissions("system:region:edit")
    @Log(title = "地址维护", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysRegion region)
    {
        return toAjax(regionService.updateRegion(region));
    }

    @RequiresPermissions("system:region:remove")
    @Log(title = "地址维护", businessType = BusinessType.DELETE)
    @DeleteMapping("/{code}")
    public AjaxResult remove(@PathVariable String code)
    {
        return toAjax(regionService.deleteRegionByCode(code));
    }
}
