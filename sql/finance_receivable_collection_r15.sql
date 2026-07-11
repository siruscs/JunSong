CREATE TABLE IF NOT EXISTS finance_receivable_collection (
  collection_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '催收记录ID',
  sale_id BIGINT NOT NULL COMMENT '销售单ID',
  sale_no VARCHAR(64) DEFAULT NULL COMMENT '销售单号',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  member_id BIGINT DEFAULT NULL COMMENT '会员ID',
  customer_name VARCHAR(128) DEFAULT NULL COMMENT '客户/会员名称',
  sale_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  paid_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已缴金额',
  unpaid_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '未缴金额',
  age_days INT NOT NULL DEFAULT 0 COMMENT '账龄天数',
  age_bucket VARCHAR(16) NOT NULL DEFAULT 'AGE_0_7' COMMENT '账龄分层',
  collection_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '催收状态',
  priority_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级',
  owner_id BIGINT DEFAULT NULL COMMENT '负责人ID',
  owner_name VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
  promised_pay_date DATE DEFAULT NULL COMMENT '承诺回款日期',
  promised_amount DECIMAL(18,2) DEFAULT NULL COMMENT '承诺回款金额',
  next_follow_time DATETIME DEFAULT NULL COMMENT '下次跟进时间',
  last_follow_time DATETIME DEFAULT NULL COMMENT '最近跟进时间',
  follow_count INT NOT NULL DEFAULT 0 COMMENT '跟进次数',
  source_task_id BIGINT DEFAULT NULL COMMENT '来源复盘任务ID',
  tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
  del_flag CHAR(1) DEFAULT '0' COMMENT '删除标记',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (collection_id),
  UNIQUE KEY uk_receivable_collection_sale (sale_id, tenant_id),
  KEY idx_receivable_collection_dept_status (dept_id, collection_status),
  KEY idx_receivable_collection_next_follow (next_follow_time),
  KEY idx_receivable_collection_age (age_bucket, priority_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收催收跟进表';

CREATE TABLE IF NOT EXISTS finance_receivable_collection_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  collection_id BIGINT NOT NULL COMMENT '催收记录ID',
  old_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
  new_status VARCHAR(32) NOT NULL COMMENT '新状态',
  follow_note VARCHAR(1000) DEFAULT NULL COMMENT '跟进备注',
  promised_pay_date DATE DEFAULT NULL COMMENT '承诺回款日期',
  promised_amount DECIMAL(18,2) DEFAULT NULL COMMENT '承诺回款金额',
  next_follow_time DATETIME DEFAULT NULL COMMENT '下次跟进时间',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (log_id),
  KEY idx_receivable_collection_log_collection (collection_id),
  KEY idx_receivable_collection_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收催收跟进日志表';

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @collectionMenuId := 2680;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @collectionMenuId, '应收催收作战台', @financeRootId, 90, 'receivableCollection', 'finance/receivableCollection/index', '', '', 1, 0, 'C', '0', '0', 'finance:receivableCollection:list', 'money', 'admin', NOW(), '应收催收作战台'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:receivableCollection:list');

SET @collectionMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:receivableCollection:list' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '催收跟进', @collectionMenuId, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:receivableCollection:edit', '#', 'admin', NOW(), '催收跟进'
WHERE @collectionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:receivableCollection:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '同步应收', @collectionMenuId, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:receivableCollection:sync', '#', 'admin', NOW(), '同步应收'
WHERE @collectionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:receivableCollection:sync');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('finance:receivableCollection:list', 'finance:receivableCollection:edit', 'finance:receivableCollection:sync')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
