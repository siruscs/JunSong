package com.junsong.finance.controller;

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
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinDeptProfitConfig;
import com.junsong.finance.service.IFinDeptProfitConfigService;

@RestController
@RequestMapping("/deptProfitConfig")
public class FinDeptProfitConfigController extends BaseController
{
    @Autowired
    private IFinDeptProfitConfigService finDeptProfitConfigService;

    @RequiresPermissions("finance:deptProfitConfig:list")
    @GetMapping("/list")
    public TableDataInfo list(FinDeptProfitConfig finDeptProfitConfig)
    {
        startPage();
        List<FinDeptProfitConfig> list = finDeptProfitConfigService.selectFinDeptProfitConfigList(finDeptProfitConfig);
        return getDataTable(list);
    }

    @RequiresPermissions("finance:deptProfitConfig:query")
    @GetMapping("/{configId}")
    public AjaxResult getInfo(@PathVariable Long configId)
    {
        return success(finDeptProfitConfigService.selectFinDeptProfitConfigByConfigId(configId));
    }

    @RequiresPermissions("finance:deptProfitConfig:query")
    @GetMapping("/dept/{deptId}")
    public AjaxResult getByDeptId(@PathVariable Long deptId)
    {
        return success(finDeptProfitConfigService.selectFinDeptProfitConfigByDeptId(deptId));
    }

    @RequiresPermissions("finance:deptProfitConfig:add")
    @Log(title = "店面分润配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody FinDeptProfitConfig finDeptProfitConfig)
    {
        finDeptProfitConfig.setCreateBy(SecurityUtils.getUsername());
        return toAjax(finDeptProfitConfigService.insertFinDeptProfitConfig(finDeptProfitConfig));
    }

    @RequiresPermissions("finance:deptProfitConfig:edit")
    @Log(title = "店面分润配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody FinDeptProfitConfig finDeptProfitConfig)
    {
        finDeptProfitConfig.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(finDeptProfitConfigService.updateFinDeptProfitConfig(finDeptProfitConfig));
    }

    @RequiresPermissions("finance:deptProfitConfig:remove")
    @Log(title = "店面分润配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        return toAjax(finDeptProfitConfigService.deleteFinDeptProfitConfigByConfigIds(configIds));
    }
}
