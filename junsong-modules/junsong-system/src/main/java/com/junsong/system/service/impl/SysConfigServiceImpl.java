package com.junsong.system.service.impl;

import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.constant.CacheConstants;
import com.junsong.common.core.constant.UserConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.text.Convert;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.system.domain.SysConfig;
import com.junsong.system.mapper.SysConfigMapper;
import com.junsong.system.service.ISysConfigService;

/**
 * 参数配置 服务层实现
 * 
 * @author junsong
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService
{
    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     * 
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfig selectConfigById(Long configId)
    {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        return configMapper.selectConfig(config);
    }

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        String configValue = Convert.toStr(redisService.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue))
        {
            return configValue;
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNull(retConfig))
        {
            retConfig = selectPublicConfig(configKey);
        }
        if (StringUtils.isNotNull(retConfig))
        {
            redisService.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 查询公共配置（tenant_id=0）
     */
    private SysConfig selectPublicConfig(String configKey)
    {
        TenantContext.setIgnore(true);
        try
        {
            return configMapper.selectPublicConfig(configKey);
        }
        finally
        {
            TenantContext.setIgnore(false);
        }
    }

    /**
     * 查询参数配置列表
     * 
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config)
    {
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config)
    {
        int row = configMapper.insertConfig(config);
        if (row > 0)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 修改参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(SysConfig config)
    {
        SysConfig temp = configMapper.selectConfigById(config.getConfigId());
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey()))
        {
            redisService.deleteObject(getCacheKey(temp.getConfigKey()));
        }
        // 敏感配置前端返回的是脱敏占位符 "******"，此时保留 DB 里的原值，避免被误覆盖为占位符
        String effectiveKey = temp != null ? temp.getConfigKey() : config.getConfigKey();
        if (isSensitiveKey(effectiveKey) && MASKED_VALUE.equals(config.getConfigValue()) && temp != null)
        {
            config.setConfigValue(temp.getConfigValue());
        }

        int row = configMapper.updateConfig(config);
        if (row > 0)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 批量删除参数信息
     * 
     * @param configIds 需要删除的参数ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configMapper.deleteConfigById(configId);
            redisService.deleteObject(getCacheKey(config.getConfigKey()));
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        for (SysConfig config : configsList)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * 清空参数缓存数据
     *
     * <p>注意：对外暴露的 {@link #clearConfigCache()} 仍保留原来的"全量 SCAN 删除"语义，
     * 因为它是 {@link ISysConfigService} 公开接口契约，外部可能直接调用以强制清缓存。
     * 内部重置流程（见 {@link #resetConfigCache()}）已切换为"覆盖写 + 只删多余 key"，
     * 避免出现中间空窗口导致 sys_config 被误读为 null 并退回到默认值。
     */
    @Override
    public void clearConfigCache()
    {
        Collection<String> keys = redisService.scan(CacheConstants.SYS_CONFIG_KEY + "*");
        redisService.deleteObject(keys);
    }

    /**
     * 重置参数缓存数据
     *
     * <p>BUG1 修复：原先流程是 {@code clearConfigCache()} 全量 SCAN 清空 →
     * {@code loadingConfigCache()} 再写回，两步之间存在"读 null"窗口，
     * 对于任何直接从 Redis 取 {@code sys_config:*} 的消费者（如 gateway 的
     * {@code ValidateCodeFilter}）会把 null 退回到默认行为，
     * 典型现象是"参数表明明设 FALSE，但是刷新缓存的那一瞬间还需要图形验证码"，
     * 缓存压力大时甚至持续退不回来。
     *
     * <p>新流程改成覆盖式无窗口：
     * <ol>
     *   <li>先 {@code loadingConfigCache()}：按 DB 最新值覆盖 Redis 中已有的 key。
     *       这一步不会让任何一个已存在的 key 变成 null。</li>
     *   <li>再用 SCAN 扫出当前 Redis 里所有 {@code SYS_CONFIG_KEY + "*"} key，
     *       与 DB 中这一次加载的 key 做差集，只删掉"DB 已经不存在"的旧 key。</li>
     * </ol>
     *
     * <p>注意：由于系统参数条目总数很小（通常几十到几百条），在应用节点本地构建 keySet
     * 再做差集完全可接受；相比"先全删后写"的实现代价更低且完全规避空窗口。
     */
    @Override
    public void resetConfigCache()
    {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        // 1) 先覆盖写所有 DB 里仍存在的 key（原子覆盖，不暴露空窗口）。
        java.util.Set<String> dbKeys = new java.util.HashSet<>();
        for (SysConfig config : configsList)
        {
            String cacheKey = getCacheKey(config.getConfigKey());
            dbKeys.add(cacheKey);
            redisService.setCacheObject(cacheKey, config.getConfigValue());
        }
        // 2) 再扫出现有 Redis key，删除"DB 已经没有"的多余缓存。
        Collection<String> existingKeys = redisService.scan(CacheConstants.SYS_CONFIG_KEY + "*");
        if (existingKeys == null || existingKeys.isEmpty())
        {
            return;
        }
        java.util.List<String> staleKeys = new java.util.ArrayList<>(existingKeys.size());
        for (String k : existingKeys)
        {
            if (!dbKeys.contains(k))
            {
                staleKeys.add(k);
            }
        }
        if (!staleKeys.isEmpty())
        {
            redisService.deleteObject(staleKeys);
        }
    }

    /**
     * 校验参数键名是否唯一
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询指定租户是否启用微信登录
     * 读取 mp.wechat.login.enabled 参数，按 tenant_id 隔离。
     * 参数缺失、非法值或读取异常时一律返回 false（fail-closed）。
     *
     * @param tenantId 租户ID（null 时使用当前租户上下文）
     * @return true=已启用 false=未启用或读取失败
     */
    @Override
    public boolean isWechatLoginEnabled(Long tenantId)
    {
        Long originalTenantId = TenantContext.getTenantId();
        boolean tenantChanged = false;
        if (tenantId != null && !tenantId.equals(originalTenantId))
        {
            TenantContext.setTenantId(tenantId);
            tenantChanged = true;
        }
        try
        {
            String value = selectConfigByKey("mp.wechat.login.enabled");
            return "true".equalsIgnoreCase(value);
        }
        catch (Exception e)
        {
            // fail-closed：读取异常时返回 false
            return false;
        }
        finally
        {
            if (tenantChanged)
            {
                TenantContext.setTenantId(originalTenantId);
            }
        }
    }

    /**
     * 设置cache key（多租户隔离）
     * 默认租户(tenant_id=1)使用旧格式，兼容网关等无租户上下文的服务
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.equals(TenantContext.DEFAULT_TENANT_ID))
        {
            return CacheConstants.SYS_CONFIG_KEY + configKey;
        }
        return CacheConstants.SYS_CONFIG_KEY + tenantId + ":" + configKey;
    }

    /** 脱敏占位符 */
    private static final String MASKED_VALUE = "******";

    /** 敏感关键词（全小写子串匹配） */
    private static final String[] SENSITIVE_KEYWORDS = { "password", "secret", "token", "credential" };

    /**
     * 判断配置键是否包含敏感含义。
     * 子串匹配 password/secret/token/credential，段匹配 "key"（避免 keyboard 等误报）。
     */
    public static boolean isSensitiveKey(String configKey)
    {
        if (configKey == null)
        {
            return false;
        }
        String lower = configKey.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS)
        {
            if (lower.contains(keyword))
            {
                return true;
            }
        }
        // "key" 按段匹配：开头 key.、中间 .key.、结尾 .key、或整个字符串就是 key
        if (lower.equals("key") || lower.startsWith("key.") || lower.endsWith(".key")
                || lower.contains(".key.") || lower.startsWith("key_") || lower.endsWith("_key")
                || lower.contains("_key_"))
        {
            return true;
        }
        return false;
    }

    /**
     * 返回脱敏后的配置值。敏感键返回 "******"，否则返回原值。
     */
    public static String maskConfigValue(String configKey, String configValue)
    {
        return isSensitiveKey(configKey) ? MASKED_VALUE : configValue;
    }
}
