# 小程序微信登录绑定现有账号开发计划

> 提交给 TRAE 执行。目标是让微信小程序用户可以将微信身份绑定到现有系统账号，绑定后使用微信快捷登录，同时不破坏现有用户名密码登录、租户隔离和门店权限。

## 1. 目标与范围

### 目标

- 小程序支持微信快捷登录。
- 首次使用必须绑定一个已有系统账号，不自动创建无归属账号。
- 一个微信身份只能绑定一个系统用户。
- 一个系统用户可按产品规则绑定一个或多个小程序身份，但默认一个 AppID 下只能绑定一个微信身份。
- 绑定、解绑、换绑、登录失败和异常情况均可审计。
- 绑定后获得的权限、租户、门店范围完全来自现有系统账号。

### 不在本期范围

- 微信手机号一键授权注册新账号。
- 自动创建会员账号或员工账号。
- PC 端微信扫码登录。
- 使用微信 UnionID 跨多个小程序统一账号，除非后续明确提出多 AppID 需求。

## 2. 当前系统背景

- 小程序当前使用 `/auth/mp/login` 用户名密码登录。
- 认证服务已有 `SysLoginService`、TokenService 和 Redis 登录会话机制。
- 登录用户携带 `tenantId`、用户身份、角色、权限和当前门店上下文。
- 新方案必须复用现有 Token 生成和权限加载逻辑，不另起一套用户会话体系。

## 3. 推荐业务流程

### 首次绑定

1. 小程序调用 `wx.login()` 获取临时 `code`。
2. 小程序将 `code` 发送到后端，不得把 AppSecret 放在小程序端。
3. 后端调用微信官方接口换取 `openid`，按需获取 `unionid`。
4. 若微信身份已绑定，直接进入现有账号登录流程。
5. 若未绑定，要求用户输入现有系统账号和密码，或采用已批准的二次验证方式。
6. 后端校验账号状态、租户归属和绑定唯一性。
7. 在事务中写入微信绑定关系并生成现有系统 Token。
8. 小程序刷新用户信息、可用模块、门店和权限。

### 已绑定登录

1. 小程序调用 `wx.login()`。
2. 后端换取并校验 `openid`。
3. 查询绑定关系和系统账号状态。
4. 复用现有登录用户组装和 Token 生成逻辑。
5. 返回与 `/auth/mp/login` 兼容的登录响应结构。

### 解绑/换绑

- 默认要求当前登录 Token、密码或二次验证。
- 已绑定微信身份不得被另一个账号静默覆盖。
- 解绑后立即失效微信快捷登录，但不影响用户名密码登录。
- 所有绑定变更记录操作者、时间、用户、AppID、结果和失败原因。

## 4. 数据库设计

新增表建议：`sys_user_mp_binding`。

核心字段：

- `binding_id`
- `tenant_id`
- `user_id`
- `app_id`
- `openid`
- `unionid`，可空
- `status`：`ACTIVE` / `REVOKED`
- `bound_time`
- `last_login_time`
- `bound_by`
- `revoked_time`
- `revoked_by`
- `remark`
- `create_time`、`update_time`

约束要求：

- `(app_id, openid)` 唯一。
- 同一租户内的绑定关系必须可审计。
- 查询必须带 `tenant_id`，不能只按 `openid` 查找。
- 解绑不直接删除历史记录，使用撤销状态保留审计链。
- SQL 以 `SET NAMES utf8mb4;` 开始，脚本可重复执行并输出校验结果。

## 5. 后端任务

### Task 1：微信身份配置与安全边界

- 增加服务端 AppID/AppSecret 配置，Secret 只允许在 Nacos/环境变量或服务端安全配置中保存。
- 禁止小程序包、前端源码、日志输出 AppSecret。
- 增加微信接口超时、错误码、重试和限流策略。
- 明确 DEV/PROD 使用不同 AppID 或明确的环境隔离策略。

### Task 2：绑定关系表与 Mapper

- 创建迁移 SQL、实体、Mapper、Service 和审计记录。
- 添加唯一约束、租户过滤、状态过滤和并发绑定保护。
- 对重复绑定使用条件写入或行锁，不能依赖前端判断。

### Task 3：微信登录接口

建议接口：

- `POST /auth/mp/wechat/login`
- `POST /auth/mp/wechat/bind`
- `POST /auth/mp/wechat/unbind`
- `GET /auth/mp/wechat/binding`

要求：

- 登录接口只接收临时 `code` 和必要的环境标识。
- 绑定接口必须同时验证当前系统账号凭据或已批准的二次验证。
- 绑定前后校验用户状态、租户、门店和角色，不允许越权绑定。
- 返回结构兼容现有小程序 Token 登录逻辑。
- 错误信息不得泄露 openid、AppSecret、微信接口原始错误或用户是否存在的敏感细节。

### Task 4：认证与权限回归

- 复用 `SysLoginService` 和 TokenService。
- 验证微信登录后 `tenantId`、`deptId`、角色、权限和模块能力与密码登录一致。
- 绑定关系不存在、被撤销、账号停用、租户不匹配、门店无权限时必须 fail-closed。
- 增加登录日志、绑定日志和异常告警字段。

## 6. 小程序任务

- 登录页增加“微信快捷登录”。
- 未绑定时进入“绑定现有账号”页面，不自动注册。
- 已绑定时直接登录并进入现有工作台。
- 绑定成功后刷新用户、租户、门店、模块权限和首页数据。
- 处理微信取消授权、code 过期、网络失败、账号停用、绑定冲突和服务异常。
- 保留用户名密码登录入口作为兜底。
- 登录按钮必须防重复提交，保留请求幂等行为。
- 解绑后清理本地 Token 和用户缓存，避免旧会话继续展示受保护数据。

## 7. PC 端任务

建议增加“微信绑定管理”能力：

- 查看当前账号绑定状态。
- 绑定/解绑操作。
- 显示绑定时间和最近登录时间，不显示 openid 等敏感标识。
- 管理员只能在授权范围内处理账号绑定。
- 独立权限码，例如 `system:user:wechatBinding`，不得复用普通用户编辑权限。

如果本期不做 PC 管理，也必须提供后端管理员应急解绑接口，并记录审计日志。

## 8. 测试要求

### 后端单元与集成测试

至少覆盖：

- 未绑定微信首次绑定成功。
- 已绑定微信快捷登录成功。
- 同一 `(appId, openid)` 不能绑定第二个账号。
- 同一账号绑定冲突时事务回滚。
- 不同租户同名账号不能串绑。
- 账号停用、删除、租户不匹配时登录失败。
- 解绑后微信登录失败，密码登录仍成功。
- 并发绑定只有一个请求成功。
- 微信接口超时、错误码和重复提交处理。
- Token 中的租户、门店、角色和权限与密码登录一致。

### Node/契约测试

- SQL 表、唯一键、租户条件和非删除撤销策略契约测试。
- API 响应形状契约测试。
- 小程序登录、绑定、解绑页面行为测试。

### 前端测试

- PC 构建成功。
- 小程序 `node --test test/*.test.mjs` 全部通过。
- 小程序 PROD 构建成功。
- 至少一次真机或开发者工具体验：首次绑定、已绑定登录、解绑、失败重试。

## 9. 验收文档要求

TRAE 完成每个 Task 后必须创建：

`docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-<N>-completion.md`

最终必须创建：

`docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-acceptance.md`

验收文档必须包含以下章节：

### 9.1 版本与范围

- 代码提交号。
- 涉及后端、PC、小程序、SQL 文件清单。
- DEV/PROD 使用的 AppID 标识，禁止写 AppSecret。
- 本期完成项和明确未完成项。

### 9.2 数据库验收

- 建表/迁移脚本路径。
- 执行环境和执行时间。
- 重复执行结果。
- 唯一键、索引、租户字段检查结果。
- 绑定关系数量、ACTIVE 数量、REVOKED 数量。
- 不允许出现明文 Secret、明文 code、敏感 openid 日志。

### 9.3 接口验收

每个接口列出：

- 请求路径和方法。
- 权限/登录要求。
- 脱敏后的请求示例。
- 脱敏后的成功响应示例。
- 失败响应示例。
- 租户和账号归属校验结果。
- 幂等、并发和重复绑定验证结果。

### 9.4 业务场景验收矩阵

至少提供以下表格：

| 场景 | 预期结果 | 实际结果 | 证据 |
|---|---|---|---|
| 首次微信绑定现有账号 | 成功并生成绑定关系 |  | 截图/测试编号 |
| 已绑定微信快捷登录 | 登录并获得原账号权限 |  |  |
| 微信绑定其他账号 | 阻断 |  |  |
| 账号停用后微信登录 | 阻断 |  |  |
| 解绑后微信登录 | 阻断 |  |  |
| 不同租户尝试绑定 | 阻断且无数据泄露 |  |  |
| 并发绑定 | 仅一个成功 |  |  |
| 密码登录回归 | 不受影响 |  |  |

### 9.5 测试证据

- 每条命令、执行环境、通过数、失败数和既有失败项。
- 后端测试报告。
- Node 测试报告。
- PC 构建结果。
- 小程序构建结果。
- 真机/开发者工具测试截图或录屏路径。
- 失败项必须说明是否为本次引入，不能只写“无影响”。

### 9.6 安全验收

- AppSecret 仅服务端保存。
- code、openid、unionid 未进入普通业务日志。
- 绑定和解绑需要身份验证。
- 唯一键和事务并发保护已验证。
- 租户、门店、角色、权限未因微信登录绕过。
- 账号停用和撤销关系即时生效。
- 错误信息不泄露账号存在性和微信内部错误。

### 9.7 发布与回滚

- DEV 验证记录。
- PROD 发布前备份记录。
- 配置变更记录和回滚值。
- 后端 JAR、PC 构建包、小程序包版本信息。
- PROD 健康检查和真实接口验证。
- 数据库回滚策略：撤销绑定功能开关优先，禁止直接删除绑定历史。
- 小程序审核/上传状态和发布版本号。

## 10. 完成标准

- 后端、PC、小程序代码和 SQL 均通过对应测试。
- 首次绑定、快捷登录、解绑、异常和并发场景全部有证据。
- 现有密码登录和权限模型回归通过。
- 验收文档完整，敏感信息已脱敏。
- PROD 发布前完成独立复核，不能以“构建成功”代替业务验收。

## 11. 成本与外部依赖说明

- 本功能通常不需要购买微信“调用额度”。`wx.login` 和服务端换取微信身份属于小程序基础登录能力。
- 需要确认小程序主体认证、AppID/AppSecret、合法域名和隐私/用户协议配置是否已具备。
- 若增加短信二次验证、云开发、第三方身份服务或消息通知，相关服务可能产生费用。
- TRAE 不得自行购买服务或开通付费能力；发现需要付费时，先在执行报告中列出供应商、用途、预计费用和替代方案，等待明确批准。

## 12. TRAE 逐任务执行清单

### 全局执行规则

每个 Task 开始前：

- 读取根目录 `AGENTS.md` 和本计划。
- 执行 `git status --short`，保留用户已有修改。
- 修改已有方法、类或组件前，执行 GitNexus upstream impact；若不可用，记录工具不可用和人工影响分析。
- 先新增失败测试，确认测试确实失败，再写实现。
- 不修改 `junsong-miniprogram/dist` 生成物。

每个 Task 完成后：

- 执行本任务指定测试和构建。
- 创建对应 completion 报告。
- 运行 `git diff --check`。
- 记录未解决问题，不得用“与本次无关”替代证据。

### Task 1：确认微信小程序配置和环境前置条件

**目标：** 在写代码前确认微信 AppID、合法域名、隐私配置和 DEV/PROD 配置方式。

**Files:**

- Inspect: `junsong-miniprogram/src/pages/login/index.vue`
- Inspect: `junsong-auth/src/main/java/com/junsong/auth/controller/TokenController.java`
- Inspect: `junsong-auth/src/main/java/com/junsong/auth/service/SysLoginService.java`
- Inspect: `junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-1-completion.md`

**Steps:**

- [ ] 记录当前小程序 AppID 配置位置，禁止记录 AppSecret。
- [ ] 确认 DEV 和 PROD 是否使用不同 AppID。
- [ ] 确认 `/auth/mp/*` 网关白名单和合法域名。
- [ ] 确认隐私政策和用户协议入口。
- [ ] 输出阻塞项清单；缺少 AppID/AppSecret 时只阻断联调，不得伪造配置。

**Validation:**

```bash
rg -n "mp/login|APPID|AppSecret|wechat|wx.login" junsong-auth junsong-miniprogram junsong-gateway
git status --short
```

**Done:** 报告中明确“可开发/等待配置”，并列出不含 Secret 的环境变量名。

### Task 2：数据库绑定关系表

**目标：** 增加可撤销、可审计、租户隔离的微信绑定关系。

**Files:**

- Create: `sql/system_user_mp_binding.sql`
- Create: `scripts/system-user-mp-binding-contract.test.mjs`
- Create: `junsong-modules/junsong-system/src/main/java/com/junsong/system/domain/SysUserMpBinding.java`
- Create: `junsong-modules/junsong-system/src/main/java/com/junsong/system/mapper/SysUserMpBindingMapper.java`
- Create: `junsong-modules/junsong-system/src/main/resources/mapper/system/SysUserMpBindingMapper.xml`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-2-completion.md`

**Steps:**

- [ ] 先写 SQL 契约失败测试：utf8mb4、tenant_id、唯一键、ACTIVE/REVOKED、不可物理删除。
- [ ] 执行测试并确认失败。
- [ ] 实现幂等 SQL 和校验输出。
- [ ] 为 `(app_id, openid)` 建唯一约束。
- [ ] 所有查询显式带 `tenant_id`。
- [ ] 对绑定写入使用唯一键和事务保护并发请求。

**Validation:**

```bash
node --test scripts/system-user-mp-binding-contract.test.mjs
```

### Task 3：服务端微信 code 换身份

**目标：** 只在后端调用微信接口，安全取得 openid/unionid。

**Files:**

- Create/Modify: `junsong-auth/src/main/java/com/junsong/auth/service/WechatMiniProgramService.java`
- Create/Modify: `junsong-auth/src/main/java/com/junsong/auth/config/WechatMiniProgramProperties.java`
- Create: `junsong-auth/src/test/java/com/junsong/auth/service/WechatMiniProgramServiceTest.java`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-3-completion.md`

**Steps:**

- [ ] 先写 code 过期、微信错误、超时、成功响应解析失败测试。
- [ ] 实现 HTTP 客户端超时和错误码映射。
- [ ] AppSecret 仅从服务端配置读取。
- [ ] 日志只记录 requestId、错误分类和耗时，不记录 code/openid/AppSecret。
- [ ] 禁止客户端直接调用微信换身份接口。

**Validation:**

```bash
cd junsong-auth
mvn -Dtest=WechatMiniProgramServiceTest test
```

### Task 3A：增加租户级“是否启用微信登录”参数

**目标：** 由租户控制小程序是否展示微信登录入口；参数缺失、读取失败或值非法时一律按关闭处理。

**建议参数：** `mp.wechat.login.enabled`

**Files:**

- Inspect/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/domain/SysConfig.java`
- Inspect/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/service/ISysConfigService.java`
- Inspect/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysConfigServiceImpl.java`
- Inspect/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysConfigController.java`
- Inspect/Modify: `junsong-modules/junsong-system/src/main/resources/mapper/system/SysConfigMapper.xml`
- Create/Modify: `junsong-modules/junsong-system/src/test/java/com/junsong/system/service/SysWechatLoginConfigTest.java`
- Create/Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMpController.java`
- Modify: `junsong-miniprogram/src/pages/login/index.vue`
- Create: `scripts/wechat-login-tenant-switch-contract.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-3a-completion.md`

**Steps:**

- [ ] 先写失败测试：租户参数为 `true` 时返回启用；`false`、缺失、非法值、读取异常时返回关闭。
- [ ] 确定参数按 `tenant_id` 隔离，禁止使用全局 Redis key 或固定租户配置。
- [ ] 增加租户参数默认值 `false`，已有租户不得被默认开启。
- [ ] 增加小程序能力接口，例如 `GET /member/mp/capabilities` 或并入现有小程序启动信息接口。
- [ ] 返回最小字段：`wechatLoginEnabled`，不返回配置内部键、Secret 或系统配置详情。
- [ ] 后端微信登录接口也必须再次校验该参数，不能只依赖前端隐藏按钮。
- [ ] 小程序登录页仅当 `wechatLoginEnabled === true` 时渲染微信登录按钮。
- [ ] 能力接口失败、超时或返回异常时隐藏按钮，并保留用户名密码登录。
- [ ] 参数修改后，下一次能力查询即可生效；已存在 Token 不因开关关闭而越权获得微信登录能力。
- [ ] 在 PC 租户/系统参数页面增加独立开关和说明，只有授权管理员可修改。

**Validation:**

```bash
node --test scripts/wechat-login-tenant-switch-contract.test.mjs
cd junsong-modules/junsong-system
mvn -Dtest=SysWechatLoginConfigTest test
cd ../../..
cd junsong-miniprogram
node --test test/*.test.mjs
cd ..
```

**Acceptance cases:**

| 租户参数 | 能力接口 | 小程序按钮 | 直接调用微信登录接口 |
|---|---|---|---|
| `true` | `wechatLoginEnabled=true` | 显示 | 按正常流程处理 |
| `false` | `wechatLoginEnabled=false` | 不显示 | 后端拒绝 |
| 缺失 | `false` | 不显示 | 后端拒绝 |
| 非法值 | `false` | 不显示 | 后端拒绝 |
| 能力接口异常 | 前端 fail-closed | 不显示 | 由后端开关决定 |

**Done:** Task 报告必须附上至少两个租户的开关隔离证据、按钮显示/隐藏截图或录屏、直接调用接口被后端拒绝的接口测试结果。

### Task 4：绑定、解绑和绑定状态 API

**目标：** 完成安全绑定流程并复用现有账号登录和 Token 机制。

**Files:**

- Modify: `junsong-auth/src/main/java/com/junsong/auth/controller/TokenController.java`
- Modify: `junsong-auth/src/main/java/com/junsong/auth/service/SysLoginService.java`
- Create/Modify: `junsong-auth/src/main/java/com/junsong/auth/controller/WechatMpBindingController.java`
- Create: `junsong-auth/src/test/java/com/junsong/auth/controller/WechatMpBindingControllerTest.java`
- Create: `scripts/wechat-mp-binding-api-contract.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-4-completion.md`

**Steps:**

- [ ] 先写未绑定、重复绑定、跨租户、停用账号、解绑后登录失败测试。
- [ ] 实现 `POST /auth/mp/wechat/login`。
- [ ] 实现 `POST /auth/mp/wechat/bind`。
- [ ] 实现 `POST /auth/mp/wechat/unbind`。
- [ ] 实现 `GET /auth/mp/wechat/binding`。
- [ ] 绑定接口必须验证已有账号凭据或批准的二次验证。
- [ ] 登录成功后调用现有 `createTokenMp` 或等价公共逻辑。
- [ ] 错误响应统一脱敏。

**Validation:**

```bash
cd junsong-auth
mvn -Dtest=WechatMpBindingControllerTest test
cd ../..
node --test scripts/wechat-mp-binding-api-contract.test.mjs
```

### Task 5：小程序登录和绑定页面

**目标：** 用户可以在小程序完成首次绑定、快捷登录和解绑。

**Files:**

- Modify: `junsong-miniprogram/src/pages/login/index.vue`
- Create/Modify: `junsong-miniprogram/src/pages/wechat-bind/index.vue`
- Modify: `junsong-miniprogram/src/pages/mine/index.vue`
- Modify: `junsong-miniprogram/pages.json`
- Create: `junsong-miniprogram/test/wechat-binding.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-5-completion.md`

**Steps:**

- [ ] 先写登录按钮、code 失败、未绑定跳转、绑定成功、重复点击测试。
- [ ] 增加“微信快捷登录”按钮。
- [ ] 未绑定时进入绑定页，保留密码登录入口。
- [ ] 绑定成功后刷新 Token、用户、租户、门店和模块权限。
- [ ] 解绑成功后清理本地登录态和缓存。
- [ ] 防止重复提交，微信取消授权时显示可操作提示。

**Validation:**

```bash
cd junsong-miniprogram
node --test test/*.test.mjs
cd ..
env PATH="/opt/homebrew/bin:/usr/local/bin:$PATH" ./bin/build-miniprogram.sh prod
```

### Task 6：PC 绑定管理

**目标：** 为用户或管理员提供受控的绑定状态和解绑能力。

**Files:**

- Modify: `junsong-ui-v3/src/views/system/user/index.vue`
- Create/Modify: `junsong-ui-v3/src/api/system/wechatBinding.ts`
- Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java`
- Create: `scripts/wechat-binding-pc-contract.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-6-completion.md`

**Steps:**

- [ ] 先写权限隐藏、解绑成功、越权解绑失败测试。
- [ ] 增加独立权限码，不复用普通用户编辑权限。
- [ ] 展示绑定状态、绑定时间和最近登录时间。
- [ ] 不展示 openid、unionid 等敏感标识。
- [ ] 解绑必须二次确认并写审计日志。

**Validation:**

```bash
node --test scripts/wechat-binding-pc-contract.test.mjs
cd junsong-ui-v3 && npm run build
```

### Task 6A：租户级一键使微信登录会话失效

**目标：** 不解除微信绑定、不删除绑定历史，只让指定租户下所有当前微信登录会话立即失效；用户名密码登录会话保持有效。

**Files:**

- Modify: `junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/model/LoginUser.java`
- Modify: `junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java`
- Modify: `junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/interceptor/HeaderInterceptor.java`
- Modify: `junsong-auth/src/main/java/com/junsong/auth/service/SysLoginService.java`
- Create/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java`
- Create/Modify: `junsong-modules/junsong-system/src/main/java/com/junsong/system/service/SysWechatSessionService.java`
- Create: `junsong-modules/junsong-system/src/test/java/com/junsong/system/service/SysWechatSessionServiceTest.java`
- Modify: `junsong-ui-v3/src/views/system/user/index.vue`
- Create/Modify: `junsong-ui-v3/src/api/system/wechatBinding.ts`
- Create: `scripts/wechat-session-revoke-contract.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-6a-completion.md`

**Implementation decision:**

- 微信登录 Token 必须标记 `authSource=WECHAT_MP`，密码登录标记为 `PASSWORD`。
- 登录用户会话携带租户级 `wechatSessionEpoch` 或等价版本号。
- “一键失效”只递增指定租户的版本号，不扫描或删除全库 Redis Token。
- 每次请求校验微信会话版本；版本不一致时返回统一登录失效提示并清理当前 Token。
- 密码会话不参与该校验，不能被误注销。
- 已绑定关系、`openid`、绑定历史和账号本身均保留。

**Steps:**

- [ ] 先写失败测试：微信会话失效、密码会话不受影响、不同租户不受影响、重复点击幂等。
- [ ] 在微信登录时记录会话来源和租户版本。
- [ ] 增加租户级失效接口，例如 `POST /system/wechat-session/revoke-all`。
- [ ] 接口要求独立权限，例如 `system:user:wechatSession:revokeAll`。
- [ ] 操作必须指定当前租户、填写原因、二次确认，并记录失效前后版本和受影响租户。
- [ ] PC 页面显示“当前微信会话失效”操作，不使用“解除绑定”文字，避免误解。
- [ ] 成功后不改变绑定状态；用户下次仍可通过微信重新登录。
- [ ] 接口返回受影响租户和版本号，不返回用户 openid 或 Token 明细。

**Acceptance cases:**

| 场景 | 预期结果 |
|---|---|
| 租户 A 一键失效微信会话 | 租户 A 已登录微信用户下次请求均需重新登录 |
| 租户 A 一键失效后密码登录会话 | 继续有效 |
| 租户 B 的微信会话 | 不受影响 |
| 微信绑定关系 | 仍为 ACTIVE，不被删除 |
| 用户再次微信登录 | 可以重新建立新会话 |
| 重复点击 | 版本继续安全递增，接口幂等且有审计 |
| 无权限用户 | 后端拒绝，不能依赖前端隐藏 |

**Validation:**

```bash
node --test scripts/wechat-session-revoke-contract.test.mjs
cd junsong-modules/junsong-system
mvn -Dtest=SysWechatSessionServiceTest test
cd ../../..
cd junsong-ui-v3 && npm run build
```

### Task 7：安全、并发和回归验收

**目标：** 在发布前证明微信登录没有绕过原有权限体系。

**Files:**

- Create: `scripts/wechat-mp-binding-acceptance.test.mjs`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-task-7-completion.md`
- Create: `docs/superpowers/reports/2026-07-14-miniprogram-wechat-account-binding-acceptance.md`

**Steps:**

- [ ] 执行后端绑定和登录全量相关测试。
- [ ] 执行 Node 契约和小程序测试。
- [ ] 执行 PC 构建和小程序 PROD 构建。
- [ ] 在 DEV 做真实微信登录、首次绑定、已绑定登录、解绑测试。
- [ ] 验证两个租户、两个门店、不同角色权限均不串数据。
- [ ] 验证并发绑定只有一个成功。
- [ ] 填写第 9 节验收文档全部章节。
- [ ] 执行 `git diff --check` 和 `git status --short`。

**完成门槛：** 任一安全场景失败、验收证据缺失、敏感信息泄露或 PROD 配置未确认，均不得发布。
