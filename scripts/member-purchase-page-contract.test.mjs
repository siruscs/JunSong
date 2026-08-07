import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../junsong-ui-v3/src/views/member/purchase/index.vue', import.meta.url), 'utf8')

test('member purchase page exposes stable detail and business action entry points', () => {
  assert.match(page, /新建购买单/)
  assert.match(page, />查看</)
  assert.match(page, />收款</)
  assert.match(page, />领取</)
})

test('create action is placed after the query form and before the table', () => {
  const formEnd = page.indexOf('</el-form>')
  const toolbar = page.indexOf('class="mb8 table-toolbar"')
  const table = page.indexOf('<el-table v-loading')
  assert.ok(formEnd >= 0 && toolbar > formEnd && table > toolbar)
})

test('purchase form uses business selectors instead of raw internal ids', () => {
  assert.match(page, /MemberSelect/)
  assert.match(page, /请选择商品/)
  assert.match(page, /销售政策（可选）/)
  assert.match(page, /购买套餐（可选）/)
  assert.doesNotMatch(page, /label="商品ID"/)
  assert.doesNotMatch(page, /label="政策ID"/)
  assert.doesNotMatch(page, /label="套餐ID"/)
})

test('campaign policy form auto-generates policy number and selects accounting period', () => {
  const policyPage = fs.readFileSync(new URL('../junsong-ui-v3/src/views/member/campaignPolicy/index.vue', import.meta.url), 'utf8')
  assert.match(policyPage, /generatePolicyNo/)
  assert.match(policyPage, /请选择核算周期/)
  assert.match(policyPage, /v-model="form.periodId"/)
  assert.doesNotMatch(policyPage, /label="核算周期"><el-input/)
  assert.doesNotMatch(policyPage, /label="政策编号"><el-input v-model="form\.policyNo" placeholder=/)
})

test('campaign policy periods are scoped to the current department', () => {
  const policyPage = fs.readFileSync(new URL('../junsong-ui-v3/src/views/member/campaignPolicy/index.vue', import.meta.url), 'utf8')
  assert.match(policyPage, /deptId: userStore\.currentDeptId/)
  assert.match(policyPage, /String\(item\.deptId\) === String\(userStore\.currentDeptId\)/)
})

test('purchase creation assigns a server-side purchase number before insert', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  const createBlock = service.slice(service.indexOf('public int createPurchase'), service.indexOf('    @Override', service.indexOf('public int createPurchase') + 10))
  assert.match(createBlock, /if \(order\.getPurchaseNo\(\) == null/)
  assert.match(createBlock, /order\.setPurchaseNo\(/)
})

test('purchase creation defaults identity confirmation and supports direct non-policy purchases', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  const createBlock = service.slice(service.indexOf('public int createPurchase'), service.indexOf('    @Override', service.indexOf('public int createPurchase') + 10))
  const page = fs.readFileSync(new URL('../junsong-ui-v3/src/views/member/purchase/index.vue', import.meta.url), 'utf8')
  assert.match(createBlock, /if \(order\.getIdentityConfirmed\(\) == null\)/)
  assert.match(createBlock, /order\.setIdentityConfirmed\(Boolean\.FALSE\)/)
  assert.doesNotMatch(page, /请选择核算周期、商品、销售政策和购买套餐/)
  assert.match(page, /请选择核算周期和商品/)
  assert.match(page, /v-model="createForm\.item\.policyId"[^>]*clearable/)
})

test('purchase items snapshot the authoritative product name before insert', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  assert.match(service, /selectProductNameById\(order\.getTenantId\(\), order\.getDeptId\(\), item\.getProductId\(\)\)/)
  assert.match(mapper, /id="selectProductNameById"/)
  assert.match(mapper, /product_name/)
})

test('new purchase items start with zero delivery balances', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  const createBlock = service.slice(service.indexOf('public int createPurchase'), service.indexOf('    private String generatePurchaseNo'))
  assert.match(createBlock, /item\.setDeliveredQuantity\(BigDecimal\.ZERO\)/)
  assert.match(createBlock, /item\.setDeliveredSaleQuantity\(BigDecimal\.ZERO\)/)
  assert.match(createBlock, /item\.setDeliveredGiftQuantity\(BigDecimal\.ZERO\)/)
})

test('opening a new purchase resets the previous draft', () => {
  assert.match(page, /function openCreate\(\)\s*\{\s*resetCreateForm\(\);?\s*createOpen\.value = true\s*\}/)
  assert.match(page, /function resetCreateForm\(\)/)
})

test('purchase pricing is resolved from the scoped product and policy package', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  assert.match(service, /selectProductSalePriceById\(\s*order\.getTenantId\(\),\s*order\.getDeptId\(\),\s*item\.getProductId\(\)\)/)
  assert.match(service, /item\.setItemAmount\(matched\.getPackagePrice\(\)\.setScale\(2, RoundingMode\.HALF_UP\)\)/)
  assert.match(mapper, /id="selectProductSalePriceById"/)
})

test('purchase statuses and customer types render labels instead of storage codes', () => {
  assert.match(page, /function customerTypeLabel\(/)
  assert.match(page, /function paymentStatusLabel\(/)
  assert.match(page, /function deliveryStatusLabel\(/)
  assert.match(page, /function orderStatusLabel\(/)
  assert.match(page, /customerTypeLabel\(scope\.row\.customerType\)/)
  assert.match(page, /orderStatusLabel\(detail\.orderStatus\)/)
})

test('payment dialog defaults to the purchase order remaining receivable', () => {
  assert.match(page, /remainingAmount/)
  assert.match(page, /const remainingAmount = Number\(row\.receivableAmount \|\| 0\)/)
  assert.match(page, /paymentAmount: remainingAmount/)
  assert.match(page, /剩余应收金额/)
})

test('payment and delivery services generate missing business numbers before insert', () => {
  const paymentService = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchasePaymentServiceImpl.java', import.meta.url), 'utf8')
  const deliveryService = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseDeliveryServiceImpl.java', import.meta.url), 'utf8')
  assert.match(paymentService, /payment\.setPaymentNo\(generatePaymentNo\(\)\)/)
  assert.match(paymentService, /payment\.setPaymentDate\(new Date\(\)\)/)
  assert.match(deliveryService, /delivery\.setDeliveryNo\(generateDeliveryNo\(\)\)/)
  assert.match(deliveryService, /delivery\.setDeliveryDate\(new Date\(\)\)/)
})

test('purchase detail exposes payment and delivery records', () => {
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  assert.match(mapper, /collection property="payments"/)
  assert.match(mapper, /collection property="deliveries"/)
  assert.match(mapper, /id="selectPaymentsByPurchaseId"/)
  assert.match(mapper, /id="selectDeliveriesByPurchaseId"/)
  assert.match(page, /收款记录/)
  assert.match(page, /领取记录/)
})

test('cancelled purchase orders are excluded from list and statistics', () => {
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  const listBlock = mapper.slice(mapper.indexOf('id="selectPurchaseList"'), mapper.indexOf('id="selectPurchaseStatistics"'))
  const statisticsBlock = mapper.slice(mapper.indexOf('id="selectPurchaseStatistics"'), mapper.indexOf('id="selectItemsByPurchaseId"'))
  assert.match(listBlock, /order_status &lt;&gt; '4'/)
  assert.match(statisticsBlock, /o\.order_status &lt;&gt; '4'/)
})

test('purchase list filters by an inclusive creation date range across list, statistics and export', () => {
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  assert.match(page, /购买日期/)
  assert.match(page, /type="daterange"/)
  assert.match(page, /beginTime/)
  assert.match(page, /endTime/)
  assert.match(mapper, /o\.purchase_date &gt;= #\{beginTime\}/)
  assert.match(mapper, /o\.purchase_date &lt; DATE_ADD\(#\{endTime\}, INTERVAL 1 DAY\)/)
  assert.match(mapper.slice(mapper.indexOf('id="selectPurchaseStatistics"'), mapper.indexOf('id="selectItemsByPurchaseId"')), /o\.purchase_date &gt;= #\{beginTime\}/)
})

test('purchase date is separate from data entry time and defaults in the create form', () => {
  const domain = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/domain/MemPurchaseOrder.java', import.meta.url), 'utf8')
  const mapper = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/resources/mapper/member/MemPurchaseMapper.xml', import.meta.url), 'utf8')
  assert.match(page, /label="购买日期"/)
  assert.match(page, /createForm\.purchaseDate/)
  assert.match(page, /purchaseDate:/)
  assert.match(domain, /purchaseDate/)
  assert.match(mapper, /purchase_date/)
  assert.match(mapper, /#\{purchaseDate\}/)
})

test('delivery dialog defaults to the selected item remaining sale and gift quantities', () => {
  assert.match(page, /function remainingDeliveryQuantity\(/)
  assert.match(page, /saleDeliveryQuantity: remainingDeliveryQuantity\(item, 'sale'\)/)
  assert.match(page, /giftDeliveryQuantity: remainingDeliveryQuantity\(item, 'gift'\)/)
  assert.match(page, /@change="handleDeliveryItemChange"/)
})

test('purchase detail renders payment method labels instead of storage codes', () => {
  assert.match(page, /function paymentMethodLabel\(/)
  assert.match(page, /paymentMethodLabel\(scope\.row\.paymentMethod\)/)
})

test('purchase amounts use the shared yuan formatter', () => {
  assert.match(page, /import \{ money \} from '@\/utils\/money'/)
  assert.match(page, /money\(scope\.row\.totalAmount\)/)
  assert.match(page, /money\(detail\.totalAmount\)/)
  assert.match(page, /money\(scope\.row\.paymentAmount\)/)
})

test('direct purchase allows manual positive unit price when product sale price is absent', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  assert.match(service, /item\.getPolicyId\(\) == null \? item\.getUnitPrice\(\) : productSalePrice/)
  assert.match(service, /getUnitPrice\(\)\.signum\(\) <= 0/)
  assert.match(page, /:disabled="!!createForm\.item\.policyId"/)
  assert.match(page, /单价（可修改）/)
})

test('purchase list exposes separate edit permission from query permission', () => {
  const menu = fs.readFileSync(new URL('../sql/member_purchase_menu.sql', import.meta.url), 'utf8')
  assert.match(menu, /member:purchase:edit/)
})

test('purchase operation buttons use their own permissions without requiring edit permission', () => {
  assert.doesNotMatch(page, /hasPermiAnd\(\['member:purchase:edit', 'member:purchase:(payment|delivery|bind|cancel)'\]\)/)
  assert.match(page, /v-hasPermi="\['member:purchase:payment'\]"/)
  assert.match(page, /v-hasPermi="\['member:purchase:delivery'\]"/)
  assert.match(page, /v-hasPermi="\['member:purchase:cancel'\]"/)
})

test('cancelled delivered purchase explains the business restriction in Chinese', () => {
  const service = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberPurchaseServiceImpl.java', import.meta.url), 'utf8')
  assert.match(service, /已全部领取的购买单不能作废/)
  assert.doesNotMatch(service, /delivered purchase cannot be cancelled/)
})

test('purchase list exposes a separately authorized edit entry point', () => {
  assert.match(page, /编辑购买单/)
  assert.match(page, /v-hasPermi="\['member:purchase:edit'\]"/)
  const api = fs.readFileSync(new URL('../junsong-ui-v3/src/api/member/purchase.ts', import.meta.url), 'utf8')
  const controller = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchaseController.java', import.meta.url), 'utf8')
  assert.match(api, /updateMemberPurchase/)
  assert.match(controller, /@PutMapping\("\/{purchaseId}"\)/)
  assert.match(controller, /member:purchase:edit/)
})

test('purchase edit uses the full purchase detail and exposes editable payment and delivery records', () => {
  assert.match(page, /detailEditMode/)
  assert.match(page, /detailEditMode \? '编辑购买单' : '购买单详情'/)
  assert.match(page, /编辑收款记录/)
  assert.match(page, /编辑领取记录/)
  const api = fs.readFileSync(new URL('../junsong-ui-v3/src/api/member/purchase.ts', import.meta.url), 'utf8')
  assert.match(api, /updateMemberPurchasePayment/)
  assert.match(api, /updateMemberPurchaseDelivery/)
  const paymentController = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchasePaymentController.java', import.meta.url), 'utf8')
  const deliveryController = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchaseDeliveryController.java', import.meta.url), 'utf8')
  assert.match(paymentController, /@PutMapping\("\/{purchaseId}\/payment\/{paymentId}"\)/)
  assert.match(paymentController, /member:purchase:edit/)
  assert.match(deliveryController, /@PutMapping\("\/{purchaseId}\/delivery\/{deliveryId}"\)/)
  assert.match(deliveryController, /member:purchase:edit/)
})

test('purchase list provides pagination, quantities, summary and xlsx export', () => {
  assert.match(page, /pagination/)
  assert.match(page, /purchaseQuantity/)
  assert.match(page, /giftQuantity/)
  assert.match(page, /汇总|summary/)
  assert.match(page, /导出 XLSX|导出Excel|导出/)
  const controller = fs.readFileSync(new URL('../junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemPurchaseController.java', import.meta.url), 'utf8')
  assert.match(controller, /startPage\(\)/)
  assert.match(controller, /@PostMapping\("\/export"\)/)
  assert.match(controller, /member:purchase:export/)
})

test('purchase list uses compact Chinese operation labels and designed summary cards', () => {
  assert.match(page, /class="purchase-summary-card purchase-summary-card--orders"/)
  assert.match(page, /class="purchase-summary-card purchase-summary-card--receivable"/)
  assert.match(page, /class="purchase-summary-card purchase-summary-card--paid"/)
  assert.match(page, /class="purchase-summary-card purchase-summary-card--debt"/)
  assert.match(page, /width="270"/)
  assert.match(page, />查看</)
  assert.match(page, />编辑</)
  assert.match(page, />收款</)
  assert.match(page, />领取</)
  assert.match(page, />作废</)
  assert.doesNotMatch(page, />查看详情</)
  assert.doesNotMatch(page, />编辑购买单</)
  assert.doesNotMatch(page, />登记收款</)
  assert.doesNotMatch(page, />登记领取</)
})
