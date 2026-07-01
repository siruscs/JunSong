package com.junsong.open.controller.openapi;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenAppSecretService;
import com.junsong.open.service.IOpenAppService;

/**
 * 平台基础能力(Foundation as a Service)开放API
 *
 * 对外暴露应用查询和Key概览能力。
 * 注意：此控制器提供公共API路径(/apps)，区别于管理后台路径(/app)。
 *
 * @author junsong
 */
@RestController
@RequestMapping("/open/apps")
public class OpenFoundationController extends BaseController
{
    @Autowired
    private IOpenAppService openAppService;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    /**
     * 查询授权应用基础信息（只返回当前租户可见应用）
     */
    @GetMapping
    @RequiresPermissions("open:foundation:app:list")
    public AjaxResult listOpenApps()
    {
        OpenApp query = new OpenApp();
        List<OpenApp> list = openAppService.selectOpenAppList(query);
        return success(list);
    }

    /**
     * 查询应用Key概览（敏感字段不通过公共接口返回）
     */
    @GetMapping("/{appId}/keys")
    @RequiresPermissions("open:foundation:key:list")
    public AjaxResult listAppKeys(@PathVariable("appId") Long appId)
    {
        List<OpenAppSecret> keys = openAppSecretService.selectKeysByAppId(appId);
        // 脱敏处理：移除secret字段
        keys.forEach(key -> key.setAppSecret(null));
        return success(keys);
    }
}
