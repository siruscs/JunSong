-- =====================================================================
-- sys_config 参数表补数脚本（PROD 缺行修复）
-- 生成日期: 2026-07-01
--
-- 用途：修复 PROD 环境 sys_config 相对 DEV/代码基线缺失的参数行。
-- 安全原则：
--   1) 只补"缺失的 config_key"，绝不覆盖 PROD 已存在的行/值（用 NOT EXISTS 守卫）。
--   2) 不指定 config_id，由自增分配，避免与 PROD 已有主键冲突。
--   3) 幂等：可重复执行，已存在则跳过。
--   4) 本脚本不含任何 UPDATE/DELETE，不修改现有配置。
--
-- 执行前务必：
--   - 先跑【第一段：审计】确认到底缺哪些行，再决定是否执行【第二段：补数】。
--   - 在 PROD 执行前先备份 sys_config：
--       CREATE TABLE sys_config_bak_20260701 AS SELECT * FROM sys_config;
--   - 对安全敏感项（captchaEnabled/singleLogin/preventSavePassword）的默认值，
--     请结合贵司安全策略人工确认后再执行（见下方注释标注）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 第一段：审计对比（只读）——列出代码基线要求、但 PROD 缺失的 config_key
-- 直接执行本段查看缺行清单；结果为空表示 PROD 参数完整。
-- ---------------------------------------------------------------------
SELECT expected.config_key, expected.config_name, expected.default_value
FROM (
    SELECT 'sys.index.skinName'                 AS config_key, '主框架页-默认皮肤样式名称'     AS config_name, 'skin-blue'  AS default_value UNION ALL
    SELECT 'sys.user.initPassword',                             '用户管理-账号初始密码',         '123456'      UNION ALL
    SELECT 'sys.index.sideTheme',                               '主框架页-侧边栏主题',           'theme-dark'  UNION ALL
    SELECT 'sys.account.registerUser',                          '账号自助-是否开启用户注册功能', 'false'       UNION ALL
    SELECT 'sys.login.blackIPList',                             '用户登录-黑名单列表',           ''            UNION ALL
    SELECT 'sys.account.initPasswordModify',                    '用户管理-初始密码修改策略',     '1'           UNION ALL
    SELECT 'sys.account.passwordValidateDays',                  '用户管理-账号密码更新周期',     '0'           UNION ALL
    SELECT 'sys.account.chrtype',                               '用户管理-密码字符范围',         '0'           UNION ALL
    SELECT 'sys.login.singleLogin',                             'PC端单点登录',                  'false'       UNION ALL
    SELECT 'sys.login.preventSavePassword',                     '登录页禁止保存密码',            'false'       UNION ALL
    SELECT 'sys.account.captchaEnabled',                        '登录验证码开关',                'true'        UNION ALL
    SELECT 'sys.account.registerIdCardRequired',                '注册-身份证必填',               'false'       UNION ALL
    SELECT 'sys.account.registerInviteCode',                    '注册-需要邀请码',               'false'       UNION ALL
    SELECT 'sys.account.registerNeedAudit',                     '注册-新用户需审核',             'true'
) AS expected
LEFT JOIN sys_config c ON c.config_key = expected.config_key
WHERE c.config_id IS NULL;

-- ---------------------------------------------------------------------
-- 第二段：幂等补数（仅插入缺失行，不覆盖已有值）
-- 审计确认后执行。每条均带 NOT EXISTS 守卫。
-- ---------------------------------------------------------------------

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'system_backfill', NOW(),
       '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.index.skinName');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'system_backfill', NOW(),
       '初始化密码 123456'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.user.initPassword');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'system_backfill', NOW(),
       '深色主题theme-dark，浅色主题theme-light'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.index.sideTheme');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'system_backfill', NOW(),
       '是否开启注册用户功能（true开启，false关闭）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.registerUser');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'system_backfill', NOW(),
       '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.login.blackIPList');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '1', 'Y', 'system_backfill', NOW(),
       '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.initPasswordModify');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'system_backfill', NOW(),
       '密码更新周期（0不限制，1-365正整数），超过周期登录时提醒修改密码'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.passwordValidateDays');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'system_backfill', NOW(),
       '0任意 1数字 2字母 3字母和数字 4字母数字和特殊字符'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.chrtype');

-- 安全敏感项：默认 false（沿用最新全量 init 基线），如需开启单点登录请人工确认
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'PC端单点登录', 'sys.login.singleLogin', 'false', 'Y', 'system_backfill', NOW(),
       '开启后同一账号PC端只允许一个在线，新登录踢掉旧登录'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.login.singleLogin');

-- 安全敏感项：默认 false，如需禁止浏览器保存密码请人工确认改 true
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '登录页禁止保存密码', 'sys.login.preventSavePassword', 'false', 'Y', 'system_backfill', NOW(),
       '开启后登录页清空浏览器自动填充的密码，阻止密码保存'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.login.preventSavePassword');

-- 安全敏感项：默认 true（开启验证码更安全），如需关闭请人工确认
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '登录验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'system_backfill', NOW(),
       '是否开启登录验证码（true开启/false关闭）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.captchaEnabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '注册-身份证必填', 'sys.account.registerIdCardRequired', 'false', 'Y', 'system_backfill', NOW(),
       '注册时是否必填身份证号（true必填 false选填）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.registerIdCardRequired');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '注册-需要邀请码', 'sys.account.registerInviteCode', 'false', 'Y', 'system_backfill', NOW(),
       '注册时是否需要邀请码（true需要 false不需要）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.registerInviteCode');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '注册-新用户需审核', 'sys.account.registerNeedAudit', 'true', 'Y', 'system_backfill', NOW(),
       '新注册用户是否需要管理员审核（true需要 false直接激活）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.account.registerNeedAudit');

-- ---------------------------------------------------------------------
-- 第三段：执行后校验（只读）——应返回 14，表示基线参数齐全
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS baseline_config_count
FROM sys_config
WHERE config_key IN (
    'sys.index.skinName','sys.user.initPassword','sys.index.sideTheme','sys.account.registerUser',
    'sys.login.blackIPList','sys.account.initPasswordModify','sys.account.passwordValidateDays','sys.account.chrtype',
    'sys.login.singleLogin','sys.login.preventSavePassword','sys.account.captchaEnabled',
    'sys.account.registerIdCardRequired','sys.account.registerInviteCode','sys.account.registerNeedAudit'
);

-- 提示：修改 sys_config 后，需在系统管理后台"刷新缓存"或重启相关服务，
--       使 Redis 中的参数缓存（sys_config:*）与数据库同步生效。
