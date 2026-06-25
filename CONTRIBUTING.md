# 贡献指南

> **作者：** Genesis·峻松
> **更新日期：** 2026-06-24

感谢你对 JunSong-Cloud 峻松云的关注！无论你是想修复 Bug、新增功能、改进文档，还是报告问题，我们都欢迎你的参与。

## 目录

1. [开发环境准备](#一开发环境准备)
2. [代码规范](#二代码规范)
3. [分支策略](#三分支策略)
4. [提交规范](#四提交规范)
5. [Pull Request 流程](#五pull-request-流程)
6. [安全与敏感信息](#六安全与敏感信息)
7. [文档同步](#七文档同步)
8. [获取帮助](#八获取帮助)

---

## 一、开发环境准备

### 必备工具

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 后端编译运行 |
| Maven | 3.6+ | 依赖管理与构建 |
| Node.js | 16+ | 前端构建 |
| Docker & Docker Compose | 20.10+ | 本地部署与测试 |
| Git | 任意 | 版本控制 |

### 首次克隆与启动

```bash
# 1. 克隆仓库
git clone https://github.com/siruscs/JunSong.git
cd JunSong

# 2. 启动最小环境（MySQL + Redis + Nacos）
cd docker
docker compose up -d junsong-mysql junsong-redis junsong-nacos
sleep 45

# 3. 初始化数据库
cd ..
docker exec -i junsong-mysql sh -lc 'mysql -uroot -p$MYSQL_ROOT_PASSWORD' < sql/junsong-cloud-init-20260614.sql
docker exec -i junsong-mysql sh -lc 'mysql -uroot -p$MYSQL_ROOT_PASSWORD' < sql/junsong-config-init-20260614.sql

# 4. 编译后端
mvn clean package -DskipTests

# 5. 编译前端
cd junsong-ui-v3 && npm install && npm run build:prod && cd ..

# 6. 启动全部服务（详见部署运维手册）
cd docker && docker compose up -d
```

完整部署说明请参阅 [部署运维手册](./部署运维手册.md)。

---

## 二、代码规范

### Java 后端

- 包名：`com.junsong.{模块名}`，全部小写
- 类名：帕斯卡命名（`FinPurchaseServiceImpl`）
- 方法名/变量名：驼峰命名（`selectFinPurchaseList`）
- 常量：全大写下划线（`MAX_RETRY_COUNT`）
- 实体类继承 `BaseEntity`，Controller 继承 `BaseController`
- 接口权限：`@RequiresPermissions("模块:业务:操作")`
- 操作日志：增删改方法加 `@Log(title="...", businessType=...)`
- 禁止 Java 硬编码路由，路由统一由 Nacos 配置管理

### Vue 前端

- 使用 Composition API + `<script setup>` 语法
- API 封装统一放在 `src/api/{模块}/` 下
- 组件命名：帕斯卡命名（`SchemaForm.vue`）
- 页面路由与 `sys_menu` 的 `component` 字段保持一致
- 国际化使用 `vue-i18n`，新增词条需在 `src/i18n/` 补充

### 通用原则

- **DRY**：不要重复造轮子，优先复用 `junsong-common` 中的工具类
- **KISS**：保持简单，避免过度设计
- **配置外置**：业务配置（路由、数据源、白名单）全部放 Nacos，禁止硬编码

---

## 三分支策略

```
main        ← 生产环境代码，受保护，只能通过 PR 合并
  ↑
develop     ← 日常开发分支，功能开发基于此
  ↑
feature/xxx ← 功能分支，从 develop 切出，完成后合并回 develop
  ↑
hotfix/xxx  ← 紧急修复，从 main 切出，完成后同时合并到 main 和 develop
```

| 分支类型 | 命名规范 | 来源分支 | 目标分支 |
|---------|---------|---------|---------|
| 功能分支 | `feature/功能描述` | `develop` | `develop` |
| 修复分支 | `fix/问题描述` | `develop` | `develop` |
| 热修复 | `hotfix/问题描述` | `main` | `main` + `develop` |
| 文档更新 | `docs/描述` | `develop` | `develop` |

---

## 四提交规范

采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型（type）

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 代码重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具链变更 |

### 示例

```
feat(finance): 新增采购单审批流程集成

- 在 FinPurchaseController 增加提交审批接口
- 集成 Flowable 工作流，processKey = purchase-approval
- 新增采购单状态流转：草稿 → 审批中 → 已通过

Fixes #123
```

```
fix(lowcode): 修复计算字段除零导致页面卡死

- computed 类型字段 expression 中除零时返回空而非抛异常
- 前端 FieldRenderer 增加对空值的兜底展示

Fixes #456
```

---

## 五Pull Request 流程

1. **Fork 仓库**（外部贡献者）或 **创建功能分支**（内部成员）
2. **本地开发**：按上述规范编写代码，确保编译通过
3. **本地测试**：
   ```bash
   # 后端编译
   mvn clean package -DskipTests
   # 前端编译
   cd junsong-ui-v3 && npm run build:prod
   ```
4. **提交代码**：按提交规范写 commit message
5. **推送到远程**：`git push origin feature/xxx`
6. **创建 Pull Request**：
   - 标题格式：`[类型] 简要描述`，如 `[feat] 新增合同管理模块`
   - 填写 PR 模板（类型、关联 Issue、测试说明、影响范围等）
   - 至少 1 位维护者 Review 通过后方可合并
7. **合并后**：删除功能分支

---

## 六安全与敏感信息

### 绝对禁止的行为

- ❌ 在代码中硬编码数据库密码、Nacos 密码、MinIO 密钥
- ❌ 在代码中硬编码高德地图 API Key、微信 AppID/Secret
- ❌ 提交生产环境配置文件（含真实密码）到仓库
- ❌ 在日志中打印用户密码、Token、身份证号等敏感信息
- ❌ 在 Issue/PR/Commit 中暴露服务器 IP、账号密码

### 正确做法

- ✅ 密码等敏感信息通过环境变量或 Nacos 配置中心注入
- ✅ 本地开发使用占位符（如 `change-me`）
- ✅ 代码审查时重点关注新增配置项是否含敏感信息

---

## 七文档同步

任何涉及以下变更的 PR，**必须同步更新相关文档**：

| 变更类型 | 需更新的文档 |
|---------|------------|
| 新增/删除服务 | [部署运维手册](./部署运维手册.md)、[二次开发指南](./二次开发指南.md) |
| 新增/修改 API | 相关模块的接口文档（如 Swagger 注解） |
| 新增/修改 Nacos 配置项 | [部署运维手册](./部署运维手册.md) 配置说明章节 |
| 数据库表结构变更 | 附迁移 SQL，并更新数据库设计文档（如有） |
| 新增前端页面/组件 | 更新前端组件使用说明（如适用） |
| 低代码字段类型/功能变更 | [低代码配置后台手册](./docs/lowcode-delivery/operation/config-admin-manual.md) |

---

## 八获取帮助

- 📖 [README](./README.md) — 项目概览与快速入口
- 📖 [部署运维手册](./部署运维手册.md) — 环境搭建、启动、更新
- 📖 [二次开发指南](./二次开发指南.md) — 如何新增服务、模块、接口
- 📖 [低代码交付文档](./docs/lowcode-delivery/) — 低代码平台配置与使用
- 🐛 [提交 Issue](https://github.com/siruscs/JunSong/issues) — Bug 报告或功能请求
- 💬 联系维护者：通过 GitHub Issue 或邮件

---

*本指南由 Genesis·峻松 维护，最后更新：2026-06-24*
