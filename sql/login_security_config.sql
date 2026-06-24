-- 系统参数：单点登录、禁止保存密码、验证码开关
-- 执行日期: 2026-06-06

-- PC端单点登录开关
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('PC端单点登录', 'sys.login.singleLogin', 'true', 'Y', 'admin', NOW(), '开启后同一账号PC端只允许一个在线，新登录踢掉旧登录');

-- 登录页禁止保存密码开关
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('登录页禁止保存密码', 'sys.login.preventSavePassword', 'true', 'Y', 'admin', NOW(), '开启后登录页清空浏览器自动填充的密码，阻止密码管理器保存');

-- 登录验证码开关
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES ('登录验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', NOW(), '是否开启登录验证码（true开启/false关闭）');
