# PROD 工作流与低代码扩展表补齐设计

## 问题

PROD 已有 Flowable 原生 `act_*`、`flw_*` 表，但当前工作流模块使用的全部 `wf_*`、`lc_*` 扩展表缺失。菜单恢复后，字段权限页面首先访问 `wf_node_field_permission`，因此暴露“表不存在”。当前多租户拦截器还会向业务查询注入 `tenant_id`，而仓库部分旧迁移没有该字段，不能直接用于 PROD。

## 方案

新增一份与当前 Mapper/Domain 对齐的统一迁移：

- 工作流扩展表：`wf_node_field_permission`、`wf_node_timeout`、`wf_timeout_trigger_log`、`wf_task_addsign`、`wf_task_attachment`、`wf_task_cc`。
- 低代码扩展表：`lc_biz_object`、`lc_biz_field`、`lc_biz_page_schema`、`lc_biz_node_assignee`、`lc_biz_branch_rule`、`lc_biz_instance`、`lc_biz_config_snapshot`、`lc_biz_node_timer`、`lc_biz_template`、`lc_biz_action`、`lc_biz_post_action`。
- 所有业务扩展表显式包含非空 `tenant_id`，默认租户为 1；业务唯一键和常用查询索引以租户为首列。
- 所有表使用 `CREATE TABLE IF NOT EXISTS`；禁止 `DROP TABLE`、`TRUNCATE` 和破坏性回填。
- 对未来可能已部分存在的表，迁移先检查表结构；若同名表存在但缺少关键租户列则 fail closed，不做猜测式 ALTER。
- SQL 以 `SET NAMES utf8mb4;` 开头，通过统一 PROD SQL 部署脚本执行并自动备份。

## 验证

- 静态契约核对 17 张表、租户字段、禁止破坏性语句及 reconciliation 查询。
- PROD 执行后从 `information_schema` 核对 17 张表均存在且都有 `tenant_id`。
- 核对 `wf_node_field_permission` 的租户索引和唯一键，并重新访问字段权限查询。
- 只提交本迁移、测试、设计和计划，不混入当前工作区其他改动。
