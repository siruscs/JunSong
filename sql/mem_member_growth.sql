-- mem_member_growth.sql
-- 会员成长体系数据库脚本（幂等，可在 DEV/PROD 安全执行）
-- 包含：字段扩展 + 新表 + 默认数据 + 菜单权限 + 字典数据
-- 注意：首行 SET NAMES 确保中文注释不乱码

SET NAMES utf8mb4;

-- ============================================================
-- 1. 扩展 mem_member 表
-- ============================================================

-- 1.1 补列 growth_value
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member' AND COLUMN_NAME = 'growth_value');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member ADD COLUMN growth_value BIGINT NOT NULL DEFAULT 0 COMMENT ''成长值''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 补列 last_active_time
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member' AND COLUMN_NAME = 'last_active_time');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member ADD COLUMN last_active_time DATETIME DEFAULT NULL COMMENT ''最后活跃时间''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 扩展 mem_member_card_type 表
-- ============================================================

-- 2.1 补列 tenant_id（如不存在）
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_card_type' AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_card_type ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.2 补列 min_growth
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_card_type' AND COLUMN_NAME = 'min_growth');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_card_type ADD COLUMN min_growth BIGINT NOT NULL DEFAULT 0 COMMENT ''升级所需最低成长值''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.3 补列 sign_in_points（各等级单次签到奖励积分）
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_card_type' AND COLUMN_NAME = 'sign_in_points');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_card_type ADD COLUMN sign_in_points DECIMAL(12,2) NOT NULL DEFAULT 1.00 COMMENT ''单次签到奖励积分''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.4 更新默认门槛值和签到积分
UPDATE mem_member_card_type SET min_growth = 0,     sign_in_points = 1 WHERE type_code = 'experience';
UPDATE mem_member_card_type SET min_growth = 100,   sign_in_points = 1 WHERE type_code = 'formal';
UPDATE mem_member_card_type SET min_growth = 500,   sign_in_points = 2 WHERE type_code = 'star1';
UPDATE mem_member_card_type SET min_growth = 1500,  sign_in_points = 2 WHERE type_code = 'star2';
UPDATE mem_member_card_type SET min_growth = 3000,  sign_in_points = 3 WHERE type_code = 'star3';
UPDATE mem_member_card_type SET min_growth = 6000,  sign_in_points = 4 WHERE type_code = 'star4';
UPDATE mem_member_card_type SET min_growth = 10000, sign_in_points = 5 WHERE type_code = 'star5';

-- ============================================================
-- 3. 新增 mem_member_sign_in 签到记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS mem_member_sign_in (
  sign_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  member_no VARCHAR(64) DEFAULT NULL COMMENT '会员编号',
  member_name VARCHAR(64) DEFAULT NULL COMMENT '会员姓名',
  sign_date DATE NOT NULL COMMENT '签到日期',
  continuous_days INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
  points_earned DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '获得积分',
  growth_earned BIGINT NOT NULL DEFAULT 0 COMMENT '获得成长值',
  batch_id BIGINT DEFAULT NULL COMMENT '补录批次ID，实时签到为空',
  sign_type VARCHAR(16) NOT NULL DEFAULT 'REALTIME' COMMENT '签到类型: REALTIME/BACKFILL',
  reward_level_code VARCHAR(32) DEFAULT NULL COMMENT '奖励等级快照',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (sign_id),
  UNIQUE KEY uk_tenant_member_sign_date (tenant_id, member_id, sign_date),
  KEY idx_tenant_sign_date (tenant_id, sign_date),
  KEY idx_tenant_member_id (tenant_id, member_id),
  KEY idx_tenant_batch_id (tenant_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员签到记录';

-- 3.1 若表已存在则补列（幂等，兼容老版本）
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_sign_in' AND COLUMN_NAME = 'batch_id');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_sign_in ADD COLUMN batch_id BIGINT DEFAULT NULL COMMENT ''补录批次ID，实时签到为空''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_sign_in' AND COLUMN_NAME = 'sign_type');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_sign_in ADD COLUMN sign_type VARCHAR(16) NOT NULL DEFAULT ''REALTIME'' COMMENT ''签到类型: REALTIME/BACKFILL''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_sign_in' AND COLUMN_NAME = 'reward_level_code');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE mem_member_sign_in ADD COLUMN reward_level_code VARCHAR(32) DEFAULT NULL COMMENT ''奖励等级快照''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE mem_member_sign_in SET sign_type = 'REALTIME' WHERE sign_type IS NULL OR sign_type = '';

-- ============================================================
-- 4. 新增 mem_member_sign_in_batch 签到补录批次表
-- ============================================================
CREATE TABLE IF NOT EXISTS mem_member_sign_in_batch (
  batch_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '补录批次ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  member_no VARCHAR(64) DEFAULT NULL COMMENT '会员编号',
  member_name VARCHAR(64) DEFAULT NULL COMMENT '会员姓名',
  target_month CHAR(7) NOT NULL COMMENT '目标月份 yyyy-MM',
  fill_mode VARCHAR(16) NOT NULL COMMENT '补录方式: SELECT_DATES/COUNT_ONLY',
  requested_count INT NOT NULL DEFAULT 0 COMMENT '请求补录次数',
  actual_count INT NOT NULL DEFAULT 0 COMMENT '实际补录次数',
  selected_dates VARCHAR(1000) DEFAULT NULL COMMENT '选择或自动分配日期，逗号分隔',
  reward_level_code VARCHAR(32) DEFAULT NULL COMMENT '奖励等级快照',
  points_per_sign DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '单次签到积分',
  growth_per_sign BIGINT NOT NULL DEFAULT 0 COMMENT '单次签到成长值',
  total_points DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本批次总积分',
  total_growth BIGINT NOT NULL DEFAULT 0 COMMENT '本批次总成长值',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (batch_id),
  KEY idx_tenant_member_month (tenant_id, member_id, target_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员签到补录批次';

-- ============================================================
-- 5. 新增 mem_growth_record 成长值变动记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS mem_growth_record (
  record_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  member_no VARCHAR(64) DEFAULT NULL COMMENT '会员编号',
  member_name VARCHAR(64) DEFAULT NULL COMMENT '会员姓名',
  source_type VARCHAR(32) NOT NULL COMMENT '来源: SALE/SIGN_IN/SIGN_IN_BACKFILL/SIGN_IN_DELETE/MANUAL/DECAY',
  source_id BIGINT DEFAULT NULL COMMENT '来源业务ID',
  dedup_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  growth_change BIGINT NOT NULL COMMENT '成长值变动',
  balance BIGINT NOT NULL COMMENT '变动后成长值',
  before_level VARCHAR(32) DEFAULT NULL COMMENT '变动前等级',
  after_level VARCHAR(32) DEFAULT NULL COMMENT '变动后等级',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_tenant_dedup_key (tenant_id, dedup_key),
  KEY idx_tenant_member_id (tenant_id, member_id),
  KEY idx_tenant_source_type (tenant_id, source_type),
  KEY idx_tenant_create_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员成长值变动记录';

-- 5.1 补充 source_type 注释（兼容老版本）
ALTER TABLE mem_growth_record MODIFY COLUMN source_type VARCHAR(32) NOT NULL COMMENT '来源: SALE/SIGN_IN/SIGN_IN_BACKFILL/SIGN_IN_DELETE/MANUAL/DECAY';

-- ============================================================
-- 6. 新增 mem_growth_rule 成长规则表（含签到+消费+衰减规则）
-- ============================================================
CREATE TABLE IF NOT EXISTS mem_growth_rule (
  rule_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  sign_in_points DECIMAL(12,2) NOT NULL DEFAULT 1.00 COMMENT '默认签到积分，仅当等级未配置时兜底',
  sign_in_growth BIGINT NOT NULL DEFAULT 5 COMMENT '签到获得成长值',
  sale_growth_ratio DECIMAL(6,2) NOT NULL DEFAULT 1.00 COMMENT '消费成长值倍率',
  decay_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否启用衰减 0否 1是',
  inactive_days INT NOT NULL DEFAULT 180 COMMENT '不活跃天数阈值',
  decay_ratio DECIMAL(5,2) NOT NULL DEFAULT 0.50 COMMENT '衰减比例',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_tenant_growth_rule (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员成长规则';

-- 6.1 兼容老版本：修正 sign_in_points 默认值和注释
ALTER TABLE mem_growth_rule MODIFY COLUMN sign_in_points DECIMAL(12,2) NOT NULL DEFAULT 1.00 COMMENT '默认签到积分，仅当等级未配置时兜底';

-- 默认数据
INSERT INTO mem_growth_rule
  (tenant_id, sign_in_points, sign_in_growth, sale_growth_ratio, decay_enabled, inactive_days, decay_ratio, create_by)
SELECT 1, 1.00, 5, 1.00, '0', 180, 0.50, 'system'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mem_growth_rule WHERE tenant_id = 1);

-- ============================================================
-- 7. 字典数据：积分记录类型增加 5=签到得积分
-- ============================================================

SET @dictTypeId := (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'mem_points_record_type' LIMIT 1);

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '积分记录类型', 'mem_points_record_type', '0', 'admin', NOW(), '会员积分记录类型'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mem_points_record_type');
SET @dictTypeId := (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'mem_points_record_type' LIMIT 1);

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '消费得积分', '1', 'mem_points_record_type', '', 'primary', 'Y', '0', 'admin', NOW(), '消费获得积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mem_points_record_type' AND dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '兑换扣积分', '2', 'mem_points_record_type', '', 'danger', 'N', '0', 'admin', NOW(), '兑换扣除积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mem_points_record_type' AND dict_value = '2');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '过期清零', '3', 'mem_points_record_type', '', 'info', 'N', '0', 'admin', NOW(), '积分过期清零'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mem_points_record_type' AND dict_value = '3');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '手动调整', '4', 'mem_points_record_type', '', 'warning', 'N', '0', 'admin', NOW(), '手动调整积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mem_points_record_type' AND dict_value = '4');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '签到得积分', '5', 'mem_points_record_type', '', 'success', 'N', '0', 'admin', NOW(), '签到获得积分'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mem_points_record_type' AND dict_value = '5');

-- ============================================================
-- 8. 菜单与权限
-- ============================================================

SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE path = 'member' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);
SET @memberRootId := COALESCE(@memberRootId, 0);

-- 8.1 签到记录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '签到记录', @memberRootId, 15, 'signIn', 'member/signIn/index', 1, 0, 'C', '0', '0', 'member:signIn:list', 'date', 'admin', NOW(), '会员签到记录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:signIn:list');

SET @signInMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'member:signIn:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '签到查询', @signInMenuId, 1, '', '', 1, 0, 'F', '0', '0', 'member:signIn:query', '#', 'admin', NOW(), '签到查询'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:signIn:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '签到操作', @signInMenuId, 2, '', '', 1, 0, 'F', '0', '0', 'member:signIn:add', '#', 'admin', NOW(), '签到操作'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:signIn:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '签到补录', @signInMenuId, 3, '', '', 1, 0, 'F', '0', '0', 'member:signIn:backfill', '#', 'admin', NOW(), '签到补录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:signIn:backfill');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '签到删除', @signInMenuId, 4, '', '', 1, 0, 'F', '0', '0', 'member:signIn:remove', '#', 'admin', NOW(), '签到删除'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:signIn:remove');

-- 8.2 成长值记录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成长值记录', @memberRootId, 16, 'growth', 'member/growth/index', 1, 0, 'C', '0', '0', 'member:growth:list', 'chart', 'admin', NOW(), '会员成长值变动记录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growth:list');

SET @growthMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'member:growth:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成长值查询', @growthMenuId, 1, '', '', 1, 0, 'F', '0', '0', 'member:growth:query', '#', 'admin', NOW(), '成长值查询'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growth:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成长值调整', @growthMenuId, 2, '', '', 1, 0, 'F', '0', '0', 'member:growth:adjust', '#', 'admin', NOW(), '成长值调整'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growth:adjust');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '成长规则修改', @growthMenuId, 3, '', '', 1, 0, 'F', '0', '0', 'member:growth:edit', '#', 'admin', NOW(), '成长规则修改'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growth:edit');

-- 8.3 等级配置菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '等级配置', @memberRootId, 17, 'level', 'member/level/index', 1, 0, 'C', '0', '0', 'member:level:list', 'peoples', 'admin', NOW(), '会员等级配置'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:level:list');

SET @levelMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'member:level:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '等级查询', @levelMenuId, 1, '', '', 1, 0, 'F', '0', '0', 'member:level:query', '#', 'admin', NOW(), '等级查询'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:level:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '等级修改', @levelMenuId, 2, '', '', 1, 0, 'F', '0', '0', 'member:level:edit', '#', 'admin', NOW(), '等级配置修改'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:level:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '等级新增', @levelMenuId, 4, '', '', 1, 0, 'F', '0', '0', 'member:level:add', '#', 'admin', NOW(), '新增等级配置'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:level:add');

-- ============================================================
-- 9. 给 role_id=1 授权所有新菜单
-- ============================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('member:signIn:list', 'member:signIn:query', 'member:signIn:add', 'member:signIn:backfill', 'member:signIn:remove')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('member:growth:list', 'member:growth:query', 'member:growth:adjust', 'member:growth:edit')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('member:level:list', 'member:level:query', 'member:level:edit', 'member:level:add')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
