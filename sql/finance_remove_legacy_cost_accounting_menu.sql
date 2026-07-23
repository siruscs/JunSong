SET NAMES utf8mb4;

-- 成本核算为旧版核算周期入口，仅下线 PC 菜单和权限，不删除历史成本数据。
START TRANSACTION;

DELETE FROM sys_menu
 WHERE menu_id IN (2061, 2062, 2063, 2064, 2065, 2060)
    OR perms LIKE 'finance:costAccounting:%';

DELETE FROM sys_role_menu
 WHERE menu_id NOT IN (SELECT menu_id FROM sys_menu);

COMMIT;

SELECT COUNT(*) AS remaining_cost_accounting_menu_count
  FROM sys_menu
 WHERE perms LIKE 'finance:costAccounting:%'
    OR path = 'finance/costAccounting/index';
