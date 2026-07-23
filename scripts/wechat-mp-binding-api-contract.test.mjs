import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');

function readMain(name) {
  return readFileSync(join(repoRoot, 'junsong-auth', 'src', 'main', 'java', 'com', 'junsong', 'auth', name), 'utf8');
}

function readForm(name) {
  return readFileSync(join(repoRoot, 'junsong-auth', 'src', 'main', 'java', 'com', 'junsong', 'auth', 'form', name), 'utf8');
}

// ============================================================================
// API 契约：微信小程序绑定/登录/解绑
// 计划文档 Task 4 要求：
//   - POST /auth/mp/wechat/login   微信快捷登录
//   - POST /auth/mp/wechat/bind    绑定现有账号
//   - POST /auth/mp/wechat/unbind  解绑
//   - GET  /auth/mp/wechat/binding 查询绑定状态
//   - 绑定接口必须验证已有账号凭据
//   - 登录成功后调用 createTokenMp
//   - 错误响应统一脱敏（不泄露 openid / unionid / AppSecret）
// ============================================================================

// ---------------------------------------------------------------------------
// WechatMpBindingController.java 端点契约
// ---------------------------------------------------------------------------

test('WechatMpBindingController: 类存在且映射到 /auth/mp/wechat', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /@RequestMapping\s*\(\s*"\/auth\/mp\/wechat"\s*\)/);
});

test('WechatMpBindingController: POST /login 端点存在', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /@PostMapping\s*\(\s*"\/login"\s*\)/);
  assert.match(src, /WechatMpLoginBody/);
});

test('WechatMpBindingController: POST /bind 端点存在', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /@PostMapping\s*\(\s*"\/bind"\s*\)/);
  assert.match(src, /WechatMpBindBody/);
});

test('WechatMpBindingController: POST /unbind 端点存在', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /@PostMapping\s*\(\s*"\/unbind"\s*\)/);
  assert.match(src, /WechatMpUnbindBody/);
});

test('WechatMpBindingController: GET /binding 端点存在', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /@GetMapping\s*\(\s*"\/binding"\s*\)/);
});

test('WechatMpBindingController: login 和 bind 调用 createTokenMp 生成小程序专用 Token', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  const loginMethod = src.match(/login\s*\([^)]*\)\s*\{[\s\S]*?\}/);
  assert.ok(loginMethod, 'login 方法存在');
  assert.match(loginMethod[0], /createTokenMp/);
  const bindMethod = src.match(/bind\s*\([^)]*\)\s*\{[\s\S]*?\}/);
  assert.ok(bindMethod, 'bind 方法存在');
  assert.match(bindMethod[0], /createTokenMp/);
});

test('WechatMpBindingController: unbind 和 binding 从 SecurityUtils 获取当前用户', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /SecurityUtils\.getUserId\s*\(\s*\)/);
  assert.match(src, /SecurityUtils\.getUsername\s*\(\s*\)/);
  assert.match(src, /SecurityUtils\.getLoginUser\s*\(\s*\)/);
});

test('WechatMpBindingController: unbind/binding 在登录上下文缺失时 fail-closed', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  assert.match(src, /ServiceException/);
  assert.match(src, /登录信息已失效/);
});

// ---------------------------------------------------------------------------
// SysLoginService.java 方法契约
// ---------------------------------------------------------------------------

test('SysLoginService: wechatLogin 方法签名正确', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /public\s+LoginUser\s+wechatLogin\s*\(\s*String\s+code\s*,\s*Long\s+deptId\s*\)/);
});

test('SysLoginService: wechatBind 方法签名正确', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /public\s+LoginUser\s+wechatBind\s*\(\s*String\s+code\s*,\s*String\s+username\s*,\s*String\s+password\s*,\s*Long\s+deptId\s*\)/);
});

test('SysLoginService: wechatUnbind 方法签名正确', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /public\s+void\s+wechatUnbind\s*\(\s*Long\s+tenantId\s*,\s*Long\s+userId\s*,\s*String\s+username\s*,\s*String\s+revokeReason\s*\)/);
});

test('SysLoginService: getWechatBindings 方法签名正确', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /public\s+List<SysUserMpBinding>\s+getWechatBindings\s*\(\s*Long\s+tenantId\s*,\s*Long\s+userId\s*\)/);
});

test('SysLoginService: wechatLogin 使用 getUserInfoById 而非 getUserInfo（绑定提供 userId）', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  assert.match(wechatLoginMethod[0], /getUserInfoById/);
  assert.doesNotMatch(wechatLoginMethod[0], /getUserInfo\s*\(/);
});

test('SysLoginService: wechatBind 使用 getUserInfo（绑定使用用户名验证凭据）', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatBindMethod = src.match(/public\s+LoginUser\s+wechatBind[\s\S]*?\n    \}/);
  assert.ok(wechatBindMethod, 'wechatBind 方法存在');
  assert.match(wechatBindMethod[0], /getUserInfo\s*\(/);
  assert.match(wechatBindMethod[0], /passwordService\.validate/);
});

test('SysLoginService: wechatLogin 包含 IP 黑名单校验', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  assert.match(wechatLoginMethod[0], /SYS_LOGIN_BLACKIPLIST/);
});

test('SysLoginService: wechatBind 包含 IP 黑名单校验', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatBindMethod = src.match(/public\s+LoginUser\s+wechatBind[\s\S]*?\n    \}/);
  assert.ok(wechatBindMethod, 'wechatBind 方法存在');
  assert.match(wechatBindMethod[0], /SYS_LOGIN_BLACKIPLIST/);
});

test('SysLoginService: wechatLogin 校验账号删除状态', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  assert.match(wechatLoginMethod[0], /UserStatus\.DELETED/);
  assert.match(wechatLoginMethod[0], /已删除/);
});

test('SysLoginService: wechatLogin 校验账号停用状态', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  assert.match(wechatLoginMethod[0], /UserStatus\.DISABLE/);
  assert.match(wechatLoginMethod[0], /停用/);
});

test('SysLoginService: wechatBind 校验账号删除和停用状态', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatBindMethod = src.match(/public\s+LoginUser\s+wechatBind[\s\S]*?\n    \}/);
  assert.ok(wechatBindMethod, 'wechatBind 方法存在');
  assert.match(wechatBindMethod[0], /UserStatus\.DELETED/);
  assert.match(wechatBindMethod[0], /UserStatus\.DISABLE/);
});

test('SysLoginService: wechatUnbind 查找 ACTIVE 绑定并调用 revoke', () => {
  const src = readMain('service/SysLoginService.java');
  const unbindMethod = src.match(/public\s+void\s+wechatUnbind[\s\S]*?\n    \}/);
  assert.ok(unbindMethod, 'wechatUnbind 方法存在');
  assert.match(unbindMethod[0], /ACTIVE/);
  assert.match(unbindMethod[0], /revoke/);
});

test('SysLoginService: 注入 WechatMiniProgramService 和 WechatMiniProgramProperties', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /@Autowired\s+private\s+WechatMiniProgramService\s+wechatMiniProgramService/);
  assert.match(src, /@Autowired\s+private\s+WechatMiniProgramProperties\s+wechatMpProperties/);
});

test('SysLoginService: 注入 RemoteUserMpBindingService', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /@Autowired\s+private\s+RemoteUserMpBindingService\s+remoteUserMpBindingService/);
});

// ---------------------------------------------------------------------------
// 错误信息脱敏契约
// ---------------------------------------------------------------------------

test('SysLoginService: 错误信息不包含 openid（wechatLogin 方法）', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  // 所有 throw new ServiceException 的消息中不能包含 openid
  const throws = wechatLoginMethod[0].matchAll(/throw\s+new\s+ServiceException\s*\(\s*"([^"]*)"/g);
  for (const m of throws) {
    const msg = m[1].toLowerCase();
    assert.ok(!msg.includes('openid'), `错误信息不应包含 openid: "${m[1]}"`);
    assert.ok(!msg.includes('appsecret'), `错误信息不应包含 appsecret: "${m[1]}"`);
  }
});

test('SysLoginService: 错误信息不包含 openid（wechatBind 方法）', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatBindMethod = src.match(/public\s+LoginUser\s+wechatBind[\s\S]*?\n    \}/);
  assert.ok(wechatBindMethod, 'wechatBind 方法存在');
  const throws = wechatBindMethod[0].matchAll(/throw\s+new\s+ServiceException\s*\(\s*"([^"]*)"/g);
  for (const m of throws) {
    const msg = m[1].toLowerCase();
    assert.ok(!msg.includes('openid'), `错误信息不应包含 openid: "${m[1]}"`);
    assert.ok(!msg.includes('appsecret'), `错误信息不应包含 appsecret: "${m[1]}"`);
  }
});

test('SysLoginService: 错误信息不包含 openid（wechatUnbind 方法）', () => {
  const src = readMain('service/SysLoginService.java');
  const unbindMethod = src.match(/public\s+void\s+wechatUnbind[\s\S]*?\n    \}/);
  assert.ok(unbindMethod, 'wechatUnbind 方法存在');
  const throws = unbindMethod[0].matchAll(/throw\s+new\s+ServiceException\s*\(\s*"([^"]*)"/g);
  for (const m of throws) {
    const msg = m[1].toLowerCase();
    assert.ok(!msg.includes('openid'), `错误信息不应包含 openid: "${m[1]}"`);
  }
});

test('SysLoginService: 日志中使用 maskOpenid 脱敏而非明文 openid', () => {
  const src = readMain('service/SysLoginService.java');
  assert.match(src, /maskOpenid\s*\(/);
  // 日志中不应直接输出 openid 变量
  const logLines = src.matchAll(/log\.warn\s*\(\s*"([^"]*)"/g);
  for (const m of logLines) {
    assert.ok(!m[1].includes('openid='), `日志格式串不应包含 openid=: "${m[1]}"`);
  }
});

// ---------------------------------------------------------------------------
// 表单类契约
// ---------------------------------------------------------------------------

test('WechatMpLoginBody: 包含 code 和 deptId 字段', () => {
  const src = readForm('WechatMpLoginBody.java');
  assert.match(src, /private\s+String\s+code/);
  assert.match(src, /private\s+Long\s+deptId/);
});

test('WechatMpBindBody: 包含 code、username、password、deptId 字段', () => {
  const src = readForm('WechatMpBindBody.java');
  assert.match(src, /private\s+String\s+code/);
  assert.match(src, /private\s+String\s+username/);
  assert.match(src, /private\s+String\s+password/);
  assert.match(src, /private\s+Long\s+deptId/);
});

test('WechatMpUnbindBody: 包含 revokeReason 字段', () => {
  const src = readForm('WechatMpUnbindBody.java');
  assert.match(src, /private\s+String\s+revokeReason/);
});

// ---------------------------------------------------------------------------
// 安全契约
// ---------------------------------------------------------------------------

test('WechatMpBindingController: unbind 和 binding 端点需要登录态', () => {
  const src = readMain('controller/WechatMpBindingController.java');
  // unbind 方法必须从 SecurityUtils 获取 tenantId/userId
  const unbindMethod = src.match(/public\s+R<\?>\s+unbind[\s\S]*?\n    \}/);
  assert.ok(unbindMethod, 'unbind 方法存在');
  assert.match(unbindMethod[0], /getCurrentTenantId/);
  assert.match(unbindMethod[0], /SecurityUtils\.getUserId/);
  // binding 方法必须从 SecurityUtils 获取 tenantId/userId
  const bindingMethod = src.match(/public\s+R<List<SysUserMpBinding>>\s+binding[\s\S]*?\n    \}/);
  assert.ok(bindingMethod, 'binding 方法存在');
  assert.match(bindingMethod[0], /getCurrentTenantId/);
  assert.match(bindingMethod[0], /SecurityUtils\.getUserId/);
});

test('SysLoginService: wechatBind 不自动创建账号（必须验证已有账号凭据）', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatBindMethod = src.match(/public\s+LoginUser\s+wechatBind[\s\S]*?\n    \}/);
  assert.ok(wechatBindMethod, 'wechatBind 方法存在');
  // 必须调用 passwordService.validate 验证密码
  assert.match(wechatBindMethod[0], /passwordService\.validate/);
  // 不应调用 registerUserInfo
  assert.doesNotMatch(wechatBindMethod[0], /registerUserInfo/);
});

test('SysLoginService: wechatLogin 调用 updateLastLoginTime 更新最近登录时间', () => {
  const src = readMain('service/SysLoginService.java');
  const wechatLoginMethod = src.match(/public\s+LoginUser\s+wechatLogin[\s\S]*?\n    \}/);
  assert.ok(wechatLoginMethod, 'wechatLogin 方法存在');
  assert.match(wechatLoginMethod[0], /updateLastLoginTime/);
});
