# 数据库设计文档

> **作者：** Genesis·峻松
> **更新日期：** 2026-06-24
> **数据库：** MySQL 8.0
> **字符集：** utf8mb4

本文档说明 JunSong-Cloud 核心数据库表结构、关系与设计原则。完整表结构请参阅 `sql/junsong-cloud-init-*.sql` 初始化脚本。

---

## 一、数据库划分

| 数据库名 | 用途 | 说明 |
|---------|------|------|
| `junsong-cloud` | 业务主库 | 系统管理、门店、会员、财务、低代码等全部业务数据 |
| `junsong-config` | Nacos 配置库 | Nacos 配置中心持久化存储 |
| `junsong-job` | 定时任务库 | Quartz 调度框架独立数据源 |
| `junsong-gen` | 代码生成库 | 代码生成器独立数据源 |

---

## 二、核心模块表结构

### 2.1 系统管理模块（sys_*）

用户-角色-权限-菜单-部门 五维权限模型：

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   sys_user  │◄────┤ sys_user_   │────►│  sys_role   │
│  (用户表)   │     │   role      │     │  (角色表)   │
└──────┬──────┘     │ (关联表)    │     └──────┬──────┘
       │            └─────────────┘            │
       │                                       │
       │            ┌─────────────┐            │
       └───────────►│  sys_dept   │◄───────────┘
                    │  (部门表)   │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │  sys_menu   │
                    │  (菜单表)   │
                    └─────────────┘
```

| 表名 | 核心字段 | 说明 |
|------|---------|------|
| `sys_user` | `user_id`, `user_name`, `nick_name`, `password`, `dept_id`, `status` | 系统用户，密码 BCrypt 哈希 |
| `sys_role` | `role_id`, `role_name`, `role_key`, `status` | 角色定义 |
| `sys_user_role` | `user_id`, `role_id` | 用户-角色多对多关联 |
| `sys_menu` | `menu_id`, `menu_name`, `parent_id`, `path`, `component`, `perms`, `menu_type` | 菜单与按钮权限 |
| `sys_role_menu` | `role_id`, `menu_id` | 角色-菜单权限关联 |
| `sys_dept` | `dept_id`, `dept_name`, `parent_id`, `longitude`, `latitude`, `province_code`, `city_code`, `district_code`, `street_code` | 部门（门店），含地理坐标与行政区划 |
| `sys_dict_type` / `sys_dict_data` | `dict_type`, `dict_label`, `dict_value` | 系统字典 |
| `sys_post` | `post_id`, `post_name`, `post_code` | 岗位 |
| `sys_config` | `config_key`, `config_value` | 系统参数配置 |
| `sys_logininfor` | `info_id`, `user_name`, `ipaddr`, `status`, `msg` | 登录日志 |
| `sys_oper_log` | `oper_id`, `title`, `business_type`, `method`, `status` | 操作日志 |

### 2.2 低代码审批模块（lc_*）

低代码平台的核心数据模型：业务对象定义 → 字段定义 → 流程配置 → 实例运行。

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  lc_biz_object  │◄────┤  lc_biz_field   │     │lc_biz_page_schema│
│  (业务对象定义)  │     │  (字段定义)      │     │  (页面配置)      │
└────────┬────────┘     └─────────────────┘     └─────────────────┘
         │
         │            ┌─────────────────┐
         ├───────────►│lc_biz_node_     │
         │            │  assignee       │
         │            │(节点处理人配置)  │
         │            └─────────────────┘
         │
         │            ┌─────────────────┐
         ├───────────►│ lc_biz_branch_  │
         │            │     rule        │
         │            │  (分支规则)      │
         │            └─────────────────┘
         │
         │            ┌─────────────────┐
         └───────────►│ lc_biz_instance │
                      │  (业务实例/单据) │
                      └─────────────────┘
```

| 表名 | 核心字段 | 说明 |
|------|---------|------|
| `lc_biz_object` | `biz_code`, `biz_name`, `process_key`, `storage_mode`, `order_no_prefix`, `workflow_enabled`, `fulfillment_enabled` | 业务对象元数据 |
| `lc_biz_field` | `biz_code`, `field_key`, `field_label`, `field_type`, `required`, `is_query`, `is_list`, `is_process_var`, `stage`, `parent_field_key` | 字段元数据（22 种类型） |
| `lc_biz_page_schema` | `biz_code`, `page_type`, `schema_json` | 页面渲染配置（列表/表单/详情） |
| `lc_biz_node_assignee` | `biz_code`, `task_key`, `assignee_source`, `assignee_value` | 流程节点处理人配置（10 种来源） |
| `lc_biz_branch_rule` | `biz_code`, `task_key`, `field_key`, `operator`, `target_value` | 流程分支规则（AND/OR 组合） |
| `lc_biz_action` | `biz_code`, `action_key`, `action_name`, `trigger_statuses` | 操作按钮配置 |
| `lc_biz_instance` | `instance_id`, `biz_code`, `form_data`, `workflow_status`, `order_no`, `process_instance_id` | 业务实例（JSON 存储表单数据） |
| `lc_biz_config_snapshot` | `snapshot_id`, `biz_code`, `version`, `status`, `snapshot_json` | 配置版本快照（草稿/已发布隔离） |

### 2.3 财务模块（fin_*）

| 表名 | 说明 |
|------|------|
| `fin_purchase` / `fin_purchase_detail` | 进货单及明细 |
| `fin_sale_record` / `fin_sale_payment` | 销售记录及收款 |
| `fin_expense` | 费用报销 |
| `fin_product` / `fin_supplier` | 商品与供应商 |
| `fin_cost_accounting` | 成本核算 |
| `fin_investor` / `fin_invest_record` / `fin_investor_payment` | 投资人及投资记录 |
| `fin_profit_share_record` / `fin_profit_share_detail` | 分润结转 |
| `fin_accounting_period` | 会计核算期 |

### 2.4 会员模块（mem_*）

| 表名 | 说明 |
|------|------|
| `mem_member` | 会员基础信息 |
| `mem_integral` | 积分记录 |
| `mem_integral_rule` | 积分规则 |
| `mem_seckill` / `mem_seckill_record` | 秒杀活动及记录 |
| `mem_refund` | 退款记录 |

### 2.5 工作流引擎（flowable 自带表 + 扩展）

| 表前缀 | 说明 |
|--------|------|
| `act_re_*` / `act_ru_*` / `act_hi_*` | Flowable 流程定义、运行时、历史表 |
| `lc_biz_instance` | 低代码业务实例（与 Flowable 流程实例关联） |

---

## 三、设计原则

1. **统一业务库**：除 gen/job 外，所有业务数据集中在 `junsong-cloud` 库，便于关联查询
2. **低代码 JSON 存储**：`lc_biz_instance.form_data` 以 JSON 存储动态表单数据，灵活扩展
3. **地理坐标标准化**：`sys_dept` 表存储经纬度与行政区划代码（国标 6 位 adcode），支持地图可视化
4. **操作日志分离**：`sys_oper_log` 记录业务操作，便于审计与追溯
5. **版本隔离**：低代码配置采用快照机制（`lc_biz_config_snapshot`），草稿与已发布隔离，在途流程不受草稿影响

---

*本文档由 Genesis·峻松 维护，最后更新：2026-06-24*
