-- =====================================================================
-- Phase 1: 经营任务中心（SysOperatingTask）建表与权限注册
-- 可重复执行：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- 表 sys_operating_task / sys_operating_task_log 带 tenant_id 列，
-- 由 TenantSqlInterceptor 自动注入 SELECT/UPDATE/DELETE 条件，
-- 由 TenantInterceptor 自动填充 INSERT 的 tenant_id。
-- =====================================================================

SET NAMES utf8mb4;

-- ==================== 建表：sys_operating_task ====================
CREATE TABLE IF NOT EXISTS sys_operating_task (
  task_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  tenant_id        BIGINT       NOT NULL DEFAULT 1      COMMENT '租户ID',
  title            VARCHAR(200) NOT NULL                 COMMENT '任务标题',
  reason           VARCHAR(500) DEFAULT NULL             COMMENT '触发原因',
  suggestion       VARCHAR(500) DEFAULT NULL             COMMENT '建议动作',
  source_module    VARCHAR(20)  NOT NULL                 COMMENT '来源模块: FINANCE/MEMBER/STOCK/SYSTEM/WORKFLOW',
  source_type      VARCHAR(40)  NOT NULL                 COMMENT '来源类型: REVIEW_TASK/RECEIVABLE_COLLECTION/...',
  source_id        VARCHAR(64)  DEFAULT NULL             COMMENT '来源单据ID',
  source_route     VARCHAR(200) DEFAULT NULL             COMMENT '来源单据跳转路由（PC）',
  task_type        VARCHAR(40)  NOT NULL                 COMMENT '任务类型码',
  status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/DONE/REJECTED/REOPENED',
  priority         VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: URGENT/HIGH/MEDIUM/LOW',
  severity         VARCHAR(10)  DEFAULT NULL             COMMENT '严重级别: HIGH/MEDIUM/LOW',
  dept_id          BIGINT       NOT NULL                 COMMENT '关联门店ID',
  dept_name        VARCHAR(50)  DEFAULT NULL             COMMENT '门店名称（冗余）',
  assignee_id      BIGINT       DEFAULT NULL             COMMENT '负责人ID',
  assignee_name    VARCHAR(50)  DEFAULT NULL             COMMENT '负责人姓名',
  creator_id       BIGINT       DEFAULT NULL             COMMENT '创建人ID',
  due_time         DATETIME     DEFAULT NULL             COMMENT '截止时间',
  occur_time       DATETIME     DEFAULT NULL             COMMENT '来源发生时间',
  impact_amount    DECIMAL(18,2) DEFAULT NULL            COMMENT '影响金额',
  idempotency_key  VARCHAR(120) NOT NULL                COMMENT '幂等键 {tenantId}:{sourceModule}:{sourceType}:{sourceId}',
  handler_note     VARCHAR(500) DEFAULT NULL             COMMENT '处理备注',
  reject_reason    VARCHAR(500) DEFAULT NULL             COMMENT '驳回原因',
  reopen_count     INT          NOT NULL DEFAULT 0       COMMENT '重开次数',
  version          INT          NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
  del_flag         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 2删除）',
  create_by        VARCHAR(64)  DEFAULT NULL             COMMENT '创建者',
  create_time      DATETIME     DEFAULT NULL             COMMENT '创建时间',
  update_by        VARCHAR(64)  DEFAULT NULL             COMMENT '更新者',
  update_time      DATETIME     DEFAULT NULL             COMMENT '更新时间',
  remark           VARCHAR(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_idempotency_key (tenant_id, idempotency_key),
  KEY idx_status_dept (status, dept_id),
  KEY idx_assignee_status (assignee_id, status),
  KEY idx_source (source_module, source_type, source_id),
  KEY idx_due_time (due_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经营任务表';

-- ==================== 建表：sys_operating_task_log ====================
CREATE TABLE IF NOT EXISTS sys_operating_task_log (
  log_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  tenant_id     BIGINT       NOT NULL DEFAULT 1      COMMENT '租户ID',
  task_id       BIGINT       NOT NULL                COMMENT '关联任务ID',
  action        VARCHAR(20)  NOT NULL                COMMENT '动作: CREATE/CLAIM/COMPLETE/REJECT/REOPEN',
  old_status    VARCHAR(20)  DEFAULT NULL            COMMENT '变更前状态',
  new_status    VARCHAR(20)  DEFAULT NULL            COMMENT '变更后状态',
  operator_id   BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name VARCHAR(50)  DEFAULT NULL            COMMENT '操作人姓名',
  note          VARCHAR(500) DEFAULT NULL            COMMENT '处理备注/驳回原因/重开原因',
  create_time   DATETIME     DEFAULT NULL            COMMENT '操作时间',
  PRIMARY KEY (log_id),
  KEY idx_task_log_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经营任务操作日志表';

-- ==================== 注册5个权限码菜单 ====================
-- parent_id 用 SELECT 查找系统管理根菜单ID，不硬编码
SET @systemRootId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' LIMIT 1);
SET @systemRootId := COALESCE(@systemRootId, 1);

-- 权限1: system:operatingTask:list
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '经营任务查询', @systemRootId, 50, '', NULL, 1, 0, 'F', '0', '0', 'system:operatingTask:list', '#', 'admin', NOW(), '经营任务列表/详情查询权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operatingTask:list' AND menu_type = 'F');

-- 权限2: system:operatingTask:claim
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认领经营任务', @systemRootId, 51, '', NULL, 1, 0, 'F', '0', '0', 'system:operatingTask:claim', '#', 'admin', NOW(), '认领经营任务权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operatingTask:claim' AND menu_type = 'F');

-- 权限3: system:operatingTask:complete
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '完成经营任务', @systemRootId, 52, '', NULL, 1, 0, 'F', '0', '0', 'system:operatingTask:complete', '#', 'admin', NOW(), '完成经营任务权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operatingTask:complete' AND menu_type = 'F');

-- 权限4: system:operatingTask:reject
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '驳回经营任务', @systemRootId, 53, '', NULL, 1, 0, 'F', '0', '0', 'system:operatingTask:reject', '#', 'admin', NOW(), '驳回经营任务权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operatingTask:reject' AND menu_type = 'F');

-- 权限5: system:operatingTask:reopen
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '重开经营任务', @systemRootId, 54, '', NULL, 1, 0, 'F', '0', '0', 'system:operatingTask:reopen', '#', 'admin', NOW(), '重开经营任务权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operatingTask:reopen' AND menu_type = 'F');

-- 授权给超管 role_id=1
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('system:operatingTask:list', 'system:operatingTask:claim', 'system:operatingTask:complete', 'system:operatingTask:reject', 'system:operatingTask:reopen')
  AND m.menu_type = 'F'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu e WHERE e.role_id = 1 AND e.menu_id = m.menu_id
  );

-- ==================== 校验输出 ====================
SELECT '表创建校验' AS check_item,
       (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('sys_operating_task','sys_operating_task_log')) AS expected_2;

SELECT '权限码校验' AS check_item,
       COUNT(*) AS expected_5
FROM sys_menu
WHERE perms LIKE 'system:operatingTask:%' AND menu_type = 'F';

SELECT menu_name, perms, HEX(perms) AS perms_hex
FROM sys_menu
WHERE perms LIKE 'system:operatingTask:%' AND menu_type = 'F'
ORDER BY order_num;
