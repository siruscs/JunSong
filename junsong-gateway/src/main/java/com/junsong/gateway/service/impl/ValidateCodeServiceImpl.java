package com.junsong.gateway.service.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import com.google.code.kaptcha.Producer;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.Constants;
import com.junsong.common.core.exception.CaptchaException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.uuid.IdUtils;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.redis.service.RedisService;
import com.junsong.gateway.config.properties.CaptchaProperties;
import com.junsong.gateway.service.ValidateCodeService;

/**
 * 验证码实现处理
 *
 * @author ruoyi
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CaptchaProperties captchaProperties;

    /**
     * 生成验证码
     */
    @Override
    public AjaxResult createCaptcha(String themeColor) throws IOException, CaptchaException
    {
        AjaxResult ajax = AjaxResult.success();

        boolean captchaEnabled = captchaProperties.getEnabled();
        // 如果 Nacos 配置开启，再检查 sys_config 参数
        if (captchaEnabled)
        {
            Object captchaObj = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.captchaEnabled");
            if (isFalse(captchaObj))
            {
                captchaEnabled = false;
            }
        }

        if (!captchaEnabled)
        {
            ajax.put("captchaEnabled", false);
            Object pspObj = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.login.preventSavePassword");
            ajax.put("preventSavePassword", isTrue(pspObj));
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        String captchaType = captchaProperties.getType();
        // 生成验证码
        if ("math".equals(captchaType))
        {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }
        else if ("char".equals(captchaType))
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }
        image = tintCaptchaImage(image, themeColor);

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.getEncoder().encodeToString(os.toByteArray()));
        ajax.put("captchaEnabled", true);
        Object pspObj2 = redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.login.preventSavePassword");
        ajax.put("preventSavePassword", isTrue(pspObj2));
        return ajax;
    }

    private BufferedImage tintCaptchaImage(BufferedImage source, String themeColor)
    {
        Color target = parseThemeColor(themeColor);
        if (source == null || target == null)
        {
            return source;
        }
        float[] targetHsb = Color.RGBtoHSB(target.getRed(), target.getGreen(), target.getBlue(), null);
        BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++)
        {
            for (int x = 0; x < source.getWidth(); x++)
            {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                if (hsb[1] > 0.18f)
                {
                    float saturation = Math.max(0.52f, Math.min(0.9f, Math.max(hsb[1], targetHsb[1])));
                    float brightness = Math.max(0.28f, Math.min(0.94f, hsb[2]));
                    rgb = Color.HSBtoRGB(targetHsb[0], saturation, brightness);
                }
                tinted.setRGB(x, y, rgb);
            }
        }
        return tinted;
    }

    private Color parseThemeColor(String themeColor)
    {
        if (StringUtils.isEmpty(themeColor))
        {
            return null;
        }
        String normalized = themeColor.trim();
        if (normalized.startsWith("#"))
        {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[0-9a-fA-F]{6}"))
        {
            return null;
        }
        return new Color(Integer.parseInt(normalized, 16));
    }

    private boolean isFalse(Object value)
    {
        return "false".equalsIgnoreCase(normalizeBooleanValue(value));
    }

    private boolean isTrue(Object value)
    {
        return "true".equalsIgnoreCase(normalizeBooleanValue(value));
    }

    private String normalizeBooleanValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = value.toString().trim();
        while (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\""))
        {
            text = text.substring(1, text.length() - 1).trim();
        }
        text = text.replace("\\n", "").replace("\\r", "");
        return text.trim();
    }

    /**
     * 校验验证码
     */
    @Override
    public void checkCaptcha(String code, String uuid) throws CaptchaException
    {
        if (StringUtils.isEmpty(code))
        {
            throw new CaptchaException("验证码不能为空");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisService.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaException("验证码已失效");
        }
        redisService.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException("验证码错误");
        }
    }
}
