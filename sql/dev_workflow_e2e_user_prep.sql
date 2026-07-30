SET NAMES utf8mb4;

-- DEV only：为接口级 ADMIN/wjs 工作流链路准备可登录测试账号。
-- 只修改 DEV 用户 wjs 的密码为 admin123；原密码会备份，方便回滚。
-- 回滚：
-- UPDATE sys_user u
-- JOIN dev_workflow_e2e_user_password_backup b ON b.user_id = u.user_id
-- SET u.password = b.old_password
-- WHERE u.user_name = 'wjs';

CREATE TABLE IF NOT EXISTS dev_workflow_e2e_user_password_backup (
    user_id BIGINT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL,
    old_password VARCHAR(128) NOT NULL,
    backup_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DEV 工作流 E2E 用户密码备份';

INSERT IGNORE INTO dev_workflow_e2e_user_password_backup (user_id, user_name, old_password)
SELECT user_id, user_name, password
FROM sys_user
WHERE user_name = 'wjs';

UPDATE sys_user wjs
JOIN sys_user admin_user ON admin_user.user_name = 'admin'
SET wjs.password = admin_user.password,
    wjs.status = '0',
    wjs.del_flag = '0',
    wjs.update_by = 'dev-workflow-e2e',
    wjs.update_time = NOW()
WHERE wjs.user_name = 'wjs';

SELECT 'dev_workflow_e2e_user_prep' result_type,
       (SELECT COUNT(*) FROM sys_user WHERE user_name='wjs' AND status='0' AND del_flag='0') active_wjs_count,
       (SELECT COUNT(*) FROM dev_workflow_e2e_user_password_backup WHERE user_name='wjs') backup_count,
       (SELECT COUNT(*) FROM sys_user u
         JOIN sys_user_role ur ON u.user_id=ur.user_id
         JOIN sys_role r ON ur.role_id=r.role_id
         JOIN sys_role_menu rm ON r.role_id=rm.role_id
         JOIN sys_menu m ON rm.menu_id=m.menu_id
        WHERE u.user_name='wjs' AND m.perms IN ('workflow:task:list','workflow:task:approve','workflow:task:reject')) workflow_perm_count;
