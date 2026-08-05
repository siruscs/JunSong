# 会员优化与跨机构配置同步实施计划

> **For agentic workers:** 按任务顺序执行本计划；每个任务先补充失败测试，再实现最小代码，最后运行任务级回归。

**Goal:** 补齐会员购买域未完成的业务闭环，并实现商品、供应商、会员等级、会员商品销售政策的安全跨机构差异同步。

**Architecture:** 会员域继续复用现有购买、付款、领取、等级和积分成长服务；跨机构同步新增通用批次/明细编排层，由商品、供应商、会员等级和会员政策适配器分别负责业务编码匹配、依赖检查、字段差异和写入。所有写入按目标机构独立事务执行，不跨机构共享主数据。

**Tech Stack:** Java 17、Spring Boot、MyBatis、MySQL、Vue 3、Element Plus、uni-app、JUnit 5、Node test。

---

## 任务 0：建立基线和保护现有工作区

**Files:**
- Inspect: `AGENTS.md`
- Inspect: `docs/superpowers/specs/2026-08-05-organization-config-sync-design.zh-CN.md`
- Inspect: `docs/superpowers/plans/2026-08-04-member-purchase-domain-tracking.zh-CN.md`

- [ ] 运行 `git status --short`，确认不覆盖现有会员域、小程序和 PC 改动。
- [ ] 运行会员模块测试、PC 构建和小程序购买相关测试，记录基线失败项。
- [ ] 使用 GitNexus（若当前环境提供）对将修改的现有服务符号执行 upstream impact；若工具不可用，在任务日志中记录不可用并通过现有测试和窄范围检查替代。

## 任务 1：补齐散客绑定会员能力

**Files:**
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchaseController.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/IMemberPurchaseService.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemPurchaseMapper.java`
- Modify: `junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/MemberPurchaseBindingTest.java`

- [ ] 先写测试：匿名/实名购买单绑定到当前机构会员时，校验会员归属、订单状态、已付款/已领取状态和幂等键；跨机构会员和已作废订单必须拒绝。
- [ ] 运行该测试确认在缺少绑定接口时失败。
- [ ] 增加 `POST /purchase/{purchaseId}/bind-member`，服务端锁定购买单和会员，确认来源机构一致后更新顾客类型、会员 ID 和展示快照。
- [ ] 对已经产生积分奖励的订单禁止重复绑定奖励；绑定动作只建立关系，不追溯发放购买奖励，除非订单原本就是会员购买单。
- [ ] 运行绑定测试和会员模块全量测试。

## 任务 2：补齐 PC 端购买、收款、领取闭环

**Files:**
- Modify: `junsong-ui-v3/src/api/member/purchase.ts`
- Modify: `junsong-ui-v3/src/views/member/purchase/index.vue`
- Create/Modify: `junsong-ui-v3/src/api/member/campaignPolicy.ts`
- Create/Modify: `junsong-ui-v3/src/views/member/campaignPolicy/index.vue`

- [ ] 先增加 API/UI 行为测试，覆盖详情中收款、分批领取、撤销和欠款展示。
- [ ] 增加收款弹窗：金额、付款方式、备注和幂等提交；金额不能超过应收金额。
- [ ] 增加领取弹窗：按购买明细输入领取数量，显示购买数、赠送数、已领取数和剩余可领取数。
- [ ] 增加撤销确认和会员奖励冲正结果展示。
- [ ] 刷新购买单详情、列表和统计数据，不能读取或修改财务销售统计。
- [ ] 运行 PC 构建和相关组件测试。

## 任务 3：补齐小程序购买、收款、领取交互

**Files:**
- Modify: `junsong-miniprogram/src/api/memberPurchase.js`
- Modify: `junsong-miniprogram/src/config/modules.js`
- Create/Modify: `junsong-miniprogram/src/pages/member/purchase/index.vue`
- Create/Modify: `junsong-miniprogram/src/pages/member/purchase/detail.vue`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemMpPermController.java`
- Create: `junsong-miniprogram/test/member-purchase.test.mjs`

- [ ] 先写小程序测试：模块未授权不展示入口；提交收款/领取自动携带幂等键；超时重试复用原键；成功后刷新详情、待领取和会员摘要。
- [ ] 运行测试确认新交互尚未满足时失败。
- [ ] 接入购买列表、详情、收款、分批领取和撤销能力；所有按钮由后端能力码控制。
- [ ] 统一处理 401、403、状态冲突和网络超时，不显示原始异常。
- [ ] 运行购买相关小程序测试和 `npm run build:mp-weixin`。
- [ ] 记录全量小程序测试中与本次无关的既有失败，不混入本次修复。

## 任务 4：会员域发布验收和历史对账收口

**Files:**
- Modify: `docs/superpowers/plans/2026-08-04-member-purchase-domain-tracking.zh-CN.md`
- Inspect: `sql/member_purchase_domain_reconcile.sql`
- Inspect: `scripts/member-purchase-domain-release-check.mjs`

- [ ] 执行只读对账，确认购买订单、明细、付款、领取、政策和积分成长流水无越权异常。
- [ ] 验证重复执行 DDL、菜单脚本和发布检查脚本可重复执行。
- [ ] 使用有效登录会话验证会员购买、收款、领取、撤销、散客绑定和权限拒绝。
- [ ] 将 MP-008、MP-012、MP-013 和小程序验收状态更新为实际结果。

## 任务 5：跨机构同步基础模型和权限

**Files:**
- Create: `junsong-modules/junsong-common/src/main/java/com/junsong/common/configsync/ConfigSyncType.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/domain/MemConfigSyncBatch.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/domain/MemConfigSyncDetail.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemConfigSyncMapper.java`
- Create: `junsong-modules/junsong-member/src/main/resources/mapper/member/MemConfigSyncMapper.xml`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/IMemberConfigSyncService.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java`
- Create: `sql/member_config_sync.sql`
- Create: `sql/member_config_sync_menu.sql`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/MemberConfigSyncSecurityTest.java`

- [ ] 先写安全测试：未授权目标机构、跨租户目标、来源机构作为目标和缺少同步权限均拒绝。
- [ ] 运行测试确认同步域尚不存在时失败。
- [ ] 新增租户隔离的同步批次/明细表，保存目标机构、业务键、快照、差异、决策、行版本和结果。
- [ ] 增加独立权限：`finance:product:sync`、`finance:supplier:sync`、`member:level:sync`、`member:campaignPolicy:sync`、`member:configSync:query`。
- [ ] 菜单 SQL 使用可重复执行写法，管理员默认授权；不删除已有菜单或角色授权。
- [ ] 运行安全测试并部署 DEV DDL/菜单。

## 任务 6：通用同步预览和执行编排

**Files:**
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/ConfigSyncAdapter.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/ConfigSyncDiff.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/ConfigSyncPreview.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberConfigSyncServiceImpl.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemConfigSyncController.java`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/MemberConfigSyncOrchestrationTest.java`

- [ ] 先写测试：相同业务键返回无需处理；不存在返回新增；字段不同返回差异；未选择决策不能执行。
- [ ] 运行测试确认编排层缺失时失败。
- [ ] 实现 `preview`、批次查询、`execute`、失败/冲突重试接口。
- [ ] 执行时按目标机构独立事务处理；目标行版本变化进入冲突，不覆盖新数据。
- [ ] 使用幂等键和批次版本防止重复写入。
- [ ] 返回脱敏后的字段差异和可操作错误，不返回 SQL 或堆栈。
- [ ] 运行编排测试、会员模块测试和 MyBatis XML 检查。

## 任务 7：商品和供应商同步适配器

**Files:**
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/FinanceProductConfigSyncAdapter.java`
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/FinanceSupplierConfigSyncAdapter.java`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinProductMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinProductMapper.xml`
- Modify: `junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/FinSupplierMapper.java`
- Modify: `junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinSupplierMapper.xml`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/FinanceConfigSyncAdapterTest.java`

- [ ] 先写测试：按商品编码/供应商编码匹配，不复制来源主键；新增、覆盖、跳过和版本冲突分别得到正确结果。
- [ ] 运行测试确认适配器尚不存在时失败。
- [ ] 增加按目标机构和业务编码查询、带版本条件的新增/更新 Mapper 方法。
- [ ] 商品同步不复制库存数量、库存流水和财务历史；供应商同步不复制采购单引用。
- [ ] 注册适配器并运行适配器测试、finance 窄测和 member 窄测。

## 任务 8：会员等级同步适配器

**Files:**
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberLevelConfigSyncAdapter.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemMemberCardTypeMapper.java`
- Modify: `junsong-modules/junsong-member/src/main/resources/mapper/member/MemMemberCardTypeMapper.xml`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/MemberLevelConfigSyncAdapterTest.java`

- [ ] 先写测试：等级编码匹配，积分倍率、成长门槛和状态可预览差异；来源主键不得写入目标机构。
- [ ] 运行测试确认适配器尚不存在时失败。
- [ ] 增加目标机构范围查询和版本条件更新，保留目标机构现有会员关联关系。
- [ ] 注册等级适配器并运行会员全量测试。

## 任务 9：会员销售政策同步适配器

**Files:**
- Create: `junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberCampaignPolicyConfigSyncAdapter.java`
- Modify: `junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemCampaignPolicyMapper.java`
- Modify: `junsong-modules/junsong-member/src/main/resources/mapper/member/MemCampaignPolicyMapper.xml`
- Create: `junsong-modules/junsong-member/src/test/java/com/junsong/member/service/MemberCampaignPolicyConfigSyncAdapterTest.java`

- [ ] 先写测试：目标周期、商品编码、政策编码和套餐档位均可解析；缺少目标周期、商品或等级时拒绝执行。
- [ ] 运行测试确认适配器尚不存在时失败。
- [ ] 预览请求显式携带目标周期映射，执行时固化目标政策版本和套餐快照。
- [ ] 同步政策不修改已生成会员购买单，按单次购买规则保持历史数据不变。
- [ ] 注册适配器并运行政策、购买域和会员全量测试。

## 任务 10：PC 四类维护页面接入

**Files:**
- Create: `junsong-ui-v3/src/api/member/configSync.ts`
- Create: `junsong-ui-v3/src/components/ConfigSyncDialog/index.vue`
- Modify: `junsong-ui-v3/src/views/finance/product/index.vue`
- Modify: `junsong-ui-v3/src/views/finance/supplier/index.vue`
- Modify: `junsong-ui-v3/src/views/member/level/index.vue`
- Modify: `junsong-ui-v3/src/views/member/campaignPolicy/index.vue`
- Create: `junsong-ui-v3/src/components/ConfigSyncDialog/__tests__/ConfigSyncDialog.spec.ts`

- [ ] 先写测试：四类页面能打开同步入口；目标机构只能来自 API；差异项必须逐条覆盖/跳过；执行后显示批次结果。
- [ ] 运行前端测试确认组件尚不存在时失败。
- [ ] 实现三步弹窗：目标机构、差异决策、执行结果；政策同步增加目标周期选择。
- [ ] 入口使用独立同步权限；请求失败显示服务端原因并保留批次查询入口。
- [ ] 运行 `npm run build` 和组件测试。

## 任务 11：审计、重试、发布和跟踪收口

**Files:**
- Create: `sql/member_config_sync_reconcile.sql`
- Create: `scripts/member-config-sync-release-check.mjs`
- Modify: `docs/superpowers/plans/2026-08-04-member-purchase-domain-tracking.zh-CN.md`
- Modify: `docs/superpowers/plans/2026-08-05-member-optimization-and-cross-org-sync-plan.zh-CN.md`

- [ ] 先写发布检查：要求同步表、权限码、后端适配器、PC API 和页面文件齐全。
- [ ] 运行检查确认缺文件或缺表时失败。
- [ ] 增加只读对账：批次明细与目标配置数量、冲突、失败和重复业务键一致。
- [ ] 执行后端窄测、PC 构建、小程序购买构建和同步相关 Node 测试。
- [ ] 部署 DEV DDL、菜单、会员服务和 PC 构建；检查服务启动和 Nacos 注册。
- [ ] 执行 `git diff --check`，检查 staged 文件名，并在提交前运行 GitNexus `detect_changes(scope="staged")`（若工具可用）。
- [ ] 更新跟踪文档，明确已完成、待登录验收和既有无关失败项。
