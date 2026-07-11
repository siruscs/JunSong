# 变更日志

> 本项目采用 [Semantic Versioning](https://semver.org/lang/zh-CN/) 进行版本管理。
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### Added
- **经营平台能力大迭代完成**
  - 新增经营总览、财务概览、会员概览和系统概览，按角色汇总待办、异常、风险和关键指标。
  - 新增财务闭环能力：复盘任务、复盘日志、复盘质量评分、复盘知识库、应收统计、应收催收作战台、现金流看板和现金流预测。
  - 新增会员增长能力：会员等级、签到、分层、增长动作、活动 ROI、生命周期任务和动作效果回填。
  - 新增系统治理能力：健康规则配置、统一工作台、运营调度、动作中心触达、通知去重和治理任务留痕。
  - 新增预测辅助 V2：现金流、应收、会员动作、库存风险四类可解释预测，以及只读 what-if 模拟。
  - 新增企业级硬化能力：敏感数据脱敏、高危操作 before/after 审计、数据归档策略、告警事件和性能基线。
- **开放平台外部集成增强**
  - 网关验签后注入 `X-Open-*` 可信上下文，开放服务按应用、租户和 requestId 透传下游。
  - 移除固定 admin 冒用，开放 API 通过可信来源和租户上下文访问授权数据。
  - 新增调用日志真实写入链路，覆盖成功、拒绝、限流和异常请求。
  - Webhook 订阅从返回假成功改为持久化落库，并绑定应用与租户。
  - 内部接口移出公网白名单，叠加 `@InnerAuth` 与 `X-Inner-Token`，open 服务端口仅 Docker 内网暴露。
- 新增 R1-R25 能力总览文档：[docs/R1-R25_RELEASE_OVERVIEW.zh-CN.md](./docs/R1-R25_RELEASE_OVERVIEW.zh-CN.md)
- 新增低代码审批平台（工作流 / 低代码引擎）完整 MVP 能力
- 新增门店地图查询与密度查询（高德地图 API 集成）
- 新增会员管理、财务管理、积分系统模块
- 新增微信小程序端（uni-app + Vue3）
- 新增公共开放平台文档：贡献指南、变更日志、Issue/PR 模板
- **开放平台完整能力上线（12 项任务全部完成）**
  - 新增开放平台后端微服务（junsong-open）：应用管理、API Key 发放（测试自动 + 生产审批）
  - 新增网关 ApiKeyAuthFilter：HMAC-SHA256 签名校验 + 时间戳防过期 + Nonce 防重放
  - 新增开放平台前端门户：Vue3 + Element Plus 应用管理界面（菜单已配置）
  - 新增多版本 API 路由：v1（已废弃）/ v2（稳定）/ latest（别名），自动添加 Deprecation/Sunset 头
  - 新增应用级限流 RateLimitFilter：基于 Redis 计数器的每日配额限流（测试 100/天，生产 10000/天）
  - 新增可观测性体系：Prometheus + Grafana + Loki，9 个微服务 metrics 采集 + 容器日志聚合
  - 新增 CI/CD 流水线：3 个 GitHub Actions workflow（CI 编译、CD-Docker、CD-K8s）
  - 新增 Kubernetes 部署清单：36 个 K8s 资源（Deployment/Service/Ingress/HPA/ConfigMap）
  - 新增多语言 SDK：基于 OpenAPI Generator 生成 Java/Python/Go/JavaScript 四种语言 SDK
  - 新增差异化能力输出：工作流即服务、会员能力即服务、门店选址即服务（30 个开放 API 端点）
- 新增开发者文档：OPEN_API_QUICKSTART.md（开放 API 快速入门）

### Changed
- 升级 Spring Boot 至 4.0.3，Spring Cloud 至 2025.1.0
- 升级 Vue 至 3.5，Vite 至 8
- 低代码字段类型从 5 种扩展至 22 种（文本/数值/选择/系统引用/日期/地理媒体）

---

## [1.2.0] - 2026-06-24

### Added
- **文档体系全面重构**
  - 新增 [部署运维手册](./部署运维手册.md)：合并 4 份旧部署文档，含按需启动、最小启动集（7 服务）、13 服务全量部署、公网 PROD 切换、小程序构建
  - 新增 [二次开发指南](./二次开发指南.md)：以新建合同管理服务为完整示例，覆盖 Maven 模块、Nacos 配置、网关路由、Docker 编排、前端对接 11 步流程
  - 新增 [贡献指南](./CONTRIBUTING.md)：开发环境、代码规范、分支策略、提交规范、PR 流程、安全准则
  - 新增 [变更日志](./CHANGELOG.md)
  - 新增 GitHub Issue 模板（Bug 报告、功能请求）和 PR 模板
- **低代码交付文档 v1.2**：补全 22 种字段类型详细说明，修正处理人来源（10 种）、单据状态枚举、存储模式、条件显隐结构等与代码不一致内容
- README 技术架构图新增 OpenFeign、LoadBalancer、服务发现、Sentinel 服务间调用标注
- LICENSE 从 MIT 更新为峻松云软件使用许可协议（明确不提供技术支持、免责声明）

### Changed
- 低代码配置后台手册（config-admin-manual.md）字段类型章节从 5 种重写为 22 种完整分类
- Nacos 控制台访问地址从 `8848/nacos` 更新为 Nacos 3.x 新版控制台 `7080/next`
- 根目录 package.json 去除 RuoYi 残留信息，更新为 JunSong-Cloud 项目信息

### Removed
- 删除 4 份旧部署文档（项目部署及启动说明、DEPLOYMENT_GUIDE、PROD部署更新、小程序模块安装部署更新）
- 删除 .gitignore 中排除的 RuoYi 历史残留文件

### Security
- 全量脱敏：数据库密码、Nacos 密码、MinIO 密钥等敏感信息从配置文件/脚本中移除，改为占位符
- Git 历史清洗：通过 orphan 分支创建全新干净历史，排除构建产物、运行数据、AI 工具配置

---

## [1.1.0] - 2026-06-24

### Added
- 代码审查修复：18 项安全/架构/规范问题
- Demo 清理：删除 POC 演示数据，清理过渡代码

### Changed
- 低代码交付文档版本从 v1.0 更新至 v1.1

---

## [1.0.0] - 2026-06-23

### Added
- **系统管理模块**：用户、角色、权限、部门、菜单、岗位、字典、参数配置
- **低代码审批平台 MVP**：业务对象配置、字段元数据（5 种基础类型）、页面 Schema、节点处理人（9 种来源）、分支规则、动作配置、版本管理
- **门店地图**：基于 Leaflet + 高德 API 的门店分布展示、密度查询、省市区筛选
- **工作流引擎**：基于 Flowable 的审批流程定义与运行
- **文件服务**：基于 MinIO 的文件上传/下载/管理
- **代码生成器**：基于模板的 CRUD 代码自动生成
- **定时任务**：基于 Quartz 的分布式定时调度
- **前端框架**：Vue 3.5 + Vite 8 + TypeScript + Element Plus 管理后台
- **基础设施**：Nacos 3.x 注册/配置中心、Redis 缓存、MySQL 8.0 业务库、Docker Compose 容器化部署

---

## 版本说明

| 版本 | 日期 | 里程碑 |
|------|------|--------|
| v1.0.0 | 2026-06-23 | MVP 首次交付（系统管理 + 低代码审批 + 门店地图） |
| v1.1.0 | 2026-06-24 | 代码审查修复 + Demo 清理 |
| v1.2.0 | 2026-06-24 | 文档体系重构 + 开源合规 + 平台化准备 |

---

*本变更日志由 Genesis·峻松 维护*
