import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

// 复现"导出长图文字严重变形、内容偏移、内容只出现在左侧三分之一"的根因：
// canvas-id 路径下，组件 attached() 调用了 ctx.scale(dpr, dpr)，使逻辑坐标 (X, Y)
// 写入 backing buffer 像素 (X·dpr, Y·dpr)。但 canvas 的 width/height 属性仍是逻辑
// 尺寸（如 375×6000），buffer 仅能容纳左上角 1/dpr × 1/dpr 区域的绘制，其余被裁掉。
// canvasToTempFilePath 再把残缺的 1/dpr 区域拉伸到完整输出图，导致文字变形、内容偏移。
// 修复要求：backing buffer 必须等于 CSS 逻辑尺寸 × dpr，让 ctx.scale 后的绘制刚好填满
// buffer，导出时横纵使用同一 dpr 缩放比例，文字不被拉伸/压扁/裁剪。

test('legacy canvas backing buffer must be sized to CSS logical size × dpr to avoid drawing overflow', () => {
  const source = read('src/components/WxmlToCanvas/index.js')

  // 组件必须暴露 bufferWidth/bufferHeight 两个数据字段，用于绑定 canvas 的 width/height 属性。
  assert.match(source, /bufferWidth/, 'WxmlToCanvas 必须暴露 bufferWidth 数据字段')
  assert.match(source, /bufferHeight/, 'WxmlToCanvas 必须暴露 bufferHeight 数据字段')

  // bufferWidth/bufferHeight 由逻辑尺寸 × exportDpr 计算（同一比例，横纵一致）。
  // exportDpr 默认等于系统 dpr，超长内容会降级为 floor(8192/height)，确保 buffer 不超过
  // 设备 Canvas 最大边长（4096/8192），避开内容被 native 画布静默截断的问题。
  assert.match(
    source,
    /bufferWidth\s*=\s*Math\.round\(\s*width\s*\*\s*exportDpr\s*\)/,
    'bufferWidth 必须等于 width × exportDpr（Math.round）'
  )
  assert.match(
    source,
    /bufferHeight\s*=\s*Math\.round\(\s*height\s*\*\s*exportDpr\s*\)/,
    'bufferHeight 必须等于 height × exportDpr（Math.round）'
  )

  // dpr 必须来源于系统信息；observers 或 attached 中按 exportDpr 对 canvas context scale。
  assert.match(source, /pixelRatio/, 'dpr 必须从 wx.getSystemInfoSync 的 pixelRatio 读取')
  assert.match(source, /scale\(exportDpr,\s*exportDpr\)/, '必须存在 scale(exportDpr, exportDpr) 以填满 buffer')
})

test('legacy canvas wxml binds bufferWidth/bufferHeight to canvas width/height attributes while keeping CSS logical size', () => {
  const template = read('src/components/WxmlToCanvas/index.wxml')

  // canvas-id 路径的 canvas 元素：width/height 属性绑定 bufferWidth/bufferHeight（buffer 像素）。
  assert.match(
    template,
    /<canvas[^>]*canvas-id="weui-canvas"[^>]*width="\{\{bufferWidth\}\}"[^>]*height="\{\{bufferHeight\}\}"/,
    'canvas-id 路径的 canvas width/height 必须绑定 bufferWidth/bufferHeight'
  )

  // canvas 的 CSS 显示尺寸仍保持逻辑尺寸（width × height px），让 1 CSS px = dpr buffer px。
  assert.match(
    template,
    /style="width: \{\{width\}\}px; height: \{\{height\}\}px;"/,
    'canvas CSS 尺寸必须保持逻辑 width × height px'
  )
})

test('canvasToTempFilePath uses the same dpr scale for both axes to avoid text stretching', () => {
  const source = read('src/components/WxmlToCanvas/index.js')

  // canvasToTempFilePath 的 width/height 必须是逻辑尺寸（CSS px，等于绘制坐标系），
  // destWidth/destHeight 必须是 round(srcW × exportDpr) / round(srcH × exportDpr)（PNG 物理像素），
  // 横纵缩放比例一致，文字不会被拉伸或压扁。
  // exportDpr 优先于 dpr：超长内容会自动降低导出像素密度以避开设备 Canvas 上限。
  assert.match(source, /const exportDpr = Number\(host\.exportDpr\) \|\| Number\(host\.dpr\) \|\| 1/, '导出像素密度必须优先使用 exportDpr')
  // 主 copyArgs + 2d 分支都使用 Math.round(srcW/srcH * exportDpr)
  assert.match(source, /destWidth:\s*Math\.round\(srcW\s*\*\s*exportDpr\)/, 'destWidth 必须为 round(srcW × exportDpr)')
  assert.match(source, /destHeight:\s*Math\.round\(srcH\s*\*\s*exportDpr\)/, 'destHeight 必须为 round(srcH × exportDpr)')
})

test('verification record list page does not expose an export button', () => {
  const listSource = fs.readFileSync(
    new URL('../src/pages/verification-record/index.vue', import.meta.url),
    'utf8'
  )
  const detailSource = fs.readFileSync(
    new URL('../src/pages/verification-record/detail.vue', import.meta.url),
    'utf8'
  )

  // 列表页不得出现导出按钮（核销记录列表页不显示导出按钮）。
  assert.doesNotMatch(listSource, /export-button|exportImage|导出图片/, '核销记录列表页不得显示导出按钮')

  // 详情页必须保留导出按钮（核销详情页显示导出按钮）。
  assert.match(detailSource, /class="export-button"/, '核销详情页必须保留导出按钮')
  assert.match(detailSource, /导出图片/, '核销详情页必须保留导出按钮文案')
})
