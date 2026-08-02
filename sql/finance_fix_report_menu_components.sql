SET NAMES utf8mb4;

START TRANSACTION;

UPDATE sys_menu SET component = 'finance/report/expense' WHERE perms = 'finance:report:expense';
UPDATE sys_menu SET component = 'finance/report/profitShare' WHERE perms = 'finance:report:profitShare';
UPDATE sys_menu SET component = 'finance/report/sale' WHERE perms = 'finance:report:sale';
UPDATE sys_menu SET component = 'finance/report/profit' WHERE perms = 'finance:report:profit';
UPDATE sys_menu SET component = 'finance/report/stock' WHERE perms = 'finance:report:stock';
UPDATE sys_menu SET component = 'finance/report/store' WHERE perms = 'finance:report:store';

COMMIT;

SELECT menu_id, menu_name, path, component, visible, status, perms
  FROM sys_menu
 WHERE perms LIKE 'finance:report:%'
 ORDER BY menu_id;
