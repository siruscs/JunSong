# 开放平台 API 开发者快速入门

> **更新日期：** 2026-07-05
> **适用版本：** JunSong-Cloud 开放平台 v1/v2

本文档帮助第三方开发者在 5 分钟内完成开放 API 的接入和首次调用。

---

## 一、接入流程概览

```
注册应用 → 获取测试Key → 实现签名 → 调用API → (可选)申请生产Key
```

平台会在网关验签通过后生成可信调用上下文，并由开放服务透传给下游业务服务。外部调用方只需要按本文携带 `X-App-*` 请求头，不能自行伪造内部身份头。

---

## 二、第一步：注册应用

1. 登录管理后台（http://localhost，账号 admin / admin123）
2. 进入「开放平台 > 应用管理」
3. 点击「新增」，填写应用信息：
   - 应用名称、应用类型、联系人信息
   - 官网地址、回调地址（用于 Webhook）
4. 提交后系统**自动发放测试 Key**（配额 100 次/天）

---

## 三、第二步：获取 API Key

在应用列表中点击「API Key」按钮，可查看：

| 字段 | 说明 |
|------|------|
| AppKey | 公开标识，用于请求头 `X-App-Key` |
| AppSecret | 私密密钥，**仅用于服务端签名，不可泄露** |
| Key 类型 | 测试 / 生产 |
| 日配额 | 测试 100 次/天，生产 10000 次/天 |

> **安全提示**：AppSecret 等同于密码，切勿在前端代码或客户端 APP 中硬编码。

---

## 四、第三步：实现 HMAC-SHA256 签名

### 4.1 签名算法

```
签名串 = HTTP方法 + 完整请求路径 + 时间戳 + nonce + 请求体
签名值 = HMAC-SHA256(AppSecret, 签名串)
```

- **HTTP方法**：GET / POST / PUT / DELETE
- **完整请求路径**：含版本前缀，如 `/openapi/v2/app/list`
- **时间戳**：毫秒级（5 分钟有效）
- **nonce**：随机串（10 分钟内不可重复，防重放）
- **请求体**：POST/PUT 的 JSON body（GET 请求为空字符串）

### 4.2 请求头

| Header | 说明 |
|--------|------|
| `X-App-Key` | AppKey |
| `X-App-Timestamp` | 毫秒时间戳 |
| `X-App-Nonce` | 随机串（建议 UUID） |
| `X-App-Signature` | HMAC-SHA256 签名（十六进制小写） |

### 4.3 平台生成的可信上下文

以下请求头由网关和开放服务内部生成，外部调用方不需要也不应该主动传入：

| Header | 说明 |
|--------|------|
| `X-Open-App-Id` | 当前开放应用 ID |
| `X-Open-App-Key` | 当前 AppKey |
| `X-Open-Tenant-Id` | 当前授权租户 |
| `X-Open-Request-Id` | 当前请求 ID，用于日志追踪 |
| `X-Open-Key-Type` | 测试 / 生产 Key 类型 |
| `X-Open-Auth-Version` | 开放平台鉴权版本 |

平台会剥离外部请求中可能伪造的内部身份头，并在验签后重新注入可信上下文。

### 4.4 Python 示例

```python
import hmac, hashlib, time, uuid, urllib.request, json

APP_KEY = "your_app_key"
APP_SECRET = "your_app_secret"

def call_openapi(method, path, body=None):
    timestamp = str(int(time.time() * 1000))
    nonce = uuid.uuid4().hex
    body_str = json.dumps(body) if body else ""
    sign_str = method + path + timestamp + nonce + body_str
    signature = hmac.new(
        APP_SECRET.encode(), sign_str.encode(), hashlib.sha256
    ).hexdigest()

    url = "http://localhost:8081" + path
    req = urllib.request.Request(url, method=method)
    req.add_header("X-App-Key", APP_KEY)
    req.add_header("X-App-Timestamp", timestamp)
    req.add_header("X-App-Nonce", nonce)
    req.add_header("X-App-Signature", signature)
    if body:
        req.add_header("Content-Type", "application/json")
        req.data = json.dumps(body).encode()

    resp = urllib.request.urlopen(req, timeout=15)
    return json.loads(resp.read().decode())

# 示例：查询会员仪表盘
result = call_openapi("GET", "/openapi/v2/members/dashboard/stats")
print(result)
```

### 4.5 JavaScript / Node.js 示例

```javascript
const crypto = require('crypto');
const http = require('http');

const APP_KEY = 'your_app_key';
const APP_SECRET = 'your_app_secret';

function callOpenApi(method, path, body = '') {
    const timestamp = Date.now().toString();
    const nonce = crypto.randomBytes(16).toString('hex');
    const signStr = method + path + timestamp + nonce + body;
    const signature = crypto.createHmac('sha256', APP_SECRET)
        .update(signStr).digest('hex');

    const options = {
        hostname: 'localhost',
        port: 8081,
        path: path,
        method: method,
        headers: {
            'X-App-Key': APP_KEY,
            'X-App-Timestamp': timestamp,
            'X-App-Nonce': nonce,
            'X-App-Signature': signature,
        },
    };
    // ... 发送请求
}
```

---

## 五、第四步：调用 API

### 5.1 API 版本选择

| 版本 | 路径 | 推荐场景 |
|------|------|---------|
| v2 | `/openapi/v2/**` | **推荐**，当前稳定版 |
| latest | `/openapi/latest/**` | 别名，自动指向最新版 |
| v1 | `/openapi/v1/**` | 已废弃，响应头含 Deprecation |

### 5.2 可用端点

**工作流即服务：**
- `GET /openapi/v2/workflow/definitions` — 流程定义列表
- `POST /openapi/v2/workflow/instances` — 发起流程
- `GET /openapi/v2/workflow/tasks/todo` — 待办任务
- `POST /openapi/v2/workflow/tasks/{taskId}/approve` — 审批通过

**会员能力即服务：**
- `GET /openapi/v2/members` — 会员列表
- `GET /openapi/v2/members/dashboard/stats` — 仪表盘统计
- `GET /openapi/v2/members/dashboard/trend` — 趋势
- `GET /openapi/v2/members/dashboard/ranking` — 排行榜

**门店选址即服务：**
- `GET /openapi/v2/store-opening` — 开店申请列表
- `POST /openapi/v2/store-opening` — 新增申请

### 5.3 响应格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

限流响应头：
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 98
X-API-Version: v2
```

调用日志：

- 认证失败、签名失败、限流、业务成功和业务异常都会写入开放平台调用日志。
- 排查问题时请记录请求时间、AppKey、接口路径和响应中的 requestId（如有）。

### 5.4 Webhook 订阅

Webhook 订阅已绑定应用和租户，订阅信息会持久化保存。创建订阅时至少需要提供回调地址和事件范围：

```http
POST /openapi/v1/open/webhooks/subscriptions
Content-Type: application/json

{
  "callbackUrl": "https://example.com/junsong/webhook",
  "events": "member.created,workflow.completed"
}
```

回调地址必须使用服务端可访问的 HTTPS 地址。生产环境订阅建议先在测试 Key 下完成签名、超时和重试验证。

---

## 六、第五步：使用 SDK（可选）

项目提供 4 种语言 SDK，免去手动签名：

```bash
# 生成 SDK（需要 Node.js）
cd sdk
sh generate-sdk.sh
```

| 语言 | 目录 | 使用方式 |
|------|------|---------|
| Java | `sdk/sdk-java/` | Maven 依赖引入 |
| Python | `sdk/sdk-python/` | `pip install ./sdk-python` |
| Go | `sdk/sdk-go/` | `import "junsongsdk"` |
| JavaScript | `sdk/sdk-js/` | `npm install ./sdk-js` |

---

## 七、申请生产 Key

当测试 Key 的配额无法满足需求时：

1. 在应用列表点击「审批」按钮
2. 管理员审批通过后，系统自动发放生产 Key（10000 次/天）
3. 生产 Key 可用于正式业务调用

---

## 八、常见问题

### Q1: 返回 "缺少API认证请求头"
检查是否携带了全部 4 个签名请求头。

### Q2: 返回 "请求已过期"
时间戳超过 5 分钟有效期，请使用当前时间。

### Q3: 我可以调用 `/open/internal/**` 吗？
不可以。`/open/internal/**` 是服务内部接口，不属于开放 API。生产部署中 open 服务端口不发布到宿主机，内部接口还需要 `@InnerAuth` 和 `X-Inner-Token` 双重校验。

### Q4: 为什么我传入的 `from-source` 或 `user_id` 没有效？
这些属于内部身份头。网关会剥离外部请求中可伪造的身份头，并在验签通过后注入可信 `X-Open-*` 上下文。

### Q5: 返回 "请求不可重放"
nonce 在 10 分钟内已被使用，每次请求请生成新的随机 nonce。

### Q6: 返回 "签名校验失败"
签名串拼接顺序为：`方法 + 完整路径 + 时间戳 + nonce + 请求体`，路径需含 `/openapi/v2` 前缀。

### Q7: 返回 HTTP 429 "请求超出每日配额限制"
当日调用次数超限，等待次日自动重置或申请生产 Key。

---

## 九、相关文档

- [接口文档](./API.md) — 完整 REST API 说明
- [演进规划](./OPEN_PLATFORM_ROADMAP.md) — 开放平台 12 项任务详情
- [快速开始](./QUICKSTART.md) — 平台本地部署
