SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 已获用户确认：仅清理 DEV 工作流/低代码配置及业务数据，不动用户、权限、租户、菜单、字典和业务表结构。
SELECT 'BEFORE' reset_phase,
       (SELECT COUNT(*) FROM lc_biz_object) lc_biz_object_count,
       (SELECT COUNT(*) FROM lc_biz_instance) lc_biz_instance_count,
       (SELECT COUNT(*) FROM act_ru_task) act_ru_task_count,
       (SELECT COUNT(*) FROM act_hi_procinst) act_hi_procinst_count,
       (SELECT COUNT(*) FROM act_re_procdef) act_re_procdef_count;

DELETE FROM wf_task_cc;
DELETE FROM wf_task_addsign;
DELETE FROM wf_task_attachment;
DELETE FROM wf_timeout_trigger_log;
DELETE FROM wf_node_timeout;
DELETE FROM wf_node_field_permission;

DELETE FROM lc_biz_instance;
DELETE FROM lc_biz_post_action;
DELETE FROM lc_biz_node_timer;
DELETE FROM lc_biz_node_assignee;
DELETE FROM lc_biz_branch_rule;
DELETE FROM lc_biz_action;
DELETE FROM lc_biz_page_schema;
DELETE FROM lc_biz_field;
DELETE FROM lc_biz_config_snapshot;
DELETE FROM lc_biz_object;
DELETE FROM lc_biz_template;

DELETE FROM act_ru_deadletter_job;
DELETE FROM act_ru_external_job;
DELETE FROM act_ru_history_job;
DELETE FROM act_ru_suspended_job;
DELETE FROM act_ru_timer_job;
DELETE FROM act_ru_job;
DELETE FROM act_ru_event_subscr;
DELETE FROM act_ru_identitylink;
DELETE FROM act_ru_variable;
DELETE FROM act_ru_entitylink;
DELETE FROM act_ru_actinst;
DELETE FROM act_ru_task;
DELETE FROM act_ru_execution;

DELETE FROM act_hi_tsk_log;
DELETE FROM act_hi_identitylink;
DELETE FROM act_hi_attachment;
DELETE FROM act_hi_comment;
DELETE FROM act_hi_detail;
DELETE FROM act_hi_varinst;
DELETE FROM act_hi_taskinst;
DELETE FROM act_hi_actinst;
DELETE FROM act_hi_entitylink;
DELETE FROM act_hi_procinst;

DELETE FROM act_re_deployment;
DELETE FROM act_re_procdef;
DELETE FROM act_re_model;
DELETE FROM act_procdef_info;
DELETE FROM act_ge_bytearray;
DELETE FROM act_evt_log;

SELECT 'AFTER' reset_phase,
       (SELECT COUNT(*) FROM lc_biz_object) lc_biz_object_count,
       (SELECT COUNT(*) FROM lc_biz_instance) lc_biz_instance_count,
       (SELECT COUNT(*) FROM act_ru_task) act_ru_task_count,
       (SELECT COUNT(*) FROM act_hi_procinst) act_hi_procinst_count,
       (SELECT COUNT(*) FROM act_re_procdef) act_re_procdef_count;

SET FOREIGN_KEY_CHECKS = 1;
