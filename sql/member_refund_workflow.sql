SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS mem_refund_apply (
  id BIGINT NOT NULL AUTO_INCREMENT,
  refund_no VARCHAR(64) NOT NULL,
  member_id BIGINT DEFAULT NULL,
  member_name VARCHAR(64) NOT NULL,
  member_phone VARCHAR(32) DEFAULT NULL,
  store_id BIGINT DEFAULT NULL,
  store_name VARCHAR(64) DEFAULT NULL,
  refund_amount DECIMAL(12,2) NOT NULL,
  refund_reason VARCHAR(255) DEFAULT NULL,
  refund_remark VARCHAR(500) DEFAULT NULL,
  store_approver VARCHAR(64) DEFAULT NULL,
  finance_approver VARCHAR(64) DEFAULT NULL,
  process_instance_id VARCHAR(64) DEFAULT NULL,
  process_definition_key VARCHAR(64) DEFAULT NULL,
  workflow_status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
  current_task_name VARCHAR(128) DEFAULT NULL,
  submitter VARCHAR(64) DEFAULT NULL,
  submit_time DATETIME DEFAULT NULL,
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mem_refund_apply_no (refund_no)
);

SET @memberRootId = (
  SELECT menu_id
  FROM sys_menu
  WHERE path = 'member'
  ORDER BY menu_id
  LIMIT 1
);

DELETE FROM sys_menu
WHERE perms IN (
  'member:refund:query',
  'member:refund:add',
  'member:refund:edit',
  'member:refund:remove',
  'member:refund:submit',
  'member:refund:withdraw'
);

DELETE FROM sys_menu
WHERE perms = 'member:refund:list'
   OR path = 'refund';

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('退款申请', @memberRootId, 8, 'refund', 'member/refund/index', 1, 0, 'C', '0', '0', 'member:refund:list', 'money', 'admin', NOW(), '会员退款申请菜单');

SET @refundMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
('退款申请查询', @refundMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'member:refund:query', '#', 'admin', NOW(), ''),
('退款申请新增', @refundMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'member:refund:add', '#', 'admin', NOW(), ''),
('退款申请修改', @refundMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'member:refund:edit', '#', 'admin', NOW(), ''),
('退款申请删除', @refundMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'member:refund:remove', '#', 'admin', NOW(), ''),
('退款申请提交审批', @refundMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'member:refund:submit', '#', 'admin', NOW(), ''),
('退款申请撤回', @refundMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'member:refund:withdraw', '#', 'admin', NOW(), '');
