import { execFileSync } from 'node:child_process'

const clearedCacheCount = Number(execFileSync('docker', [
  'exec',
  'junsong-redis',
  'redis-cli',
  'DEL',
  'lc-metadata::stocktake:fields',
  'lc-metadata::stocktake:assignees',
  'lc-metadata::stocktake:rules',
], { encoding: 'utf8' }).trim() || '0')

const sql = `
SELECT 'biz_object' check_name, COUNT(*) ok_count
FROM lc_biz_object
WHERE biz_code='stocktake'
  AND workflow_enabled='1'
  AND process_key='stocktake_apply'
  AND storage_mode='NATIVE'
  AND config_status='PUBLISHED'
  AND tenant_id=1
  AND del_flag='0';

SELECT 'page_schema' check_name, COUNT(*) ok_count
FROM lc_biz_page_schema
WHERE biz_code='stocktake'
  AND page_type IN ('FORM','LIST','DETAIL')
  AND JSON_VALID(schema_json)=1
  AND tenant_id=1
  AND del_flag='0';

SELECT 'process_definition' check_name, COUNT(*) ok_count
FROM act_re_procdef
WHERE KEY_='stocktake_apply'
  AND SUSPENSION_STATE_=1;

SELECT 'assignee_config' check_name, COUNT(*) ok_count
FROM lc_biz_node_assignee
WHERE biz_code='stocktake'
  AND tenant_id=1
  AND del_flag='0'
  AND (
    (task_key='Task_Count' AND assignee_source='FORM_FIELD_USER' AND assignee_value='counter_user_id' AND process_var_name='counterUsername')
    OR (task_key='Task_Recount' AND assignee_source='FORM_FIELD_USER' AND assignee_value='recount_user_id' AND process_var_name='recountUsername')
    OR (task_key='Task_Approve' AND assignee_source='FIXED_USER' AND assignee_value='admin' AND process_var_name='approverUsername')
  );

SELECT 'branch_rule' check_name, COUNT(*) ok_count
FROM lc_biz_branch_rule
WHERE biz_code='stocktake'
  AND gateway_key='Gateway_NeedRecount'
  AND field_key='recount_user_id'
  AND operator='NOT_EMPTY'
  AND target_var_name='needRecount'
  AND tenant_id=1
  AND del_flag='0';
`

const output = execFileSync('docker', [
  'exec',
  'junsong-mysql',
  'mysql',
  '--default-character-set=utf8mb4',
  '-uroot',
  '-proot_123',
  'junsong-cloud',
  '-N',
  '-e',
  sql,
], { encoding: 'utf8' })

const rows = output
  .split('\n')
  .map((line) => line.trim())
  .filter((line) => line && !line.startsWith('mysql:'))
  .map((line) => {
    const [name, count] = line.split(/\s+/)
    return { name, count: Number(count) }
  })

const expected = new Map([
  ['biz_object', 1],
  ['page_schema', 3],
  ['process_definition', 1],
  ['assignee_config', 3],
  ['branch_rule', 1],
])

const failures = rows.filter((row) => row.count < expected.get(row.name))
if (failures.length > 0 || rows.length !== expected.size) {
  console.error(JSON.stringify({ status: 'FAIL', rows, expected: Object.fromEntries(expected) }, null, 2))
  process.exit(1)
}

console.log(JSON.stringify({ status: 'PASS', clearedCacheCount, rows }, null, 2))
