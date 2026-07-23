import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

function readMapperXml(path) {
  return readFileSync(join(repoRoot, path), 'utf8');
}

// ============================================================================
// SQL 契约：system_user_mp_binding.sql
// 计划文档第 4 节要求：
//   - 表 sys_user_mp_binding
//   - 字段：binding_id, tenant_id, user_id, app_id, openid, unionid(可空),
//           status(ACTIVE/REVOKED), bound_time, last_login_time, bound_by,
//           revoked_time, revoked_by, remark, create_time, update_time
//   - (app_id, openid) 唯一
//   - 查询必须带 tenant_id
//   - 解绑使用 REVOKED 状态保留审计链，不物理删除
//   - SQL 以 SET NAMES utf8mb4; 开始
//   - 脚本可重复执行并输出校验结果
// ============================================================================

test('system_user_mp_binding.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('system_user_mp_binding.sql: 创建 sys_user_mp_binding 表', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /CREATE TABLE[\s\S]*?sys_user_mp_binding/i);
});

test('system_user_mp_binding.sql: 使用 IF NOT EXISTS 保证幂等', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /CREATE TABLE IF NOT EXISTS[\s\S]*?sys_user_mp_binding/i);
});

test('system_user_mp_binding.sql: 包含 tenant_id BIGINT NOT NULL', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /tenant_id\s+BIGINT\s+NOT\s+NULL/i);
});

test('system_user_mp_binding.sql: 包含 user_id BIGINT NOT NULL', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /user_id\s+BIGINT\s+NOT\s+NULL/i);
});

test('system_user_mp_binding.sql: 包含 app_id VARCHAR NOT NULL', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /app_id\s+VARCHAR\s*\(\s*\d+\s*\)\s+NOT\s+NULL/i);
});

test('system_user_mp_binding.sql: 包含 openid VARCHAR NOT NULL', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /openid\s+VARCHAR\s*\(\s*\d+\s*\)\s+NOT\s+NULL/i);
});

test('system_user_mp_binding.sql: 包含 unionid VARCHAR 可空', () => {
  const sql = readSql('system_user_mp_binding.sql');
  // unionid 必须存在，且 NOT NULL 不能出现（即可空）
  assert.match(sql, /unionid\s+VARCHAR/i);
  const unionidLine = sql.match(/unionid\s+VARCHAR[^\n,]*/i)?.[0] || '';
  assert.doesNotMatch(unionidLine, /NOT\s+NULL/i);
});

test('system_user_mp_binding.sql: status 字段使用 ACTIVE/REVOKED 取值（CHAR 或 VARCHAR）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /status\s+(?:CHAR|VARCHAR)/i);
  // 注释中要说明 ACTIVE / REVOKED 取值
  assert.match(sql, /ACTIVE\s*\/\s*REVOKED/i);
  // 默认值应为 ACTIVE
  assert.match(sql, /status\s+(?:CHAR|VARCHAR)[^\n]*DEFAULT\s+'ACTIVE'/i);
});

test('system_user_mp_binding.sql: 包含 bound_time / last_login_time / bound_by / revoked_time / revoked_by / remark', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /bound_time/i);
  assert.match(sql, /last_login_time/i);
  assert.match(sql, /bound_by/i);
  assert.match(sql, /revoked_time/i);
  assert.match(sql, /revoked_by/i);
  assert.match(sql, /remark/i);
  assert.match(sql, /create_time/i);
  assert.match(sql, /update_time/i);
});

test('system_user_mp_binding.sql: (app_id, openid) 唯一键', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(
    sql,
    /UNIQUE KEY\s+\w*[^\(]*\(\s*app_id\s*,\s*openid\s*\)/i
  );
});

test('system_user_mp_binding.sql: tenant_id 普通索引（用于按租户过滤查询）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /KEY\s+\w*[^\(]*\(\s*tenant_id\s*\)/i);
});

test('system_user_mp_binding.sql: user_id 索引（用于按用户查询绑定）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /KEY\s+\w*[^\(]*\(\s*user_id\s*\)/i);
});

test('system_user_mp_binding.sql: (tenant_id, user_id) 复合索引（用于管理员按租户+用户查绑定）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /KEY\s+\w*[^\(]*\(\s*tenant_id\s*,\s*user_id\s*\)/i);
});

test('system_user_mp_binding.sql: 使用 utf8mb4 字符集', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.match(sql, /ENGINE=InnoDB\s+DEFAULT\s+CHARSET=utf8mb4/i);
});

test('system_user_mp_binding.sql: 不允许包含物理 DELETE 语句（解绑不删除历史）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  // 禁止 DELETE FROM sys_user_mp_binding
  assert.doesNotMatch(sql, /DELETE\s+FROM\s+sys_user_mp_binding/i);
});

test('system_user_mp_binding.sql: 不允许 DROP TABLE（非破坏迁移）', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.doesNotMatch(sql, /DROP\s+TABLE/i);
});

test('system_user_mp_binding.sql: 不允许 TRUNCATE TABLE', () => {
  const sql = readSql('system_user_mp_binding.sql');
  assert.doesNotMatch(sql, /TRUNCATE\s+TABLE/i);
});

test('system_user_mp_binding.sql: 必须输出对账/校验 SELECT 语句', () => {
  const sql = readSql('system_user_mp_binding.sql');
  // 至少有一条 SELECT 用于校验（如统计绑定数量、ACTIVE 数量、REVOKED 数量）
  assert.match(sql, /SELECT[\s\S]*?sys_user_mp_binding/i);
});

test('system_user_mp_binding.sql: 校验输出包含 ACTIVE / REVOKED 数量统计', () => {
  const sql = readSql('system_user_mp_binding.sql');
  // 期望出现按 status 分组的统计，或单独的 ACTIVE/REVOKED 计数
  const hasActiveCount = /COUNT[\s\S]*?status\s*=\s*'ACTIVE'/i.test(sql) ||
    /status\s+IN\s*\(\s*'ACTIVE'[\s\S]*?'REVOKED'[\s\S]*\)[\s\S]*GROUP\s+BY\s+status/i.test(sql) ||
    /GROUP\s+BY\s+status/i.test(sql);
  assert.ok(hasActiveCount, 'SQL 必须输出 ACTIVE/REVOKED 数量统计');
});

// ============================================================================
// Mapper XML 契约：所有查询必须显式带 tenant_id
// ============================================================================

const MAPPER_XML_PATH =
  'junsong-modules/junsong-system/src/main/resources/mapper/system/SysUserMpBindingMapper.xml';

test('SysUserMpBindingMapper.xml: 文件存在且 namespace 正确', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  assert.match(xml, /<mapper[^>]*namespace="com\.junsong\.system\.mapper\.SysUserMpBindingMapper"/);
});

test('SysUserMpBindingMapper.xml: 所有 select 必须显式带 tenant_id 条件（selectActiveByAppOpenidForLogin 除外）', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  // 提取所有 select 语句
  const selects = xml.match(/<select[\s\S]*?<\/select>/gi) || [];
  assert.ok(selects.length > 0, '至少应有一个 select 语句');
  for (const sel of selects) {
    // selectActiveByAppOpenidForLogin 是唯一例外：登录时尚不知道租户，需全局查找
    if (/id="selectActiveByAppOpenidForLogin"/i.test(sel)) {
      continue;
    }
    assert.match(
      sel,
      /tenant_id\s*=\s*#\{tenantId\}/i,
      `select 语句必须包含 tenant_id = #{tenantId}：${sel.slice(0, 120)}...`
    );
  }
});

test('SysUserMpBindingMapper.xml: 不允许物理 DELETE，只能 UPDATE status 为 REVOKED', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  // 禁止 <delete> 标签操作 sys_user_mp_binding
  assert.doesNotMatch(xml, /<delete[\s\S]*?sys_user_mp_binding/i);
  // 必须有更新 status 为 REVOKED 的 update 语句
  assert.match(xml, /<update[\s\S]*?sys_user_mp_binding[\s\S]*?status\s*=\s*'REVOKED'/i);
});

test('SysUserMpBindingMapper.xml: insert 必须包含 tenant_id、user_id、app_id、openid、status', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  const insertMatch = xml.match(/<insert[\s\S]*?<\/insert>/i);
  assert.ok(insertMatch, '必须有 insert 语句');
  const insert = insertMatch[0];
  assert.match(insert, /tenant_id/i);
  assert.match(insert, /user_id/i);
  assert.match(insert, /app_id/i);
  assert.match(insert, /openid/i);
  assert.match(insert, /status/i);
});

test('SysUserMpBindingMapper.xml: 按 (tenantId, appId, openid) 查询 ACTIVE 绑定关系', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  // 必须有按 tenantId + appId + openid 查询的方法
  const hasMethod = /tenant_id\s*=\s*#\{tenantId\}[\s\S]*?app_id\s*=\s*#\{appId\}[\s\S]*?openid\s*=\s*#\{openid\}/i.test(xml) ||
    /app_id\s*=\s*#\{appId\}[\s\S]*?openid\s*=\s*#\{openid\}[\s\S]*?tenant_id\s*=\s*#\{tenantId\}/i.test(xml);
  assert.ok(hasMethod, '必须能按 tenantId + appId + openid 查询');
});

test('SysUserMpBindingMapper.xml: 存在 selectActiveByAppOpenidForLogin 登录全局查找方法', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  assert.match(xml, /id="selectActiveByAppOpenidForLogin"/i);
  // 该方法按 app_id + openid 查询，不带 tenant_id
  const loginSelect = xml.match(/<select\s+id="selectActiveByAppOpenidForLogin"[\s\S]*?<\/select>/i)?.[0] || '';
  assert.match(loginSelect, /app_id\s*=\s*#\{appId\}/i);
  assert.match(loginSelect, /openid\s*=\s*#\{openid\}/i);
  assert.match(loginSelect, /status\s*=\s*'ACTIVE'/i);
});

test('SysUserMpBindingMapper.xml: 按 (tenantId, userId) 查询绑定列表（管理员查看）', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  const hasMethod = /tenant_id\s*=\s*#\{tenantId\}[\s\S]*?user_id\s*=\s*#\{userId\}/i.test(xml);
  assert.ok(hasMethod, '必须能按 tenantId + userId 查询');
});

test('SysUserMpBindingMapper.xml: 更新 last_login_time 方法存在', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  assert.match(xml, /last_login_time/i);
});

test('SysUserMpBindingMapper.xml: 更新 status / revoked_time / revoked_by 的撤销方法存在', () => {
  const xml = readMapperXml(MAPPER_XML_PATH);
  assert.match(xml, /revoked_time/i);
  assert.match(xml, /revoked_by/i);
});

// ============================================================================
// Java 实体契约
// ============================================================================

const ENTITY_PATH =
  'junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/domain/SysUserMpBinding.java';

test('SysUserMpBinding.java: 文件存在且包名正确', () => {
  const java = readMapperXml(ENTITY_PATH);
  assert.match(java, /package\s+com\.junsong\.system\.api\.domain;/);
  assert.match(java, /class\s+SysUserMpBinding/);
});

test('SysUserMpBinding.java: 包含全部字段（驼峰命名）', () => {
  const java = readMapperXml(ENTITY_PATH);
  assert.match(java, /private\s+Long\s+bindingId;/);
  assert.match(java, /private\s+Long\s+tenantId;/);
  assert.match(java, /private\s+Long\s+userId;/);
  assert.match(java, /private\s+String\s+appId;/);
  assert.match(java, /private\s+String\s+openid;/);
  assert.match(java, /private\s+String\s+unionid;/);
  assert.match(java, /private\s+String\s+status;/);
  assert.match(java, /private\s+Date\s+boundTime;/);
  assert.match(java, /private\s+Date\s+lastLoginTime;/);
  assert.match(java, /private\s+String\s+boundBy;/);
  assert.match(java, /private\s+Date\s+revokedTime;/);
  assert.match(java, /private\s+String\s+revokedBy;/);
});

test('SysUserMpBinding.java: 不暴露 openid/unionid 的明文 toString（避免日志泄露）', () => {
  const java = readMapperXml(ENTITY_PATH);
  // 简单校验：不存在打印 openid 的 toString 方法
  // 允许 getter/setter，但不允许 toString 包含 openid
  const toStringMatch = java.match(/public\s+String\s+toString\s*\(\)\s*\{[\s\S]*?\}/);
  if (toStringMatch) {
    assert.doesNotMatch(toStringMatch[0], /openid/i);
    assert.doesNotMatch(toStringMatch[0], /unionid/i);
  }
});

// ============================================================================
// Mapper 接口契约
// ============================================================================

const MAPPER_JAVA_PATH =
  'junsong-modules/junsong-system/src/main/java/com/junsong/system/mapper/SysUserMpBindingMapper.java';

test('SysUserMpBindingMapper.java: 文件存在且包名正确', () => {
  const java = readMapperXml(MAPPER_JAVA_PATH);
  assert.match(java, /package\s+com\.junsong\.system\.mapper;/);
  assert.match(java, /interface\s+SysUserMpBindingMapper/);
});

test('SysUserMpBindingMapper.java: 包含核心方法', () => {
  const java = readMapperXml(MAPPER_JAVA_PATH);
  // 按 (tenantId, appId, openid) 查询 ACTIVE 绑定
  assert.match(java, /selectByAppOpenid[\s\S]*?Long\s+tenantId[\s\S]*?String\s+appId[\s\S]*?String\s+openid/);
  // 按 (tenantId, userId) 查询绑定列表
  assert.match(java, /selectByUserId[\s\S]*?Long\s+tenantId[\s\S]*?Long\s+userId/);
  // 按 bindingId 查询（必须带 tenantId）
  assert.match(java, /selectByBindingId[\s\S]*?Long\s+tenantId[\s\S]*?Long\s+bindingId/);
  // insert
  assert.match(java, /int\s+insert\s*\(/);
  // 撤销（不是 delete）
  assert.match(java, /int\s+revoke\s*\(/);
  // 更新最近登录时间
  assert.match(java, /int\s+updateLastLoginTime\s*\(/);
});
