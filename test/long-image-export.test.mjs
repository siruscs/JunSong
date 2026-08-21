import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('long image exporter draws the page card layout on a native canvas', () => {
  const source = read('src/utils/longImageExport.js')
  assert.match(source, /exportLongImage/)
  assert.match(source, /canvasToTempFilePath/)
  assert.match(source, /saveImageToPhotosAlbum/)
  assert.match(source, /writePhotosAlbum/)
  assert.match(source, /authorize/)
  assert.match(source, /setTimeout\(resolve,/)
  assert.match(source, /getContext\('2d'\)/)
  assert.match(source, /node\.getContext/)
  assert.match(source, /summary\.expenseAmount/)
  assert.match(source, /summary\.explanation/)
  assert.match(source, /fillRect\(0, 0, CANVAS_WIDTH, heroHeight\)/)
})

test('2d canvas export exports the full canvas 1:1 without logical-unit source rect', () => {
  const source = read('src/components/WxmlToCanvas/index.js')
  const twoDStart = source.indexOf('// 2d 主路径：整画布导出')
  const twoDExport = source.slice(twoDStart, source.indexOf('// legacy 降级路径：x/y/w/h 为逻辑'))

  // 2d 为主路径：buffer 与 scale 全显式控制，消除 legacy canvas-id 的隐式 DPR 语义
  // （该隐式约定在长短内容下行为不一致，曾导致短内容导出“放大且不完整”）。
  assert.ok(twoDStart > -1, '2d 导出分支必须存在')
  assert.match(source, /use2dCanvas: true/, '2d canvas 必须是默认主路径')
  // 2d 导出传 canvas node（无 canvasId、无 component scope）
  assert.match(twoDExport, /canvas: node/)
  // 关键：2d 的导出坐标系是物理像素（node.width/height），不得传逻辑尺寸 x/y/w/h ——
  // 传逻辑 srcW/srcH 只会截取左上 1/exportDpr 区域，导出图表现为放大且不完整。
  assert.doesNotMatch(twoDExport, /width:\s*srcW/)
  assert.doesNotMatch(twoDExport, /x:\s*\d/)
  assert.doesNotMatch(twoDExport, /canvasId/)
  // destWidth/destHeight = buffer 尺寸，整画布 1:1 输出
  assert.match(twoDExport, /destWidth: node\.width/)
  assert.match(twoDExport, /destHeight: node\.height/)
})

test('2d render explicitly sizes the backing buffer and applies scale after layout settles', () => {
  const source = read('src/components/WxmlToCanvas/index.js')
  // renderToCanvas 2d 分支：先等布局落地（fields 查询布局宽 == 逻辑宽），再显式设
  // node.width/height = 逻辑 × exportDpr（赋值会重置画布状态），随后 getContext+scale。
  assert.match(source, /_query2dNode\(\)/)
  assert.match(source, /Math\.round\(Number\(item\.width\)\) === layoutWidth/)
  assert.match(source, /canvas\.width = Math\.round\(layoutWidth \* exportDpr\)/)
  assert.match(source, /canvas\.height = Math\.round\(layoutHeight \* exportDpr\)/)
  assert.match(source, /ctx = host\.ctx = canvas\.getContext\('2d'\)/)
  assert.match(source, /ctx\.scale\(exportDpr, exportDpr\)/)
  // 2d 节点不可用时必须自动降级 legacy（use2dCanvas: false）
  assert.match(source, /host\.setData\(\{use2dCanvas: false\}\)/)
  // 降级后 legacy 每次渲染重建 context 并 scale
  assert.match(source, /if\s*\(scale\s*>\s*1\)\s*ctx\.scale\(scale,\s*scale\)/)
})

test('legacy canvas keeps backing dimensions aligned with the export layout', () => {
  const template = read('src/components/WxmlToCanvas/index.wxml')
  // backing buffer 必须绑定 bufferWidth/bufferHeight（= width × dpr / height × dpr），
  // 与 ctx.scale(dpr, dpr) 配合让逻辑坐标绘制刚好填满 buffer，避免溢出导致文字变形。
  assert.match(template, /<canvas[^>]+width="\{\{bufferWidth\}\}"[^>]+height="\{\{bufferHeight\}\}"/)
  // CSS 显示尺寸仍保持逻辑尺寸 width × height px。
  assert.match(template, /style="width: \{\{width\}\}px; height: \{\{height\}\}px;"/)
})

test('legacy fallback export uses full canvas layout dimensions (srcW/srcH) to avoid content clipping', () => {
  const source = read('src/components/WxmlToCanvas/index.js')
  const legacyStart = source.indexOf('// legacy 降级路径：x/y/w/h 为逻辑')
  assert.ok(legacyStart > -1, 'legacy 降级导出分支必须存在')
  const legacyExport = source.slice(legacyStart)
  // 关键修复：导出源区域宽高必须使用 renderToCanvas 记录的真实 canvas 逻辑尺寸
  // (_layoutWidth / _layoutHeight → srcW / srcH)，而不是 css-layout 引擎计算的
  // boundary.width/height（flex + marginTop 场景下会低估，导致右侧/底部被裁切）。
  assert.match(source, /_layoutWidth:\s*layoutWidth/)
  assert.match(source, /_layoutHeight:\s*layoutHeight/)
  assert.match(source, /const srcW = Number\(boundary\._layoutWidth\)/)
  assert.match(source, /const srcH = Number\(boundary\._layoutHeight\)/)
  assert.match(legacyExport, /width:\s*srcW/)
  assert.match(legacyExport, /height:\s*srcH/)
  // 横纵使用同一 exportDpr 比例，destWidth/destHeight = round(srcW * exportDpr), round(srcH * exportDpr)
  assert.match(source, /const exportDpr = Number\(host\.exportDpr\) \|\| Number\(host\.dpr\) \|\| 1/)
  assert.match(legacyExport, /destWidth:\s*Math\.round\(srcW\s*\*\s*exportDpr\)/)
  assert.match(legacyExport, /destHeight:\s*Math\.round\(srcH\s*\*\s*exportDpr\)/)
  // 禁止再回退到超高的固定画布（6000×dpr 会超过设备 Canvas 上限导致内容被截断）
  assert.doesNotMatch(source, /\|\| 6000/)
  // canvas-id（legacy）路径必须传入真身组件实例（host）作为 canvas 查找范围。
  // 传包装 Proxy 会找不到组件内 canvas 返回空白小画布。
  assert.match(legacyExport, /\},\s*host\)/)
  // methods 块内禁止再出现裸 this.setData（包装 Proxy 上会崩 __treeManager__）；
  // attached 里的 this.setData 合法（微信运行时调用，this 必为真身），故只查 methods。
  const methodsBlock = source.slice(source.indexOf('methods: {'))
  assert.doesNotMatch(methodsBlock, /this\.setData\(/)
  assert.doesNotMatch(methodsBlock, /wx\.createCanvasContext\(canvasId,\s*this\)/)
})

test('export hero cards use the same gradient as the page CSS (not solid blue)', () => {
  const component = read('src/components/WxmlToCanvas/index.js')
  const verification = read('src/pages/verification-record/detail.vue')
  const sale = read('src/pages/detail/index.vue')

  // wxml-to-canvas 只支持纯色 backgroundColor；组件扩展 drawView 支持
  // backgroundGradient（ctx.fill() 直接接受 CanvasGradient），135deg 对角渐变。
  assert.match(component, /backgroundGradient/)
  assert.match(component, /createLinearGradient\(x, y, x \+ w, y \+ h\)/)
  assert.match(component, /addColorStop/)

  // 核销详情页 hero 渐变 = 页面 .summary-hero 的
  // linear-gradient(135deg, #123F73 0%, #087CF0 72%, #5AA9E8 100%)
  const vHero = verification.match(/hero: \{[^\n]+/)
  assert.ok(vHero, '核销导出样式必须包含 hero 定义')
  assert.match(vHero[0], /backgroundGradient: \{ colors: \[\{ offset: 0, color: '#123F73' \}, \{ offset: 0\.72, color: '#087CF0' \}, \{ offset: 1, color: '#5AA9E8' \}\] \}/)
  assert.doesNotMatch(vHero[0], /backgroundColor: '#123F73'/, 'hero 不得再使用纯蓝背景')

  // 销售详情页 hero 渐变 = 页面 .hero-bg 的
  // linear-gradient(135deg, #087CF0, #5AA9E8, #A8C7E5)（无 stops 均分 0/0.5/1）
  const sHero = sale.match(/hero: \{[^\n]+/)
  assert.ok(sHero, '销售导出样式必须包含 hero 定义')
  assert.match(sHero[0], /backgroundGradient: \{ colors: \[\{ offset: 0, color: '#087CF0' \}, \{ offset: 0\.5, color: '#5AA9E8' \}, \{ offset: 1, color: '#A8C7E5' \}\] \}/)
  assert.doesNotMatch(sHero[0], /backgroundColor: '#123F73'/, 'hero 不得再使用纯蓝背景')
})

test('component methods must resolve the native host before any setData (wrapper proxy crashes)', () => {
  const source = read('src/components/WxmlToCanvas/index.js')
  // 真机故障：页面经 $refs/selectComponent 拿到 uni-app 包装 Proxy，在其上调用
  // 组件方法时 this.setData 报 __treeManager__ undefined（基础库 3.16.1 实测）。
  // 且包装 Proxy 会把属性读取转发到真身 —— 标记位（this.__isNativeHost）经 Proxy
  // 读出来也是 true，会误判后照样崩在 Proxy.setData 上（真机二次复现）。
  // 唯一可靠判据是恒等比较（=== 无法被 Proxy 转发）。
  assert.match(source, /let nativeHost = null/)
  assert.match(source, /nativeHost = this/)
  assert.doesNotMatch(source, /__isNativeHost/)
  assert.match(source, /detached\(\)\s*\{[\s\S]*?nativeHost = null/)
  assert.match(source, /_resolveHost\(\)\s*\{/)
  assert.match(source, /if \(nativeHost && this === nativeHost\) return nativeHost/)
  assert.match(source, /return nativeHost \|\| this/)
  // 三个公开方法都必须先解析 host
  assert.match(source, /_applySize\(width, height\)\s*\{[\s\S]*?const host = this\._resolveHost\(\)/)
  assert.match(source, /renderToCanvas\(args\)\s*\{[\s\S]*?const host = this\._resolveHost\(\)/)
  assert.match(source, /canvasToTempFilePath\(args = \{\}\)\s*\{[\s\S]*?const host = this\._resolveHost\(\)/)
  // 内部实例字段一律通过 host 读写
  assert.match(source, /host\.setData\(\{/)
  assert.match(source, /host\.ctx = wx\.createCanvasContext\(canvasId, host\)/)
  assert.match(source, /host\.boundary = \{/)
})

test('renderToCanvas resizes the canvas to the real content height before drawing', () => {
  const source = read('src/components/WxmlToCanvas/index.js')
  // 关键修复：画布高度必须等于实际内容高度（style.page.height）。
  // 固定 6000 逻辑高 × dpr = 18000 物理像素，超过设备 Canvas 最大边长（4096/8192），
  // native 画布被静默截断，超出部分内容（明细列表）根本画不上去。
  // 尺寸经 renderToCanvas({width, height}) 参数传入，组件在原生实例内部 setData
  // （页面 Vue prop 会被 uni-app 编译成 u-p 透传，原生 Component() 收不到，
  // 画布停留在默认 400×300，导出只剩左上小角 —— 真机已验证的故障模式）。
  assert.match(source, /MAX_BUFFER_SIDE = 8192/)
  // _computeExportSize 中按 exportDpr 上限自动降级像素密度
  assert.match(source, /exportDpr = Math\.max\(1,\s*Math\.floor\(MAX_BUFFER_SIDE\s*\/\s*height\)\)/)
  assert.match(source, /const bufferWidth = Math\.round\(width \* exportDpr\)/)
  assert.match(source, /const bufferHeight = Math\.round\(height \* exportDpr\)/)
  // renderToCanvas 里先 _applySize（原生 setData 下发逻辑尺寸+buffer）再绘制
  assert.match(source, /if \(width\s*&&\s*height\)\s*\{[\s\S]*?_applySize\(width,\s*height\)/)
  assert.match(source, /width:\s*targetWidth,\s*height:\s*targetHeight,\s*bufferWidth,\s*bufferHeight/)
  assert.match(source, /host\.ctx = wx\.createCanvasContext\(canvasId, host\)/)
  // legacy canvas draw 回调后再等一帧落盘，避免 canvasToTempFilePath 读到空白
  assert.match(source, /await host\.canvasDraw\(ctx\)[\s\S]{0,500}setTimeout\(\s*r\s*,\s*16\s*\)/)
})

test('exportLongImage passes the style page size to renderToCanvas', () => {
  const source = read('src/utils/longImageExport.js')
  // 从 style.page 读取真实内容尺寸，让画布按内容高度自适应
  assert.match(source, /const canvasWidth = Number\(style\?\.page\?\.width\) \|\| 375/)
  assert.match(source, /const canvasHeight = Number\(style\?\.page\?\.height\) \|\| 0/)
  assert.match(source, /component\.renderToCanvas\(\{ wxml, style, width: canvasWidth, height: canvasHeight \}\)/)
})

test('export pages must not pass canvas size via Vue props (u-p never reaches native component)', () => {
  const verification = read('src/pages/verification-record/detail.vue')
  const sale = read('src/pages/detail/index.vue')
  // 固定 6000 会超过设备 Canvas 上限（6000×dpr≈18000 物理像素），内容被截断
  assert.doesNotMatch(verification, /height="6000"/)
  assert.doesNotMatch(sale, /height="6000"/)
  assert.doesNotMatch(verification, /\.export-canvas\s*\{[^}]*height:\s*6000px/)
  assert.doesNotMatch(sale, /\.export-canvas\s*\{[^}]*height:\s*6000px/)
  // 关键：uni-app Vue3 会把组件 props 编译成 u-p="{{F}}" 序列化透传，原生 Component()
  // 不认识 u-p，:width/:height 绑定永远不会生效（observers 也不触发）。因此页面
  // 模板不得绑定尺寸 props，尺寸统一走 renderToCanvas({width, height}) 参数。
  assert.match(verification, /<wxml-to-canvas class="export-canvas"><\/wxml-to-canvas>/)
  assert.match(sale, /<wxml-to-canvas class="export-canvas"><\/wxml-to-canvas>/)
  assert.doesNotMatch(verification, /:width="exportCanvasWidth"/)
  assert.doesNotMatch(sale, /:width="exportCanvasWidth"/)
  assert.doesNotMatch(verification, /exportCanvasWidth:\s*\d/)
  assert.doesNotMatch(sale, /exportCanvasWidth:\s*\d/)
  // 两个详情页的 exportStyle 都必须声明真实内容高度（pageHeight），供画布自适应
  assert.match(verification, /page:\s*\{\s*width:\s*375,\s*height:\s*pageHeight/)
  assert.match(sale, /page:\s*\{\s*width:\s*375,\s*height:\s*pageHeight/)
})

test('export pages acquire the raw native canvas instance before the $refs proxy', () => {
  const verification = read('src/pages/verification-record/detail.vue')
  const sale = read('src/pages/detail/index.vue')
  // 关键修复：uni-app Vue3 的 $refs 指向包装代理，代理上 this.setData 报
  // __treeManager__ undefined（真机复现）。必须优先用原生页面实例的
  // selectComponent('.export-canvas') 取原生 Component 实例；$refs 只作兜底。
  for (const source of [verification, sale]) {
    const match = source.match(/getExportCanvasComponent\(\)\s*\{[\s\S]*?\n    \}/)
    assert.ok(match, '必须存在 getExportCanvasComponent 方法')
    const body = match[0]
    const nativeIdx = body.indexOf("page?.selectComponent?.('.export-canvas')")
    const refIdx = body.indexOf('this.$refs?.exportCanvas')
    assert.ok(nativeIdx > -1, '必须通过原生 selectComponent 获取组件实例')
    assert.ok(refIdx > nativeIdx, '原生 selectComponent 必须优先于 $refs 兜底')
  }
})

test('verification-record exportStyle uses marginTop for first-child spacing and numeric margins', () => {
  const source = read('src/pages/verification-record/detail.vue')
  // heroTitle 作为 hero 第一个子元素，使用 marginTop: 16 创建顶部间距
  assert.match(source, /heroTitle:[^}]*marginTop:\s*16/)
  // heroTitle 使用数字 marginLeft: 16（不是字符串 '16rpx'）
  assert.match(source, /heroTitle:[^}]*marginLeft:\s*16\b/)
  // hero 高度为 192
  assert.match(source, /heroHeight = 192/)
  // heroSub 使用数字 marginTop: 12（标题与副标题之间的间距）
  assert.match(source, /heroSub:[^}]*marginTop:\s*12/)
  // summaryExpense 使用 marginTop: 6
  assert.match(source, /summaryExpense:[^}]*marginTop:\s*6/)
  // infoCard 使用 marginLeft: 12 创建水平间距
  assert.match(source, /infoCard:[^}]*marginLeft:\s*12/)
  // infoHeader 作为 infoCard 第一个子元素，使用 marginTop: 14 创建顶部间距
  assert.match(source, /infoHeader:[^}]*marginTop:\s*14/)
  // detailTop 作为 detailCard 第一个子元素，使用 marginTop: 14 创建顶部间距
  assert.match(source, /detailTop:[^}]*marginTop:\s*14/)
  // expenseSection 使用 marginLeft: 12
  assert.match(source, /expenseSection:[^}]*marginLeft:\s*12/)
  // summaryAdvance 使用 marginTop: 4
  assert.match(source, /summaryAdvance:[^}]*marginTop:\s*4/)
  // explanation 使用 marginTop: 4
  assert.match(source, /explanation:[^}]*marginTop:\s*4/)
  // 不再使用 spacer 元素（heroSpacer、detailSpacer）
  assert.doesNotMatch(source, /heroSpacer:/)
  assert.doesNotMatch(source, /detailSpacer:/)
})

test('sale detail exportStyle adds marginTop/marginLeft to compensate for flex+padding layout defects', () => {
  const source = read('src/pages/detail/index.vue')
  // heroTitle 必须有 marginTop+marginLeft 补偿
  assert.match(source, /heroTitle:[^}]*marginTop:\s*16/)
  assert.match(source, /heroTitle:[^}]*marginLeft:\s*16/)
  // hero 高度从 140 增加到 156
  assert.match(source, /hero:[^}]*height:\s*156/)
  // detailTop 必须有 marginTop+marginLeft 补偿
  assert.match(source, /detailTop:[^}]*marginTop:\s*12/)
  assert.match(source, /detailTop:[^}]*marginLeft:\s*12/)
  // detailCard 高度从 60 增加到 72
  assert.match(source, /detailCard:[^}]*height:\s*72/)
})
