# JunSong-Cloud 公共开放平台演进路线图

> 本文档记录 JunSong-Cloud 面向"公共开放平台"的演进规划与进度，作为后续跟进的权威清单。
> 最后更新：2026-07-05

---

## 总进度概览

```
阶段一（P0）开放平台地基    ████████████████████ 4/4 完成 100%
阶段二（P1）平台能力深化    ████████████████████ 4/4 完成 100%
阶段三（P2）生态与商业化    ████████████████████ 4/4 完成 100%
R23 外部集成可信闭环       ████████████████████ 完成
R25 企业级治理加固         ████████████████████ 完成
                                            ─────────────
                                            当前开放平台能力已收口
```

---

## 当前状态补充（2026-07）

R23 与 R25 完成后，开放平台从“能调用 API”升级为“可信外部集成入口”。当前能力重点如下：

| 能力 | 当前状态 | 说明 |
|:---|:---:|:---|
| API Key + HMAC 鉴权 | ✅ 完成 | AppKey/AppSecret、timestamp、nonce、body 签名校验，防篡改与防重放 |
| 可信上下文 | ✅ 完成 | 网关验签后注入 `X-Open-*`，包含应用、租户、请求 ID 和鉴权版本上下文 |
| 下游权限边界 | ✅ 完成 | 移除固定 admin 冒用，开放 API 通过可信来源和租户上下文读取授权数据 |
| 调用日志 | ✅ 完成 | 成功、拒绝、限流和异常请求均有真实写入链路 |
| Webhook 持久化 | ✅ 完成 | 公开订阅端点真实落库，绑定 appId、tenantId、callbackUrl 和事件 |
| 内部接口隔离 | ✅ 完成 | `/open/internal/**` 移出公网白名单，Controller 加 `@InnerAuth`，内部调用加 `X-Inner-Token` |
| 部署边界 | ✅ 完成 | open 服务使用 Docker `expose`，不发布 9208 到宿主机；PROD 强制注入 `OPEN_INTERNAL_SECRET` |
| 外部文档与 SDK | ✅ 完成 | OpenAPI 清单、多语言 SDK、快速入门和 drift check 保持同步 |

高风险能力边界：

- 财务写入、预测辅助、低代码模板发布等能力不默认开放。
- 涉及写入、资金、审批、字段级敏感数据的接口必须先走场景审批、租户白名单、scope 评审和审计策略确认。
- 内部管理接口不作为开放 API 暴露。

---

## 阶段一：开放平台地基（P0）

> 目标：让第三方开发者能注册应用、获取 API Key、调用受保护的开放 API。

| # | 任务 | 状态 | 说明 |
|:---|:---|:---:|:---|
| 1 | 开放平台后端微服务（junsong-modules-open） | ✅ 完成 | 端口 9208, open_app / open_app_secret / open_api_log 表 + 控制器 |
| 2 | 网关 ApiKeyAuthFilter | ✅ 完成 | 拦截 /openapi/v1/**，HMAC-SHA256签名+时间戳+nonce防重放 |
| 3 | 开放平台前端门户 | ✅ 完成 | Vue3+ElementPlus, 应用管理+API Key管理+审批界面 |
| 4 | API 版本路由 /openapi/v1 | ✅ 完成 | 多版本路由(v1/v2/latest) + VersionAliasFilter(版本头+废弃标记) |

---

## 阶段二：平台能力深化（P1）

> 目标：从"能调用 API"升级为"有治理能力的开放平台"。

| # | 任务 | 状态 | 完成日期 | 说明 |
|:---|:---|:---:|:---:|:---|
| 5 | 多租户体系 | ✅ 完成 | 2026-06-27 | 见下方详情 |
| 6 | Webhook 订阅与投递 | ✅ 完成 | 2026-06-27 | 见下方详情 |
| 7 | 应用级限流与计量计费 | ✅ 完成 | 2026-06-27 | Redis计数器 + 每日配额限流(test:100/prod:10000) |
| 8 | API 文档聚合 | ✅ 完成 | Knife4j 4.5.0 不兼容 Spring Cloud 2025，改用 springdoc 原生聚合 |

### #4 API 版本路由 — 已完成 ✅

**实现内容：**
- 网关配置3个版本路由组：`/openapi/v1/**`、`/openapi/v2/**`、`/openapi/latest/**`
- 新增 [VersionAliasFilter](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-gateway/src/main/java/com/junsong/gateway/filter/VersionAliasFilter.java)
- ApiKeyAuthFilter 拦截所有 `/openapi/` 版本路径（不只 v1）

**版本策略：**
| 路径 | 版本 | 说明 |
|:---|:---|:---|
| `/openapi/v1/**` | v1 | 旧版本，标记 Deprecation + Sunset(6个月后废弃) |
| `/openapi/v2/**` | v2 | 最新稳定版 |
| `/openapi/latest/**` | latest | 别名，透明转发到 v2 |

**响应头管理：**
```
X-API-Version: v1/v2           当前API版本
X-API-Latest-Version: v2      最新稳定版本号
Deprecation: true             仅v1有，标记为即将废弃
Sunset: Sun, 27 Dec 2026      仅v1有，废弃时间
```

**验证结果：**
```
测试1: /openapi/v1   -> X-API-Version=v1, Deprecation=true  ✅
测试2: /openapi/v2   -> X-API-Version=v2, 无废弃标记        ✅
测试3: /openapi/latest -> X-API-Version=v2 (转发到v2)      ✅
HMAC签名: v1/v2/latest 全部需要签名校验                      ✅
```

### #3 开放平台前端门户 — 已完成 ✅

**实现内容：**
- Vue 3 + TypeScript + Element Plus 前端门户
- API 调用层 [src/api/open/app.ts](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-ui-v3/src/api/open/app.ts)
- 应用管理页面 [src/views/open/app/index.vue](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-ui-v3/src/views/open/app/index.vue)
- 数据库菜单配置（sys_menu: 2000开放平台 + 2001应用管理 + 2002-2006按钮权限）

**页面功能：**
- 应用列表（搜索/分页/状态筛选）
- 新增/修改应用（弹窗表单）
- 审批应用（通过/驳回弹窗）
- API Key 管理（查看/显示密钥/启停切换）
- 权限控制（v-hasPermi 指令）

**验证结果：**
```
1. 登录获取Token              ✅
2. 动态路由菜单                ✅ "开放平台 > 应用管理" 出现
3. API调用 /open/app/list      ✅ code=200, total=1
4. 前端页面加载                ✅ 首页正常
```

### #2 网关 ApiKeyAuthFilter — 已完成 ✅

**实现内容：**
- 网关新增 `ApiKeyAuthFilter`（GlobalFilter，Order=-150）
- 拦截 `/openapi/v1/**` 请求，校验 HMAC-SHA256 签名
- open 服务提供内部接口 `/internal/secret/byKey/{appKey}` 供网关查询 AppSecret
- AppSecret 缓存到 Redis（5分钟），减少重复查询

**签名算法：**
```
签名串 = HTTP方法 + 请求路径 + 时间戳 + nonce + 请求体
签名值 = HMAC-SHA256(AppSecret, 签名串)
```

**请求头：**
```
X-App-Key       AppKey(公开标识)
X-App-Timestamp 时间戳(毫秒，5分钟有效期)
X-App-Nonce     随机串(防重放，Redis缓存10分钟)
X-App-Signature HMAC-SHA256签名(十六进制)
```

**安全机制：**
- ✅ 签名校验：防止请求被篡改
- ✅ 时间戳校验：5分钟有效期，防止重放
- ✅ Nonce 防重放：相同 nonce 10分钟内只能用一次
- ✅ AppKey 校验：从数据库查询，禁用的 Key 拒绝访问

**验证结果：**
```
测试1: 无认证头          -> 拒绝(缺少请求头)    ✅
测试2: 正确HMAC签名      -> 通过(查询成功)      ✅
测试3: 错误签名          -> 拒绝(防重放)        ✅
测试4: 过期时间戳        -> 拒绝(请求已过期)    ✅
测试5: 重放攻击          -> 拒绝(请求不可重放)  ✅
测试6: 不存在的AppKey    -> 拒绝(AppKey查询失败) ✅
```

### #1 开放平台后端微服务 — 已完成 ✅

**实现内容：**
- 新建 `junsong-modules-open` 微服务（端口 9208）
- 数据库：open_app（应用表）+ open_app_secret（API Key表）+ open_api_log（调用日志表）
- 后端：应用 CRUD + Key 自动发放（测试Key）+ 审批后发放（生产Key）
- 网关路由：`/open/**` → `lb://junsong-open`（StripPrefix=1）

**业务流程：**
```
开发者注册应用 → 自动发放测试Key(100次/天) → 管理员审批通过 → 发放生产Key(10000次/天)
```

**验证结果：**
- 注册应用 code=200，自动发放测试 Key（js_xxx, type=test, quota=100/天）
- 审批通过 code=200，自动发放生产 Key（js_xxx, type=production, quota=10000/天）
- 应用列表/Key查询/审批/驳回 全部正常

**踩坑记录：**
- Nacos 3.x 移除了 v1 配置 API，直接更新 MySQL 不会同步到 Nacos 内存
- 网关 Nacos 配置客户端 gRPC 连接不稳定（UNHEALTHY）
- 最终方案：将路由+Redis+安全白名单配置直接写入 bootstrap.yml，不依赖 Nacos 推送

### #5 多租户体系 — 已完成 ✅

**实现内容：**
- TenantSqlInterceptor：MyBatis 拦截器，自动注入 `WHERE tenant_id = ?`
- TenantContext：ThreadLocal 租户上下文
- TenantInterceptor：INSERT 自动填充 tenant_id
- sys_tenant 租户管理表
- 租户初始化：7 步事务（租户→部门→角色→菜单权限→用户→用户角色→配置复制）
- 参数表公共共享+租户覆盖（公共10条 tenant_id=0，定制5条/租户）
- Redis 缓存 Key 租户隔离
- 登录查询跨租户查找

**验证结果：**
- 58 张业务表 100% 覆盖 tenant_id
- 租户1（admin）看到全部数据，租户6（chen7_admin）只看到自己租户数据
- 公共配置（皮肤/初始密码）两租户相同，租户定制配置（验证码开关）独立

### #10 差异化能力输出 — 已完成 ✅

**实现内容：**
- 在 open 模块新增 3 个差异化能力 Controller，共 30 个开放API端点
- 通过 RestTemplate 聚合调用 workflow/member/system 三个微服务
- 内部调用拦截器注入身份 header，实现服务间鉴权透传

**三大差异化能力：**

| 能力 | Controller | 端点数 | 说明 |
|:---|:---|:---|:---|
| 工作流即服务 | [OpenWorkflowController](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/openapi/OpenWorkflowController.java) | 14 | 流程定义/实例/任务/历史/分析 |
| 会员能力即服务 | [OpenMemberController](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/openapi/OpenMemberController.java) | 11 | 会员/积分/秒杀/仪表盘 |
| 门店选址即服务 | [OpenStoreOpeningController](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-modules/junsong-open/src/main/java/com/junsong/open/controller/openapi/OpenStoreOpeningController.java) | 6 | 开店申请/审批/撤回 |

**开放API路径：**
```
/openapi/v1/workflow/definitions        流程定义列表
/openapi/v1/workflow/instances          发起流程实例
/openapi/v1/workflow/tasks/todo         待办任务
/openapi/v1/workflow/tasks/{id}/approve 审批通过
/openapi/v1/members                     会员列表
/openapi/v1/members/dashboard/stats     会员仪表盘
/openapi/v1/store-opening               门店开店申请
```

**验证结果：**
```
✅ 通过(无权限点接口):
  会员-积分规则  ✅ code=200
  会员-仪表盘    ✅ code=200
  会员-趋势     ✅ code=200
  会员-排行榜   ✅ code=200

⏳ 需权限适配(下游有@PreAuthorize注解):
  工作流-流程定义  (需权限点 workflow:definition:list)
  会员-列表       (需权限点 member:member:list)
  门店-申请列表   (需权限点 system:storeOpening:list)
  注: 链路已打通(签名→限流→路由→调用)，仅权限模型需后续适配
```

### #9 多语言SDK — 已完成 ✅

**实现内容：**
- 使用 OpenAPI Generator 从 open 服务的 `/v3/api-docs` 自动生成 4 种语言 SDK
- 创建 SDK 生成脚本 [generate-sdk.sh](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/sdk/generate-sdk.sh)
- 创建 3 种语言的 HMAC 签名调用示例

**SDK 文件统计：**
| 语言 | 目录 | 文件数 | 包名 |
|:---|:---|:---|:---|
| Java | sdk/sdk-java/ | 30 | com.junsong.open |
| Python | sdk/sdk-python/ | 22 | junsong_open_sdk |
| Go | sdk/sdk-go/ | 12 | junsongsdk |
| JavaScript | sdk/sdk-js/ | 14 | junsongOpenSdk |

**调用示例：**
| 语言 | 文件 | 验证结果 |
|:---|:---|:---|
| Python | [example.py](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/sdk/examples/python/example.py) | ✅ code=200, Limit=100 |
| JavaScript | [example.js](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/sdk/examples/javascript/example.js) | ✅ code=200, Limit=100 |
| Java | [JunSongOpenApiExample.java](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/sdk/examples/java/JunSongOpenApiExample.java) | ✅ 代码已就绪 |

**签名算法：**
```
签名串 = HTTP方法 + 完整路径(含/openapi/v1) + 时间戳 + nonce + 请求体
签名值 = HMAC-SHA256(AppSecret, 签名串)
```

**验证结果：**
```
Python示例:   code=200, X-API-Version=v1, Remaining=98  ✅
JavaScript:   code=200, X-API-Version=v1, Remaining=97  ✅
```

### #12 CI/CD + K8s — 已完成 ✅

**实现内容：**
- 新增 3 个 GitHub Actions 流水线（CI + CD-Docker + CD-K8s）
- 新增 K8s 部署清单（7个文件，36个K8s资源）
- 包含完整的生产级配置：探针、资源限制、HPA自动扩缩、Ingress

**GitHub Actions 流水线：**
| 文件 | 触发 | 功能 |
|:---|:---|:---|
| [ci.yml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/.github/workflows/ci.yml) | push main/dev | 编译后端+前端，上传制品 |
| [cd-docker.yml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/.github/workflows/cd-docker.yml) | tag v* | 构建并推送Docker镜像到仓库 |
| [cd-k8s.yml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/.github/workflows/cd-k8s.yml) | tag v* | 部署到K8s集群(rolling update) |

**K8s 部署清单：**
| 文件 | 资源数 | 说明 |
|:---|:---|:---|
| [namespace.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/namespace.yaml) | 1 | junsong命名空间 |
| [configmap.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/configmap.yaml) | 2 | ConfigMap + Secret |
| [infrastructure.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/infrastructure.yaml) | 6 | MySQL+Redis+Nacos |
| [gateway-auth.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/gateway-auth.yaml) | 4 | 网关+认证服务 |
| [microservices.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/microservices.yaml) | 16 | 8个业务微服务 |
| [ingress.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/ingress.yaml) | 4 | Ingress路由+Nginx |
| [hpa.yaml](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/k8s/hpa.yaml) | 3 | 水平自动扩缩 |

**K8s 资源统计：**
```
14 Service + 13 Deployment + 3 HPA + 2 Ingress
1 StatefulSet + 1 Secret + 1 Namespace + 1 ConfigMap
```

**生产级特性：**
- ✅ 就绪/存活探针（readinessProbe + livenessProbe）
- ✅ 资源限制（requests + limits）
- ✅ HPA 自动扩缩（CPU/内存阈值触发）
- ✅ Ingress 域名路由（api/www/openapi.junsong.local）
- ✅ 多副本（核心服务2副本，非核心1副本）
- ✅ StatefulSet（MySQL持久化存储）

### #11 可观测性体系 — 已完成 ✅

**实现内容：**
- 部署 4 个监控容器：Prometheus(9090) + Grafana(3000) + Loki(3100) + Promtail(9080)
- 微服务接入 micrometer-registry-prometheus（common-security + gateway pom）
- 所有微服务暴露 `/actuator/prometheus` 端点
- Grafana 配置数据源：Prometheus(id=1) + Loki(id=2)
- Promtail 自动采集所有 Docker 容器日志到 Loki

**架构：**
```
微服务 /actuator/prometheus  →  Prometheus(采集)  →  Grafana(展示)
Docker容器日志                →  Promtail(采集)    →  Loki(存储) → Grafana(查询)
```

**验证结果：**
```
Prometheus采集:    9/10 微服务UP ✅
Loki日志采集:      12个日志流(所有容器) ✅
Grafana数据源:     Prometheus + Loki ✅
JVM内存指标:       72个指标(gateway 134MB) ✅
服务可访问:        Prom(200) + Grafana(200) + Loki(200) ✅
```

**访问地址：**
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100

### #7 应用级限流与计量计费 — 已完成 ✅

**实现内容：**
- 网关新增 [RateLimitFilter](file:///Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-gateway/src/main/java/com/junsong/gateway/filter/RateLimitFilter.java)（Order=-145）
- 基于 Redis 计数器实现每日配额限流（每个AppKey每天调用次数）
- ApiKeyAuthFilter 查询 AppSecret 时同步缓存 daily_quota 到 Redis
- common-redis 新增 `increment()` 方法支持原子计数

**限流机制：**
```
Redis Key: openapi:quota:{appKey}:{yyyy-MM-dd}
过期时间：25小时(跨天后自动清理)

配额来源：
  测试Key  → 100次/天
  生产Key  → 10000次/天
```

**响应头：**
```
X-RateLimit-Limit: 3         每日配额
X-RateLimit-Remaining: 0      剩余次数
```

**验证结果：**
```
配额=3, 连续请求5次：
  第1次: Limit=3, Remaining=2   ✅ 允许
  第2次: Limit=3, Remaining=1   ✅ 允许
  第3次: Limit=3, Remaining=0   ✅ 允许(最后一次)
  第4次: 429 请求超出每日配额    ✅ 限流
  第5次: 429 请求超出每日配额    ✅ 限流
```

### #6 Webhook 订阅与投递 — 已完成 ✅

**实现内容：**
- 数据库：webhook_subscription（订阅表）+ webhook_delivery（投递记录表）
- 后端：订阅管理 CRUD + 投递服务（MQ 异步投递 + HMAC-SHA256 签名 + 指数退避重试）
- 前端：订阅管理页面（增删改查 + 状态切换 + 测试事件 + 投递记录查看）
- 定时任务：每 2 分钟扫描待重试的投递记录
- 技术方案：RabbitMQ（DirectExchange + 死信队列，生产者发消息→消费者执行HTTP投递）

**验证结果：**
- 新增订阅 → 发送测试事件 → 消息进入MQ队列 → 消费者消费 → HTTP投递 status=200
- HMAC-SHA256 签名验证（X-Webhook-Signature 头）
- 重试机制：指数退避（2/4/8 分钟），最大重试 3 次后进入死信队列
- MQ 队列：webhook.delivery.queue（投递队列）+ webhook.delivery.dlq.queue（死信队列）

### #8 API 文档聚合 — 已完成 ✅

**实现内容：**
- 网关自建 RouterFunction 提供 `/v3/api-docs/swagger-config` 聚合端点
- 聚合 8 个微服务的 OpenAPI 文档（认证/系统/财务/会员/工作流/文件/定时任务/代码生成）
- SpringDocConfig 通过 Nacos 服务发现动态注册 swagger urls
- swagger-ui 统一入口页面，下拉切换各服务文档

**方案决策：**
- 原规划用 Knife4j Gateway 聚合，但 knife4j 4.5.0（2024年发布，已停更）不兼容 Spring Cloud 2025 新版 WebFlux 网关
- 改用 springdoc 原生聚合 + 自建 RouterFunction 端点方案

**验证结果：**
- swagger-config 聚合端点返回 8 个服务（中文名称+文档URL）
- swagger-ui 页面正常加载，可切换查看各服务文档
- 各微服务 /v3/api-docs 全部返回 200
- 通过 nginx 代理也可访问

---

## 阶段三：生态化与商业化（P2）

> 目标：从"开放 API 平台"升级为"开发者生态"。

| # | 任务 | 状态 | 说明 |
|:---|:---|:---:|:---|
| 9 | 多语言 SDK | ✅ 完成 | 2026-06-27 | OpenAPI Generator 生成 Java/Python/Go/JS SDK |
| 10 | 差异化能力输出 | ✅ 完成 | 2026-06-27 | 工作流/会员/门店选址 即服务，30个开放API端点 |
| 11 | 可观测性体系 | ✅ 完成 | 2026-06-27 | Prometheus + Grafana + Loki，9微服务metrics采集 |
| 12 | CI/CD + K8s | ✅ 完成 | 2026-06-27 | GitHub Actions(3流水线) + K8s(36资源) |

---

## 技术选型

| 能力 | 方案 |
|:---|:---|
| 多租户 | MyBatis 拦截器（已实现） |
| OAuth2 | Spring Authorization Server |
| API Key | 自研 HMAC-SHA256 签名 |
| 消息队列 | RabbitMQ |
| API 文档聚合 | SpringDoc 原生聚合 + 网关 RouterFunction |
| 限流 | Sentinel + Nacos 数据源 |
| SDK 生成 | OpenAPI Generator |
| 监控 | Prometheus + Grafana |
| 日志 | Loki + Promtail |
| 追踪 | 调用日志 + requestId；分布式链路追踪按部署环境扩展 |

---

## 变更记录

| 日期 | 任务 | 变更 |
|:---|:---|:---|
| 2026-06-27 | #5 多租户体系 | 完成，58表100%覆盖，端到端验证通过 |
| 2026-06-27 | #6 Webhook | 完成，RabbitMQ生产者+消费者+死信队列+签名，端到端验证通过 |
| 2026-06-27 | #8 文档聚合 | 完成，springdoc聚合8个微服务文档，swagger-ui统一入口 |
| 2026-06-27 | #1 开放平台后端 | 完成，应用管理+Key发放(测试自动+生产审批)，端到端验证通过 |
| 2026-06-27 | #2 网关ApiKeyAuthFilter | 完成，HMAC-SHA256签名+时间戳+nonce防重放，6项测试全部通过 |
| 2026-06-27 | #3 前端门户 | 完成，Vue3+ElementPlus应用管理界面，菜单已配置 |
| 2026-06-27 | #4 版本路由 | 完成，多版本路由(v1/v2/latest)+废弃标记，阶段一(P0)全部完成 |
| 2026-06-27 | #7 应用级限流 | 完成，Redis计数器+每日配额限流，阶段二(P1)全部完成 |
| 2026-06-27 | #11 可观测性 | 完成，Prometheus+Grafana+Loki，9微服务metrics+12日志流 |
| 2026-06-27 | #12 CI/CD+K8s | 完成，3个GitHub Actions流水线+36个K8s资源(含HPA/Ingress) |
| 2026-06-27 | #9 多语言SDK | 完成，OpenAPI Generator生成4语言SDK+HMAC签名示例验证通过 |
| 2026-06-27 | #10 差异化能力 | 完成，3个聚合Controller+30个开放API端点，链路验证通过。**全部12个任务完成！🎉** |
| 2026-07-04 | R23 外部集成 | 完成可信上下文、调用日志、Webhook 持久化、内部接口隔离和 PROD compose 门禁 |
| 2026-07-04 | R25 企业级硬化 | 完成开放平台敏感信息脱敏、高危操作审计、告警治理和性能基线接入 |
