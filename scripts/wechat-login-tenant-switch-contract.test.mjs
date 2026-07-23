import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');

function readFile(path) {
  return readFileSync(join(repoRoot, path), 'utf8');
}

// ============================================================================
// Task 3A 契约：租户级"是否启用微信登录"参数
// 计划文档要求：
//   - 参数 mp.wechat.login.enabled 按 tenant_id 隔离
//   - 默认值 false，已有租户不得被默认开启
//   - 小程序能力接口 GET /member/mp/capabilities 返回 {wechatLoginEnabled}
//   - 后端微信登录接口也必须再次校验该参数
//   - 小程序登录页仅当 wechatLoginEnabled === true 时渲染微信登录按钮
//   - 能力接口失败/超时/返回异常时隐藏按钮（fail-closed）
// ============================================================================

// --- SQL 契约 ---

test('system_wechat_login_config.sql: 文件以 SET NAMES utf8mb4 开头', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  assert.match(sql, /^SET NAMES utf8mb4;/);
});

test('system_wechat_login_config.sql: 插入 mp.wechat.login.enabled 配置', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  assert.match(sql, /mp\.wechat\.login\.enabled/i);
});

test('system_wechat_login_config.sql: 默认值为 false', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  assert.match(sql, /'false'/i);
});

test('system_wechat_login_config.sql: 公共配置 tenant_id=0', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  // INSERT ... SELECT 0 ... 或 INSERT INTO sys_config (tenant_id, ...) VALUES (0, ...)
  assert.match(sql, /tenant_id\s*,[\s\S]*?0[\s,)]/);
});

test('system_wechat_login_config.sql: 幂等（INSERT ... WHERE NOT EXISTS 或 INSERT IGNORE）', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  const isIdempotent = /WHERE\s+NOT\s+EXISTS/i.test(sql) || /INSERT\s+IGNORE/i.test(sql);
  assert.ok(isIdempotent, 'SQL 必须幂等，使用 WHERE NOT EXISTS 或 INSERT IGNORE');
});

test('system_wechat_login_config.sql: 标记为系统内置 config_type=Y', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  assert.match(sql, /config_type/i);
  assert.match(sql, /'Y'/);
});

test('system_wechat_login_config.sql: 包含校验 SELECT 输出', () => {
  const sql = readFile('sql/system_wechat_login_config.sql');
  assert.match(sql, /SELECT[\s\S]*?mp\.wechat\.login\.enabled/i);
});

// --- Java API 层契约：RemoteUserService ---

const REMOTE_USER_PATH =
  'junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/RemoteUserService.java';

test('RemoteUserService: 包含 isWechatLoginEnabled Feign 方法', () => {
  const java = readFile(REMOTE_USER_PATH);
  assert.match(java, /isWechatLoginEnabled/i);
  // 必须是 Feign 调用（@GetMapping）
  assert.match(java, /@GetMapping[\s\S]*?isWechatLoginEnabled/i);
  // 必须接受 tenantId 参数
  assert.match(java, /isWechatLoginEnabled[\s\S]*?Long\s+tenantId/i);
  // 必须接受 source 参数（@RequestHeader FROM_SOURCE）
  assert.match(java, /isWechatLoginEnabled[\s\S]*?FROM_SOURCE/i);
});

// --- Java API 层契约：RemoteUserFallbackFactory ---

const FALLBACK_PATH =
  'junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/factory/RemoteUserFallbackFactory.java';

test('RemoteUserFallbackFactory: isWechatLoginEnabled fallback 返回 false（fail-closed）', () => {
  const java = readFile(FALLBACK_PATH);
  assert.match(java, /isWechatLoginEnabled/i);
  // fallback 必须返回 false（R.ok(false)），不能返回 true 或 null
  const fallbackMatch = java.match(/isWechatLoginEnabled[\s\S]*?\{([\s\S]*?)\}/);
  assert.ok(fallbackMatch, '必须有 isWechatLoginEnabled 的 fallback 实现');
  assert.match(fallbackMatch[1], /false/i);
});

// --- System 模块契约：ISysConfigService ---

const CONFIG_SERVICE_PATH =
  'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/ISysConfigService.java';

test('ISysConfigService: 包含 isWechatLoginEnabled 方法声明', () => {
  const java = readFile(CONFIG_SERVICE_PATH);
  assert.match(java, /isWechatLoginEnabled/i);
  assert.match(java, /boolean\s+isWechatLoginEnabled/i);
});

// --- System 模块契约：SysConfigServiceImpl ---

const CONFIG_SERVICE_IMPL_PATH =
  'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysConfigServiceImpl.java';

test('SysConfigServiceImpl: 实现 isWechatLoginEnabled，读取 mp.wechat.login.enabled', () => {
  const java = readFile(CONFIG_SERVICE_IMPL_PATH);
  assert.match(java, /isWechatLoginEnabled/i);
  assert.match(java, /mp\.wechat\.login\.enabled/i);
});

test('SysConfigServiceImpl: isWechatLoginEnabled 只有值为 true（忽略大小写）时返回 true', () => {
  const java = readFile(CONFIG_SERVICE_IMPL_PATH);
  // 必须使用 "true".equalsIgnoreCase 判断，而非解析为布尔值
  assert.match(java, /["']true["']\.equalsIgnoreCase/i);
});

test('SysConfigServiceImpl: isWechatLoginEnabled 异常时 fail-closed 返回 false', () => {
  const java = readFile(CONFIG_SERVICE_IMPL_PATH);
  // isWechatLoginEnabled 方法体内必须有 try-catch，catch 块返回 false
  const methodMatch = java.match(/isWechatLoginEnabled[\s\S]*?\{([\s\S]*?)\n\s{4}\}/);
  assert.ok(methodMatch, '必须有 isWechatLoginEnabled 方法实现');
  const methodBody = methodMatch[1];
  assert.match(methodBody, /catch/i);
  assert.match(methodBody, /return\s+false/i);
});

test('SysConfigServiceImpl: isWechatLoginEnabled 支持按 tenantId 查询（设置 TenantContext）', () => {
  const java = readFile(CONFIG_SERVICE_IMPL_PATH);
  assert.match(java, /isWechatLoginEnabled[\s\S]*?TenantContext\.setTenantId/i);
});

// --- System 模块契约：SysUserController ---

const USER_CONTROLLER_PATH =
  'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java';

test('SysUserController: 包含 @InnerAuth isWechatLoginEnabled 内部接口', () => {
  const java = readFile(USER_CONTROLLER_PATH);
  assert.match(java, /isWechatLoginEnabled/i);
  // 必须有 @InnerAuth 注解
  assert.match(java, /@InnerAuth[\s\S]*?isWechatLoginEnabled/i);
  // 必须是 GetMapping
  assert.match(java, /@GetMapping[\s\S]*?isWechatLoginEnabled/i);
});

// --- Member 模块契约：MemMpController ---

const MP_CONTROLLER_PATH =
  'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMpController.java';

test('MemMpController: 包含 getCapabilities 能力接口', () => {
  const java = readFile(MP_CONTROLLER_PATH);
  assert.match(java, /getCapabilities/i);
  assert.match(java, /@GetMapping[\s\S]*?getCapabilities/i);
});

test('MemMpController: getCapabilities 返回 wechatLoginEnabled 字段', () => {
  const java = readFile(MP_CONTROLLER_PATH);
  assert.match(java, /wechatLoginEnabled/i);
});

test('MemMpController: getCapabilities 接受可选 tenantId 参数', () => {
  const java = readFile(MP_CONTROLLER_PATH);
  assert.match(java, /getCapabilities[\s\S]*?tenantId/i);
  assert.match(java, /required\s*=\s*false/i);
});

test('MemMpController: getCapabilities 异常时 fail-closed 返回 false', () => {
  const java = readFile(MP_CONTROLLER_PATH);
  const methodMatch = java.match(/getCapabilities[\s\S]*?\{([\s\S]*?)\n\s{4}\}/);
  assert.ok(methodMatch, '必须有 getCapabilities 方法实现');
  const methodBody = methodMatch[1];
  // 必须有 try-catch，catch 块设置 enabled=false
  assert.match(methodBody, /catch/i);
  assert.match(methodBody, /false/i);
});

test('MemMpController: 注入 RemoteUserService', () => {
  const java = readFile(MP_CONTROLLER_PATH);
  assert.match(java, /RemoteUserService/i);
  assert.match(java, /@Autowired/i);
});

// --- Auth 模块契约：SysLoginService 后端再校验 ---

const LOGIN_SERVICE_PATH =
  'junsong-auth/src/main/java/com/junsong/auth/service/SysLoginService.java';

test('SysLoginService: wechatLogin 方法包含微信登录开关校验', () => {
  const java = readFile(LOGIN_SERVICE_PATH);
  // wechatLogin 方法体内必须调用 isWechatLoginEnabled 或 assertWechatLoginEnabled
  const wechatLoginMatch = java.match(/wechatLogin[\s\S]*?\n\s{4}\}/);
  assert.ok(wechatLoginMatch, '必须有 wechatLogin 方法');
  assert.match(
    wechatLoginMatch[0],
    /isWechatLoginEnabled|assertWechatLoginEnabled/i,
    'wechatLogin 方法必须校验微信登录开关'
  );
});

test('SysLoginService: wechatBind 方法包含微信登录开关校验', () => {
  const java = readFile(LOGIN_SERVICE_PATH);
  const wechatBindMatch = java.match(/wechatBind[\s\S]*?\n\s{4}\}/);
  assert.ok(wechatBindMatch, '必须有 wechatBind 方法');
  assert.match(
    wechatBindMatch[0],
    /isWechatLoginEnabled|assertWechatLoginEnabled/i,
    'wechatBind 方法必须校验微信登录开关'
  );
});

test('SysLoginService: 开关关闭时抛出 ServiceException（fail-closed）', () => {
  const java = readFile(LOGIN_SERVICE_PATH);
  // 校验方法中必须有 throw ServiceException
  const assertMatch = java.match(/assertWechatLoginEnabled[\s\S]*?\{([\s\S]*?)\n\s{4}\}/);
  if (assertMatch) {
    assert.match(assertMatch[1], /throw\s+new\s+ServiceException/i);
  } else {
    // 如果没有独立的 assertWechatLoginEnabled 方法，则在 wechatLogin/wechatBind 中直接校验
    assert.match(java, /isWechatLoginEnabled[\s\S]*?throw\s+new\s+ServiceException/i);
  }
});

// --- 小程序契约：login/index.vue ---

const LOGIN_PAGE_PATH = 'junsong-miniprogram/src/pages/login/index.vue';

test('login/index.vue: 调用 capabilities 接口查询微信登录开关', () => {
  const vue = readFile(LOGIN_PAGE_PATH);
  assert.match(vue, /capabilities/i);
  assert.match(vue, /wechatLoginEnabled/i);
});

test('login/index.vue: 仅当 wechatLoginEnabled === true 时渲染微信登录按钮', () => {
  const vue = readFile(LOGIN_PAGE_PATH);
  // 模板中必须有 v-if 或 v-show 控制微信登录按钮显示
  assert.match(vue, /v-if.*wechatLoginEnabled|v-show.*wechatLoginEnabled/i);
});

test('login/index.vue: capabilities 接口失败时隐藏微信登录按钮（fail-closed）', () => {
  const vue = readFile(LOGIN_PAGE_PATH);
  // 默认值必须为 false
  assert.match(vue, /wechatLoginEnabled.*false|wechatLoginEnabled:\s*false/i);
});

// --- 网关契约：白名单 ---

const GATEWAY_PATH = 'junsong-gateway/src/main/resources/bootstrap.yml';

test('gateway bootstrap.yml: /member/mp/capabilities 加入白名单', () => {
  const yml = readFile(GATEWAY_PATH);
  assert.match(yml, /\/member\/mp\/capabilities/i);
});

test('gateway bootstrap.yml: /auth/mp/wechat/login 加入白名单', () => {
  const yml = readFile(GATEWAY_PATH);
  assert.match(yml, /\/auth\/mp\/wechat\/login/i);
});

test('gateway bootstrap.yml: /auth/mp/wechat/bind 加入白名单', () => {
  const yml = readFile(GATEWAY_PATH);
  assert.match(yml, /\/auth\/mp\/wechat\/bind/i);
});
