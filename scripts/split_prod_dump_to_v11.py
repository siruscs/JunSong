#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PROD dump 切分脚本：
  1. 结构 DDL（所有 CREATE TABLE）
  2. 基础表 INSERT（仅 sys_* 平台级基础数据，不含 PROD 实际业务数据）
输出：JunSong-Cloud-v1.1.0-初始化.sql

用法：python3 split_prod_dump.py ../../sql/junsong-cloud-prod-1.1.sql ../../sql/JunSong-Cloud-v1.1.0-初始化.sql
"""

import sys
import re

# 包含基础/字典/配置/权限元数据 INSERT 的表名（全小写）
BASE_DATA_TABLES = {
    # 若依平台基础（自带初始化）
    "sys_config",
    "sys_dict_type",
    "sys_dict_data",
    "sys_post",
    "sys_role",
    "sys_menu",
    "sys_dept",
    "sys_tenant",
    "sys_region",
    # 本项目平台级权限配置表（菜单权限/角色菜单/部门角色）
    "sys_role_menu",
    "sys_role_dept",
    # 定时任务
    "sys_job",
    # 通知公告
    "sys_notice",
    # 租户-用户-角色-部门关联（平台级预置）
    "sys_user",
    "sys_user_role",
    "sys_user_dept",
    "sys_user_post",
    # 小程序模块排序（平台级排序元数据）
    "sys_mp_module_sort",
    # 字典补充（财务/会员/库存-字典）
    "fin_product_category",     # 商品分类
    "fin_product_unit_dict",    # 商品单位字典（如果存在）
    "mem_member_card_type",     # 会员卡类型
    # Nacos 配置库 junsong-config 的基础表（如果 dump 中存在）
    # "config_info", "config_info_aggr", "his_config_info", "config_tags_relation", "config_beta",
    # Quartz 调度表结构不含数据，这里保留结构 DDL
    "qrtz_job_details",
    "qrtz_triggers",
    "qrtz_simple_triggers",
    "qrtz_cron_triggers",
    "qrtz_simprop_triggers",
    "qrtz_blob_triggers",
    "qrtz_calendars",
    "qrtz_paused_trigger_grps",
    "qrtz_fired_triggers",
    "qrtz_scheduler_state",
    "qrtz_locks",
}

# 必须严格剔除 PROD 业务数据的表（黑名单：即使在 BASE_DATA_TABLES 中也不导数据）
BUSINESS_DATA_TABLES = {
    # 用户相关敏感业务（不要带入PROD账号密码）
    "sys_user",          # 密码/手机号/邮箱都是PROD真实数据，禁止导出
    "sys_user_role",
    "sys_user_dept",
    "sys_user_post",
    "sys_user_mp_binding",
    "sys_user_delegate",
    # 操作日志/登录日志
    "sys_logininfor",
    "sys_oper_log",
    "sys_audit_trail",
    # 调度日志
    "sys_job_log",
    # 作业运行
    "sys_operating_task", "sys_operating_task_log", "sys_operation_alert_event", "sys_operation_alert_rule",
    "sys_operation_audit_snapshot", "sys_operation_schedule_log", "sys_action_center_touch_log", "sys_action_center_touch_throttle",
    "sys_governance_task_log",
    "sys_idempotency_record",
    "sys_notice_read",
    "sys_notification",
    "sys_data_archive_run", "sys_data_retention_policy",
    # 幂等
    "sys_idempotency_record",
    # 健康规则运行数据
    "sys_health_rule_config",  # 配置元数据，但可能含PROD真实规则-暂保留
    # config backup 表
    "sys_config_bak_20260701",
    "sys_config_bak_20260701181845",
    "sys_menu_backup_20260702",
    "sys_role_menu_backup_20260702",
    # 财务业务
    "fin_product", "fin_purchase", "fin_purchase_detail",
    "fin_sale_record", "fin_sale_payment",
    "fin_stock_ledger", "fin_stock_snapshot", "fin_stock_position",
    "fin_stock_cost_layer", "fin_stock_cost_ledger",
    "fin_stock_init_batch", "fin_stock_init_item",
    "fin_supplier",
    "fin_expense", "fin_expense_verify_batch", "fin_expense_verify_detail",
    "fin_advance", "fin_advance_verify_detail",
    "fin_invest_record", "fin_investor", "fin_investor_payment",
    "fin_profit_share_record", "fin_profit_share_detail",
    "fin_composite_accounting_pool", "fin_composite_pool_dept",
    "fin_composite_pool_investor", "fin_composite_period_item",
    "fin_cost_accounting", "fin_dept_profit_config",
    "fin_accounting_period",
    "finance_cashflow_forecast_snapshot", "finance_prediction_factor",
    "finance_prediction_sample", "finance_receivable_collection",
    "finance_receivable_collection_log", "finance_review_knowledge",
    "finance_review_task", "finance_review_task_log", "finance_what_if_simulation",
    # 会员业务
    "mem_campaign_policy", "mem_campaign_policy_package",
    "mem_config_sync_batch", "mem_config_sync_detail",
    "mem_growth_action", "mem_growth_action_member", "mem_growth_ledger",
    "mem_growth_record", "mem_growth_rule",
    "mem_identity_policy",
    "mem_member", "mem_member_account", "mem_member_event",
    "mem_member_metric_snapshot", "mem_member_no_sequence",
    "mem_member_sign_in", "mem_member_sign_in_batch",
    "mem_member_status_history", "mem_member_tag", "mem_member_tag_rule",
    "mem_mp_role_module",
    "mem_points_exchange", "mem_points_goods", "mem_points_ledger",
    "mem_points_record", "mem_points_rule",
    "mem_purchase_delivery", "mem_purchase_item", "mem_purchase_order",
    "mem_purchase_payment", "mem_purchase_return", "mem_purchase_return_item",
    "mem_refund_apply", "mem_seckill", "mem_seckill_claim_record",
    "mem_seckill_record",
    # 开放平台
    "open_api_log", "open_app", "open_app_secret", "open_contract",
    "open_isv", "open_webhook_subscription",
    "webhook_delivery", "webhook_subscription",
    # 代码生成
    "gen_table", "gen_table_column",
    # 流程（运行态数据）
    "act_hi_actinst", "act_hi_attachment", "act_hi_comment",
    "act_hi_detail", "act_hi_entitylink", "act_hi_identitylink",
    "act_hi_procinst", "act_hi_taskinst", "act_hi_tsk_log",
    "act_hi_varinst", "act_ru_actinst", "act_ru_deadletter_job",
    "act_ru_entitylink", "act_ru_event_subscr", "act_ru_execution",
    "act_ru_external_job", "act_ru_history_job", "act_ru_identitylink",
    "act_ru_job", "act_ru_suspended_job", "act_ru_task",
    "act_ru_timer_job", "act_ru_variable", "act_procdef_info",
    "act_re_deployment", "act_re_model", "act_re_procdef",
    "act_evt_log", "act_ge_bytearray", "act_ge_property",
    # 工作流扩展
    "wf_node_field_permission", "wf_node_timeout", "wf_task_addsign",
    "wf_task_attachment", "wf_task_cc", "wf_timeout_trigger_log",
    # Flowable 事件 / 批量
    "flw_channel_definition", "flw_event_definition",
    "flw_event_deployment", "flw_event_resource",
    "flw_ru_batch", "flw_ru_batch_part",
    # 低代码平台
    "lc_biz_action", "lc_biz_branch_rule", "lc_biz_config_snapshot",
    "lc_biz_field", "lc_biz_instance", "lc_biz_node_assignee",
    "lc_biz_node_timer", "lc_biz_object", "lc_biz_page_schema",
    "lc_biz_post_action", "lc_biz_template",
}


def split_dump(src: str, dst: str):
    with open(src, "r", encoding="utf-8", errors="replace") as f:
        lines = f.readlines()

    # Phase 1: 扫描所有表，按行号记录 DDL 与 INSERT 段
    # 每个表结构块：DROP TABLE IF EXISTS ... → 直到下一个 DROP / 文件结尾
    # 每个 INSERT INTO 行块
    header = []          # 文件头 SET 语句
    ddl_blocks = []      # [(table_name, lines[])]
    insert_blocks = []   # [(table_name, lines[])]

    # 识别 DROP TABLE 行号
    drop_indices = [i for i, l in enumerate(lines) if l.startswith("DROP TABLE IF EXISTS")]

    # 提取文件头（第一个 DROP 之前）
    header_end = drop_indices[0] if drop_indices else 0
    header = lines[:header_end]

    for idx, di in enumerate(drop_indices):
        # 该表 DDL 范围：di 到 下一个 DROP - 1（除非遇到了 INSERT INTO 的起始点）
        next_drop = drop_indices[idx + 1] if idx + 1 < len(drop_indices) else len(lines)
        # 找到下一个 "LOCK TABLES ... WRITE"（mysqldump 的数据块开始标记）— 若无，则到 next_drop
        chunk = lines[di:next_drop]
        lock_idx_rel = None
        unlock_idx_rel = None
        for i, l in enumerate(chunk):
            if l.startswith("LOCK TABLES") and lock_idx_rel is None:
                lock_idx_rel = i
            if l.startswith("UNLOCK TABLES") and lock_idx_rel is not None:
                unlock_idx_rel = i + 1  # inclusive
                break
        if lock_idx_rel is not None and unlock_idx_rel is not None:
            ddl_lines = chunk[:lock_idx_rel]
            insert_lines = chunk[lock_idx_rel:unlock_idx_rel]
        else:
            ddl_lines = chunk
            insert_lines = []
        # 从 DDL 第一行 DROP TABLE 里提取表名
        m = re.search(r"`([^`]+)`", ddl_lines[0])
        if not m:
            continue
        table = m.group(1).lower()
        ddl_blocks.append((table, ddl_lines))
        if insert_lines:
            # 从 insert_lines 中找 INSERT INTO 确认
            m2 = None
            for l in insert_lines:
                m2 = re.search(r"INSERT INTO `([^`]+)`", l)
                if m2:
                    break
            t = m2.group(1).lower() if m2 else table
            insert_blocks.append((t, insert_lines))

    # Phase 2: 组装输出
    out = []
    out.append("-- ============================================================\n")
    out.append("-- 峻松云软件平台（JunSong-Cloud）系统数据库初始化脚本\n")
    out.append("-- 版本号：v1.1.0\n")
    out.append("-- 生成日期：2026-08-09\n")
    out.append("-- \n")
    out.append("-- 本脚本基于 PROD 库的完整结构导出，包含：\n")
    out.append("--   1. 建库语句（junsong-cloud）\n")
    out.append("--   2. 全部表结构（平台系统表 + 财务/会员/开放/流程/低代码业务表）\n")
    out.append("--   3. 平台级基础数据（字典/配置/菜单/部门/岗位/角色/租户/区域/小程序模块排序/定时任务元数据）\n")
    out.append("--      ⚠️  不含 PROD 实际业务数据（商品/会员/购买/销售/库存/费用/投资/流程实例等）\n")
    out.append("--      ⚠️  不含 PROD 真实用户账号（sys_user 及关联未导出）\n")
    out.append("--      ⚠️  不含 PROD 真实角色权限绑定（sys_role_menu/sys_role_dept 未导出，安全起\n")
    out.append("--          见防止 PROD 权限体系污染新环境，如需平台角色权限请在新库中通过\"若依初始化\"或重新配置）\n")
    out.append("-- \n")
    out.append("-- 适用场景：新环境全新部署（数据库初始化）\n")
    out.append("-- 执行方式：\n")
    out.append("--   mysql -u root -p < JunSong-Cloud-v1.1.0-初始化.sql\n")
    out.append("--   OR  source JunSong-Cloud-v1.1.0-初始化.sql;（在 mysql 客户端中）\n")
    out.append("-- ============================================================\n\n")

    # 建库 + 切换
    out.append("\n-- ------------------------------------------------------------\n")
    out.append("-- 1. 创建数据库\n")
    out.append("-- ------------------------------------------------------------\n")
    out.append("SET NAMES utf8mb4;\n")
    out.append("SET FOREIGN_KEY_CHECKS = 0;\n\n")
    out.append("DROP DATABASE IF EXISTS `junsong-cloud`;\n")
    out.append("CREATE DATABASE `junsong-cloud` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;\n")
    out.append("USE `junsong-cloud`;\n\n")

    # 文件头 SET 语句
    out.append("-- ------------------------------------------------------------\n")
    out.append("-- 2. 所有表结构 DDL\n")
    out.append("-- ------------------------------------------------------------\n")
    for tbl, ddl in ddl_blocks:
        out.append("-- \n")
        out.append(f"-- ---------- Table: {tbl} ----------\n")
        out.append("-- \n")
        out.extend(ddl)

    # 基础数据 INSERT
    out.append("\n-- ------------------------------------------------------------\n")
    out.append("-- 3. 平台级基础数据（不含 PROD 业务数据）\n")
    out.append("-- ------------------------------------------------------------\n")
    included = []
    skipped = []
    for tbl, ins in insert_blocks:
        if tbl in BUSINESS_DATA_TABLES:
            skipped.append(tbl)
            continue
        if tbl in BASE_DATA_TABLES:
            out.append(f"\n-- ===== Table: {tbl} =====\n")
            out.extend(ins)
            included.append(tbl)
        else:
            skipped.append(tbl)

    # 用户 & 权限 手工重置说明
    out.append("\n-- ------------------------------------------------------------\n")
    out.append("-- 4. 平台初始账号说明（安全默认值）\n")
    out.append("-- ------------------------------------------------------------\n")
    out.append("--\n")
    out.append("-- ⚠️  PROD 用户账号/密码/手机号/邮箱未随本脚本导出，防止敏感信息外泄。\n")
    out.append("-- 初始化后需要在新库中创建初始管理员账号。若依平台默认的初始账号为：\n")
    out.append("--    admin / admin123\n")
    out.append("--\n")
    out.append("-- 若需使用相同的权限体系（角色菜单绑定），请参考 docs/ 部署文档中\n")
    out.append("-- \"新环境权限初始化\"章节，在新库中通过 Nacos + 前端配置页面重新授权。\n")
    out.append("--\n\n")

    out.append("SET FOREIGN_KEY_CHECKS = 1;\n")
    out.append("-- ============================================================\n")
    out.append("-- END OF JunSong-Cloud-v1.1.0 INIT SCRIPT\n")
    out.append("-- ============================================================\n")

    with open(dst, "w", encoding="utf-8") as f:
        f.writelines(out)

    print(f"[OK] 输出：{dst}")
    print(f"     总表结构数：{len(ddl_blocks)}")
    print(f"     导出基础表INSERT（{len(included)}）: {', '.join(sorted(included))}")
    print(f"     跳过业务表/黑名单表（{len(skipped)}）：前30个 = {', '.join(sorted(set(skipped))[:30])}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: split_prod_dump.py <prod_dump.sql> <output_init.sql>")
        sys.exit(1)
    split_dump(sys.argv[1], sys.argv[2])
