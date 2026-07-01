package com.junsong.open.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenAppSecretService;

/**
 * 开放平台内部接口(供网关调用)
 *
 * @author junsong
 */
@RestController
@RequestMapping("/internal/secret")
public class OpenInternalController extends BaseController
{
    @Autowired
    private IOpenAppSecretService openAppSecretService;

    /**
     * 根据AppKey查询Secret信息(供网关验签使用)
     */
    @GetMapping("/byKey/{appKey}")
    public AjaxResult getByAppKey(@PathVariable("appKey") String appKey)
    {
        OpenAppSecret secret = openAppSecretService.selectByAppKey(appKey);
        if (secret == null)
        {
            return AjaxResult.error("AppKey不存在");
        }
        if (!"0".equals(secret.getStatus()))
        {
            return AjaxResult.error("AppKey已被禁用");
        }
        return AjaxResult.success(secret);
    }
}
