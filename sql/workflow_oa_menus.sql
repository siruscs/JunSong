-- ============================================================
-- 工作流OA增强：补全 sys_menu 菜单注册 + admin角色权限分配
-- Phase 4-8 的代码和路由已存在，但数据库菜单从未注册，导致用户看不到
-- 本脚本补全所有缺失菜单
-- ============================================================

-- 使用固定 menu_id 避免重复（从 2310 开始）
-- 父菜单：工作流管理 menu_id=2220 (path=workflow)

-- ===== 1. 流程分析 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2310, '流程分析', 2220, 5, 'analytics', 'workflow/analytics/index', '', 1, 0, 'C', '0', '0', 'workflow:analytics:list', 'chart', 'admin', NOW(), '流程分析报表');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2311, '分析查询', 2310, 1, '', '', '', 1, 0, 'F', '0', '0', 'workflow:analytics:query', '#', 'admin', NOW(), '');

-- ===== 2. 超时配置 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2320, '超时配置', 2220, 6, 'timeout', 'workflow/timeout/index', '', 1, 0, 'C', '0', '0', 'workflow:timeout:list', 'time', 'admin', NOW(), '节点超时配置');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2321, '超时查询', 2320, 1, '', '', '', 1, 0, 'F', '0', '0', 'workflow:timeout:query', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2322, '超时新增', 2320, 2, '', '', '', 1, 0, 'F', '0', '0', 'workflow:timeout:add', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2323, '超时修改', 2320, 3, '', '', '', 1, 0, 'F', '0', '0', 'workflow:timeout:edit', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2324, '超时删除', 2320, 4, '', '', '', 1, 0, 'F', '0', '0', 'workflow:timeout:remove', '#', 'admin', NOW(), '');

-- ===== 3. 版本管理 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2330, '版本管理', 2220, 7, 'version', 'workflow/version/index', '', 1, 0, 'C', '0', '0', 'workflow:version:list', 'version', 'admin', NOW(), '流程版本管理');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2331, '版本查询', 2330, 1, '', '', '', 1, 0, 'F', '0', '0', 'workflow:version:query', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2332, '版本挂起', 2330, 2, '', '', '', 1, 0, 'F', '0', '0', 'workflow:version:suspend', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2333, '版本激活', 2330, 3, '', '', '', 1, 0, 'F', '0', '0', 'workflow:version:activate', '#', 'admin', NOW(), '');

-- ===== 4. 字段权限 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2340, '字段权限', 2220, 8, 'field-permission', 'workflow/field-permission/index', '', 1, 0, 'C', '0', '0', 'workflow:fieldPermission:list', 'lock', 'admin', NOW(), '节点字段权限配置');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2341, '权限查询', 2340, 1, '', '', '', 1, 0, 'F', '0', '0', 'workflow:fieldPermission:query', '#', 'admin', NOW(), '');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2342, '权限配置', 2340, 2, '', '', '', 1, 0, 'F', '0', '0', 'workflow:fieldPermission:edit', '#', 'admin', NOW(), '');

-- ===== 5. 实例干预 =====
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2350, '实例干预', 2220, 9, 'intervene', 'workflow/intervene/index', '', 1, 0, 'C', '0', '0', 'workflow:instance:intervene', 'tool', 'admin', NOW(), '流程实例干预');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2351, '干预查询', 2350, 1, '', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:intervene:query', '#', 'admin', NOW(), '');

-- ===== 分配给 admin 角色 (role_id=1) =====
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2310), (1, 2311),
(1, 2320), (1, 2321), (1, 2322), (1, 2323), (1, 2324),
(1, 2330), (1, 2331), (1, 2332), (1, 2333),
(1, 2340), (1, 2341), (1, 2342),
(1, 2350), (1, 2351);
