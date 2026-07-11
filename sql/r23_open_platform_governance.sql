-- R23 开放平台治理基线：菜单、权限、日志字段与 DEV 安全配置
-- 不写入真实 AppSecret、真实伙伴数据或真实 Webhook URL。

SET @schema_name := DATABASE();

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND column_name = 'request_id'),
  'SELECT 1',
  'ALTER TABLE open_api_log ADD COLUMN request_id varchar(64) DEFAULT NULL COMMENT ''开放请求ID'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND column_name = 'error_code'),
  'SELECT 1',
  'ALTER TABLE open_api_log ADD COLUMN error_code varchar(64) DEFAULT NULL COMMENT ''开放错误码'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND column_name = 'status'),
  'SELECT 1',
  'ALTER TABLE open_api_log ADD COLUMN status varchar(32) DEFAULT NULL COMMENT ''调用状态'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND column_name = 'key_type'),
  'SELECT 1',
  'ALTER TABLE open_api_log ADD COLUMN key_type varchar(32) DEFAULT NULL COMMENT ''Key类型'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND column_name = 'response_message'),
  'SELECT 1',
  'ALTER TABLE open_api_log ADD COLUMN response_message varchar(512) DEFAULT NULL COMMENT ''响应摘要'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND index_name = 'idx_open_api_log_request_id'),
  'SELECT 1',
  'CREATE INDEX idx_open_api_log_request_id ON open_api_log(request_id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND index_name = 'idx_open_api_log_app_day'),
  'SELECT 1',
  'CREATE INDEX idx_open_api_log_app_day ON open_api_log(app_key, create_time)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'open_api_log' AND index_name = 'idx_open_api_log_status'),
  'SELECT 1',
  'CREATE INDEX idx_open_api_log_status ON open_api_log(status)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R23开放平台日额度开关', 'r23.openapi.dailyQuota.enabled', 'true', 'N', 'admin', NOW(), 'R23 DEV 默认启用日额度校验'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r23.openapi.dailyQuota.enabled');

INSERT INTO sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R23开放平台默认日额度', 'r23.openapi.dailyQuota.default', '100', 'N', 'admin', NOW(), '未配置 Key 额度时的 DEV 默认额度'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r23.openapi.dailyQuota.default');

SET @open_parent_id := (
  SELECT menu_id FROM sys_menu
  WHERE menu_name = '开放平台' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

SET @open_parent_id := IFNULL(@open_parent_id, GREATEST(2900, (SELECT IFNULL(MAX(menu_id), 0) + 100 FROM sys_menu)));

INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @open_parent_id, '开放平台', 0, 90, 'open', NULL, 1, 0, 'M', '0', '0', '', 'connection', 'admin', NOW(), 'R23开放平台治理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '开放平台' AND menu_type = 'M');

SET @log_menu_id := GREATEST(2910, (SELECT IFNULL(MAX(menu_id), 0) + 1 FROM sys_menu));
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @log_menu_id, '开放调用日志', @open_parent_id, 20, 'log', 'open/log/index', 1, 0, 'C', '0', '0', 'open:log:list', 'list', 'admin', NOW(), 'R23开放调用日志'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:log:list');

INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('open:log:list', 'open:app:list', 'open:app:key:list')
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);

-- ── Webhook 订阅表 ──────────────────────────────────

SET @ddl := IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = @schema_name AND table_name = 'open_webhook_subscription'),
  'SELECT 1',
  'CREATE TABLE open_webhook_subscription (
    id bigint NOT NULL AUTO_INCREMENT COMMENT ''主键'',
    tenant_id bigint DEFAULT NULL COMMENT ''租户ID'',
    app_id bigint DEFAULT NULL COMMENT ''应用ID'',
    callback_url varchar(512) NOT NULL COMMENT ''回调地址'',
    events varchar(1024) DEFAULT NULL COMMENT ''订阅事件列表(JSON)'',
    status char(1) DEFAULT ''0'' COMMENT ''状态(0正常 1停用)'',
    create_by varchar(64) DEFAULT NULL COMMENT ''创建者'',
    create_time datetime DEFAULT NULL COMMENT ''创建时间'',
    update_by varchar(64) DEFAULT NULL COMMENT ''更新者'',
    update_time datetime DEFAULT NULL COMMENT ''更新时间'',
    remark varchar(255) DEFAULT NULL COMMENT ''备注'',
    PRIMARY KEY (id),
    INDEX idx_webhook_sub_tenant_app (tenant_id, app_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''Webhook订阅记录'''
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
