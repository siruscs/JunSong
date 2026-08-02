SET NAMES utf8mb4;

-- 成本核算已由核算周期取代：移除小程序模块入口，不删除历史业务表、接口或数据。
DELETE FROM mem_mp_role_module
 WHERE module_key = 'costAccounting';

SELECT COUNT(*) AS remaining_cost_accounting_module_count
  FROM mem_mp_role_module
 WHERE module_key = 'costAccounting';
