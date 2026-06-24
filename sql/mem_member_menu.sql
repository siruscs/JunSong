-- 会员服务模块菜单SQL
-- 创建日期: 2026-05-31

-- 0. 删除旧的会员服务菜单
SET @menuRootId = (SELECT menu_id FROM sys_menu WHERE menu_name = '会员服务' AND parent_id = 0);
IF @menuRootId IS NOT NULL THEN
    DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE menu_id = @menuRootId OR parent_id = @menuRootId);
    DELETE FROM sys_menu WHERE menu_id = @menuRootId OR parent_id = @menuRootId;
END IF;

-- 1. 查询最大菜单ID
SELECT @maxMenuId := MAX(menu_id) FROM sys_menu;

-- 2. 创建会员服务一级菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (@maxMenuId + 1, '会员服务', 0, 6, 'member', 'member/index', 1, 0, 'M', '0', '0', '', 'el-icon-user-solid', 'admin', NOW(), '', NULL, '会员服务目录');

-- 3. 创建会员信息子菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 2, '会员信息', @maxMenuId + 1, 1, 'member', 'member/index', 1, 0, 'C', '0', '0', 'member:member:list', 'el-icon-user', 'admin', NOW(), '', NULL, '会员信息菜单'),
(@maxMenuId + 3, '秒杀活动', @maxMenuId + 1, 2, 'seckill', 'member/seckill/index', 1, 0, 'C', '0', '0', 'member:seckill:list', 'el-icon-lightning', 'admin', NOW(), '', NULL, '秒杀活动菜单'),
(@maxMenuId + 4, '秒杀记录', @maxMenuId + 1, 3, 'seckillRecord', 'member/seckill/record', 1, 0, 'C', '0', '0', 'member:seckillRecord:list', 'el-icon-document', 'admin', NOW(), '', NULL, '秒杀记录菜单'),
(@maxMenuId + 5, '积分物品', @maxMenuId + 1, 4, 'pointsGoods', 'member/pointsGoods/index', 1, 0, 'C', '0', '0', 'member:pointsGoods:list', 'el-icon-goods', 'admin', NOW(), '', NULL, '积分物品菜单'),
(@maxMenuId + 6, '积分规则', @maxMenuId + 1, 5, 'pointsRule', 'member/pointsRule/index', 1, 0, 'C', '0', '0', 'member:pointsRule:list', 'el-icon-setting', 'admin', NOW(), '', NULL, '积分规则菜单'),
(@maxMenuId + 7, '积分记录', @maxMenuId + 1, 6, 'pointsRecord', 'member/pointsRecord/index', 1, 0, 'C', '0', '0', 'member:pointsRecord:list', 'el-icon-tickets', 'admin', NOW(), '', NULL, '积分记录菜单'),
(@maxMenuId + 8, '积分兑换', @maxMenuId + 1, 7, 'pointsExchange', 'member/pointsExchange/index', 1, 0, 'C', '0', '0', 'member:pointsExchange:list', 'el-icon-shopping-cart-2', 'admin', NOW(), '', NULL, '积分兑换菜单');

-- 4. 创建按钮权限
-- 会员信息按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 9, '会员信息查询', @maxMenuId + 2, 1, '', '', 1, 0, 'F', '0', '0', 'member:member:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 10, '会员信息新增', @maxMenuId + 2, 2, '', '', 1, 0, 'F', '0', '0', 'member:member:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 11, '会员信息修改', @maxMenuId + 2, 3, '', '', 1, 0, 'F', '0', '0', 'member:member:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 12, '会员信息删除', @maxMenuId + 2, 4, '', '', 1, 0, 'F', '0', '0', 'member:member:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 13, '会员信息导出', @maxMenuId + 2, 5, '', '', 1, 0, 'F', '0', '0', 'member:member:export', '#', 'admin', NOW(), '', NULL, '');

-- 秒杀活动按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 14, '秒杀活动查询', @maxMenuId + 3, 1, '', '', 1, 0, 'F', '0', '0', 'member:seckill:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 15, '秒杀活动新增', @maxMenuId + 3, 2, '', '', 1, 0, 'F', '0', '0', 'member:seckill:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 16, '秒杀活动修改', @maxMenuId + 3, 3, '', '', 1, 0, 'F', '0', '0', 'member:seckill:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 17, '秒杀活动删除', @maxMenuId + 3, 4, '', '', 1, 0, 'F', '0', '0', 'member:seckill:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 18, '秒杀活动导出', @maxMenuId + 3, 5, '', '', 1, 0, 'F', '0', '0', 'member:seckill:export', '#', 'admin', NOW(), '', NULL, '');

-- 秒杀记录按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 19, '秒杀记录查询', @maxMenuId + 4, 1, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 20, '秒杀记录新增', @maxMenuId + 4, 2, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 21, '秒杀记录修改', @maxMenuId + 4, 3, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 22, '秒杀记录删除', @maxMenuId + 4, 4, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 23, '秒杀记录导出', @maxMenuId + 4, 5, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:export', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 60, '秒杀记录领取', @maxMenuId + 4, 6, '', '', 1, 0, 'F', '0', '0', 'member:seckillRecord:receive', '#', 'admin', NOW(), '', NULL, '');

-- 积分物品按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 24, '积分物品查询', @maxMenuId + 5, 1, '', '', 1, 0, 'F', '0', '0', 'member:pointsGoods:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 25, '积分物品新增', @maxMenuId + 5, 2, '', '', 1, 0, 'F', '0', '0', 'member:pointsGoods:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 26, '积分物品修改', @maxMenuId + 5, 3, '', '', 1, 0, 'F', '0', '0', 'member:pointsGoods:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 27, '积分物品删除', @maxMenuId + 5, 4, '', '', 1, 0, 'F', '0', '0', 'member:pointsGoods:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 28, '积分物品导出', @maxMenuId + 5, 5, '', '', 1, 0, 'F', '0', '0', 'member:pointsGoods:export', '#', 'admin', NOW(), '', NULL, '');

-- 积分规则按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 29, '积分规则查询', @maxMenuId + 6, 1, '', '', 1, 0, 'F', '0', '0', 'member:pointsRule:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 30, '积分规则新增', @maxMenuId + 6, 2, '', '', 1, 0, 'F', '0', '0', 'member:pointsRule:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 31, '积分规则修改', @maxMenuId + 6, 3, '', '', 1, 0, 'F', '0', '0', 'member:pointsRule:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 32, '积分规则删除', @maxMenuId + 6, 4, '', '', 1, 0, 'F', '0', '0', 'member:pointsRule:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 33, '积分规则导出', @maxMenuId + 6, 5, '', '', 1, 0, 'F', '0', '0', 'member:pointsRule:export', '#', 'admin', NOW(), '', NULL, '');

-- 积分记录按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 34, '积分记录查询', @maxMenuId + 7, 1, '', '', 1, 0, 'F', '0', '0', 'member:pointsRecord:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 35, '积分记录新增', @maxMenuId + 7, 2, '', '', 1, 0, 'F', '0', '0', 'member:pointsRecord:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 36, '积分记录修改', @maxMenuId + 7, 3, '', '', 1, 0, 'F', '0', '0', 'member:pointsRecord:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 37, '积分记录删除', @maxMenuId + 7, 4, '', '', 1, 0, 'F', '0', '0', 'member:pointsRecord:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 38, '积分记录导出', @maxMenuId + 7, 5, '', '', 1, 0, 'F', '0', '0', 'member:pointsRecord:export', '#', 'admin', NOW(), '', NULL, '');

-- 积分兑换按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES 
(@maxMenuId + 39, '积分兑换查询', @maxMenuId + 8, 1, '', '', 1, 0, 'F', '0', '0', 'member:pointsExchange:query', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 40, '积分兑换新增', @maxMenuId + 8, 2, '', '', 1, 0, 'F', '0', '0', 'member:pointsExchange:add', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 41, '积分兑换修改', @maxMenuId + 8, 3, '', '', 1, 0, 'F', '0', '0', 'member:pointsExchange:edit', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 42, '积分兑换删除', @maxMenuId + 8, 4, '', '', 1, 0, 'F', '0', '0', 'member:pointsExchange:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 43, '积分兑换导出', @maxMenuId + 8, 5, '', '', 1, 0, 'F', '0', '0', 'member:pointsExchange:export', '#', 'admin', NOW(), '', NULL, '');

-- 5. 给admin角色分配所有权限
INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id >= @maxMenuId + 1;

-- 说明：
-- 1. 执行此SQL前，请先在数据库中执行 mem_member.sql 创建表结构
-- 2. 如果菜单ID冲突，请调整SQL中的ID值
-- 3. 执行完成后，需要重新登录后台管理系统才能看到新菜单
