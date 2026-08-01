# 财务概览经营驾驶舱 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 重构 PC 财务概览为异常优先的经营驾驶舱，并保留现有真实数据能力。

**Architecture:** 仅修改现有 Vue 页面和前端静态验收脚本；复用当前概览、现金流、预测、预警和任务接口，六类报表入口通过现有路由跳转。

**Tech Stack:** Vue 3、Element Plus、TypeScript/Vite、Node test。

## 文件范围

- Modify: `junsong-ui-v3/src/views/finance/overview/index.vue` — 页面信息架构、样式和入口展示。
- Create: `scripts/finance-overview-dashboard.test.mjs` — 静态验收页面结构、路由和接口兼容性。
- Create: `docs/superpowers/reports/2026-08-01-finance-overview-dashboard-acceptance.md` — 记录测试、构建和遗留风险。

## Tasks

### Task 1: 先建立静态验收基线

- [ ] 添加断言：页面包含五个核心指标、六个报表入口、现有接口 URL 和权限/错误状态。
- [ ] 运行 `node scripts/finance-overview-dashboard.test.mjs`，确认在重构前因结构断言缺失而失败。

### Task 2: 重构页面模板

- [ ] 将页面顺序调整为页头、经营结论、风险待办、趋势/排行、预警/任务、报表工作台。
- [ ] 保留现有请求、数据映射、路由跳转和权限失败逻辑。
- [ ] 六个入口使用 `/finance/report/sale`、`/finance/report/profit`、`/finance/report/expense`、`/finance/report/profitShare`、`/finance/report/stock`、`/finance/report/store`。

### Task 3: 重构样式与可访问状态

- [ ] 使用冷静浅色后台视觉、单一风险色、表格数字等宽显示和清晰焦点态。
- [ ] 为入口、待办、预警、加载失败和空状态提供 hover/focus/empty/error 表现。
- [ ] 不修改全局主题，不引入新依赖。

### Task 4: 验证

- [ ] 运行 `node scripts/finance-overview-dashboard.test.mjs`。
- [ ] 运行 `npm run build`（目录 `junsong-ui-v3`）。
- [ ] 检查 `git diff --check`，确认仅包含本任务文件。

### Task 5: 文档与本地提交

- [ ] 写入验收报告，记录通过项、构建结果和未覆盖的真实环境验证。
- [ ] 仅暂存本任务文件并创建本地 Git 提交，不推送远程。
