SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS expected_prod_platform_menu;
CREATE TEMPORARY TABLE expected_prod_platform_menu (
  menu_id BIGINT PRIMARY KEY,
  menu_name VARCHAR(50) NOT NULL,
  parent_id BIGINT NOT NULL,
  order_num INT NOT NULL,
  path VARCHAR(200) NOT NULL,
  component VARCHAR(255) NULL,
  menu_type CHAR(1) NOT NULL,
  perms VARCHAR(100) NOT NULL,
  icon VARCHAR(100) NOT NULL,
  remark VARCHAR(500) NOT NULL
);

INSERT INTO expected_prod_platform_menu VALUES
(2220,'工作流中心',0,7,'workflow',NULL,'M','','guide','工作流中心顶级目录'),
(2221,'流程定义',2220,1,'definition','workflow/definition/index','C','workflow:definition:list','tree','流程定义中心'),
(2222,'发起流程',2220,2,'start','workflow/start/index','C','workflow:instance:start','guide','发起新流程'),
(2223,'流程实例',2220,3,'instance','workflow/instance/index','C','workflow:instance:list','form','流程实例管理'),
(2224,'任务中心',2220,4,'task','workflow/task/index','C','workflow:task:list','skill','待办任务中心'),
(2225,'历史记录',2220,5,'history','workflow/history/index','C','workflow:history:list','log','流程历史记录'),
(2226,'流程分析',2220,6,'analytics','workflow/analytics/index','C','workflow:analytics:list','chart','流程分析报表'),
(2227,'超时配置',2220,7,'timeout','workflow/timeout/index','C','workflow:timeout:list','time','节点超时配置'),
(2228,'版本管理',2220,8,'version','workflow/version/index','C','workflow:version:list','version','流程版本管理'),
(2229,'字段权限',2220,9,'field-permission','workflow/field-permission/index','C','workflow:fieldPermission:list','lock','节点字段权限配置'),
(2230,'实例干预',2220,10,'intervene','workflow/intervene/index','C','workflow:instance:intervene','tool','流程实例干预'),
(2231,'流程监控',2220,11,'monitor','workflow/monitor/index','C','workflow:monitor:list','monitor','流程运维监控'),
(2240,'流程定义新增',2221,1,'','','F','workflow:definition:add','#',''),
(2241,'流程定义修改',2221,2,'','','F','workflow:definition:edit','#',''),
(2242,'流程定义发布',2221,3,'','','F','workflow:definition:deploy','#',''),
(2243,'流程定义导出',2221,4,'','','F','workflow:definition:export','#',''),
(2244,'流程定义删除',2221,5,'','','F','workflow:definition:remove','#',''),
(2245,'实例查询全部',2223,1,'','','F','workflow:instance:queryAll','#',''),
(2246,'实例删除',2223,2,'','','F','workflow:instance:remove','#',''),
(2247,'实例终止',2223,3,'','','F','workflow:instance:terminate','#',''),
(2248,'实例撤回',2223,4,'','','F','workflow:instance:withdraw','#',''),
(2249,'任务审批',2224,1,'','','F','workflow:task:approve','#',''),
(2250,'任务驳回',2224,2,'','','F','workflow:task:reject','#',''),
(2251,'任务转办',2224,3,'','','F','workflow:task:delegate','#',''),
(2252,'任务认领',2224,4,'','','F','workflow:task:claim','#',''),
(2253,'任务催办',2224,5,'','','F','workflow:task:urge','#',''),
(2254,'任务抄送',2224,6,'','','F','workflow:task:cc','#',''),
(2255,'任务加签',2224,7,'','','F','workflow:task:addsign','#',''),
(2256,'超时新增',2227,1,'','','F','workflow:timeout:add','#',''),
(2257,'超时修改',2227,2,'','','F','workflow:timeout:edit','#',''),
(2258,'超时删除',2227,3,'','','F','workflow:timeout:remove','#',''),
(2259,'字段权限新增',2229,1,'','','F','workflow:fieldPermission:add','#',''),
(2260,'字段权限配置',2229,2,'','','F','workflow:fieldPermission:edit','#',''),
(2261,'字段权限删除',2229,3,'','','F','workflow:fieldPermission:remove','#',''),
(2280,'低代码平台',0,8,'lowcode',NULL,'M','','build','低代码平台顶级目录'),
(2281,'配置后台',2280,1,'admin','lowcode/admin/index','C','lowcode:meta:list','build','低代码业务配置后台'),
(2282,'配置保存',2281,1,'','','F','lowcode:meta:config','#',''),
(2283,'配置删除',2281,2,'','','F','lowcode:meta:remove','#',''),
(2284,'配置发布',2281,3,'','','F','lowcode:meta:publish','#',''),
(2285,'业务查询',2281,4,'','','F','lowcode:biz:list','#','动态业务菜单由发布后的业务配置生成'),
(2286,'业务新增',2281,5,'','','F','lowcode:biz:add','#',''),
(2287,'业务修改',2281,6,'','','F','lowcode:biz:edit','#',''),
(2288,'业务删除',2281,7,'','','F','lowcode:biz:remove','#',''),
(2289,'业务提交',2281,8,'','','F','lowcode:biz:submit','#',''),
(2290,'业务履约',2281,9,'','','F','lowcode:biz:fulfill','#',''),
(2291,'业务导入',2281,10,'','','F','lowcode:biz:import','#',''),
(2292,'业务导出',2281,11,'','','F','lowcode:biz:export','#',''),
(2293,'报表查询',2281,12,'','','F','lowcode:report:list','#','API 权限，不生成空白页面'),
(2294,'报表统计',2281,13,'','','F','lowcode:report:stat','#','API 权限，不生成空白页面');

DELIMITER //
DROP PROCEDURE IF EXISTS repair_prod_platform_menu_and_config//
CREATE PROCEDURE repair_prod_platform_menu_and_config()
BEGIN
  DECLARE v_expected_count INT;
  DECLARE v_conflict_count INT;
  DECLARE v_actual_count INT;
  DECLARE v_admin_grant_count INT;
  DECLARE v_non_admin_grant_count INT;
  DECLARE v_config_identity_count INT;
  DECLARE v_config_count INT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  SELECT COUNT(*) INTO v_expected_count FROM expected_prod_platform_menu;
  SELECT COUNT(*) INTO v_conflict_count
  FROM sys_menu m JOIN expected_prod_platform_menu e ON e.menu_id=m.menu_id
  WHERE NOT (m.menu_name <=> e.menu_name)
     OR NOT (m.parent_id <=> e.parent_id)
     OR NOT (m.order_num <=> e.order_num)
     OR NOT (m.path <=> e.path)
     OR NOT (m.component <=> e.component)
     OR NOT (COALESCE(m.query,'') <=> '')
     OR NOT (COALESCE(m.route_name,'') <=> '')
     OR NOT (m.is_frame <=> 1)
     OR NOT (m.is_cache <=> 0)
     OR NOT (m.menu_type <=> e.menu_type)
     OR NOT (COALESCE(m.perms,'') <=> e.perms)
     OR NOT (m.icon <=> e.icon);
  IF v_conflict_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow/lowcode stable menu id conflict';
  END IF;

  START TRANSACTION;
  INSERT INTO sys_menu
    (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,
     menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
  SELECT menu_id,menu_name,parent_id,order_num,path,component,'','',1,0,
         menu_type,'0','0',perms,icon,'system',NOW(),'system',NOW(),remark
  FROM expected_prod_platform_menu
  ON DUPLICATE KEY UPDATE
    menu_name=VALUES(menu_name),parent_id=VALUES(parent_id),order_num=VALUES(order_num),
    path=VALUES(path),component=VALUES(component),query='',route_name='',is_frame=1,is_cache=0,
    menu_type=VALUES(menu_type),
    visible='0',status='0',perms=VALUES(perms),icon=VALUES(icon),
    update_by='system',update_time=NOW(),remark=VALUES(remark);

  SELECT COUNT(*) INTO v_actual_count
  FROM sys_menu m JOIN expected_prod_platform_menu e ON e.menu_id=m.menu_id
  WHERE m.menu_name=e.menu_name AND m.parent_id=e.parent_id AND m.order_num=e.order_num
    AND m.path=e.path AND m.component <=> e.component
    AND COALESCE(m.query,'')='' AND COALESCE(m.route_name,'')=''
    AND m.is_frame=1 AND m.is_cache=0 AND m.menu_type=e.menu_type
    AND COALESCE(m.perms,'')=e.perms AND m.icon=e.icon
    AND m.visible='0' AND m.status='0';
  IF v_actual_count <> v_expected_count THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow/lowcode menu reconciliation failed';
  END IF;

  DELETE rm FROM sys_role_menu rm
  JOIN expected_prod_platform_menu e ON e.menu_id=rm.menu_id
  JOIN sys_role r ON r.role_id=rm.role_id AND r.tenant_id=rm.tenant_id
  WHERE rm.tenant_id=1 AND NOT (r.role_key='admin' AND r.status='0' AND r.del_flag='0');

  INSERT IGNORE INTO sys_role_menu(role_id,menu_id,tenant_id)
  SELECT r.role_id,e.menu_id,r.tenant_id
  FROM sys_role r CROSS JOIN expected_prod_platform_menu e
  WHERE r.tenant_id=1 AND r.role_key = 'admin' AND r.status='0' AND r.del_flag='0';

  SELECT COUNT(*) INTO v_admin_grant_count
  FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id
  JOIN expected_prod_platform_menu e ON e.menu_id=rm.menu_id
  WHERE rm.tenant_id=1 AND r.tenant_id=1 AND r.role_key = 'admin' AND r.status='0' AND r.del_flag='0';
  IF v_admin_grant_count <> v_expected_count THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='admin platform menu grant reconciliation failed';
  END IF;

  SELECT COUNT(*) INTO v_non_admin_grant_count
  FROM sys_role_menu rm JOIN expected_prod_platform_menu e ON e.menu_id=rm.menu_id
  JOIN sys_role r ON r.role_id=rm.role_id AND r.tenant_id=rm.tenant_id
  WHERE rm.tenant_id=1 AND r.role_key<>'admin';
  IF v_non_admin_grant_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='non-admin platform menu grants remain';
  END IF;

  SELECT COUNT(*) INTO v_config_identity_count FROM sys_config WHERE tenant_id=1 AND
    (config_id=109 AND config_key='r22.touch.wework.enabled' OR
     config_id=110 AND config_key='r22.touch.wework.dryRun' OR
     config_id=111 AND config_key='r22.touch.wework.webhookUrl' OR
     config_id=112 AND config_key='r22.touch.rateLimit.perTarget24h' OR
     config_id=113 AND config_key='r23.openapi.dailyQuota.enabled' OR
     config_id=114 AND config_key='r23.openapi.dailyQuota.default');
  IF v_config_identity_count <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='sys_config id/key preconditions failed';
  END IF;

  UPDATE sys_config SET config_name='R22企业微信群机器人启用',remark='R22触达通道开关',update_by='system',update_time=NOW()
  WHERE config_id=109 AND config_key='r22.touch.wework.enabled' AND tenant_id=1;
  UPDATE sys_config SET config_name='R22企业微信群机器人DryRun',remark='DEV默认不真实发送',update_by='system',update_time=NOW()
  WHERE config_id=110 AND config_key='r22.touch.wework.dryRun' AND tenant_id=1;
  UPDATE sys_config SET config_name='R22企业微信群机器人Webhook',remark='真实环境由部署人员配置',update_by='system',update_time=NOW()
  WHERE config_id=111 AND config_key='r22.touch.wework.webhookUrl' AND tenant_id=1;
  UPDATE sys_config SET config_name='R22触达单目标24小时上限',remark='防刷屏',update_by='system',update_time=NOW()
  WHERE config_id=112 AND config_key='r22.touch.rateLimit.perTarget24h' AND tenant_id=1;
  UPDATE sys_config SET config_name='R23开放平台日额度开关',remark='R23 DEV 默认启用日额度校验',update_by='system',update_time=NOW()
  WHERE config_id=113 AND config_key='r23.openapi.dailyQuota.enabled' AND tenant_id=1;
  UPDATE sys_config SET config_name='R23开放平台默认日额度',remark='未配置 Key 额度时的 DEV 默认额度',update_by='system',update_time=NOW()
  WHERE config_id=114 AND config_key='r23.openapi.dailyQuota.default' AND tenant_id=1;

  SELECT COUNT(*) INTO v_config_count FROM sys_config WHERE tenant_id=1 AND
    (config_id=109 AND config_key='r22.touch.wework.enabled' AND config_name='R22企业微信群机器人启用' AND remark='R22触达通道开关' OR
     config_id=110 AND config_key='r22.touch.wework.dryRun' AND config_name='R22企业微信群机器人DryRun' AND remark='DEV默认不真实发送' OR
     config_id=111 AND config_key='r22.touch.wework.webhookUrl' AND config_name='R22企业微信群机器人Webhook' AND remark='真实环境由部署人员配置' OR
     config_id=112 AND config_key='r22.touch.rateLimit.perTarget24h' AND config_name='R22触达单目标24小时上限' AND remark='防刷屏' OR
     config_id=113 AND config_key='r23.openapi.dailyQuota.enabled' AND config_name='R23开放平台日额度开关' AND remark='R23 DEV 默认启用日额度校验' OR
     config_id=114 AND config_key='r23.openapi.dailyQuota.default' AND config_name='R23开放平台默认日额度' AND remark='未配置 Key 额度时的 DEV 默认额度');
  IF v_config_count <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='sys_config mojibake repair reconciliation failed';
  END IF;
  COMMIT;
END//
CALL repair_prod_platform_menu_and_config()//
DROP PROCEDURE repair_prod_platform_menu_and_config//
DELIMITER ;

SELECT m.menu_id,m.parent_id,m.menu_name,HEX(m.menu_name),m.path,m.component,m.menu_type,m.perms
FROM sys_menu m JOIN expected_prod_platform_menu e ON e.menu_id=m.menu_id
ORDER BY m.menu_id;
SELECT r.role_key,COUNT(*) AS granted_menu_count
FROM sys_role_menu rm JOIN sys_role r ON r.role_id=rm.role_id
JOIN expected_prod_platform_menu e ON e.menu_id=rm.menu_id
WHERE rm.tenant_id=1 AND r.tenant_id=1 AND r.role_key='admin' GROUP BY r.role_key;
SELECT config_id,config_key,config_name,HEX(config_name),remark,HEX(remark)
FROM sys_config WHERE tenant_id=1 AND config_id BETWEEN 109 AND 114 AND config_key IN
('r22.touch.wework.enabled','r22.touch.wework.dryRun','r22.touch.wework.webhookUrl',
 'r22.touch.rateLimit.perTarget24h','r23.openapi.dailyQuota.enabled','r23.openapi.dailyQuota.default')
ORDER BY config_id;

DROP TEMPORARY TABLE expected_prod_platform_menu;
