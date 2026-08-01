# 财务库存与小程序能力修复进度

## 当前状态

进行中，已完成根因调查、设计及阶段 1/2，正在完成本轮用户反馈的后端修复与生产部署。

## 进度

| 阶段 | 状态 | 结果 |
|---|---|---|
| 根因调查与设计 | 已完成 | 已确认报表固定路由缺失；期初、成本、小程序能力待分阶段验证 |
| 阶段 1：PC 概览与路由 | 已完成 | 已补齐六个报表固定路由；概览单行标题验收通过 |
| 阶段 2：期初库存与成本调整 | 已完成 | 期初库存入口统一为 `/finance/stockInit`；成本调整入口、接口调用和成功后刷新链路已纳入回归检查 |
| 阶段 3：小程序库存与成本只读 | 已完成 | 新增 `stockCost` 模块、授权入口和只读库存价值页面；小程序独立仓库已提交 |
| 阶段 4：小程序缴款编辑 | 已完成 | 销售详情缴款记录新增按权限显示的修改入口，复用后端 PUT 更新接口 |
| 阶段 5：验收与部署 | 已完成 | DEV/PROD 前端部署完成；PROD 页面与容器健康检查通过 |

## 阶段 2 验收

- `node scripts/finance-stock-miniprogram-repair.test.mjs`：通过。
- `junsong-ui-v3`：`npx vite build` 通过。
- 说明：本阶段未修改成本调整后端事务规则，只修复并验证 PC 端入口路由与现有后端闭环；小程序库存与成本仍按只读范围开发。

## 阶段 3 验收

- 小程序构建：`npm run build:mp-weixin` 完成。
- 专项检查：`node scripts/finance-stock-miniprogram-repair.test.mjs` 通过。
- 小程序独立提交：`7a3fc7f`。
- 权限边界：库存与成本入口通过 `stockCost` 模块权限控制；页面仅查询 `/report/stock/value`，不提供盘点、成本调整、过账。
- 完整历史测试集仍有既有失败项，未归因于本阶段；后续验收报告会附失败清单。

## 阶段 4 验收

- 专项检查：`node scripts/finance-stock-miniprogram-repair.test.mjs` 通过。
- 小程序构建：`npm run build:mp-weixin` 完成。
- 权限边界：修改入口要求 `finance:sale:payment`；新增缴款仍要求原有销售编辑权限。

## 阶段 5 验收与部署

- DEV：`bin/deploy-ui.sh dev` 完成；部署后本机 80 端口未监听，无法完成本机 HTTP 验证，需后续启动本机 nginx/Compose 后复核。
- PROD：`bin/deploy-ui.sh prod` 完成，远端 nginx 配置检查通过并成功重建 nginx 容器。
- PROD 验证：`https://120.55.243.17/` 返回 HTTP 200；nginx、gateway、finance 容器均为 Up。
- PROD API 未登录访问返回 HTTP 403，符合接口鉴权预期。
- 本次无 SQL/Nacos 配置变更。

## 2026-08-02 用户反馈复核

- 成本调整：库存价值商品明细已增加“成本调整”列，直接按商品核对本期调整金额；后端仍按成本层和成本流水计算期末金额。
- 小程序权限：PC 小程序模块权限默认清单已补充“库存与成本”。
- 经营总览：标题已移至顶部刷新按钮同一行，并压缩顶部高度。
- 期初库存图标：SQL 已补充历史菜单图标同步语句；DEV 已执行成功，PROD 因脚本同时包含表结构/权限幂等语句，待完成生产备份和回滚审批后执行。
- 期初库存影响：只有完成“审批 → 过账”后才会写入库存流水和成本层；仅录入/保存草稿不会改变库存与成本报表。
- 本轮 PC 前端已重新部署 DEV/PROD，PROD 页面返回 200，服务容器正常。

## 本轮追加修复

- 成本调整明细改为按商品聚合 `COST_ADJUST` 流水，不再固定显示 0；毛利统一按“销售收入 - 销售成本 - 成本调整”计算。
- 经营总览移除“经营驾驶舱 / 今日 / 经营结论”标题块；顶部统计改为当前核算周期，未结束周期按当前时间统计。
- 小程序模块清单与权限定义均补充 `stockCost`，解决仅修改 PC 默认清单但服务端模块列表未下发的问题；WJS 需退出小程序后重新登录或刷新授权缓存。
- 财务主服务无测试编译参数构建通过：`mvn -pl junsong-modules/junsong-finance -am -Dmaven.test.skip=true package`。
- 全量财务测试仍被既有测试桩与接口签名不一致阻断，未纳入本轮修复范围；专项检查与 PC 构建通过。
- PROD 期初库存菜单 SQL：已由 `bin/deploy-sql.sh sql/finance_stock_init.sql prod` 执行；脚本先完成 `/root/deploy/backup/YYYYMMDD/mysql/junsong-cloud-before-*.sql.gz` 数据库备份，再完成菜单、权限、索引及字符集核验。

## 本轮部署结果

- DEV：财务服务、会员服务、PC 前端部署完成；因后端部署脚本并发构建会共享清理依赖，财务服务改为使用单独验证后的构建产物串行部署。
- PROD：财务服务 SHA-256 `99b4d777b20f61336eb7f55dbf922a0e8574fb4b3af0bdfb79c989a31f202364`；会员服务 SHA-256 `58f74a588fa77a0bf74c238f70bb77a17e06bcf5b45c8a4b54667c8fc65b1151`。
- PROD：nginx、finance、member 容器均为运行状态；首页返回 HTTP 200，nginx 配置检查通过。
- PROD SQL：期初库存菜单及 6 条关联菜单核验通过，`fin_stock_init_batch`、`fin_stock_init_item` 表和唯一索引核验通过。

## 2026-08-02 库存调整重构进度

- 设计文档：`docs/superpowers/specs/2026-08-02-inventory-adjustment-redesign-design.md`。
- 实施计划：`docs/superpowers/plans/2026-08-02-inventory-adjustment-redesign.md`。
- 后端已支持六种调整类型、`OTHER` 增减方向、数量三位小数、单位成本两位小数，并按方向调用入库/盘亏成本流水。
- PC 表单已改为“库存调整”，增加调整类型、调整日历和库存方向提示；选商品时自动带入商品进价。
- PC 小程序权限已新增“库存调整”；会员服务已新增 `stockAdjustment` 模块；小程序已增加只读库存调整页面。
- 专项检查、PC 构建、财务/会员主程序构建和小程序构建通过。
- DEV 字典、字段和菜单脚本已部署；DEV 财务服务、会员服务和 PC 前端已部署。
- PROD 字段/菜单 SQL 与库存调整类型字典已部署，脚本均先完成数据库备份并核验字段/字典数量。
- PROD finance/member/PC 已部署；finance SHA-256 `441d5627177966d9ff45c70dd5f729be43e71ffac8cbbd8b86acf0943fafdf26`，member SHA-256 `3d58e5a7626b4babcadf6f07b71d09529face10cccad527f9a7a432a03236d92`；首页 HTTP 200，三个容器正常。
- 小程序源代码提交为 `ea70f55`；构建包需通过微信开发者工具上传发布后，WJS 才能看到新入口。发布后需在 PC 小程序权限中给角色配置“库存调整”，再让 WJS 退出重登刷新模块缓存。

## 2026-08-02 反馈修复

- 数量校验误用字符串比较导致整数被错误拦截，已改为数值精度判断。
- 调整单明细“期初数量”已更名为“调整数量”。
- 小程序工作台此前只读本地模块缓存，已在工作台显示时重新请求 `/member/mp/modules`；小程序提交 `816f973`，构建通过。
- PC 修复已重新构建，待再次部署 PROD；WJS 仍必须使用新小程序构建包，并为角色配置 `stockAdjustment` 模块。
- 调整单查看已改为列表页右侧抽屉，直接加载详情接口，不再跳转错误的新页面；专项检查和 PC 构建通过，待部署 PROD。
- 未过账调整单删除能力已补齐：`DRAFT / VALIDATED / SUBMITTED / APPROVED` 可删除，`POSTED` 禁止删除；后端采用状态标记并保留日志字段，删除权限为 `finance:stockInit:remove`。
- PROD 已部署删除接口、删除权限 SQL 和最新 PC 前端；财务服务 SHA-256 `022f90d42132760d9ae59e4a8909e822a913e0a2417c7e14e1bb71aba2f5e513`。
- 小程序强制清理旧 dist 后全量编译，已核验生成包包含 `stockCost`、`stockAdjustment` 及对应页面；微信开发者工具需重新导入 `/Users/sirius/Documents/TRAE/JunSong-Cloud/junsong-miniprogram/dist`，并重新编译/预览。

## 约束

- 小程序库存与成本只读，不开放盘点、成本调整、过账。
- 不覆盖用户现有未提交改动。
- 每个通过阶段创建本地 Git 提交，不推送远程。
