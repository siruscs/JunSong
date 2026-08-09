package com.junsong.gateway.service.impl;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 验证码实现处理
 *
 * @author ruoyi
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService
{
    private static final Logger log = LoggerFactory.getLogger(ValidateCodeServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 验证码一次性复用窗口（分钟）。多部门用户首次登录后弹出部门选择，二次登录仍会带
     *  相同的 uuid/code。为了不立刻因为"key 已删"而报"验证码已失效"进入死循环，
     *  首次验证成功后改为覆盖写入同 code 值并设置此 TTL，窗口期内允许同 code 再通过一次。
     *  复用仍需严格 code 匹配；不同 code 一律拒绝，防止暴力破解。窗口建议 5 分钟，
     *  已远大于用户从"看到部门选择弹框 → 点击部门 → 进入工作台"的正常用时，
     *  且远短于 CAPTCHA_EXPIRATION（12 小时），避免长期暴露复用态。 */
    public static final int CAPTCHA_REUSE_WINDOW_MINUTES = 5;

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
            else if (captchaObj == null)
            {
                // Redis 缓存丢失（如 FLUSHDB 后），兜底查库避免配置失效
                if (isCaptchaDisabledFromRemote())
                {
                    captchaEnabled = false;
                }
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
        // 严格 code 匹配（大小写不敏感）；复用窗口期内也必须同 code，
        // 否则拒绝，防止"uuid 存在就过"的降级安全漏洞。
        if (!code.equalsIgnoreCase(captcha))
        {
            // 注意：失败时不要删除 key，否则用户第一次输错就连再输正确的机会都没有。
            // 允许用户在原本的 CAPTCHA_EXPIRATION 之内重试，直到 uuid 自然过期。
            throw new CaptchaException("验证码错误");
        }
        // 首次验证成功后，不立即删除 key。覆盖成相同 code 并重置一个更短的复用窗口，
        // 以支持多部门用户二次带 deptId 的登录请求继续通过。窗口结束后 key 自然过期，
        // 下一次必须刷新验证码重新获取，保留一次性语义的同时避免部门选择死循环。
        redisService.setCacheObject(verifyKey, code, (long) CAPTCHA_REUSE_WINDOW_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 验证码开关兜底：当 Redis 里 {@code sys_config:sys.account.captchaEnabled} 读为 null（
     * 缓存被清理 / 未初始化 / TTL 过期）时，同步查 system 模块的 HTTP configKey 接口，
     * 把结果写回 Redis，避免把"读不到"错误地当作"默认要验证码"。
     *
     * <p>返回 true 明确表示"业务开关 = 关闭，不需要图形验证码"；返回 false 表示
     * "保持启用"（接口失败或缺省一律 fail-closed 启用图形验证码，避免绕过）。
     *
     * <p>此方法供 {@code ValidateCodeFilter} 与 {@code createCaptcha} 共享，确保两者
     * 在缓存丢失时得到同一结果，避免"刷页面时 createCaptcha 说不用验证码、
     * 点登录时 filter 说要验证码"的前后不一致体验。
     *
     * @param redisService  注入的 RedisService（用于回写缓存）
     * @param log           调用方 Logger（方便记录是哪一侧触发的兜底）
     * @param callerHint    调用方提示，如 "ValidateCodeFilter" / "createCaptcha"
     * @return true = captcha 已被业务显式关闭（false）；否则 false
     */
    public static boolean captchaEnabledFallback(RedisService redisService, Logger log, String callerHint)
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://junsong-modules-system:9201/config/configKey/sys.account.captchaEnabled"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                if (log != null)
                {
                    log.warn("[{}] Redis 缓存丢失，远程查询验证码配置 HTTP {}，默认保持启用",
                            callerHint, response.statusCode());
                }
                return false;
            }
            JsonNode node = objectMapper.readTree(response.body());
            String value = node.has("data") ? node.get("data").asText() : null;
            boolean disabled = "false".equalsIgnoreCase(value);
            // 回写缓存：下次读 Redis 就不会再穿透了。值写成字符串形式，保证下次
            // captchaEnabledObj = redisService.getCacheObject(...) 得到的 Boolean 语义
            // 与 DB 一致。即使 DB 值为 true，写回去也比留着 null 强（避免反复走 HTTP）。
            if (redisService != null)
            {
                redisService.setCacheObject(
                        CacheConstants.SYS_CONFIG_KEY + "sys.account.captchaEnabled",
                        disabled ? "false" : "true");
            }
            return disabled;
        }
        catch (Exception e)
        {
            if (log != null)
            {
                log.warn("[{}] Redis 缓存丢失，远程查询验证码配置失败，默认保持开启: {}",
                        callerHint, e.getMessage());
            }
            return false;
        }
    }

    /**
     * Redis 缓存丢失时，兜底查库获取验证码开关配置。
     * 已重构为调用共享的 {@link #captchaEnabledFallback(RedisService, Logger, String)}。
     */
    private boolean isCaptchaDisabledFromRemote()
    {
        return captchaEnabledFallback(redisService, log, "createCaptcha");
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
}
