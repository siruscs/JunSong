SET NAMES utf8mb4;
-- DEV 工作流处理人修复：部门负责人必须保存真实 username，不能保存昵称。
-- 仅在真实用户 ry 存在且当前值为历史昵称“若依”时更新，重复执行安全。
SELECT 'BEFORE' result_type, COUNT(*) affected
FROM sys_dept d
JOIN sys_user u ON u.user_name = 'ry' AND u.del_flag = '0'
WHERE d.leader = '若依' AND d.del_flag = '0';

UPDATE sys_dept d
JOIN sys_user u ON u.user_name = 'ry' AND u.del_flag = '0'
SET d.leader = u.user_name
WHERE d.leader = '若依' AND d.del_flag = '0';

SELECT 'AFTER' result_type, COUNT(*) remaining
FROM sys_dept
WHERE leader = '若依' AND del_flag = '0';
