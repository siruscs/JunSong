# Task 6 完成报告：经营库存报表查询模型和 Mapper

## 任务编号和目标

Task 6：实现经营库存报表查询模型和 Mapper。为库存报表提供租户安全的查询类型、汇总/明细/流水下钻 Mapper 和 SQL。

## 实际修改文件及其职责

| 文件 | 类型 | 职责 |
|------|------|------|
| `domain/vo/StockReportQuery.java` | 新建 | 查询参数：deptIds、startDate、endDate、keyword、status、pageNum、pageSize；含 validate() |
| `domain/vo/StockReportSummaryVO.java` | 新建 | 10 个汇总指标：期初、采购净入库、销售净出库、其他调整(0)、期末、负/低/零/滞销库存数、异常数 |
| `domain/vo/StockReportItemVO.java` | 新建 | 单商品行：租户、门店、商品、数量、时间、状态、对账状态 |
| `domain/vo/StockLedgerRowVO.java` | 新建 | 流水下钻行：变动类型、变动数量、来源等 |
| `domain/vo/StockReportVO.java` | 替换 | 复合 VO：summary + items + total + pageNum + pageSize |
| `mapper/StockReportMapper.java` | 新建 | 4 方法：selectStockReportSummary、selectStockReportItems、countStockReportItems、selectStockLedgerRows |
| `mapper/finance/StockReportMapper.xml` | 新建 | SQL 实现，含 AuthorizedStockBase 和 stockReportPerProduct 共享片段 |
| `test/.../StockReportMapperContractTest.java` | 新建 | 9 个契约断言 |

## 关键设计决定

1. **共享 SQL 片段**：`AuthorizedStockBase` 封装租户+部门过滤；`stockReportPerProduct` 封装单商品维度计算，被 summary/items/count 三查询复用，确保口径一致。
2. **change_type 分类**：采购净入库 = PURCHASE_IN(+) + PURCHASE_REVERSE(-)；销售净出库 = SALE_OUT(+) + SALE_REVERSE(-)。不按正负号分类。
3. **期初数量**：优先取 startDate 之前最近快照 quantity，无快照回退为 ledger 净累加。
4. **半开区间**：`create_time >= #{startDate} AND create_time < DATE_ADD(#{endDate}, INTERVAL 1 DAY)`，不使用 DATE(create_time)。
5. **fin_product JOIN**：确认 fin_product 表存在本库，LEFT JOIN 获取 product_code/unit/min_stock。
6. **分页**：`LIMIT #{offset}, #{query.pageSize}`，offset = (pageNum-1)*pageSize。
7. **对账状态**：流水推算期末与 endDate 当日快照对比。

## 已执行的测试命令和结果

| 命令 | 结果 |
|------|------|
| `mvn -Dtest=StockReportMapperContractTest test` | 9 测试通过 |
| `mvn compile` | BUILD SUCCESS |
| `mvn -Dtest=FinanceReportServiceImplTest test` | 15 测试通过（回归） |

## 数据库迁移

不涉及。

## 权限、租户、部门、并发和财务边界自查

- [x] 租户：所有查询显式 `tenant_id = #{tenantId}`
- [x] 部门：deptIds 非空时 IN 过滤
- [x] 财务边界：只读查询，不修改数据
- [x] 口径一致：汇总和明细使用同一 SQL 片段

## 已知限制和后续风险

1. 期初数量依赖快照完整性，若无快照则回退为 ledger 累加，可能受历史数据影响。
2. 滞销判定（30天无出库）需 Task 7 在服务层补充计算或在 SQL 中实现。
3. StockReportVO 替换后 FinanceReportServiceImpl.getStockReport() 仍抛异常，待 Task 7 实现。
