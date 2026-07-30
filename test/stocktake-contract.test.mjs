import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(new URL('../', import.meta.url).pathname)
const read = (rel) => fs.readFileSync(path.join(root, rel), 'utf8')

const apiFile = read('src/api/stocktake.js')
const legacyApiFile = apiFile // 同一文件包含 legacy 和新工作流 API（macOS 大小写不敏感合并）
const indexPage = read('src/pages/stocktake/index.vue')
const detailPage = read('src/pages/stocktake/detail.vue')
const pagesJson = read('src/pages.json')

test('Task10: 小程序 stocktake.js API 定义新工作流方法（/stocktakes 端点）', () => {
  assert.match(apiFile, /\/finance\/stocktakes/)
  assert.match(apiFile, /listStocktakes/)
  assert.match(apiFile, /getStocktakeDetail/)
  assert.match(apiFile, /createStocktake/)
  assert.match(apiFile, /assignCounter/)
  assert.match(apiFile, /startStocktake/)
  assert.match(apiFile, /countItem/)
  assert.match(apiFile, /submitStocktake/)
  assert.match(apiFile, /recountItem/)
  assert.match(apiFile, /approveStocktake/)
  assert.match(apiFile, /postStocktake/)
  assert.match(apiFile, /cancelStocktake/)
  assert.match(apiFile, /reverseStocktake/)
  assert.match(apiFile, /buildIdempotencyKey/)
})

test('Task10: 小程序 API 使用 finance:stocktake:* 权限码注释（与 PC 共享后端权限）', () => {
  assert.match(apiFile, /finance:stocktake:\*/)
  assert.match(apiFile, /盲盘保护/)
  assert.match(apiFile, /count_idempotency_key/)
})

test('Task10: 小程序 API 幂等键格式 ${stocktakeId}-${productId}-${action}-${version}', () => {
  assert.match(apiFile, /\$\{stocktakeId\}-\$\{productId\}-\$\{action\}-\$\{version\}/)
})

test('Task10: 小程序列表页 pages/stocktake/index.vue 存在并调用 listStocktakes', () => {
  assert.match(indexPage, /import \{ listStocktakes \} from '@\/api\/stocktake\.js'/)
  assert.match(indexPage, /finance:stocktake:list/)
  assert.match(indexPage, /finance:stocktake:query/)
  assert.match(indexPage, /hasExactPermission/)
  assert.match(indexPage, /stocktakeId/)
  assert.match(indexPage, /activeStatus/)
  assert.match(indexPage, /loadMore/)
})

test('Task10: 小程序详情页 pages/stocktake/detail.vue 存在并实现盲盘录入', () => {
  assert.match(detailPage, /import \{[\s\S]*getStocktakeDetail[\s\S]*\} from '@\/api\/stocktake\.js'/)
  assert.match(detailPage, /hideExpected/)
  assert.match(detailPage, /buildIdempotencyKey/)
  assert.match(detailPage, /finance:stocktake:count/)
  assert.match(detailPage, /finance:stocktake:submit/)
  assert.match(detailPage, /finance:stocktake:post/)
  assert.match(detailPage, /finance:stocktake:reverse/)
  assert.match(detailPage, /finance:stocktake:recount/)
  assert.match(detailPage, /reverseStocktake/)
  assert.match(detailPage, /盲盘模式/)
  assert.match(detailPage, /idempotencyKey/)
})

test('Task10: 小程序详情页冲销必填原因和幂等键（fail-closed）', () => {
  assert.match(detailPage, /请输入冲销原因/)
  assert.match(detailPage, /幂等键不能为空/)
  assert.match(detailPage, /dialog-warning/)
})

test('Task10: 小程序详情页状态流转覆盖 DRAFT/COUNTING/SUBMITTED/RECOUNTING/APPROVED/POSTED/REVERSED/CANCELLED', () => {
  ;['DRAFT', 'COUNTING', 'SUBMITTED', 'RECOUNTING', 'APPROVED', 'POSTED', 'REVERSED', 'CANCELLED'].forEach((s) => {
    assert.match(detailPage, new RegExp(s), `详情页应处理状态 ${s}`)
  })
})

test('Task10: 小程序 pages.json 注册 stocktake 列表页和详情页', () => {
  assert.match(pagesJson, /pages\/stocktake\/index/)
  assert.match(pagesJson, /pages\/stocktake\/detail/)
  assert.match(pagesJson, /库存盘点/)
  assert.match(pagesJson, /盘点详情/)
})

test('Task10: 小程序 stocktake.js 包含 legacy API（Task 8 收口）但新页面不导入 /stockTake 端点', () => {
  // legacy API 已合并到 stocktake.js（macOS 大小写不敏感）
  assert.match(legacyApiFile, /\/stockTake/)
  // 新页面不应导入旧 camelCase 文件名
  assert.doesNotMatch(indexPage, /from '@\/api\/stockTake\.js'/)
  assert.doesNotMatch(detailPage, /from '@\/api\/stockTake\.js'/)
})

test('Task10: 小程序详情页复盘人须与盘点人不同（由后端校验，前端不传 counterUserId）', () => {
  // 前端 recountItem 只传 recountQuantity/reason/idempotencyKey/version，不传 counterUserId
  assert.match(detailPage, /recountQuantity/)
  assert.doesNotMatch(detailPage, /counterUserId.*recount/)
})

test('Task10: 小程序复盘请求体必须包含 reasonCode（复盘原因代码进入 HTTP 请求体）', () => {
  // saveRecount 函数中 recountItem 调用必须传入 reasonCode
  // 找到 recountItem 调用位置，检查其后 300 字符内是否包含 reasonCode
  const idx = detailPage.indexOf('recountItem(')
  assert.ok(idx >= 0, 'recountItem 调用必须存在')
  const snippet = detailPage.substring(idx, idx + 300)
  assert.match(snippet, /reasonCode/, 'recountItem 调用必须传入 reasonCode（复盘原因代码必须进入 HTTP 请求体）')
})
