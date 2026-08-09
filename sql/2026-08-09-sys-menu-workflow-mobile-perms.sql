SET NAMES utf8mb4;

-- ========================================================================
-- PC 端「菜单管理 / 角色授权」：补齐小程序移动办公 3 个聚合权限码。
--
-- 背景：
--   小程序端 工作台 → 移动办公分组（待办任务 / 已办任务 / 消息通知）原先依赖
--   MpModuleCatalog 的 viewPermissions=['workflow:mobile:todo'|'done'|'notify'] 做二次过滤。
--   但这 3 个权限码是小程序端聚合入口专用，后端 workflow 服务的真实权限（task:approve 等）
--   是在点击进入后才做鉴权。为了让「PC 端小程序权限页勾了模块授权即可看到入口」，
--   后续已把 viewPermissions 置空；但管理员仍希望在 PC 端能显式看到/勾选这 3 个权限码，
--   所以在 sys_menu 菜单树（工作流中心 2220 下）里补齐这 3 条 F 类型按钮菜单。
--
-- 菜单结构：
--   2220 工作流中心           (已存在 M 型一级菜单)
--     └─ 3280 移动办公         (目录/C 型，无路由/无组件，纯聚合容器)
--         ├─ 3281 待办任务     (F 型按钮，perms=workflow:mobile:todo)
--         ├─ 3282 已办任务     (F 型按钮，perms=workflow:mobile:done)
--         └─ 3283 消息通知     (F 型按钮，perms=workflow:mobile:notify)
--
-- 幂等：
--   * 所有 INSERT ... WHERE NOT EXISTS 按 menu_id / perms 双重校验。
--   * 执行完毕会打印对账：按 perms 查 menu_id/menu_name/parent_id + HEX(menu_name)。
--
-- 执行：
--   docker exec junsong-mysql mysql -u root -p'$MYSQL_ROOT_PASSWORD' \
--     --default-character-set=utf8mb4 junsong-cloud \
--     < 2026-08-09-sys-menu-workflow-mobile-perms.sql
-- ========================================================================

-- ------------------------------------------------------------------------
-- 1) 插入目录节点： 工作流中心 / 移动办公 (C 目录，不路由)
-- ------------------------------------------------------------------------
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, is_frame,
    is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
    update_by, update_time, remark
)
SELECT
    3280, '移动办公', 2220, 12, NULL, NULL, 1,
    0, 'C', '0', '0', NULL, 'skill', 'admin', NOW(),
    '', NULL, '小程序端移动办公入口聚合目录，不直接承载 PC 页面'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 3280 OR perms = 'workflow:mobile:GROUP'
);

-- ------------------------------------------------------------------------
-- 2) 3 个权限码（F 型按钮菜单）
-- ------------------------------------------------------------------------
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, is_frame,
    is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
    update_by, update_time, remark
)
SELECT
    3281, '待办任务', 3280, 1, NULL, NULL, 1,
    0, 'F', '0', '0', 'workflow:mobile:todo', '#', 'admin', NOW(),
    '', NULL, '小程序端：工作流待办任务聚合入口（workflow:mobile:todo）'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 3281 OR perms = 'workflow:mobile:todo'
);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, is_frame,
    is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
    update_by, update_time, remark
)
SELECT
    3282, '已办任务', 3280, 2, NULL, NULL, 1,
    0, 'F', '0', '0', 'workflow:mobile:done', '#', 'admin', NOW(),
    '', NULL, '小程序端：工作流已办任务聚合入口（workflow:mobile:done）'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 3282 OR perms = 'workflow:mobile:done'
);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, is_frame,
    is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
    update_by, update_time, remark
)
SELECT
    3283, '消息通知', 3280, 3, NULL, NULL, 1,
    0, 'F', '0', '0', 'workflow:mobile:notify', '#', 'admin', NOW(),
    '', NULL, '小程序端：工作流消息通知聚合入口（workflow:mobile:notify）'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 3283 OR perms = 'workflow:mobile:notify'
);

-- ------------------------------------------------------------------------
-- 3) 对账输出：中文 menu_name 必须匹配；如果 HEX 不对说明编码被破坏，应回滚。
--    「移动办公」UTF-8 HEX = E7A7BBE58AA8E58A9EE585AC
--    「待办任务」UTF-8 HEX = E5BE85E58A9EE4BBBBE58AA1
--    「已办任务」UTF-8 HEX = E5B7B2E58A9EE4BBBBE58AA1
--    「消息通知」UTF-8 HEX = E6B688E681AFE9809AE79FA5
-- ------------------------------------------------------------------------
SELECT
    menu_id,
    parent_id,
    menu_name,
    HEX(menu_name)                                             AS hex_menu_name,
    perms,
    menu_type,
    visible,
    CASE HEX(menu_name)
        WHEN 'E7A7BBE58AA8E58A9EE585AC' THEN 'OK'
        WHEN 'E5BE85E58A9EE4BBBBE58AA1' THEN 'OK'
        WHEN 'E5B7B2E58A9EE4BBBBE58AA1' THEN 'OK'
        WHEN 'E6B688E681AFE9809AE79FA5' THEN 'OK'
        ELSE 'WARN: 编码异常，请用 NOT EXISTS 核对后手动检查'
    END                                                        AS encode_check
FROM sys_menu
WHERE perms IN (
    'workflow:mobile:todo',
    'workflow:mobile:done',
    'workflow:mobile:notify'
) OR menu_id = 3280
ORDER BY menu_id;
