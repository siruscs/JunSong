# Task 8 完成报告：PC 库存报表和流水下钻

## 任务编号和目标

Task 8：实现 PC 库存报表和流水下钻。将库存报表从"暂未开放"暂停页替换为完整可查询、可分页、可下钻、可导出的报表页面。

## 实际修改文件及其职责

| 文件 | 类型 | 职责 |
|------|------|------|
| `src/api/finance/stockreport.ts` | 新建 | 6 个 API 函数 + TypeScript 接口定义 |
| `src/views/finance/report/components/StockLedgerDrawer.vue` | 新建 | 流水下钻抽屉，含分页和错误处理 |
| `src/views/finance/report/stock.vue` | 替换 | 完整报表页：查询栏+汇总卡片+明细表+口径说明 |
| `scripts/finance-stock-report-ui.test.mjs` | 新建 | 13 项 UI 契约测试 |

## 关键设计决定

1. **fail-closed 错误处理**：API 失败时 clearData() 清空 summary/items/total，ElMessage.error 提示，不展示假数据。
2. **前端不重算赠品**：无 isGift/giftQuantity 引用，完全使用后端返回的净额。
3. **导出权限**：v-hasPermi="['finance:report:stock:export']"，blob 下载。
4. **口径说明**：el-collapse 折叠区，明确冲销归类、滞销阈值、赠品口径。
5. **汇总卡片无数据时显示 '-'**：不展示假零值。
6. **状态标签颜色**：NORMAL=success, LOW_STOCK=warning, ZERO_STOCK=info, NEGATIVE_STOCK=danger, STALE=warning。
7. **默认日期范围**：最近 30 天。

## 已执行的测试命令和结果

| 命令 | 结果 |
|------|------|
| `node --test scripts/finance-stock-report-ui.test.mjs` | 13 测试通过 |
| `cd junsong-ui-v3 && npm run build` | 成功 (15.27s) |

## 数据库迁移

不涉及。

## 权限、租户、部门、并发和财务边界自查

- [x] 权限：查看=finance:report:stock, 导出=finance:report:stock:export
- [x] 部门：useUserStore().depts 限制授权门店
- [x] fail-closed：API 失败清空数据+错误提示
- [x] 口径一致：前端不重算，使用后端返回净额

## 已知限制和后续风险

1. 流水下钻分页在服务层内存分页，大数据量需优化。
2. 导出目前限制 200 条。
3. 构建有既有依赖警告（@vueuse/core），与本次改动无关。
