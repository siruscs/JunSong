package com.junsong.open.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenAppSecretService;
import com.junsong.open.service.IOpenAppService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 开放平台应用管理Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/app")
public class OpenAppController extends BaseController
{
    @Autowired
    private IOpenAppService openAppService;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    @GetMapping("/list")
    @RequiresPermissions("open:app:list")
    public TableDataInfo list(OpenApp openApp)
    {
        startPage();
        List<OpenApp> list = openAppService.selectOpenAppList(openApp);
        return getDataTable(list);
    }

    @PostMapping("/export")
    @RequiresPermissions("open:app:export")
    public void export(HttpServletResponse response, OpenApp openApp)
    {
        List<OpenApp> list = openAppService.selectOpenAppList(openApp);
        ExcelUtil<OpenApp> util = new ExcelUtil<>(OpenApp.class);
        util.exportExcel(response, list, "开放平台应用数据");
    }

    @GetMapping("/{id}")
    @RequiresPermissions("open:app:query")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(openAppService.selectOpenAppById(id));
    }

    @PostMapping
    @RequiresPermissions("open:app:add")
    public AjaxResult add(@RequestBody OpenApp openApp)
    {
        openApp.setCreateBy(SecurityUtils.getUsername());
        return toAjax(openAppService.insertOpenApp(openApp));
    }

    @PutMapping
    @RequiresPermissions("open:app:edit")
    public AjaxResult edit(@RequestBody OpenApp openApp)
    {
        openApp.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(openAppService.updateOpenApp(openApp));
    }

    @DeleteMapping("/{ids}")
    @RequiresPermissions("open:app:remove")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(openAppService.deleteOpenAppByIds(ids));
    }

    @PutMapping("/approve/{appId}")
    @RequiresPermissions("open:app:approve")
    public AjaxResult approve(@PathVariable("appId") Long appId)
    {
        return toAjax(openAppService.approveApp(appId, SecurityUtils.getUsername()));
    }

    @PutMapping("/reject/{appId}")
    @RequiresPermissions("open:app:approve")
    public AjaxResult reject(@PathVariable("appId") Long appId, @RequestParam String rejectReason)
    {
        return toAjax(openAppService.rejectApp(appId, rejectReason, SecurityUtils.getUsername()));
    }

    @GetMapping("/keys/{appId}")
    @RequiresPermissions("open:app:key:list")
    public AjaxResult listKeys(@PathVariable("appId") Long appId)
    {
        return success(openAppSecretService.selectKeysByAppId(appId));
    }

    @GetMapping("/keys/list")
    @RequiresPermissions("open:app:key:list")
    public TableDataInfo listAllKeys(OpenAppSecret openAppSecret)
    {
        startPage();
        List<OpenAppSecret> list = openAppSecretService.selectOpenAppSecretList(openAppSecret);
        return getDataTable(list);
    }

    @PutMapping("/keys/changeStatus")
    @RequiresPermissions("open:app:key:edit")
    public AjaxResult changeKeyStatus(@RequestBody OpenAppSecret openAppSecret)
    {
        return toAjax(openAppSecretService.changeStatus(openAppSecret));
    }
}
