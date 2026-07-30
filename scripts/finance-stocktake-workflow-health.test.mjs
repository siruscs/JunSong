import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');
const financeModule = join(repoRoot, 'junsong-modules', 'junsong-finance');
const workflowModule = join(repoRoot, 'junsong-modules', 'junsong-workflow');
const apiModule = join(repoRoot, 'junsong-api', 'junsong-api-system');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function readWorkflowResource(rel) {
  return readFileSync(join(workflowModule, 'src', 'main', 'resources', rel), 'utf8');
}

function readWorkflowJava(rel) {
  return readFileSync(join(workflowModule, 'src', 'main', 'java', 'com', 'junsong', 'workflow', rel), 'utf8');
}

function readApiJava(rel) {
  return readFileSync(join(apiModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'api', rel), 'utf8');
}

function readFinanceService(name) {
  return readFileSync(
    join(financeModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'service', 'impl', name),
    'utf8'
  );
}

// ============================================================================
// SQL 契约测试：finance_stocktake_workflow_columns.sql
// ============================================================================

test('finance_stocktake_workflow_columns.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('finance_stocktake_workflow_columns.sql: 添加 process_instance_id 列', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /process_instance_id\s+VARCHAR\s*\(\s*64\s*\)/i);
  assert.match(sql, /COLUMN_NAME\s*=\s*'process_instance_id'/i);
});

test('finance_stocktake_workflow_columns.sql: 添加 process_definition_key 列', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /process_definition_key\s+VARCHAR\s*\(\s*64\s*\)/i);
  assert.match(sql, /COLUMN_NAME\s*=\s*'process_definition_key'/i);
});

test('finance_stocktake_workflow_columns.sql: 添加 business_key 列', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /business_key\s+VARCHAR\s*\(\s*128\s*\)/i);
  assert.match(sql, /COLUMN_NAME\s*=\s*'business_key'/i);
});

test('finance_stocktake_workflow_columns.sql: 添加 current_node 列', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /current_node\s+VARCHAR\s*\(\s*64\s*\)/i);
  assert.match(sql, /COLUMN_NAME\s*=\s*'current_node'/i);
});

test('finance_stocktake_workflow_columns.sql: 使用 information_schema.COLUMNS 守卫（幂等）', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /information_schema\.COLUMNS/i);
  // 使用 PREPARE/EXECUTE 模式实现幂等
  assert.match(sql, /PREPARE\s+stmt/i);
  assert.match(sql, /DEALLOCATE\s+PREPARE/i);
});

test('finance_stocktake_workflow_columns.sql: 包含对账 SELECT 输出', () => {
  const sql = readSql('finance_stocktake_workflow_columns.sql');
  assert.match(sql, /reconciliation_type/i);
});

// ============================================================================
// BPMN 契约测试：stocktake-apply.bpmn20.xml
// ============================================================================

test('stocktake-apply.bpmn20.xml: 存在且包含 process id="stocktake_apply"', () => {
  const path = join(workflowModule, 'src', 'main', 'resources', 'processes', 'stocktake-apply.bpmn20.xml');
  if (!existsSync(path)) {
    assert.fail('stocktake-apply.bpmn20.xml 未创建');
  }
  const xml = readFileSync(path, 'utf8');
  assert.match(xml, /<process\s+id="stocktake_apply"/i);
});

test('stocktake-apply.bpmn20.xml: Task_Count 节点使用 flowable:assignee', () => {
  const xml = readWorkflowResource('processes/stocktake-apply.bpmn20.xml');
  assert.match(xml, /<userTask\s+id="Task_Count"[^>]*flowable:assignee/i);
});

test('stocktake-apply.bpmn20.xml: 包含 Task_Approve 节点', () => {
  const xml = readWorkflowResource('processes/stocktake-apply.bpmn20.xml');
  assert.match(xml, /id="Task_Approve"/i);
});

test('stocktake-apply.bpmn20.xml: 包含 ExclusiveGateway 用于 needRecount 分支', () => {
  const xml = readWorkflowResource('processes/stocktake-apply.bpmn20.xml');
  assert.match(xml, /<exclusiveGateway\s+id="Gateway_NeedRecount"/i);
  assert.match(xml, /needRecount/i);
});

// ============================================================================
// API 契约测试：RemoteStocktakeService.java
// ============================================================================

test('RemoteStocktakeService.java: 存在且定义 syncWorkflowStatus Feign 端点', () => {
  const path = join(apiModule, 'src', 'main', 'java', 'com', 'junsong', 'finance', 'api', 'RemoteStocktakeService.java');
  if (!existsSync(path)) {
    assert.fail('RemoteStocktakeService.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  assert.match(content, /@FeignClient/i);
  assert.match(content, /syncWorkflowStatus/i);
  assert.match(content, /\/stocktakes\/internal\/workflow\/sync/i);
});

// ============================================================================
// 同步处理器契约测试：StocktakeWorkflowSyncHandler.java
// ============================================================================

test('StocktakeWorkflowSyncHandler.java: 存在且 supports() 匹配 stocktake_apply: 前缀', () => {
  const path = join(workflowModule, 'src', 'main', 'java', 'com', 'junsong', 'workflow', 'service', 'sync', 'StocktakeWorkflowSyncHandler.java');
  if (!existsSync(path)) {
    assert.fail('StocktakeWorkflowSyncHandler.java 未创建');
  }
  const content = readFileSync(path, 'utf8');
  // PROCESS_KEY = "stocktake_apply"
  assert.match(content, /"stocktake_apply"/);
  // supports 方法匹配 stocktake_apply: 前缀
  assert.match(content, /startsWith\s*\(\s*PROCESS_KEY\s*\+\s*":"\s*\)/);
  assert.match(content, /supports\s*\(\s*String\s+processDefinitionId\s*\)/i);
});

test('StocktakeWorkflowSyncHandler.java: 实现 afterApprove / afterReject / afterSubmit 回调', () => {
  const content = readWorkflowJava('service/sync/StocktakeWorkflowSyncHandler.java');
  assert.match(content, /afterApprove\s*\(/);
  assert.match(content, /afterReject\s*\(/);
  assert.match(content, /afterSubmit\s*\(/);
  // 调用 RemoteStocktakeService.syncWorkflowStatus
  assert.match(content, /remoteStocktakeService\.syncWorkflowStatus/i);
});

// ============================================================================
// Service 契约测试：FinStocktakeServiceImpl.java
// ============================================================================

test('FinStocktakeServiceImpl.java: 存在且包含 syncWorkflowStatus 方法', () => {
  const content = readFinanceService('FinStocktakeServiceImpl.java');
  assert.match(content, /syncWorkflowStatus\s*\(/);
});

test('FinStocktakeServiceImpl.java: 包含 startWorkflowProcess 工作流启动方法', () => {
  const content = readFinanceService('FinStocktakeServiceImpl.java');
  assert.match(content, /startWorkflowProcess\s*\(/);
  // 在提交时调用工作流启动
  assert.match(content, /startWorkflowProcess\s*\(\s*header/i);
});

test('FinStocktakeServiceImpl.java: 包含 workflowServiceUrl 配置与 RestTemplate 调用', () => {
  const content = readFinanceService('FinStocktakeServiceImpl.java');
  assert.match(content, /workflowServiceUrl/i);
  assert.match(content, /RestTemplate/i);
  // 工作流启动 URL 拼接 /instance/start
  assert.match(content, /\/instance\/start/i);
});

test('FinStocktakeServiceImpl.java: 原状态机保留（DRAFT→COUNTING→SUBMITTED→RECOUNTING→APPROVED→POSTED）', () => {
  const content = readFinanceService('FinStocktakeServiceImpl.java');
  const states = ['DRAFT', 'COUNTING', 'SUBMITTED', 'RECOUNTING', 'APPROVED', 'POSTED'];
  for (const state of states) {
    assert.match(content, new RegExp(`STATUS_${state}`, 'i'), `必须保留状态 ${state}`);
  }
});

test('FinStocktakeServiceImpl.java: 工作流启动失败时优雅降级（不阻塞提交事务）', () => {
  const content = readFinanceService('FinStocktakeServiceImpl.java');
  // try-catch 包裹工作流调用，catch 中记录日志但不抛异常
  assert.match(content, /优雅降级|不阻塞/i);
  assert.match(content, /log\.(warn|error)/i);
});
