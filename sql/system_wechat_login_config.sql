SET NAMES utf8mb4;

-- ============================================================================
-- 微信小程序登录开关：租户级参数 mp.wechat.login.enabled
-- 默认值 false（tenant_id=0 公共配置），已有租户不会被默认开启
-- 各租户可在 PC 系统参数页面将 config_value 改为 true 以启用微信登录入口
-- 重复执行安全：使用 INSERT ... SELECT ... WHERE NOT EXISTS 保证幂等
-- ============================================================================

INSERT INTO sys_config (tenant_id, config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT
    0,
    '微信登录开关',
    'mp.wechat.login.enabled',
    'false',
    'Y',
    'system',
    NOW(),
    '小程序微信快捷登录入口开关。默认关闭(false)；各租户可在系统参数页面改为 true 启用。'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config
    WHERE tenant_id = 0 AND config_key = 'mp.wechat.login.enabled'
);

-- ============================================================================
-- 校验输出
-- ============================================================================
SELECT '=== 微信登录开关配置校验 ===' AS info;

SELECT
    config_id,
    tenant_id,
    config_name,
    config_key,
    config_value,
    config_type,
    remark
FROM sys_config
WHERE config_key = 'mp.wechat.login.enabled'
ORDER BY tenant_id;

SELECT
    COUNT(*) AS total_wechat_login_config,
    SUM(CASE WHEN tenant_id = 0 THEN 1 ELSE 0 END) AS public_config_count,
    SUM(CASE WHEN tenant_id = 0 AND config_value = 'true' THEN 1 ELSE 0 END) AS public_enabled_count,
    SUM(CASE WHEN tenant_id = 0 AND config_value = 'false' THEN 1 ELSE 0 END) AS public_disabled_count
FROM sys_config
WHERE config_key = 'mp.wechat.login.enabled';
