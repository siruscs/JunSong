-- 修复 Nacos 控制台「配置列表为空」：补全 public 命名空间元数据
-- 在 MySQL 已有 config_info 数据但控制台看不到时使用
USE `junsong-config`;

INSERT INTO tenant_info (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
SELECT '1', '', 'public', 'Public Namespace', 'nacos',
       UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tenant_info WHERE tenant_id = '' LIMIT 1);

INSERT INTO tenant_info (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
SELECT '1', 'public', 'public', 'Public Namespace', 'nacos',
       UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tenant_info WHERE tenant_id = 'public' LIMIT 1);
