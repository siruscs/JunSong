SET NAMES utf8mb4;

-- PROD 菜单图标窄修复：只按稳定权限码更新 icon，不改变路由、权限或授权关系。
SELECT menu_id,menu_name,perms,icon
FROM sys_menu
WHERE perms IN ('system:action-center:view','finance:predictiveOps:view','member:refund:list','workflow:version:list','open:app:list')
ORDER BY menu_id;

DELIMITER //
DROP PROCEDURE IF EXISTS repair_prod_menu_icons//
CREATE PROCEDURE repair_prod_menu_icons()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(*) INTO v_count FROM sys_menu
  WHERE menu_type='C' AND perms IN ('system:action-center:view','finance:predictiveOps:view','member:refund:list','workflow:version:list','open:app:list');
  IF v_count <> 5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='target menu uniqueness reconciliation failed'; END IF;

  SELECT COUNT(DISTINCT perms) INTO v_count FROM sys_menu
  WHERE menu_type='C' AND perms IN ('system:action-center:view','finance:predictiveOps:view','member:refund:list','workflow:version:list','open:app:list');
  IF v_count <> 5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='target permission uniqueness reconciliation failed'; END IF;

  UPDATE sys_menu SET icon='bell', update_by='admin', update_time=NOW()
  WHERE perms='system:action-center:view' AND menu_type='C';

  UPDATE sys_menu SET icon='chart', update_by='admin', update_time=NOW()
  WHERE perms='finance:predictiveOps:view' AND menu_type='C';

  UPDATE sys_menu SET icon='money', update_by='admin', update_time=NOW()
  WHERE perms='member:refund:list' AND menu_type='C';

  UPDATE sys_menu SET icon='nested', update_by='admin', update_time=NOW()
  WHERE perms='workflow:version:list' AND menu_type='C';

  UPDATE sys_menu SET icon='client', update_by='admin', update_time=NOW()
  WHERE perms='open:app:list' AND menu_type='C';

  SELECT COUNT(*) INTO v_count FROM sys_menu WHERE menu_type='C' AND (
    (perms='system:action-center:view' AND icon='bell') OR
    (perms='finance:predictiveOps:view' AND icon='chart') OR
    (perms='member:refund:list' AND icon='money') OR
    (perms='workflow:version:list' AND icon='nested') OR
    (perms='open:app:list' AND icon='client'));
  IF v_count <> 5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='menu icon result reconciliation failed'; END IF;

  COMMIT;
END//
CALL repair_prod_menu_icons()//
DROP PROCEDURE repair_prod_menu_icons//
DELIMITER ;

SELECT menu_id,menu_name,perms,icon,HEX(icon) AS icon_hex
FROM sys_menu
WHERE perms IN ('system:action-center:view','finance:predictiveOps:view','member:refund:list','workflow:version:list','open:app:list')
ORDER BY menu_id;
