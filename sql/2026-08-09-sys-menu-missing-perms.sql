SET NAMES utf8mb4;

-- ========================================================================
-- 补齐 PROD sys_menu 缺失的 37 个权限码 + 3 个聚合目录。
--
-- 背景：
--   从后端 Java 代码（@RequiresPermissions/@PreAuthorize/AuthUtil.hasPermi/MpModuleCatalog.v()）
--   提取 391 个唯一权限码，与 PROD sys_menu.perms（435 条，排除刚加的 workflow:mobile:todo/done/notify）
--   做差集，排除 6 个已废弃的 finance:costAccounting:*，剩余 37 个"代码校验了但菜单管理里看不到"的权限码。
--
-- 目录节点（C 型）新增 3 个：
--   3284  财务管理/财务看板         (parent_id=2000, order_num=93)
--   3285  系统管理/文件管理         (parent_id=1,    order_num=96)
--   3286  日志管理/操作日志         (parent_id=108,  order_num=3)
--
-- 权限码分布（F 型按钮）：
--   工作流·移动办公 3280 下           workflow:mobile: applied/approve/detail/reject = 4
--   财务·库存与成本 2155 下           stocktake*11 + stock:reconciliation + stock:take + stockLedger:list = 14
--   财务·财务看板 3284 下             cashflow:dashboard / dailyReview:view / dashboard:list /
--                                      drilldown:{sales,expenses,profitShare} / report:cost = 7
--   会员·成长值 2509 / 签到 2504 / 会员 2077
--                                      growth:export / signIn:export / identityPolicy:{edit,query} /
--                                      member:member:piiExport = 5
--   系统·委托 2435 / 文件 3285 / 操作日志 3286 / 管理工作台 1
--                                      delegate:edit / file:remove / operlog:{list,export,remove} /
--                                      workbench:notify = 6
--   系统工具·代码生成 115 下          gen:table:list = 1
--   合计 4+14+7+5+6+1 = 37 个 F 型菜单
--
-- 幂等：所有 INSERT ... WHERE NOT EXISTS 按 menu_id + perms 双重兜底。
-- 对账：执行末尾按 perms 查回，HEX(menu_name) 校验 UTF-8 中文编码。
-- ========================================================================

-- ------------------------------------------------------------------------
-- 一、新增 3 个 C 型目录（非外链、非缓存、显示、正常状态）
-- ------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3284, '财务看板', 2000, 93, NULL, NULL, 1, 0, 'C', '0', '0', NULL, 'chart', 'admin', NOW(), '', NULL, '聚合：现金流看板/每日复核/仪表盘/下钻/成本报表'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3280 OR menu_name='财务看板' AND parent_id=2000);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3285, '文件管理', 1, 96, NULL, NULL, 1, 0, 'C', '0', '0', NULL, 'upload', 'admin', NOW(), '', NULL, '聚合：文件删除等文件管理操作权限（system:file:*）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3285 OR menu_name='文件管理' AND parent_id=1);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3286, '操作日志', 108, 3, NULL, NULL, 1, 0, 'C', '0', '0', NULL, 'form', 'admin', NOW(), '', NULL, '操作日志查询/导出/删除（system:operlog:*，与 monitor:operlog:* 双前缀兼容）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3286 OR menu_name='操作日志' AND parent_id=108);

-- ------------------------------------------------------------------------
-- 二、37 个 F 型按钮权限码
-- ------------------------------------------------------------------------

-- 工作流中心·移动办公 3280 下
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3287,'我发起的',3280,4,NULL,NULL,1,0,'F','0','0','workflow:mobile:applied','#','admin',NOW(),'',NULL,'小程序端：已发起流程列表' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3287 OR perms='workflow:mobile:applied');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3288,'审批通过',3280,5,NULL,NULL,1,0,'F','0','0','workflow:mobile:approve','#','admin',NOW(),'',NULL,'小程序端：审批通过动作' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3288 OR perms='workflow:mobile:approve');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3289,'流程详情',3280,6,NULL,NULL,1,0,'F','0','0','workflow:mobile:detail','#','admin',NOW(),'',NULL,'小程序端：流程详情查看' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3289 OR perms='workflow:mobile:detail');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3290,'审批驳回',3280,7,NULL,NULL,1,0,'F','0','0','workflow:mobile:reject','#','admin',NOW(),'',NULL,'小程序端：审批驳回动作' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3290 OR perms='workflow:mobile:reject');

-- 财务管理·库存与成本 2155 下（stocktake 11 个 + 3 个杂项，order_num 从 10 开始 避免与现有 1/2/3 冲突）
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3291,'盘点单查询',2155,10,NULL,NULL,1,0,'F','0','0','finance:stocktake:query','#','admin',NOW(),'',NULL,'库存盘点：查询' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3291 OR perms='finance:stocktake:query');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3292,'盘点单列表',2155,11,NULL,NULL,1,0,'F','0','0','finance:stocktake:list','#','admin',NOW(),'',NULL,'库存盘点：列表' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3292 OR perms='finance:stocktake:list');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3293,'盘点单新增',2155,12,NULL,NULL,1,0,'F','0','0','finance:stocktake:add','#','admin',NOW(),'',NULL,'库存盘点：新增' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3293 OR perms='finance:stocktake:add');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3294,'盘点单修改',2155,13,NULL,NULL,1,0,'F','0','0','finance:stocktake:edit','#','admin',NOW(),'',NULL,'库存盘点：修改' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3294 OR perms='finance:stocktake:edit');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3295,'盘点单删除',2155,14,NULL,NULL,1,0,'F','0','0','finance:stocktake:remove','#','admin',NOW(),'',NULL,'库存盘点：删除' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3295 OR perms='finance:stocktake:remove');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3296,'盘点单导出',2155,15,NULL,NULL,1,0,'F','0','0','finance:stocktake:export','#','admin',NOW(),'',NULL,'库存盘点：导出' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3296 OR perms='finance:stocktake:export');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3297,'盘点单提交',2155,16,NULL,NULL,1,0,'F','0','0','finance:stocktake:submit','#','admin',NOW(),'',NULL,'库存盘点：提交' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3297 OR perms='finance:stocktake:submit');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3298,'分配盘点人',2155,17,NULL,NULL,1,0,'F','0','0','finance:stocktake:assign','#','admin',NOW(),'',NULL,'库存盘点：分配盘点人' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3298 OR perms='finance:stocktake:assign');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3299,'录入盘点数',2155,18,NULL,NULL,1,0,'F','0','0','finance:stocktake:count','#','admin',NOW(),'',NULL,'库存盘点：录入盘点数量' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3299 OR perms='finance:stocktake:count');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3300,'重新盘点',2155,19,NULL,NULL,1,0,'F','0','0','finance:stocktake:recount','#','admin',NOW(),'',NULL,'库存盘点：重新盘点' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3300 OR perms='finance:stocktake:recount');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3301,'盘点单审批',2155,20,NULL,NULL,1,0,'F','0','0','finance:stocktake:approve','#','admin',NOW(),'',NULL,'库存盘点：审批' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3301 OR perms='finance:stocktake:approve');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3302,'盘点单过账',2155,21,NULL,NULL,1,0,'F','0','0','finance:stocktake:post','#','admin',NOW(),'',NULL,'库存盘点：过账' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3302 OR perms='finance:stocktake:post');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3303,'盘点单冲销',2155,22,NULL,NULL,1,0,'F','0','0','finance:stocktake:reverse','#','admin',NOW(),'',NULL,'库存盘点：冲销' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3303 OR perms='finance:stocktake:reverse');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3304,'库存对账',2155,23,NULL,NULL,1,0,'F','0','0','finance:stock:reconciliation','#','admin',NOW(),'',NULL,'库存报表：库存对账执行' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3304 OR perms='finance:stock:reconciliation');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3305,'库存盘点任务',2155,24,NULL,NULL,1,0,'F','0','0','finance:stock:take','#','admin',NOW(),'',NULL,'旧接口：发起盘点任务（兼容用，前端建议切 stocktake:*）' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3305 OR perms='finance:stock:take');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3306,'库存流水列表',2155,25,NULL,NULL,1,0,'F','0','0','finance:stockLedger:list','#','admin',NOW(),'',NULL,'库存流水列表查询（小程序库存流水模块可见性兜底）' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3306 OR perms='finance:stockLedger:list');

-- 财务管理·财务看板 3284 下（7 个）
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3307,'现金流看板',3284,1,NULL,NULL,1,0,'F','0','0','finance:cashflow:dashboard','#','admin',NOW(),'',NULL,'现金流看板视图' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3307 OR perms='finance:cashflow:dashboard');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3308,'每日复核视图',3284,2,NULL,NULL,1,0,'F','0','0','finance:dailyReview:view','#','admin',NOW(),'',NULL,'每日复核看板视图' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3308 OR perms='finance:dailyReview:view');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3309,'财务仪表盘',3284,3,NULL,NULL,1,0,'F','0','0','finance:dashboard:list','#','admin',NOW(),'',NULL,'财务仪表盘主视图' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3309 OR perms='finance:dashboard:list');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3310,'销售下钻',3284,4,NULL,NULL,1,0,'F','0','0','finance:drilldown:sales','#','admin',NOW(),'',NULL,'财务下钻：销售明细' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3310 OR perms='finance:drilldown:sales');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3311,'费用下钻',3284,5,NULL,NULL,1,0,'F','0','0','finance:drilldown:expenses','#','admin',NOW(),'',NULL,'财务下钻：费用明细' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3311 OR perms='finance:drilldown:expenses');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3312,'分润下钻',3284,6,NULL,NULL,1,0,'F','0','0','finance:drilldown:profitShare','#','admin',NOW(),'',NULL,'财务下钻：分润明细' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3312 OR perms='finance:drilldown:profitShare');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3313,'成本报表视图',3284,7,NULL,NULL,1,0,'F','0','0','finance:report:cost','#','admin',NOW(),'',NULL,'财务报表：成本报表' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3313 OR perms='finance:report:cost');

-- 会员模块 5 个
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3314,'成长值导出',2509,5,NULL,NULL,1,0,'F','0','0','member:growth:export','#','admin',NOW(),'',NULL,'成长值记录导出' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3314 OR perms='member:growth:export');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3315,'签到记录导出',2504,5,NULL,NULL,1,0,'F','0','0','member:signIn:export','#','admin',NOW(),'',NULL,'签到记录导出' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3315 OR perms='member:signIn:export');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3316,'身份策略查询',2077,10,NULL,NULL,1,0,'F','0','0','member:identityPolicy:query','#','admin',NOW(),'',NULL,'会员身份策略查询' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3316 OR perms='member:identityPolicy:query');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3317,'身份策略修改',2077,11,NULL,NULL,1,0,'F','0','0','member:identityPolicy:edit','#','admin',NOW(),'',NULL,'会员身份策略修改' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3317 OR perms='member:identityPolicy:edit');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3318,'PII明文导出',2077,12,NULL,NULL,1,0,'F','0','0','member:member:piiExport','#','admin',NOW(),'',NULL,'【敏感】会员 PII（手机号/身份证等）明文导出。⚠ 仅授予需要下载手机号用于外部营销的少数岗位，默认关闭' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3318 OR perms='member:member:piiExport');

-- 系统模块 6 个
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3319,'委托设置修改',2435,5,NULL,NULL,1,0,'F','0','0','system:delegate:edit','#','admin',NOW(),'',NULL,'用户代理修改' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3319 OR perms='system:delegate:edit');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3320,'文件删除',3285,1,NULL,NULL,1,0,'F','0','0','system:file:remove','#','admin',NOW(),'',NULL,'文件删除' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3320 OR perms='system:file:remove');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3321,'操作日志查询',3286,1,NULL,NULL,1,0,'F','0','0','system:operlog:list','#','admin',NOW(),'',NULL,'与 monitor:operlog:list 双前缀兼容' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3321 OR perms='system:operlog:list');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3322,'操作日志导出',3286,2,NULL,NULL,1,0,'F','0','0','system:operlog:export','#','admin',NOW(),'',NULL,'与 monitor:operlog:export 双前缀兼容' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3322 OR perms='system:operlog:export');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3323,'操作日志删除',3286,3,NULL,NULL,1,0,'F','0','0','system:operlog:remove','#','admin',NOW(),'',NULL,'与 monitor:operlog:remove 双前缀兼容' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3323 OR perms='system:operlog:remove');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3324,'工作台通知',1,98,NULL,NULL,1,0,'F','0','0','system:workbench:notify','#','admin',NOW(),'',NULL,'工作台通知权限' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3324 OR perms='system:workbench:notify');

-- 系统工具·代码生成 115 下
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 3325,'代码表查询',115,6,NULL,NULL,1,0,'F','0','0','gen:table:list','#','admin',NOW(),'',NULL,'与 tool:gen:list 双前缀兼容（GenController 内部注解名）' WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=3325 OR perms='gen:table:list');

-- ------------------------------------------------------------------------
-- 三、对账：按本次所有 perms 查回，校验 HEX(menu_name)
--   预期 40 行全部 encode_check=OK（3 C + 37 F），若出现 WARN 说明中文被破坏
-- ------------------------------------------------------------------------
SELECT
    menu_id,
    parent_id,
    menu_type,
    menu_name,
    HEX(menu_name)                                                              AS hex_menu_name,
    perms,
    CASE
        WHEN menu_type='C' AND menu_id IN (3284,3285,3286) THEN
            CASE HEX(menu_name)
                WHEN 'E8B4A2E58AA1E79C8BE69DBF' THEN 'OK'  -- 财务看板
                WHEN 'E69687E4BBB6E7AEA1E79086' THEN 'OK'  -- 文件管理
                WHEN 'E6938DE4BD9CE697A5E5BF97' THEN 'OK'  -- 操作日志
                ELSE 'WARN' END
        WHEN perms='workflow:mobile:applied'  AND HEX(menu_name)='E68891E58F91E8B5B7E79A84' THEN 'OK'
        WHEN perms='workflow:mobile:approve'  AND HEX(menu_name)='E5AEA1E689B9E9809AE8BF87' THEN 'OK'
        WHEN perms='workflow:mobile:detail'   AND HEX(menu_name)='E6B581E7A88BE8AFA6E68385' THEN 'OK'  -- 实际 UTF-8：E6B581 E7A88B E8AFA6 E68385（SQL 对账里写错的 expected 是旧字符串，保留 CASE 以 实际值为准，不影响执行）
        WHEN perms='workflow:mobile:reject'   AND HEX(menu_name)='E5AEA1E689B9E9A9B3E59B9E' THEN 'OK'
        WHEN perms='finance:stocktake:query'      AND HEX(menu_name)='E79B98E782B9E58D95E69FA5E8AFA2' THEN 'OK'
        WHEN perms='finance:stocktake:list'       AND HEX(menu_name)='E79B98E782B9E58D95E58897E8A1A8' THEN 'OK'
        WHEN perms='finance:stocktake:add'        AND HEX(menu_name)='E79B98E782B9E58D95E696B0E5A29E' THEN 'OK'
        WHEN perms='finance:stocktake:edit'       AND HEX(menu_name)='E79B98E782B9E58D95E4BFAEE694B9' THEN 'OK'
        WHEN perms='finance:stocktake:remove'     AND HEX(menu_name)='E79B98E782B9E58D95E588A0E999A4' THEN 'OK'
        WHEN perms='finance:stocktake:export'     AND HEX(menu_name)='E79B98E782B9E58D95E5AFBCE587BA' THEN 'OK'
        WHEN perms='finance:stocktake:submit'     AND HEX(menu_name)='E79B98E782B9E58D95E68F90E4BAA4' THEN 'OK'
        WHEN perms='finance:stocktake:assign'     AND HEX(menu_name)='E58886E9858DE79B98E782B9E4BABA' THEN 'OK'
        WHEN perms='finance:stocktake:count'      AND HEX(menu_name)='E5BD95E585A5E79B98E782B9E695B0' THEN 'OK'
        WHEN perms='finance:stocktake:recount'    AND HEX(menu_name)='E9878DE696B0E79B98E782B9'     THEN 'OK'
        WHEN perms='finance:stocktake:approve'    AND HEX(menu_name)='E79B98E782B9E58D95E5AEA1E689B9' THEN 'OK'
        WHEN perms='finance:stocktake:post'       AND HEX(menu_name)='E79B98E782B9E58D95E8BF87E8B4A6' THEN 'OK'
        WHEN perms='finance:stocktake:reverse'    AND HEX(menu_name)='E79B98E782B9E58D95E586B2E99480' THEN 'OK'
        WHEN perms='finance:stock:reconciliation' AND HEX(menu_name)='E5BA93E5AD98E5AFB9E8B4A6' THEN 'OK'
        WHEN perms='finance:stock:take'           AND HEX(menu_name)='E5BA93E5AD98E79B98E782B9E4BBBBE58AA1' THEN 'OK'
        WHEN perms='finance:stockLedger:list'     AND HEX(menu_name)='E5BA93E5AD98E6B581E6B0B4E58897E8A1A8' THEN 'OK'
        WHEN perms='finance:cashflow:dashboard'   AND HEX(menu_name)='E78EB0E98791E6B581E79C8BE69DBF' THEN 'OK'
        WHEN perms='finance:dailyReview:view'     AND HEX(menu_name)='E6AF8FE697A5E5A48DE6A0B8E8A786E59BBE' THEN 'OK'
        WHEN perms='finance:dashboard:list'       AND HEX(menu_name)='E8B4A2E58AA1E4BBAAE8A1A8E79B98' THEN 'OK'
        WHEN perms='finance:drilldown:sales'      AND HEX(menu_name)='E99480E594AEE4B88BE992BB' THEN 'OK'
        WHEN perms='finance:drilldown:expenses'   AND HEX(menu_name)='E8B4B9E794A8E4B88BE992BB' THEN 'OK'
        WHEN perms='finance:drilldown:profitShare' AND HEX(menu_name)='E58886E6B6A6E4B88BE992BB' THEN 'OK'
        WHEN perms='finance:report:cost'          AND HEX(menu_name)='E68890E69CACE68AA5E8A1A8E8A786E59BBE' THEN 'OK'
        WHEN perms='member:growth:export'         AND HEX(menu_name)='E68890E995BFE580BCE5AFBCE587BA' THEN 'OK'
        WHEN perms='member:signIn:export'         AND HEX(menu_name)='E7ADBEE588B0E8AEB0E5BD95E5AFBCE587BA' THEN 'OK'
        WHEN perms='member:identityPolicy:query'  AND HEX(menu_name)='E8BAABE4BBBDE7AD96E795A5E69FA5E8AFA2' THEN 'OK'
        WHEN perms='member:identityPolicy:edit'   AND HEX(menu_name)='E8BAABE4BBBDE7AD96E795A5E4BFAEE694B9' THEN 'OK'
        WHEN perms='member:member:piiExport'      AND HEX(menu_name)='504949E6988EE69687E5AFBCE587BA'     THEN 'OK'  -- ASCII 'PII'+中文
        WHEN perms='system:delegate:edit'         AND HEX(menu_name)='E5A794E68998E8AEBEE7BDAEE4BFAEE694B9' THEN 'OK'
        WHEN perms='system:file:remove'           AND HEX(menu_name)='E69687E4BBB6E588A0E999A4' THEN 'OK'
        WHEN perms='system:operlog:list'          AND HEX(menu_name)='E6938DE4BD9CE697A5E5BF97E69FA5E8AFA2' THEN 'OK'
        WHEN perms='system:operlog:export'        AND HEX(menu_name)='E6938DE4BD9CE697A5E5BF97E5AFBCE587BA' THEN 'OK'
        WHEN perms='system:operlog:remove'        AND HEX(menu_name)='E6938DE4BD9CE697A5E5BF97E588A0E999A4' THEN 'OK'
        WHEN perms='system:workbench:notify'      AND HEX(menu_name)='E5B7A5E4BD9CE58FB0E9809AE79FA5' THEN 'OK'
        WHEN perms='gen:table:list'               AND HEX(menu_name)='E4BBA3E7A081E8A1A8E69FA5E8AFA2' THEN 'OK'
        ELSE 'WARN: 未命中 HEX 映射，请人工核查'
    END                                                                         AS encode_check
FROM sys_menu
WHERE menu_id BETWEEN 3284 AND 3325
ORDER BY menu_id;
