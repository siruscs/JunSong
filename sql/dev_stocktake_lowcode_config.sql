SET NAMES utf8mb4;

ALTER TABLE lc_biz_branch_rule MODIFY COLUMN operator VARCHAR(32) NOT NULL COMMENT '操作符';
SET FOREIGN_KEY_CHECKS = 0;

-- 阶段二：库存盘点 NATIVE 低代码配置。只写租户 1，重复执行不产生重复配置。
INSERT INTO lc_biz_object
    (biz_code, biz_name, storage_mode, table_name, pk_field, order_no_field, order_no_prefix,
     status_field, workflow_enabled, process_key, fulfillment_enabled, menu_parent_path,
     status, config_status, published_version, del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake', '库存盘点', 'NATIVE', 'finance_stocktake', 'stocktake_id', 'take_no', 'PD',
       'status', '1', 'stocktake_apply', '0', '/finance',
       '0', 'PUBLISHED', 1, '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_object WHERE biz_code = 'stocktake' AND tenant_id = 1 AND del_flag = '0');

INSERT INTO lc_biz_field
    (biz_code, field_key, field_label, field_type, component_type, required, dict_type, field_ext,
     stage, is_query, is_list, is_detail, is_process_var, process_var_name, order_num,
     del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake', f.field_key, f.field_label, f.field_type, f.component_type, f.required, f.dict_type, f.field_ext,
       'APPLY', f.is_query, f.is_list, '1', f.is_process_var, f.process_var_name, f.order_num,
       '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 1
FROM (
    SELECT 'take_no' field_key, '盘点单号' field_label, 'computed' field_type, 'computed' component_type, '0' required, NULL dict_type, '{"span":12,"editable":false}' field_ext, '1' is_query, '1' is_list, '0' is_process_var, 'orderNo' process_var_name, 1 order_num
    UNION ALL SELECT 'dept_id','盘点门店','sys-ref','tree-select','1',NULL,'{"span":12,"source":"dept","valueField":"deptId"}','1','1','1','deptId',2
    UNION ALL SELECT 'scope_type','盘点范围','string','select','1',NULL,'{"span":12,"options":[{"label":"指定商品","value":"SELECTED_PRODUCTS"},{"label":"全门店","value":"FULL_DEPT"}]}','1','1','1','scopeType',3
    UNION ALL SELECT 'counter_user_id','盘点人','sys-ref','user-select','1',NULL,'{"span":12,"source":"user","valueField":"userId","labelField":"nickName"}','0','1','1','counterUsername',4
    UNION ALL SELECT 'recount_user_id','复盘人','sys-ref','user-select','0',NULL,'{"span":12,"source":"user","valueField":"userId","labelField":"nickName"}','0','1','1','recountUsername',5
    UNION ALL SELECT 'stocktake_items','盘点商品与数量','subform','subform','1',NULL,'{"span":24,"subFields":[{"fieldKey":"product_id","fieldLabel":"盘点商品","fieldType":"sys-ref","required":"1","fieldExt":"{\\"source\\":\\"product\\"}"},{"fieldKey":"actual_quantity","fieldLabel":"盘点数量","fieldType":"number","required":"1","fieldExt":"{\\"min\\":0}"}]}','0','1','0',NULL,6
    UNION ALL SELECT 'remark','盘点说明','textarea','textarea','0',NULL,'{"span":24}','0','0','1',NULL,7
) f
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_field WHERE biz_code = 'stocktake' AND field_key = f.field_key AND tenant_id = 1 AND del_flag = '0');

UPDATE lc_biz_field SET field_type='computed', component_type='computed', required='0', field_ext='{"span":12,"editable":false}'
 WHERE biz_code='stocktake' AND field_key='take_no' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_field SET field_type='sys-ref', component_type='tree-select', field_ext='{"span":12,"source":"dept","valueField":"deptId"}'
WHERE biz_code='stocktake' AND field_key='dept_id' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_field SET field_type='sys-ref', component_type='user-select', field_ext='{"span":12,"source":"user","valueField":"userId","labelField":"userName"}'
WHERE biz_code='stocktake' AND field_key IN ('counter_user_id', 'recount_user_id') AND tenant_id=1 AND del_flag='0';
INSERT INTO lc_biz_field
    (biz_code, field_key, field_label, field_type, component_type, required, dict_type, field_ext,
     stage, is_query, is_list, is_detail, is_process_var, process_var_name, order_num,
     del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake','stocktake_items','盘点商品与数量','subform','subform','1',NULL,
       '{"span":24,"subFields":[{"fieldKey":"product_id","fieldLabel":"盘点商品","fieldType":"sys-ref","required":"1","fieldExt":"{\\"source\\":\\"product\\"}"},{"fieldKey":"actual_quantity","fieldLabel":"盘点数量","fieldType":"number","required":"1","fieldExt":"{\\"min\\":0}"}]}',
       'APPLY','0','1','1','0',NULL,6,'0','dev-phase2',NOW(),'dev-phase2',NOW(),1
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_field WHERE biz_code='stocktake' AND field_key='stocktake_items' AND tenant_id=1 AND del_flag='0');

INSERT INTO lc_biz_page_schema
    (biz_code, page_type, schema_json, version, status, del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake', p.page_type, p.schema_json, 1, '0', '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 1
FROM (
    SELECT 'FORM' page_type, '{"layout":"grid","columns":24,"fields":[{"fieldKey":"take_no","span":12},{"fieldKey":"dept_id","span":12},{"fieldKey":"scope_type","span":12},{"fieldKey":"counter_user_id","span":12},{"fieldKey":"recount_user_id","span":12},{"fieldKey":"remark","span":24}]}' schema_json
    UNION ALL SELECT 'LIST','{"layout":"grid","columns":24,"fields":[{"fieldKey":"take_no","span":12},{"fieldKey":"dept_id","span":12},{"fieldKey":"scope_type","span":12},{"fieldKey":"counter_user_id","span":12},{"fieldKey":"remark","span":24}]}'
    UNION ALL SELECT 'DETAIL','{"layout":"grid","columns":24,"fields":[{"fieldKey":"take_no","span":12},{"fieldKey":"dept_id","span":12},{"fieldKey":"scope_type","span":12},{"fieldKey":"counter_user_id","span":12},{"fieldKey":"recount_user_id","span":12},{"fieldKey":"remark","span":24}]}'
) p
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_page_schema WHERE biz_code = 'stocktake' AND page_type = p.page_type AND tenant_id = 1 AND del_flag = '0');

UPDATE lc_biz_page_schema SET schema_json='{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","recount_user_id","stocktake_items","remark"]}' WHERE biz_code='stocktake' AND page_type IN ('FORM','DETAIL') AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_page_schema SET schema_json='{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","stocktake_items","remark"]}' WHERE biz_code='stocktake' AND page_type='LIST' AND tenant_id=1 AND del_flag='0';

INSERT INTO lc_biz_node_assignee
    (biz_code, task_key, task_name, assignee_source, assignee_value, process_var_name, del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake', a.task_key, a.task_name, a.assignee_source, a.assignee_value, a.process_var_name, '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 1
FROM (
    SELECT 'Task_Count' task_key, '盘点人录入' task_name, 'FORM_FIELD_USER' assignee_source, 'counter_user_id' assignee_value, 'counterUsername' process_var_name
    UNION ALL SELECT 'Task_Recount','复盘人复盘','FORM_FIELD_USER','recount_user_id','recountUsername'
    UNION ALL SELECT 'Task_Approve','库存审批','FIXED_USER','admin','approverUsername'
) a
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_node_assignee WHERE biz_code = 'stocktake' AND task_key = a.task_key AND tenant_id = 1 AND del_flag = '0');

UPDATE lc_biz_node_assignee
SET assignee_source='FORM_FIELD_USER', assignee_value='counter_user_id', process_var_name='counterUsername', update_by='dev-phase2', update_time=NOW()
WHERE biz_code='stocktake' AND task_key='Task_Count' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_node_assignee
SET assignee_source='FORM_FIELD_USER', assignee_value='recount_user_id', process_var_name='recountUsername', update_by='dev-phase2', update_time=NOW()
WHERE biz_code='stocktake' AND task_key='Task_Recount' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_node_assignee
SET assignee_source='FIXED_USER', assignee_value='admin', process_var_name='approverUsername', update_by='dev-phase2', update_time=NOW()
WHERE biz_code='stocktake' AND task_key='Task_Approve' AND tenant_id=1 AND del_flag='0';

INSERT INTO lc_biz_branch_rule
    (biz_code, gateway_key, field_key, operator, compare_value, target_var_name,
     group_op, parent_rule_id, remark, del_flag, create_by, create_time, update_by, update_time, tenant_id)
SELECT 'stocktake', 'Gateway_NeedRecount', 'recount_user_id', 'NOT_EMPTY', NULL, 'needRecount',
       'AND', NULL, '复盘人为空则不复盘，填写复盘人则进入复盘节点', '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 1
WHERE NOT EXISTS (
    SELECT 1 FROM lc_biz_branch_rule
    WHERE biz_code='stocktake' AND gateway_key='Gateway_NeedRecount' AND target_var_name='needRecount'
      AND tenant_id=1 AND del_flag='0'
);

UPDATE lc_biz_branch_rule
SET field_key='recount_user_id', operator='NOT_EMPTY', compare_value=NULL, target_var_name='needRecount',
    remark='复盘人为空则不复盘，填写复盘人则进入复盘节点', update_by='dev-phase2', update_time=NOW()
WHERE biz_code='stocktake' AND gateway_key='Gateway_NeedRecount' AND target_var_name='needRecount'
  AND tenant_id=1 AND del_flag='0';

INSERT INTO lc_biz_config_snapshot
    (biz_code, version_no, config_json, status, publish_remark, del_flag, create_by, create_time, update_by, update_time, remark, tenant_id)
SELECT 'stocktake', 1,
       JSON_OBJECT(
         'bizObject', JSON_OBJECT('bizCode','stocktake','bizName','库存盘点','storageMode','NATIVE','tableName','finance_stocktake','pkField','stocktake_id','orderNoField','take_no','workflowEnabled','1','processKey','stocktake_apply','configStatus','PUBLISHED','publishedVersion',1),
         'fields', JSON_ARRAY(),
         'pageSchemas', JSON_ARRAY(
           JSON_OBJECT('bizCode','stocktake','pageType','FORM','schemaJson',JSON_UNQUOTE(JSON_QUOTE('{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","recount_user_id","stocktake_items","remark"]}'))),
           JSON_OBJECT('bizCode','stocktake','pageType','LIST','schemaJson',JSON_UNQUOTE(JSON_QUOTE('{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","stocktake_items","remark"]}'))),
           JSON_OBJECT('bizCode','stocktake','pageType','DETAIL','schemaJson',JSON_UNQUOTE(JSON_QUOTE('{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","recount_user_id","stocktake_items","remark"]}')))
         )
       ), 'PUBLISHED', 'DEV 阶段二库存盘点通用流程配置', '0', 'dev-phase2', NOW(), 'dev-phase2', NOW(), 'NATIVE 配置驱动盘点流程', 1
WHERE NOT EXISTS (SELECT 1 FROM lc_biz_config_snapshot WHERE biz_code = 'stocktake' AND version_no = 1 AND status = 'PUBLISHED' AND tenant_id = 1 AND del_flag = '0');

UPDATE lc_biz_config_snapshot
SET config_json = JSON_SET(config_json,
  '$.fields[0].fieldType','computed', '$.fields[0].componentType','computed', '$.fields[0].required','0', '$.fields[0].fieldExt','{"span":12,"editable":false}',
  '$.fields[1].fieldType','sys-ref', '$.fields[1].componentType','tree-select', '$.fields[1].fieldExt','{"span":12,"source":"dept","valueField":"deptId"}',
  '$.fields', JSON_ARRAY_APPEND(JSON_EXTRACT(config_json,'$.fields'), '$', JSON_OBJECT('bizCode','stocktake','fieldKey','stocktake_items','fieldLabel','盘点商品与数量','fieldType','subform','componentType','subform','required','1','isQuery','0','isList','1','isDetail','1','isProcessVar','0','orderNum',6,'fieldExt','{"span":24,"subFields":[{"fieldKey":"product_id","fieldLabel":"盘点商品","fieldType":"sys-ref","required":"1","fieldExt":"{\\"source\\":\\"product\\"}"},{"fieldKey":"actual_quantity","fieldLabel":"盘点数量","fieldType":"number","required":"1","fieldExt":"{\\"min\\":0}"}]}')))
WHERE biz_code='stocktake' AND version_no=1 AND status='PUBLISHED' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_config_snapshot SET config_json = JSON_SET(config_json,
  '$.fields[0].fieldType','computed', '$.fields[0].componentType','computed', '$.fields[0].required','0', '$.fields[0].fieldExt','{"span":12,"editable":false}',
  '$.fields[1].fieldType','sys-ref', '$.fields[1].componentType','tree-select', '$.fields[1].fieldExt','{"span":12,"source":"dept","valueField":"deptId"}')
 WHERE biz_code='stocktake' AND version_no=1 AND status='PUBLISHED' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_config_snapshot SET config_json = JSON_SET(config_json, '$.fields', JSON_ARRAY(
  JSON_OBJECT('bizCode','stocktake','fieldKey','take_no','fieldLabel','盘点单号','fieldType','computed','componentType','computed','required','0','isQuery','1','isList','1','isDetail','1','isProcessVar','0','orderNum',1,'fieldExt','{"span":12,"editable":false}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','dept_id','fieldLabel','盘点门店','fieldType','sys-ref','componentType','tree-select','required','1','isQuery','1','isList','1','isDetail','1','isProcessVar','1','processVarName','deptId','orderNum',2,'fieldExt','{"span":12,"source":"dept","valueField":"deptId"}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','scope_type','fieldLabel','盘点范围','fieldType','string','componentType','select','required','1','isQuery','1','isList','1','isDetail','1','isProcessVar','1','processVarName','scopeType','orderNum',3,'fieldExt','{"span":12,"options":[{"label":"指定商品","value":"SELECTED_PRODUCTS"},{"label":"全门店","value":"FULL_DEPT"}]}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','counter_user_id','fieldLabel','盘点人','fieldType','sys-ref','componentType','user-select','required','1','isQuery','0','isList','1','isDetail','1','isProcessVar','1','processVarName','counterUsername','orderNum',4,'fieldExt','{"span":12,"source":"user","valueField":"userId","labelField":"nickName"}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','recount_user_id','fieldLabel','复盘人','fieldType','sys-ref','componentType','user-select','required','0','isQuery','0','isList','1','isDetail','1','isProcessVar','1','processVarName','recountUsername','orderNum',5,'fieldExt','{"span":12,"source":"user","valueField":"userId","labelField":"nickName"}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','stocktake_items','fieldLabel','盘点商品与数量','fieldType','subform','componentType','subform','required','1','isQuery','0','isList','1','isDetail','1','isProcessVar','0','orderNum',6,'fieldExt','{"span":24,"subFields":[{"fieldKey":"product_id","fieldLabel":"盘点商品","fieldType":"sys-ref","required":"1","fieldExt":"{\\"source\\":\\"product\\"}"},{"fieldKey":"actual_quantity","fieldLabel":"盘点数量","fieldType":"number","required":"1","fieldExt":"{\\"min\\":0}"}]}'),
  JSON_OBJECT('bizCode','stocktake','fieldKey','remark','fieldLabel','盘点说明','fieldType','textarea','componentType','textarea','required','0','isQuery','0','isList','0','isDetail','1','isProcessVar','0','orderNum',7,'fieldExt','{"span":24}')
)) WHERE biz_code='stocktake' AND version_no=1 AND status='PUBLISHED' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_config_snapshot SET config_json = JSON_SET(config_json,
  '$.pageSchemas[0].schemaJson','{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","recount_user_id","stocktake_items","remark"]}',
  '$.pageSchemas[1].schemaJson','{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","stocktake_items","remark"]}',
  '$.pageSchemas[2].schemaJson','{"layout":"grid","columns":24,"fields":["take_no","dept_id","scope_type","counter_user_id","recount_user_id","stocktake_items","remark"]}')
 WHERE biz_code='stocktake' AND version_no=1 AND status='PUBLISHED' AND tenant_id=1 AND del_flag='0';
UPDATE lc_biz_config_snapshot SET config_json = JSON_SET(config_json, '$.branchRules', JSON_ARRAY(
  JSON_OBJECT('bizCode','stocktake','gatewayKey','Gateway_NeedRecount','fieldKey','recount_user_id','operator','NOT_EMPTY','targetVarName','needRecount','groupOp','AND','remark','复盘人为空则不复盘，填写复盘人则进入复盘节点')
)) WHERE biz_code='stocktake' AND version_no=1 AND status='PUBLISHED' AND tenant_id=1 AND del_flag='0';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'stocktake_lowcode_reconciliation' result_type,
       (SELECT COUNT(*) FROM lc_biz_object WHERE biz_code='stocktake' AND tenant_id=1 AND config_status='PUBLISHED' AND del_flag='0') biz_object_count,
       (SELECT COUNT(*) FROM lc_biz_field WHERE biz_code='stocktake' AND tenant_id=1 AND del_flag='0') field_count,
       (SELECT COUNT(*) FROM lc_biz_page_schema WHERE biz_code='stocktake' AND tenant_id=1 AND del_flag='0') schema_count,
       (SELECT COUNT(*) FROM lc_biz_node_assignee WHERE biz_code='stocktake' AND tenant_id=1 AND del_flag='0') assignee_count,
       (SELECT COUNT(*) FROM lc_biz_branch_rule WHERE biz_code='stocktake' AND tenant_id=1 AND del_flag='0') branch_rule_count,
       (SELECT COUNT(*) FROM lc_biz_config_snapshot WHERE biz_code='stocktake' AND tenant_id=1 AND status='PUBLISHED' AND del_flag='0') snapshot_count;
