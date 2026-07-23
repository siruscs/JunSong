SET NAMES utf8mb4;

-- ==========================================================================
-- 小程序微信账号绑定关系表 sys_user_mp_binding
-- 目标：记录微信身份（appId + openid）与系统账号（tenantId + userId）的绑定关系。
-- 原则：
--   1) 一个 (app_id, openid) 只能绑定一个系统账号（唯一键）。
--   2) 解绑使用 REVOKED 状态保留审计链，不物理删除。
--   3) 所有查询必须显式带 tenant_id，禁止仅按 openid 查询。
--   4) 可重复执行（IF NOT EXISTS），非破坏性迁移。
-- 计划文档：docs/superpowers/plans/2026-07-14-miniprogram-wechat-account-binding.md 第 4 节
-- ==========================================================================

-- Step 1: 幂等创建绑定关系表
CREATE TABLE IF NOT EXISTS sys_user_mp_binding (
  binding_id      BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '绑定关系ID',
  tenant_id       BIGINT       NOT NULL                 COMMENT '租户ID（所有查询必须显式带此字段）',
  user_id         BIGINT       NOT NULL                 COMMENT '绑定的系统用户ID',
  app_id          VARCHAR(64)  NOT NULL                 COMMENT '微信小程序 AppID',
  openid          VARCHAR(64)  NOT NULL                 COMMENT '微信 openid（同一 AppID 下唯一）',
  unionid         VARCHAR(64)  DEFAULT NULL             COMMENT '微信 unionid（可空，跨小程序统一身份时使用）',
  status          CHAR(8)      NOT NULL DEFAULT 'ACTIVE' COMMENT '绑定状态 ACTIVE/REVOKED（撤销后保留审计链不删除）',
  bound_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次绑定时间',
  last_login_time DATETIME     DEFAULT NULL             COMMENT '最近一次微信快捷登录时间',
  bound_by        VARCHAR(64)  DEFAULT ''               COMMENT '绑定操作人（用户名）',
  revoked_time    DATETIME     DEFAULT NULL             COMMENT '撤销时间（status=REVOKED 时非空）',
  revoked_by      VARCHAR(64)  DEFAULT ''               COMMENT '撤销操作人（用户名）',
  revoke_reason   VARCHAR(256) DEFAULT ''               COMMENT '撤销原因（解绑/换绑/管理员强制等）',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark          VARCHAR(500) DEFAULT ''               COMMENT '备注',
  PRIMARY KEY (binding_id),
  -- 同一 AppID 下 openid 全局唯一（跨租户也不允许同一微信身份绑定多个账号）
  UNIQUE KEY uk_user_mp_binding_app_openid (app_id, openid),
  -- 租户过滤查询索引
  KEY idx_user_mp_binding_tenant (tenant_id),
  -- 按用户查询绑定列表（一个用户可能绑定多个小程序身份）
  KEY idx_user_mp_binding_user (user_id),
  -- 管理员按租户+用户查询
  KEY idx_user_mp_binding_tenant_user (tenant_id, user_id),
  -- 按状态过滤（查 ACTIVE 绑定）
  KEY idx_user_mp_binding_status (status),
  -- 按租户+状态过滤（统计 ACTIVE/REVOKED）
  KEY idx_user_mp_binding_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序微信账号绑定关系表';

-- ==========================================================================
-- 对账与校验输出（脚本执行后可在客户端查看以下结果）
-- ==========================================================================

-- Step 2: 表结构校验 —— 表存在且字段数量正确
SELECT
  'table_exists' AS check_type,
  COUNT(*) AS check_value
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user_mp_binding';

-- Step 3: 字段清单校验 —— 确认所有必需字段已创建
SELECT
  'column_check' AS check_type,
  column_name,
  data_type,
  is_nullable,
  column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user_mp_binding'
ORDER BY ordinal_position;

-- Step 4: 唯一键校验 —— 确认 (app_id, openid) 唯一约束存在
SELECT
  'unique_key_check' AS check_type,
  index_name,
  COUNT(*) AS column_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user_mp_binding'
  AND index_name = 'uk_user_mp_binding_app_openid'
GROUP BY index_name;

-- Step 5: 绑定关系数量统计（首次执行应为 0）
SELECT
  'binding_total' AS check_type,
  COUNT(*) AS check_value
FROM sys_user_mp_binding;

-- Step 6: 按状态统计 ACTIVE / REVOKED 数量
SELECT
  'binding_by_status' AS check_type,
  status,
  COUNT(*) AS check_value
FROM sys_user_mp_binding
WHERE status IN ('ACTIVE', 'REVOKED')
GROUP BY status;

-- Step 7: 按租户统计绑定数量（监控异常租户）
SELECT
  'binding_by_tenant' AS check_type,
  tenant_id,
  COUNT(*) AS total_count,
  SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count,
  SUM(CASE WHEN status = 'REVOKED' THEN 1 ELSE 0 END) AS revoked_count
FROM sys_user_mp_binding
GROUP BY tenant_id;

-- Step 8: 异常检测 —— 同一 (app_id, openid) 存在多条 ACTIVE 记录（应为 0，唯一键保证）
SELECT
  'anomaly_duplicate_active_app_openid' AS check_type,
  COUNT(*) AS check_value
FROM (
  SELECT app_id, openid, COUNT(*) AS cnt
  FROM sys_user_mp_binding
  WHERE status = 'ACTIVE'
  GROUP BY app_id, openid
  HAVING COUNT(*) > 1
) dup;

-- Step 9: 异常检测 —— 同一 (tenant_id, user_id) 绑定同一 app_id 多条 ACTIVE（业务规则：默认一个 AppID 下一个用户一个 openid）
SELECT
  'anomaly_user_multiple_active_same_app' AS check_type,
  COUNT(*) AS check_value
FROM (
  SELECT tenant_id, user_id, app_id, COUNT(*) AS cnt
  FROM sys_user_mp_binding
  WHERE status = 'ACTIVE'
  GROUP BY tenant_id, user_id, app_id
  HAVING COUNT(*) > 1
) dup;
