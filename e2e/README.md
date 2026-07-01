# JunSong Cloud E2E 回归测试套件

基于 Playwright 的端到端自动化测试，覆盖登录、工作流、低代码、系统管理、安全等核心模块。

## 快速开始

```bash
cd e2e
npm install
npx playwright install chromium
```

## 运行测试

```bash
# 全部测试
npm test

# 仅冒烟测试
npm run test:smoke

# 按模块测试
npm run test:workflow
npm run test:lowcode
npm run test:biz
npm run test:security

# 可视化调试
npm run test:ui

# 查看报告
npm run test:report
```

## 环境变量

```bash
BASE_URL=http://localhost npx playwright test
```

## 测试覆盖

| 模块 | 文件 | 用例数 |
|------|------|--------|
| 认证登录 | `tests/auth.spec.ts` | 3 |
| 工作台 | `tests/dashboard.spec.ts` | 3 |
| 流程定义 | `tests/workflow/definition.spec.ts` | 5 |
| 流程实例 | `tests/workflow/instance.spec.ts` | 2 |
| 流程任务 | `tests/workflow/task.spec.ts` | 1 |
| 流程历史 | `tests/workflow/history.spec.ts` | 2 |
| 发起流程 | `tests/workflow/start.spec.ts` | 4 |
| 系统用户 | `tests/system/user.spec.ts` | 2 |
| 低代码 | `tests/lowcode/metadata.spec.ts` | 1 |
| 安全 | `tests/security/xss-sql.spec.ts` | 2 |
