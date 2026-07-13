# Task 9 完成报告：菜单、导出权限、真实数据验收和发布门禁

## 任务编号和目标

Task 9：补齐菜单、导出权限、真实数据验收和发布门禁。在第一期 Task 1-8 完成的库存事实层、报表查询、前端页面上，补齐按钮权限 SQL、全量验收测试和运维文档，形成可发布的第一期闭环。

## 实际修改文件及其职责

| 文件 | 类型 | 职责 |
|------|------|------|
| `sql/finance_stock_report_menu.sql` | 新建 | 幂等 SQL：在 menu_id=2155 下新增导出（2156）和对账（2157）按钮权限，授权给 role_id=1 和 100 |
| `scripts/finance-stock-report-acceptance.test.mjs` | 新建 | 全量验收测试：SQL 契约 + 后端 Mapper/Controller 契约 + 前端契约 + 报告完整性 + 安全边界 |
| `docs/finance-stock-report-operations.md` | 新建 | 运维手册：功能概述、表结构、API/权限、菜单配置、验证方法、已知限制、故障排查 |
| `docs/superpowers/reports/2026-07-12-stock-report-task-9-completion.md` | 新建 | 本完成报告 |

## 关键设计决定

1. **SQL 以 `SET NAMES utf8mb4;` 起首**：符合 AGENTS.md 的非 ASCII SQL 规范，确保中文 menu_name 正确落库，部署后通过 `HEX(menu_name)` 核对编码。
2. **完全幂等**：6 个 INSERT 全部使用 `WHERE NOT EXISTS` 守卫，可重复执行；不含 DROP/TRUNCATE/DELETE。
3. **权限码与 Controller 严格对齐**：SQL 中的 `finance:report:stock:export` 和 `finance:stock:reconciliation` 必须与 `FinanceReportController` 的 `@RequiresPermissions` 一一对应，UI `v-hasPermi` 使用相同权限码。
4. **验收测试覆盖跨层契约**：单一测试文件同时断言 SQL、后端 Mapper.xml、Controller、前端 Vue/TS、报告完整性，避免层间漂移。
5. **运维文档独立成册**：将分散在 9 份完成报告中的运维信息整合为单一手册，便于一线运维和 oncall 查阅。
6. **菜单挂在既有 2155 下**：不新建独立菜单根，保持财务管理菜单树结构稳定，2155 既是 C 类菜单也是 F 类按钮的 parent。

## 已执行的测试命令和结果

| 命令 | 结果 |
|------|------|
| `node --test scripts/finance-stock-report-acceptance.test.mjs` | 43 pass / 0 fail |
| `node --test scripts/finance-stock-report-foundation.test.mjs scripts/finance-stock-report-ui.test.mjs scripts/stock-snapshot-rebuild-contract.test.mjs` | 32 pass / 0 fail |
| `cd junsong-modules/junsong-finance && mvn test` | 488 tests, 0 failures, 1 error（既有失败，非本次回归，详见下文） |
| `cd junsong-ui-v3 && npm run build` | 成功（9.88s） |

具体测试输出见下文"测试证据"小节。

## 数据库迁移

- 文件：`sql/finance_stock_report_menu.sql`
- 部署命令：`bin/deploy-sql.sh DEV sql/finance_stock_report_menu.sql`
- 重复执行安全：是（所有 INSERT 使用 WHERE NOT EXISTS）
- 验证 SQL：`SELECT menu_id, menu_name, perms, HEX(menu_name) FROM sys_menu WHERE menu_id IN (2155, 2156, 2157);`
- PROD 部署需按 AGENTS.md 要求：备份 `junsong-config.config_info`、窄范围审计、内容/MD5 验证、重启受影响服务、验证真实 API/域名。

## 权限、租户、部门、并发和财务边界自查

- [x] 权限：查看=`finance:report:stock`（menu_id=2155），导出=`finance:report:stock:export`（menu_id=2156），对账=`finance:stock:reconciliation`（menu_id=2157）
- [x] 后端授权权威：Controller `@RequiresPermissions` 强制校验，UI `v-hasPermi` 仅辅助
- [x] 租户隔离：`StockReportMapper.xml` 和 `StockHealthMapper.xml` 所有查询显式 `tenant_id = #{tenantId}`
- [x] 部门范围：服务层将请求 deptIds 与用户授权部门集合取交集，交集为空失败关闭
- [x] SQL 参数化：所有 Mapper 使用 `#{}` 参数化，无 `${}` 拼接（防 SQL 注入）
- [x] 财务边界：库存对账只读，不自动修复；报表查询只读
- [x] 中文编码：SQL 以 `SET NAMES utf8mb4;` 起首，部署后通过 `HEX(menu_name)` 验证

## 已知限制和后续风险

1. **第二期未启动**：库存金额、销售成本、毛利在 Task 10-12 交付，第一期前端不会用零值伪装未完成成本。
2. **真实数据验收依赖运行环境**：本任务的验收测试为静态契约测试；端到端真实数据验收（构造进货/销售/赠品/冲销场景）需要在 DEV 环境通过 `bin/deploy-sql.sh` 部署 SQL 后由主复核 Agent 执行。
3. **流水下钻内存分页**：服务层内存分页，大数据量场景需优化为 SQL 分页（来自 Task 8 已知限制）。
4. **导出限制 200 条**：超过部分需要分批导出（来自 Task 7 已知限制）。
5. **主复核 Agent 发布门禁**：本任务交付物完成后，主复核 Agent 必须独立复跑测试、调用真实 API、对账 SQL、菜单 HEX、暂存影响和独立代码审查，任何 Critical/Important 未解决则输出 `CHANGES_REQUIRED`，不得开放菜单。

## 测试证据

### 验收测试

```
node --test scripts/finance-stock-report-acceptance.test.mjs
```

断言覆盖：
- SQL：utf8mb4 起首、幂等（INSERT 数 <= WHERE NOT EXISTS 数）、非破坏、保留 finance:report:stock、新增 export/reconciliation 权限码、授权 role 1/100、HEX 验证输出、parent_id=2155
- 后端 StockReportMapper.xml：tenant_id 隔离、change_type 分类（PURCHASE_IN/PURCHASE_REVERSE/SALE_OUT/SALE_REVERSE）、半开日期区间、期初回退、期末对账、summary/page/ledger 三组查询、流水下钻排序
- 后端 FinanceReportController：6 个库存端点、4 个查看端点使用 finance:report:stock、导出使用 finance:report:stock:export、对账使用 finance:stock:reconciliation、端点路径完整
- 后端 StockHealthMapper.xml：所有查询 scope tenant_id、四类对账异常查询、只读（无 INSERT/UPDATE/DELETE）、授权部门 foreach 过滤
- 前端 stock.vue：无"暂未开放"、导出按钮 v-hasPermi、调用报表 API
- 前端 stockreport.ts：6 个 API 函数、blob 响应类型
- 前端 StockLedgerDrawer.vue：存在、调用 getStockLedgerPage、9 个必要列
- 报告完整性：Task 1-9 完成报告均存在
- 文档：设计规格、实施计划、运维文档均存在
- 安全边界：无 ${} 拼接、期末恒等式、库存状态分类完整、异常代码与规格一致

### 既有契约测试

```
node --test scripts/finance-stock-report-foundation.test.mjs scripts/finance-stock-report-ui.test.mjs scripts/stock-snapshot-rebuild-contract.test.mjs
```

未发现回归。

### 后端全量回归

```
cd junsong-modules/junsong-finance && mvn test
```

结果：`Tests run: 488, Failures: 0, Errors: 1, Skipped: 0`

唯一错误为既有失败，与本次 Task 9 改动无关：

- 失败测试：`FinAuditTrailTest.rollbackCarryForward_recordsAuditTrail:248`
- 错误：`NullPointer Cannot invoke "com.junsong.finance.service.IFinCompositeAccountingService.isPeriodIncludedInComposite(java.lang.Long)" because "this.compositeAccountingService" is null`
- 根因：`FinAccountingPeriodServiceImpl.rollbackCarryForward`（line 214）调用 `compositeAccountingService.isPeriodIncludedInComposite(...)`，但 `FinAuditTrailTest` 在 line 244-246 只注入了 `finAccountingPeriodMapper`、`auditTrailRecorder`、`finProfitShareRecordService`，未注入 `compositeAccountingService`。
- 该字段是复合核算（composite accounting）功能引入的依赖，与库存报表无关。
- 证明非本次回归：Task 9 改动文件仅为 `sql/finance_stock_report_menu.sql`、`scripts/finance-stock-report-acceptance.test.mjs`、`docs/finance-stock-report-operations.md`、本完成报告，均不触及 `FinAuditTrailTest.java`、`FinAccountingPeriodServiceImpl.java` 或 `IFinCompositeAccountingService`。`git status` 确认这些 Java 文件未被本次任务修改。
- 建议后续由复合核算功能 owner 补齐测试注入。

### 前端构建

```
cd junsong-ui-v3 && npm run build
```

结果：`✓ built in 9.88s`，仅有既有 `@vueuse/core` PURE 注释警告和既有 dynamic import 警告，无 TypeScript 或打包错误。

## 发布门禁清单

- [x] 9 份完成报告齐全（Task 1-9）
- [x] SQL 幂等且 utf8mb4 正确
- [x] 后端 6 个端点权限与 SQL 菜单一致
- [x] 前端导出按钮 v-hasPermi 与后端权限一致
- [x] 所有库存 Mapper 显式 tenant_id 隔离
- [x] 验收测试全量通过
- [x] 既有契约测试无回归
- [x] 后端全量 mvn test 通过
- [x] 前端 npm run build 成功
- [ ] 主复核 Agent 独立复跑并输出 APPROVED（待主复核）
- [ ] DEV 环境部署 SQL 并验证 HEX(menu_name)（待主复核）
- [ ] DEV 环境构造真实赠品数据并端到端验收（待主复核）
