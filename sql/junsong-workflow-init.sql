-- =====================================================
-- 峻松云 · 工作流模块（Workflow）初始化脚本
-- 版本: Flowable 8.0.0
-- 日期: 2026-06-21
-- 说明:
--   1. Flowable 引擎表由 flowable.database-schema-update: true 自动创建
--      首次启动后会自动生成 ACT_* 系列表，无需手动执行 DDL
--   2. 本脚本初始化:
--      - 工作流菜单与按钮权限（sys_menu / sys_role_menu）
--      - Nacos 配置中心（junsong-config.config_info）
--      - 网关路由规则（junsong-gateway-*.yml）
--      - Flowable 引擎初始属性（act_ge_property）
-- =====================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 默认数据库: junsong-cloud（sys_menu / sys_role_menu / ACT_*）
-- Nacos 配置使用: junsong-config.config_info（全限定名）
USE `junsong-cloud`;


-- =====================================================
-- Part 1. 工作流菜单与按钮权限
-- =====================================================

-- 清理旧的工作流菜单（使用临时变量绕过 MySQL 子查询限制）
SELECT menu_id INTO @workflowRootId FROM sys_menu WHERE menu_name = '工作流中心' AND parent_id = 0 LIMIT 1;

SET @workflowRootId := COALESCE(@workflowRootId, 0);

-- 先收集所有待删除的 menu_id
SELECT GROUP_CONCAT(CAST(menu_id AS CHAR)) INTO @workflowChildIds FROM sys_menu WHERE parent_id = @workflowRootId;

-- 删除 role_menu 关联
DELETE FROM sys_role_menu WHERE menu_id = @workflowRootId;
DELETE FROM sys_role_menu WHERE FIND_IN_SET(menu_id, @workflowChildIds);

-- 删除菜单
DELETE FROM sys_menu WHERE parent_id = @workflowRootId;
DELETE FROM sys_menu WHERE menu_id = @workflowRootId;

-- 获取当前最大 menu_id
SELECT MAX(menu_id) INTO @maxMenuId FROM sys_menu;

-- 一级菜单：工作流中心
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 1, '工作流中心', 0, 7, 'workflow', NULL,
 1, 0, 'M', '0', '0', '', 'guide',
 'admin', NOW(), '', NULL, '工作流中心顶级目录');

-- 二级菜单：流程定义、流程实例、任务中心、历史记录
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 2, '流程定义', @maxMenuId + 1, 1, 'definition', 'workflow/definition/index',
 1, 0, 'C', '0', '0', 'workflow:definition:list', 'tree',
 'admin', NOW(), '', NULL, '流程定义中心'),
(@maxMenuId + 3, '流程实例', @maxMenuId + 1, 2, 'instance', 'workflow/instance/index',
 1, 0, 'C', '0', '0', 'workflow:instance:list', 'form',
 'admin', NOW(), '', NULL, '流程实例管理'),
(@maxMenuId + 4, '任务中心', @maxMenuId + 1, 3, 'task', 'workflow/task/index',
 1, 0, 'C', '0', '0', 'workflow:task:list', 'skill',
 'admin', NOW(), '', NULL, '任务处理中心'),
(@maxMenuId + 5, '历史记录', @maxMenuId + 1, 4, 'history', 'workflow/history/index',
 1, 0, 'C', '0', '0', 'workflow:history:list', 'log',
 'admin', NOW(), '', NULL, '流程历史追溯');

-- ---- 流程定义 · 按钮权限（parent = @maxMenuId + 2）----
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 6,  '流程定义查询', @maxMenuId + 2, 1, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:query',  '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 7,  '流程定义新增', @maxMenuId + 2, 2, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:add',    '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 8,  '流程定义修改', @maxMenuId + 2, 3, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:edit',   '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 9,  '流程定义删除', @maxMenuId + 2, 4, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:remove', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 10, '流程定义发布', @maxMenuId + 2, 5, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:deploy', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 11, '流程定义导出', @maxMenuId + 2, 6, '', '', 1, 0, 'F', '0', '0', 'workflow:definition:export', '#', 'admin', NOW(), '', NULL, '');

-- ---- 流程实例 · 按钮权限（parent = @maxMenuId + 3）----
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 12, '流程实例查询',  @maxMenuId + 3, 1, '', '', 1, 0, 'F', '0', '0', 'workflow:instance:query',    '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 13, '流程实例发起',  @maxMenuId + 3, 2, '', '', 1, 0, 'F', '0', '0', 'workflow:instance:start',    '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 14, '流程实例挂起',  @maxMenuId + 3, 3, '', '', 1, 0, 'F', '0', '0', 'workflow:instance:suspend',  '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 15, '流程实例激活',  @maxMenuId + 3, 4, '', '', 1, 0, 'F', '0', '0', 'workflow:instance:activate', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 16, '流程实例删除',  @maxMenuId + 3, 5, '', '', 1, 0, 'F', '0', '0', 'workflow:instance:remove',   '#', 'admin', NOW(), '', NULL, '');

-- ---- 任务中心 · 按钮权限（parent = @maxMenuId + 4）----
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 17, '任务查询',  @maxMenuId + 4, 1, '', '', 1, 0, 'F', '0', '0', 'workflow:task:query',    '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 18, '任务审批',  @maxMenuId + 4, 2, '', '', 1, 0, 'F', '0', '0', 'workflow:task:approve',  '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 19, '任务驳回',  @maxMenuId + 4, 3, '', '', 1, 0, 'F', '0', '0', 'workflow:task:reject',   '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 20, '任务转办',  @maxMenuId + 4, 4, '', '', 1, 0, 'F', '0', '0', 'workflow:task:delegate', '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 21, '任务委派',  @maxMenuId + 4, 5, '', '', 1, 0, 'F', '0', '0', 'workflow:task:assign',   '#', 'admin', NOW(), '', NULL, '');

-- ---- 历史记录 · 按钮权限（parent = @maxMenuId + 5）----
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(@maxMenuId + 22, '历史记录查询', @maxMenuId + 5, 1, '', '', 1, 0, 'F', '0', '0', 'workflow:history:query',  '#', 'admin', NOW(), '', NULL, ''),
(@maxMenuId + 23, '历史记录导出', @maxMenuId + 5, 2, '', '', 1, 0, 'F', '0', '0', 'workflow:history:export', '#', 'admin', NOW(), '', NULL, '');

-- ---- 授权 admin 角色（role_id = 1）拥有所有工作流菜单 ----
INSERT INTO `sys_role_menu` (role_id, menu_id)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN @maxMenuId + 1 AND @maxMenuId + 23;


-- =====================================================
-- Part 2. Nacos 配置中心 · Workflow 模块配置
-- =====================================================

-- ---- 2.1 Workflow DEV 配置（junsong-workflow-dev.yml，租户空 + public）----
SET @workflowDevConfig = 'springdoc:
  gatewayUrl: http://localhost:8080
  api-docs:
    enabled: true
  info:
    title: workflow
    description: workflow api
    version: 1.0.0

flowable:
  database-schema-update: true
  idm:
    enabled: false
  form:
    enabled: false
  content:
    enabled: false
  dmn:
    enabled: false
  cmmn:
    enabled: false
  async-executor-activate: true
  history-level: full
  process-definition-location-prefix: classpath:/processes/
  process-definition-location-suffixes:
    - "**.bpmn"
    - "**.bpmn20.xml"
  check-process-definitions: true
  variable-json-mapper: jackson2

logging:
  level:
    org.flowable: info
    com.junsong.workflow: debug';

INSERT INTO `junsong-config`.`config_info`
(data_id, group_id, tenant_id, app_name, content, md5, encrypted_data_key, gmt_create, gmt_modified, src_user, src_ip)
VALUES
('junsong-workflow-dev.yml', 'DEFAULT_GROUP', '', 'junsong-workflow',
 @workflowDevConfig, '49dc9012fe8fc2a6f9d17a0e9a1d1e0b', '',
 NOW(), NOW(), NULL, NULL),
('junsong-workflow-dev.yml', 'DEFAULT_GROUP', 'public', 'junsong-workflow',
 @workflowDevConfig, '49dc9012fe8fc2a6f9d17a0e9a1d1e0b', '',
 NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
content = VALUES(content), md5 = VALUES(md5), gmt_modified = NOW();

-- ---- 2.2 Workflow PROD 配置（junsong-workflow-prod.yml，租户空 + public）----
SET @workflowProdConfig = 'springdoc:
  gatewayUrl: http://junsong.vip
  api-docs:
    enabled: true
  info:
    title: workflow
    description: workflow api
    version: 1.0.0

flowable:
  database-schema-update: true
  idm:
    enabled: false
  form:
    enabled: false
  content:
    enabled: false
  dmn:
    enabled: false
  cmmn:
    enabled: false
  async-executor-activate: true
  history-level: full
  process-definition-location-prefix: classpath:/processes/
  process-definition-location-suffixes:
    - "**.bpmn"
    - "**.bpmn20.xml"
  check-process-definitions: true
  variable-json-mapper: jackson2

logging:
  level:
    org.flowable: warn
    com.junsong.workflow: info';

INSERT INTO `junsong-config`.`config_info`
(data_id, group_id, tenant_id, app_name, content, md5, encrypted_data_key, gmt_create, gmt_modified, src_user, src_ip)
VALUES
('junsong-workflow-prod.yml', 'DEFAULT_GROUP', '', 'junsong-workflow',
 @workflowProdConfig, '49dc9012fe8fc2a6f9d17a0e9a1d1e0b', '',
 NOW(), NOW(), NULL, NULL),
('junsong-workflow-prod.yml', 'DEFAULT_GROUP', 'public', 'junsong-workflow',
 @workflowProdConfig, '49dc9012fe8fc2a6f9d17a0e9a1d1e0b', '',
 NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
content = VALUES(content), md5 = VALUES(md5), gmt_modified = NOW();


-- =====================================================
-- Part 3. Nacos 配置中心 · Gateway 路由（Workflow 路径）
-- =====================================================

-- 说明:
--   Gateway 使用 spring.cloud.gateway.server.webflux.routes 配置路由
--   此处将 workflow 路由追加到 junsong-gateway-*.yml 中
--   同时在 ignore-whites 中放行健康检查接口

SET @gwRoutesBlock = '            - id: junsong-workflow
              uri: lb://junsong-workflow
              predicates:
                - Path=/workflow/**
              filters:
                - StripPrefix=1
';

-- ---- 3.1 更新 DEV Gateway 配置，追加 workflow 路由 ----
UPDATE `junsong-config`.`config_info`
SET content = REPLACE(content, '    sentinel:', CONCAT(@gwRoutesBlock, '    sentinel:')),
    gmt_modified = NOW()
WHERE data_id = 'junsong-gateway-dev.yml'
  AND INSTR(content, 'junsong-workflow') = 0;

-- 追加 workflow 健康检查白名单（针对 DEV）
UPDATE `junsong-config`.`config_info`
SET content = REPLACE(content, '      - /csrf', '      - /workflow/health/**
      - /csrf'),
    gmt_modified = NOW()
WHERE data_id = 'junsong-gateway-dev.yml'
  AND INSTR(content, '/workflow/health/**') = 0;

-- ---- 3.2 更新 PROD Gateway 配置，追加 workflow 路由 ----
UPDATE `junsong-config`.`config_info`
SET content = REPLACE(content, '    sentinel:', CONCAT(@gwRoutesBlock, '    sentinel:')),
    gmt_modified = NOW()
WHERE data_id = 'junsong-gateway-prod.yml'
  AND INSTR(content, 'junsong-workflow') = 0;

UPDATE `junsong-config`.`config_info`
SET content = REPLACE(content, '      - /csrf', '      - /workflow/health/**
      - /csrf'),
    gmt_modified = NOW()
WHERE data_id = 'junsong-gateway-prod.yml'
  AND INSTR(content, '/workflow/health/**') = 0;


-- =====================================================
-- Part 4. Flowable 引擎 · 初始种子数据
-- =====================================================

-- 说明:
--   Flowable 启动时会自动创建 ACT_ 系列表并写入 act_ge_property 种子数据
--   以下语句在表已存在时初始化版本信息，不存在时静默跳过

REPLACE INTO `junsong-cloud`.act_ge_property (NAME_, VALUE_, REV_)
VALUES
('schema.version', '8.0.0.0', 1),
('schema.history', 'create(8.0.0.0)', 1),
('next.dbid', '1', 1),
('common.schema.version', '8.0.0.0', 1),
('cfg.execution-related-entities-count', 'true', 1),
('cfg.task-related-entities-count', 'true', 1),
('eventregistry.schema.version', '8.0.0.0', 1);


-- =====================================================
-- Part 5. 初始化完成 · 验证
-- =====================================================

-- ---- 5.1 验证菜单数量 ----
SET @workflowMenuCount = (
    SELECT COUNT(*) FROM sys_menu
    WHERE menu_name = '工作流中心' OR perms LIKE 'workflow:%'
);
SELECT CONCAT('工作流菜单初始化完成，共 ', @workflowMenuCount, ' 条记录') AS '初始化验证';

-- ---- 5.2 验证 Nacos Workflow 配置 ----
SET @nacosCount = (
    SELECT COUNT(*) FROM `junsong-config`.config_info
    WHERE data_id IN ('junsong-workflow-dev.yml', 'junsong-workflow-prod.yml')
);
SELECT CONCAT('Nacos Workflow 配置初始化完成，共 ', @nacosCount, ' 条记录') AS 'Nacos 配置验证';

-- ---- 5.3 验证 Gateway 路由 ----
SET @gwWorkflowCount = (
    SELECT COUNT(*) FROM `junsong-config`.config_info
    WHERE data_id LIKE 'junsong-gateway-%' AND INSTR(content, 'junsong-workflow') > 0
);
SELECT CONCAT('Gateway Workflow 路由初始化完成，共 ', @gwWorkflowCount, ' 条配置包含 workflow 路由') AS 'Gateway 路由验证';

-- ---- 5.4 验证 Flowable 引擎属性 ----
SET @flowableCount = (SELECT COUNT(*) FROM act_ge_property);
SELECT CONCAT('Flowable 引擎属性初始化完成，共 ', @flowableCount, ' 条记录') AS 'Flowable 引擎验证';
