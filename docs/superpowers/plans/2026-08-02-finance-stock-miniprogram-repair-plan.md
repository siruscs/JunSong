# 财务库存与小程序能力修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans (recommended). Steps use checkbox (`- [ ]`) syntax.

**Goal:** 修复财务概览、期初库存、成本调整，并新增小程序库存只读与缴款编辑能力。

**Architecture:** 先补齐 PC 路由与页面挂载，再验证财务成本链路；小程序复用现有模块配置、权限缓存和财务 API，新增只读库存页面与缴款编辑动作。后端仅在现有接口无法满足权限或数据一致性时做最小修改。

**Tech Stack:** Vue 3、TypeScript、uni-app/Vue 3、Java/Spring Boot、MyBatis、Node test、Maven。

## 阶段 1：PC 概览与路由

- [ ] 写静态失败测试，断言六个页面路由、概览单行标题和页面组件映射。
- [ ] 补齐 `junsong-ui-v3/src/router/constantRoutes.ts` 的六个报表固定路由。
- [ ] 调整 `junsong-ui-v3/src/views/finance/overview/index.vue` 的标题区域。
- [ ] 运行静态测试和前端构建；记录现有库存页面类型错误。
- [ ] 提交 `fix(ui): restore finance report routes`。

## 阶段 2：期初库存与成本调整

- [ ] 添加期初库存页面/路由契约测试和成本调整服务测试补充。
- [ ] 修复期初库存空白的路由、响应解包或权限根因。
- [ ] 核对成本调整写入成本台账与库存价值读取，修复结果不生效的单一根因。
- [ ] 运行财务模块聚焦测试和前端构建。
- [ ] 提交 `fix(finance): repair stock init and cost adjustment flow`。

## 阶段 3：小程序库存与成本只读

- [ ] 写模块配置、页面入口和只读字段的失败测试。
- [ ] 在 `src/config/modules.js` 增加 `stockCost` 权限配置。
- [ ] 新增库存查询 API/页面，复用库存报表只读接口，禁止写操作。
- [ ] 在小程序首页/工作台按模块权限显示入口，补充 pages.json。
- [ ] 运行小程序 Node 测试和构建。
- [ ] 提交 `feat(miniprogram): add read-only stock cost module`。

## 阶段 4：小程序缴款编辑

- [ ] 写缴款编辑入口、权限、请求参数和刷新行为的失败测试。
- [ ] 复用现有缴款更新接口，补齐小程序销售记录/详情编辑交互。
- [ ] 成功后刷新列表、详情和应收状态，失败显示服务端原因。
- [ ] 运行小程序测试和构建。
- [ ] 提交 `feat(miniprogram): add sale payment edit entry`。

## 阶段 5：验收与部署记录

- [ ] 更新验收报告，分别记录 DEV/PROD 验证和未完成项。
- [ ] 运行 `git diff --check`，确认提交范围只包含本轮任务文件。
- [ ] 每次通过后本地提交，不执行 push。
