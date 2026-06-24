package com.junsong.gateway.service;

import java.io.IOException;
import com.junsong.common.core.exception.CaptchaException;
import com.junsong.common.core.web.domain.AjaxResult;

/**
 * 验证码处理
 *
 * @author ruoyi
 */
public interface ValidateCodeService
{
    /**
     * 生成验证码
     */
    public AjaxResult createCaptcha(String themeColor) throws IOException, CaptchaException;

    /**
     * 校验验证码
     */
    public void checkCaptcha(String key, String value) throws CaptchaException;
}
