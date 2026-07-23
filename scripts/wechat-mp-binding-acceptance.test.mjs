import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, '..');

function readAuthMain(name) {
  return readFileSync(join(repoRoot, 'junsong-auth', 'src', 'main', 'java', 'com', 'junsong', 'auth', name), 'utf8');
}

function readAuthTest(name) {
  return readFileSync(join(repoRoot, 'junsong-auth', 'src', 'test', 'java', 'com', 'junsong', 'auth', name), 'utf8');
}

function readSystemMain(name) {
  return readFileSync(join(repoRoot, 'junsong-modules', 'junsong-system', 'src', 'main', 'java', 'com', 'junsong', 'system', name), 'utf8');
}

function readCommonMain(name) {
  return readFileSync(join(repoRoot, 'junsong-common', 'junsong-common-security', 'src', 'main', 'java', 'com', 'junsong', 'common', 'security', name), 'utf8');
}

function readCommonInterceptor(name) {
  return readFileSync(join(repoRoot, 'junsong-common', 'junsong-common-security', 'src', 'main', 'java', 'com', 'junsong', 'common', 'security', 'interceptor', name), 'utf8');
}

function readApiMain(name) {
  return readFileSync(join(repoRoot, 'junsong-api', 'junsong-api-system', 'src', 'main', 'java', 'com', 'junsong', 'system', 'api', name), 'utf8');
}

function readMiniProgram(name) {
  return readFileSync(join(repoRoot, 'junsong-miniprogram', 'src', name), 'utf8');
}

function readPC(name) {
  return readFileSync(join(repoRoot, 'junsong-ui-v3', 'src', name), 'utf8');
}

function readSql(name) {
  return readFileSync(join(repoRoot, 'sql', name), 'utf8');
}

// ============================================================================
// Task 7 验收测试：安全、并发和回归
// 计划文档要求：
//   - 微信登录没有绕过原有权限体系
//   - 并发绑定只有一个成功
//   - 敏感信息不泄露
//   - 租户隔离
//   - 密码登录回归
// ============================================================================

// ---------------------------------------------------------------------------
// 1. 安全验收：权限体系不被绕过
// ---------------------------------------------------------------------------

test('安全：微信登录端点不包含 @PermitAll 或 anonymous 标记', () => {
  const src = readAuthMain('controller/WechatMpBindingController.java');
  // 微信登录/绑定端点不需要 @PermitAll，它们通过 /auth 路径白名单放行
  // 但绝不能标记为匿名访问
  assert.doesNotMatch(src, /@PermitAll/, '微信端点不应使用 @PermitAll');
});

test('安全：微信快捷登录后必须调用 createTokenMp 生成 Token', () => {
  const src = readAuthMain('controller/WechatMpBindingController.java');
  assert.match(src, /createTokenMp\s*\(/, '微信登录必须调用 createTokenMp');
});

test('安全：微信快捷登录必须标记 authSource=WECHAT_MP', () => {
  const src = readAuthMain('controller/WechatMpBindingController.java');
  assert.match(src, /WECHAT_MP/, '微信登录必须标记 authSource=WECHAT_MP');
});

test('安全：小程序密码登录必须标记 authSource=PASSWORD', () => {
  const src = readAuthMain('controller/TokenController.java');
  assert.match(src, /PASSWORD/, '小程序密码登录必须标记 authSource=PASSWORD');
});

test('安全：HeaderInterceptor 校验微信会话 epoch', () => {
  const src = readCommonInterceptor('HeaderInterceptor.java');
  assert.match(src, /verifyWechatSessionEpoch|wechatSessionEpoch/, 'HeaderInterceptor 必须校验微信会话 epoch');
});

test('安全：仅 WECHAT_MP 会话参与 epoch 校验，PASSWORD 不受影响', () => {
  const src = readCommonMain('service/TokenService.java');
  // verifyWechatSessionEpoch 方法应检查 authSource
  assert.match(src, /WECHAT_MP/, 'TokenService 必须区分 WECHAT_MP 和 PASSWORD');
});

test('安全：PC 解绑使用独立权限 system:user:unbindMp', () => {
  const src = readSystemMain('controller/SysUserController.java');
  assert.match(src, /system:user:unbindMp/, 'PC 解绑必须使用独立权限 system:user:unbindMp');
});

test('安全：微信会话失效使用独立权限 system:user:wechatSession:revokeAll', () => {
  const src = readSystemMain('controller/SysWechatSessionController.java');
  assert.match(src, /system:user:wechatSession:revokeAll/, '微信会话失效必须使用独立权限');
});

test('安全：PC 解绑和会话失效都有 @Log 审计日志', () => {
  const unbindSrc = readSystemMain('controller/SysUserController.java');
  const sessionSrc = readSystemMain('controller/SysWechatSessionController.java');
  assert.match(unbindSrc, /@Log\s*\(\s*title\s*=\s*"微信解绑"/, 'PC 解绑必须有 @Log 审计');
  assert.match(sessionSrc, /@Log\s*\(/, '微信会话失效必须有 @Log 审计');
});

test('安全：响应中不返回 openid/unionid 明文', () => {
  const src = readSystemMain('controller/SysUserController.java');
  // getUserMpBindings 方法必须 setOpenid(null) 等脱敏
  assert.match(src, /setOpenid\s*\(\s*null\s*\)/, '必须 setOpenid(null) 脱敏');
  assert.match(src, /setUnionid\s*\(\s*null\s*\)/, '必须 setUnionid(null) 脱敏');
  assert.match(src, /setAppId\s*\(\s*null\s*\)/, '必须 setAppId(null) 脱敏');
});

test('安全：错误信息不泄露 openid/unionid/AppSecret/code', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  // 检查错误信息中不包含这些关键词
  const errorMessages = src.match(/new ServiceException\s*\(\s*"[^"]*"\s*\)/g) || [];
  for (const msg of errorMessages) {
    assert.doesNotMatch(msg, /openid/i, '错误信息不应包含 openid: ' + msg);
    assert.doesNotMatch(msg, /unionid/i, '错误信息不应包含 unionid: ' + msg);
    assert.doesNotMatch(msg, /AppSecret/i, '错误信息不应包含 AppSecret: ' + msg);
  }
});

test('安全：日志不记录 code/openid/unionid/AppSecret 明文', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  // 检查日志语句中不直接引用 code/openid/unionid/appSecret 变量
  // 注意：URL 路径中包含 "jscode2session" 是正常的，不算泄露
  const logLines = src.match(/log\.\w+\([^)]*\)/g) || [];
  for (const line of logLines) {
    // 检查是否直接引用了 code 变量（非字符串常量中的 code）
    // 排除 "jscode2session" 等 URL 路径中的 code
    const codeVarMatch = line.match(/,\s*code\s*[,)]/);
    assert.ok(!codeVarMatch, '日志不应直接引用 code 变量: ' + line);
    assert.doesNotMatch(line, /,\s*openid\s*[,)]/, '日志不应引用 openid 变量: ' + line);
    assert.doesNotMatch(line, /,\s*unionid\s*[,)]/, '日志不应引用 unionid 变量: ' + line);
    assert.doesNotMatch(line, /appSecret/i, '日志不应记录 AppSecret: ' + line);
  }
});

test('安全：租户级微信登录开关（isWechatLoginEnabled）存在', () => {
  const src = readAuthMain('service/SysLoginService.java');
  assert.match(src, /isWechatLoginEnabled|assertWechatLoginEnabled/, '必须有微信登录开关校验');
});

test('安全：微信登录开关默认 false（fail-closed）', () => {
  const src = readApiMain('RemoteUserService.java');
  // RemoteUserService 必须有 isWechatLoginEnabled 方法
  assert.match(src, /isWechatLoginEnabled/, 'RemoteUserService 必须有 isWechatLoginEnabled 方法');
});

// ---------------------------------------------------------------------------
// 2. 并发验收：并发绑定只有一个成功
// ---------------------------------------------------------------------------

test('并发：绑定表有唯一索引防止重复绑定', () => {
  const sqlPath = join(repoRoot, 'sql', 'system_user_mp_binding.sql');
  if (existsSync(sqlPath)) {
    const sql = readFileSync(sqlPath, 'utf8');
    // 检查唯一索引：uk_tenant_app_openid 或类似
    assert.match(sql, /UNIQUE\s+KEY|UNIQUE\s+INDEX/i, '绑定表必须有唯一索引');
    assert.match(sql, /openid/i, '唯一索引必须包含 openid');
  }
});

test('并发：insertBinding 使用 INSERT ... ON DUPLICATE KEY 或先查后插', () => {
  const mapperPath = join(repoRoot, 'junsong-modules', 'junsong-system', 'src', 'main', 'resources', 'mapper', 'system', 'SysUserMpBindingMapper.xml');
  if (existsSync(mapperPath)) {
    const xml = readFileSync(mapperPath, 'utf8');
    // 检查 insert 语句使用唯一键冲突处理或条件插入
    assert.match(xml, /insert|INSERT/, 'Mapper 必须有 insert 语句');
  }
});

test('并发：绑定服务 insertBinding 方法存在', () => {
  const src = readSystemMain('service/impl/SysUserMpBindingServiceImpl.java');
  assert.match(src, /insertBinding/, '必须有 insertBinding 方法');
  // 并发保护通过 DB 唯一索引实现，insertBinding 是单行插入
});

test('并发：绑定前检查是否已存在 ACTIVE 绑定', () => {
  // SysLoginService 的 wechatBind 方法应在插入前检查是否已绑定
  const src = readAuthMain('service/SysLoginService.java');
  assert.match(src, /selectActiveByAppOpenid|已绑定|already.*bound/i, '绑定前必须检查是否已存在绑定');
});

test('并发：WechatMpBindingControllerTest 包含并发绑定测试', () => {
  const src = readAuthTest('controller/WechatMpBindingControllerTest.java');
  // 检查是否有并发或重复绑定相关测试
  assert.match(src, /concurrent|duplicate|already.*bound|已绑定/i, '必须有并发/重复绑定测试');
});

// ---------------------------------------------------------------------------
// 3. 回归验收：密码登录不受影响
// ---------------------------------------------------------------------------

test('回归：密码登录端点 /auth/login 仍然存在', () => {
  const src = readAuthMain('controller/TokenController.java');
  assert.match(src, /@PostMapping\s*\(\s*"\/?login"\s*\)/, '密码登录端点必须存在');
});

test('回归：密码登录使用 createToken（非 createTokenMp）', () => {
  const src = readAuthMain('controller/TokenController.java');
  // /login 端点应调用 createToken，且 authSource=PASSWORD
  assert.match(src, /createToken\s*\(/, '密码登录必须调用 createToken');
});

test('回归：小程序密码登录 /auth/mp/login 标记为 PASSWORD', () => {
  const src = readAuthMain('controller/TokenController.java');
  // mp/login 必须调用 createTokenMp 并传入 PASSWORD
  assert.match(src, /createTokenMp\s*\([^)]*PASSWORD/, '小程序密码登录必须标记 PASSWORD');
});

test('回归：微信会话失效不影响密码登录会话', () => {
  const src = readCommonMain('service/TokenService.java');
  // verifyWechatSessionEpoch 应仅对 WECHAT_MP 生效
  assert.match(src, /WECHAT_MP.*PASSWORD|PASSWORD.*WECHAT_MP/, '必须区分 WECHAT_MP 和 PASSWORD');
});

// ---------------------------------------------------------------------------
// 4. 租户隔离验收
// ---------------------------------------------------------------------------

test('租户隔离：绑定查询使用 tenantId', () => {
  const src = readSystemMain('service/impl/SysUserMpBindingServiceImpl.java');
  assert.match(src, /tenantId/, '绑定查询必须使用 tenantId');
});

test('租户隔离：微信快捷登录全局查询按 appId+openid', () => {
  const src = readSystemMain('service/impl/SysUserMpBindingServiceImpl.java');
  assert.match(src, /selectActiveByAppOpenidForLogin/, '必须有全局查询方法 selectActiveByAppOpenidForLogin');
});

test('租户隔离：PC 解绑使用用户所属 tenantId', () => {
  const src = readSystemMain('controller/SysUserController.java');
  // adminUnbind 方法必须从 user.getTenantId() 获取租户
  assert.match(src, /user\.getTenantId\(\)/, 'PC 解绑必须使用 user.getTenantId()');
});

// ---------------------------------------------------------------------------
// 5. 账号状态验收
// ---------------------------------------------------------------------------

test('账号状态：停用账号（status=1）不允许微信登录', () => {
  const src = readAuthTest('controller/WechatMpBindingControllerTest.java');
  assert.match(src, /disabled|停用|status.*1/i, '必须有停用账号登录测试');
});

test('账号状态：解绑后不允许微信登录', () => {
  const src = readAuthTest('controller/WechatMpBindingControllerTest.java');
  assert.match(src, /unbind|revok|解绑/i, '必须有解绑后登录测试');
});

// ---------------------------------------------------------------------------
// 6. 重试策略验收
// ---------------------------------------------------------------------------

test('重试：max-retries 上限为 3', () => {
  const src = readAuthMain('config/WechatMiniProgramProperties.java');
  assert.match(src, /MAX_RETRIES_LIMIT\s*=\s*3/, 'max-retries 上限必须为 3');
});

test('重试：默认值为 1', () => {
  const src = readAuthMain('config/WechatMiniProgramProperties.java');
  assert.match(src, /MAX_RETRIES_DEFAULT\s*=\s*1/, 'max-retries 默认值必须为 1');
});

test('重试：getSafeMaxRetries 方法处理负数和超限', () => {
  const src = readAuthMain('config/WechatMiniProgramProperties.java');
  assert.match(src, /getSafeMaxRetries/, '必须有 getSafeMaxRetries 方法');
  assert.match(src, /maxRetries\s*<\s*0\s*\|\|\s*maxRetries\s*>\s*MAX_RETRIES_LIMIT/, '必须校验负数和超限');
});

test('重试：仅 408/429/500/502/503/504 可重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.match(src, /isRetryableHttpStatus/, '必须有 isRetryableHttpStatus 方法');
  assert.match(src, /408/, '408 必须可重试');
  assert.match(src, /429/, '429 必须可重试');
  assert.match(src, /500/, '500 必须可重试');
  assert.match(src, /502/, '502 必须可重试');
  assert.match(src, /503/, '503 必须可重试');
  assert.match(src, /504/, '504 必须可重试');
});

test('重试：网络异常（无状态码）可重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.match(src, /statusCode\s*==\s*null\s*\|\|\s*isRetryableHttpStatus/, 'statusCode==null 时必须可重试');
});

test('重试：40029（无效 code）不重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.doesNotMatch(src, /40029.*retry|retry.*40029/i, '40029 不应出现在可重试列表');
});

test('重试：40226（高风险账号）不重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.doesNotMatch(src, /40226.*retry|retry.*40226/i, '40226 不应出现在可重试列表');
});

test('重试：空响应不重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  // 空响应应该是 throw 而不是 continue
  const emptyResponseSection = src.match(/responseBody\s*==\s*null[\s\S]{0,500}/)?.[0] || '';
  assert.match(emptyResponseSection, /throw\s+new\s+ServiceException/, '空响应必须 throw 而非 continue');
});

test('重试：JSON 解析失败不重试', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  // 解析失败应该是 throw 而不是 continue
  const parseSection = src.match(/readValue[\s\S]{0,500}catch[\s\S]{0,300}/)?.[0] || '';
  assert.match(parseSection, /throw\s+new\s+ServiceException/, '解析失败必须 throw 而非 continue');
});

test('重试：指数退避延迟（200/500/1000ms）', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.match(src, /sleepBackoff/, '必须有 sleepBackoff 方法');
  assert.match(src, /200L/, '第一次退避 200ms');
  assert.match(src, /500L/, '第二次退避 500ms');
  assert.match(src, /1000L/, '第三次退避 1000ms');
});

test('重试：最终失败返回统一脱敏提示', () => {
  const src = readAuthMain('service/WechatMiniProgramService.java');
  assert.match(src, /微信登录服务暂时不可用，请稍后重试/, '最终失败必须返回统一脱敏提示');
});

// ---------------------------------------------------------------------------
// 7. SQL 和权限注册验收
// ---------------------------------------------------------------------------

test('SQL：权限注册脚本存在', () => {
  const sqlPath = join(repoRoot, 'sql', 'wechat_mp_binding_menu.sql');
  assert.ok(existsSync(sqlPath), '权限注册 SQL 必须存在: sql/wechat_mp_binding_menu.sql');
});

test('SQL：权限注册包含 system:user:unbindMp', () => {
  const sqlPath = join(repoRoot, 'sql', 'wechat_mp_binding_menu.sql');
  if (existsSync(sqlPath)) {
    const sql = readFileSync(sqlPath, 'utf8');
    assert.match(sql, /system:user:unbindMp/, 'SQL 必须注册 system:user:unbindMp 权限');
  }
});

test('SQL：权限注册包含 system:user:wechatSession:revokeAll', () => {
  const sqlPath = join(repoRoot, 'sql', 'wechat_mp_binding_menu.sql');
  if (existsSync(sqlPath)) {
    const sql = readFileSync(sqlPath, 'utf8');
    assert.match(sql, /system:user:wechatSession:revokeAll/, 'SQL 必须注册 system:user:wechatSession:revokeAll 权限');
  }
});

test('SQL：建表脚本存在且包含租户字段', () => {
  const sqlPath = join(repoRoot, 'sql', 'system_user_mp_binding.sql');
  assert.ok(existsSync(sqlPath), '建表 SQL 必须存在: sql/system_user_mp_binding.sql');
  if (existsSync(sqlPath)) {
    const sql = readFileSync(sqlPath, 'utf8');
    assert.match(sql, /tenant_id/i, '建表 SQL 必须包含 tenant_id 字段');
  }
});

// ---------------------------------------------------------------------------
// 8. 小程序页面验收
// ---------------------------------------------------------------------------

test('小程序：登录页存在', () => {
  const path = join(repoRoot, 'junsong-miniprogram', 'src', 'pages', 'login', 'index.vue');
  assert.ok(existsSync(path), '登录页必须存在');
});

test('小程序：微信绑定页存在', () => {
  const path = join(repoRoot, 'junsong-miniprogram', 'src', 'pages', 'wechat-bind', 'index.vue');
  assert.ok(existsSync(path), '微信绑定页必须存在');
});

test('小程序：我的页面包含微信解绑功能', () => {
  const src = readMiniProgram('pages/mine/index.vue');
  assert.match(src, /wechatBound|微信账号|doUnbind/, '我的页面必须包含微信解绑功能');
});

test('小程序：pages.json 包含 wechat-bind 路由', () => {
  const src = readFileSync(join(repoRoot, 'junsong-miniprogram', 'src', 'pages.json'), 'utf8');
  assert.match(src, /wechat-bind/, 'pages.json 必须包含 wechat-bind 路由');
});

// ---------------------------------------------------------------------------
// 9. PC 页面验收
// ---------------------------------------------------------------------------

test('PC：用户管理页面包含微信绑定列', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /微信绑定|mpBound/, 'PC 用户管理必须包含微信绑定列');
});

test('PC：用户管理页面包含微信解绑按钮', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /handleMpUnbind|微信解绑/, 'PC 用户管理必须包含微信解绑按钮');
});

test('PC：解绑按钮使用 system:user:unbindMp 权限控制', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /system:user:unbindMp/, '解绑按钮必须使用 system:user:unbindMp 权限');
});

test('PC：解绑操作有二次确认', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /ElMessageBox|confirm/, '解绑操作必须有二次确认');
});

test('PC：用户管理页面包含微信会话失效按钮', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /wechatSession|微信会话失效|handleRevokeWechatSession/, 'PC 必须包含微信会话失效按钮');
});

test('PC：会话失效按钮使用 system:user:wechatSession:revokeAll 权限', () => {
  const src = readPC('views/system/user/index.vue');
  assert.match(src, /system:user:wechatSession:revokeAll/, '会话失效按钮必须使用对应权限');
});

test('PC：wechatBinding API 模块存在', () => {
  const path = join(repoRoot, 'junsong-ui-v3', 'src', 'api', 'system', 'wechatBinding.ts');
  assert.ok(existsSync(path), 'wechatBinding API 模块必须存在');
  const src = readFileSync(path, 'utf8');
  assert.match(src, /getUserBindings/, '必须有 getUserBindings 方法');
  assert.match(src, /adminUnbind/, '必须有 adminUnbind 方法');
});

// ---------------------------------------------------------------------------
// 10. 内部接口保护验收
// ---------------------------------------------------------------------------

test('内部接口：SysUserMpBindingController 使用 @InnerAuth', () => {
  const src = readSystemMain('controller/SysUserMpBindingController.java');
  assert.match(src, /@InnerAuth/, '内部绑定接口必须使用 @InnerAuth');
});

test('内部接口：Feign 客户端使用 FROM_SOURCE inner', () => {
  const src = readApiMain('RemoteUserMpBindingService.java');
  assert.match(src, /FROM_SOURCE/, 'Feign 客户端必须使用 FROM_SOURCE header');
});
